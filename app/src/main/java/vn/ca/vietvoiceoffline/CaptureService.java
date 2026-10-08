package vn.ca.vietvoiceoffline;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.media.*;
import android.media.projection.*;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.nl.translate.Translator;
import com.k2fsa.sherpa.onnx.*;
import java.util.concurrent.*;
public class CaptureService extends Service{
 private volatile boolean active;private MediaProjection projection;private MediaProjection.Callback callback;private volatile AudioRecord recorder;
 private ScreenReader screenReader;private OfflineVoice voice;private Thread worker,capture,recognition;private boolean live;
 private SceneAnalyzer sceneAnalyzer;private HeavyTranslationManager heavyAi;private final SceneMemory sceneMemory=new SceneMemory();private final DialogueContext dialogue=new DialogueContext();
 static class Job{final Object data;final long time;final float[] reference;final int revision;
 Job(Object o){this(o,SystemClock.elapsedRealtime(),o instanceof float[]?(float[])o:ReferenceVoice.current(),RegionPicker.revision);}
 Job(Object o,long t,float[] r,int rev){data=o;time=t;reference=r;revision=rev;}}
 static class Recognized{final String text,engine;Recognized(String t,String e){text=t;engine=e;}}
 private final ArrayBlockingQueue<Job> jobs=new ArrayBlockingQueue<>(4),audioJobs=new ArrayBlockingQueue<>(1);private final HybridFusion fusion=new HybridFusion();private final AiRouter aiRouter=new AiRouter();private String lastResult="";private long lastResultAt,lastOutputTime;private int regionRevision;
 private WindowManager wm;private LinearLayout overlay;private TextView caption;private String language,mode;private volatile int skipped;
 private final Runnable refresh=()->{if(caption!=null)caption.setText((State.audioMood.isEmpty()?"":State.audioMood+"\n")+(State.translated.isEmpty()?State.status:State.translated));};
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent i,int flags,int id){
  if(i==null||"stop".equals(i.getAction())){State.status("Đã dừng");stopSelf();return START_NOT_STICKY;}if(active)return START_NOT_STICKY;
  active=true;live=getSharedPreferences("settings",0).getBoolean("live",true);State.running=true;State.recordId=-1;State.original="";State.translated="";State.timing="";State.engine="";State.audioMood="";State.sceneContext="";
  language=i.getStringExtra("language");if(!"ja".equals(language)&&!"zh".equals(language)&&!"ko".equals(language))language="ja";mode=i.getStringExtra("mode");if(!"audio".equals(mode)&&!"ocr".equals(mode))mode="hybrid";
  try{
   NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("capture","Dịch video",NotificationManager.IMPORTANCE_LOW));
   PendingIntent open=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
   PendingIntent stop=PendingIntent.getService(this,2,new Intent(this,CaptureService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
   Notification n=new Notification.Builder(this,"capture").setSmallIcon(R.drawable.ic_voice).setContentTitle("VietVoice · "+(mode.equals("hybrid")?"Phụ đề + âm thanh":mode.equals("ocr")?"Đọc phụ đề":"Nghe video")).setContentText("Chạm xem bản dịch · Dừng bất cứ lúc nào").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Dừng",stop).build()).build();
   boolean audioCapture=!mode.equals("ocr")||getSharedPreferences("settings",0).getBoolean("clone",true)||getSharedPreferences("settings",0).getBoolean("expression",true);
   int serviceTypes=ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION|(audioCapture?ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE:0);startForeground(7,n,serviceTypes);
   Intent token=i.getParcelableExtra("consent");if(token==null)throw new IllegalStateException("Thiếu quyền chia sẻ màn hình");
   projection=getSystemService(MediaProjectionManager.class).getMediaProjection(Activity.RESULT_OK,token);
   callback=new MediaProjection.Callback(){public void onStop(){if(active){State.status("Android đã dừng chia sẻ. Bấm Bắt đầu để cấp lại quyền.");stopSelf();}}public void onCapturedContentResize(int w,int h){if(screenReader!=null)screenReader.resize(w,h);}};
   projection.registerCallback(callback,State.main);
   voice=new OfflineVoice(this,ok->{if(!ok&&active)State.status("Giọng Việt chưa sẵn sàng: vẫn dịch chữ. Dừng rồi bấm Tải dữ liệu để kiểm tra.");});
   SharedPreferences settings=getSharedPreferences("settings",0);
   if(settings.getBoolean("sceneAi",true)&&!mode.equals("audio"))sceneAnalyzer=new SceneAnalyzer(c->{sceneMemory.add(c);State.scene(c.summary);});
   if(settings.getBoolean("heavyAi",true)&&HeavyModelStore.ready(this))heavyAi=new HeavyTranslationManager(this);
   if(i.getBooleanExtra("overlay",false)&&Settings.canDrawOverlays(this))addOverlay();State.listeners.add(refresh);
   worker=new Thread(this::work,"translation");worker.start();
  }catch(Exception|LinkageError e){State.status("Không bắt đầu được: "+State.error(e));stopSelf();}return START_NOT_STICKY;
 }
 private void enqueue(Object data){if(!active)return;offer(data instanceof float[]?audioJobs:jobs,new Job(data));}
 private void offer(ArrayBlockingQueue<Job> queue,Job job){if(!active)return;if(!queue.offer(job)){queue.poll();queue.offer(job);skipped++;}}
 private void subtitle(String text,boolean fresh){if(!active)return;if(mode.equals("hybrid")){if(fusion.observe(text,SystemClock.elapsedRealtime()))State.main.postDelayed(()->{if(active)enqueue(Boolean.TRUE);},live?450:1900);}else if(fresh&&!text.isEmpty())enqueue(text);}
 private void work(){
  TranslationJournal journal=TranslationJournal.get(this);Translator local=null;android.util.LruCache<String,String> cache=new android.util.LruCache<>(512);
  try{
   State.status("Đang kiểm tra bộ offline…");if(!ModelStore.translationPresent(language))throw new IllegalStateException("Chưa tải bộ dịch. Mở app → Chuẩn bị offline.");local=ModelStore.translator(language);
   if(!live&&heavyAi!=null){State.status("Đang nạp bộ chuyên dịch offline…");if(!heavyAi.awaitReady())throw new IllegalStateException(heavyAi.status());}
   if(!mode.equals("ocr")){recognition=new Thread(this::recognize,"recognition");recognition.start();}
   if(!active)return;
   State.main.post(()->{if(!active)return;try{
    if(!mode.equals("audio")){screenReader=new ScreenReader(this,projection,language,this::subtitle,sceneAnalyzer==null?null:(frame,time)->sceneAnalyzer.analyze(frame,time));screenReader.start();}
    if(!mode.equals("ocr")||getSharedPreferences("settings",0).getBoolean("clone",true)||getSharedPreferences("settings",0).getBoolean("expression",true)){capture=new Thread(this::audio,"playback");capture.start();}
    State.status("Đã bắt đầu. Mở video và khoanh sát vùng phụ đề.");
   }catch(Exception e){State.status("Lỗi lấy nội dung: "+State.error(e));stopSelf();}});
   while(active){
    Job job=jobs.take();if(regionRevision!=RegionPicker.revision){regionRevision=RegionPicker.revision;fusion.clear();sceneMemory.clear();dialogue.clear();State.scene("");jobs.clear();audioJobs.clear();if(voice!=null)voice.clearPending();continue;}if(RegionPicker.active){fusion.clear();jobs.clear();continue;}if(job.revision!=RegionPicker.revision||SystemClock.elapsedRealtime()-job.time>(live?(job.data instanceof Recognized?6000:3000):15000)){skipped++;continue;}long began=SystemClock.elapsedRealtime();String source,engine;
    if(job.data instanceof Recognized){Recognized r=(Recognized)job.data;source=r.text;engine=r.engine;}
    else if(job.data instanceof Boolean){HybridFusion.Choice choice=fusion.fallback(SystemClock.elapsedRealtime(),live?400:1800);source=choice.text;engine=choice.reason;}
    else{source=(String)job.data;engine="Phụ đề offline";}
    if(!active||source.trim().isEmpty())continue;String normalized=HybridFusion.normalize(source);if(normalized.equals(lastResult)&&began-lastResultAt<3500)continue;
    String vi=journal.lookup(language,source);boolean uncertain=false,fromMemory=vi!=null;
    if(vi!=null)engine+=" · Memory AI";
    else{vi=cache.get(source);if(vi==null){vi=Tasks.await(local.translate(source),30,TimeUnit.SECONDS);cache.put(source,vi);}else engine+=" · bộ đệm";
     String corrected=journal.lookup(language,source);if(corrected!=null){vi=corrected;fromMemory=true;engine+=" · Memory AI";}
     else if(getSharedPreferences("settings",0).getBoolean("qualityGuard",true)&&TranslationQuality.suspicious(language,source,vi)){
      String retry=TranslationQuality.retrySource(source);if(!retry.equals(source)&&!retry.isEmpty()){String candidate=Tasks.await(local.translate(retry),30,TimeUnit.SECONDS);if(!TranslationQuality.suspicious(language,retry,candidate)){vi=candidate;engine+=" · Translation AI kiểm tra lại";}}
      if(TranslationQuality.suspicious(language,source,vi)){uncertain=true;engine+=" · Translation AI: chưa chắc";}
     }}
    if(!live&&!fromMemory&&heavyAi!=null){
     State.status("Đang dịch theo ngữ cảnh · chờ kết quả trước khi đọc…");
     try{
      String refined=HeavyPrompt.cleanResponse(heavyAi.translateWaiting(language,source,vi,dialogue.prompt(SystemClock.elapsedRealtime())));
      if(TranslationQuality.suspicious(language,source,refined))throw new IllegalStateException("Bản dịch chưa qua kiểm tra");
      vi=refined;uncertain=false;engine+=" · HY-MT chuyên dịch offline";
     }catch(InterruptedException stop){throw stop;}catch(Exception error){engine+=" · DỊCH DỰ PHÒNG (HY-MT chưa hoàn tất)";}
    }
    aiRouter.translated(SystemClock.elapsedRealtime()-began);

    if(!active)break;
    // Discard stale results instead of reading several old lines over current video.
    long age=SystemClock.elapsedRealtime()-job.time;if(job.time<lastOutputTime||job.revision!=RegionPicker.revision||RegionPicker.active||age>(live?6000:25000)){skipped++;State.status("Máy/AI xử lý chậm: đã bỏ đoạn cũ để bắt kịp video.");continue;}
    lastOutputTime=job.time;lastResult=normalized;lastResultAt=SystemClock.elapsedRealtime();
    long record=-1;try{record=journal.add(language,source,vi,engine);}catch(Exception e){State.status("Không lưu được lịch sử: "+State.error(e));}
    State.result(source,vi,String.format(java.util.Locale.ROOT,"Sau khi thu/nhận chữ: %.1f giây · xử lý dịch %.1f giây · bỏ %d đoạn quá tải",age/1000.0,(SystemClock.elapsedRealtime()-began)/1000.0,skipped),engine,record);
    dialogue.add(job.time,source,vi);
    State.status((uncertain?"Đã giữ lại để bạn xem · ":"Đang dịch · ")+engine);
    if(voice!=null&&!uncertain&&getSharedPreferences("settings",0).getBoolean("speak",true))voice.speak(vi,job.reference,live?job.time:SystemClock.elapsedRealtime());
    if(live&&!uncertain&&!fromMemory&&record>=0&&heavyAi!=null&&getSharedPreferences("settings",0).getBoolean("heavyAi",true)){
     final long outputId=record,outputTime=job.time;final String outputSource=source,fastVi=vi,baseEngine=engine;
     long contextNow=SystemClock.elapsedRealtime();String scene=sceneMemory.prompt(contextNow),history=dialogue.prompt(contextNow);byte[] sceneImage=sceneMemory.latestImage(contextNow);
     heavyAi.refine(language,source,vi,scene,history,sceneImage,(refined,heavyMs,error)->{
      if(!active||refined==null||refined.trim().isEmpty())return;String clean=HeavyPrompt.cleanResponse(refined);
      if(clean.isEmpty()||clean.equals(fastVi)||TranslationQuality.suspicious(language,outputSource,clean))return;
      String refinedEngine=baseEngine+" · Heavy Context AI";
      try{journal.refine(outputId,clean,refinedEngine);}catch(Exception ignored){}
      dialogue.refineLatest(outputSource,clean);
      if(active&&State.recordId==outputId&&outputTime==lastOutputTime)State.refine(outputId,outputSource,clean,refinedEngine,String.format(java.util.Locale.ROOT,"Heavy AI tinh chỉnh sau %.1f giây",heavyMs/1000.0));
     });
    }
   }
  }catch(InterruptedException e){}catch(Exception|LinkageError e){if(active){State.status("Dịch dừng: "+State.error(e));State.main.post(this::stopSelf);}}
  finally{if(recognition!=null)recognition.interrupt();if(local!=null)local.close();}
 }
 private void recognize(){OfflineRecognizer asr=null;SoundEvents sounds=null;AsrSchedule schedule=new AsrSchedule();
  try{
    if(!RecognitionPack.ready(this))throw new IllegalStateException("Chưa tải bộ AI đã chọn. Bấm Tải dữ liệu & hoàn thiện app.");
    OfflineModelConfig model=new OfflineModelConfig();model.setNumThreads(2);model.setProvider("cpu");
    if(RecognitionPack.enhanced(this)){
     for(int k=0;k<2;k++)if(!ModelStore.hash(RecognitionPack.file(this,k)).equals(RecognitionPack.HASH[k]))throw new IllegalStateException("Bộ AI hỏng. Bấm tải lại.");
     OfflineSenseVoiceModelConfig sense=new OfflineSenseVoiceModelConfig();sense.setModel(RecognitionPack.file(this,0).getAbsolutePath());sense.setLanguage(language);sense.setUseInverseTextNormalization(true);model.setSenseVoice(sense);model.setTokens(RecognitionPack.file(this,1).getAbsolutePath());model.setModelType("sense_voice");
    }else{
     for(int k=0;k<3;k++)if(!ModelStore.hash(ModelStore.file(this,k)).equals(ModelStore.HASH[k]))throw new IllegalStateException("Bộ nghe hỏng. Bấm tải lại.");
     OfflineWhisperModelConfig whisper=new OfflineWhisperModelConfig();whisper.setEncoder(ModelStore.file(this,0).getAbsolutePath());whisper.setDecoder(ModelStore.file(this,1).getAbsolutePath());whisper.setLanguage(language);whisper.setTask("transcribe");model.setWhisper(whisper);model.setTokens(ModelStore.file(this,2).getAbsolutePath());model.setModelType("whisper");
    }
    OfflineRecognizerConfig config=new OfflineRecognizerConfig();config.setModelConfig(model);asr=new OfflineRecognizer(null,config);try{sounds=new SoundEvents(this);}catch(Exception|LinkageError e){State.status("Bộ lọc âm thanh không mở được, vẫn dịch lời: "+State.error(e));}

   while(active){Job job=audioJobs.take();if(RegionPicker.active||job.revision!=RegionPicker.revision||SystemClock.elapsedRealtime()-job.time>(live?2500:15000)){skipped++;continue;}
     String source,engine;float[] pcm=(float[])job.data;AudioDecision sound=new AudioDecision(false,"");if(sounds!=null)try{sound=sounds.inspect(pcm);}catch(Exception e){State.status("Bộ lọc âm thanh tạm lỗi: "+State.error(e));}String emotion="";State.audioMood=sound.label.isEmpty()?"":"Có thể: "+sound.label;State.changed();
     if(sound.nonverbal){source="";ReferenceVoice.latest=null;State.status("Nhận thấy "+sound.label+" · giữ tiếng gốc, không dịch thành lời.");}
     else {
      boolean subtitleFirst=mode.equals("hybrid")&&getSharedPreferences("settings",0).getBoolean("subtitleFirst",true);boolean covered=fusion.covers(job.time-pcm.length/16,job.time);boolean smart=getSharedPreferences("settings",0).getBoolean("smartRouter",true);boolean run;String routeReason;
      if(smart){int thermal=0;try{thermal=getSystemService(android.os.PowerManager.class).getCurrentThermalStatus();}catch(Exception ignored){}AiRouter.Decision d=aiRouter.speech(subtitleFirst,covered,job.time,jobs.size(),skipped,thermal);run=d.run;routeReason=d.reason;}
      else{run=schedule.shouldRecognize(subtitleFirst,covered,job.time);routeReason=run?"Speech AI":"Subtitle AI · phụ đề rõ";}
      if(!run){State.audioMood=sound.label.isEmpty()?routeReason:("Có thể: "+sound.label+" · "+routeReason);State.changed();continue;}
      OfflineStream stream=asr.createStream();try{stream.acceptWaveform(pcm,16000);asr.decode(stream);OfflineRecognizerResult r=asr.getResult(stream);source=r.getText().trim();if(RecognitionPack.enhanced(this)){emotion=" · cảm xúc ước đoán: "+RecognitionPack.emotion(r.getEmotion());State.audioMood=(sound.label.isEmpty()?"":sound.label+" · ")+"Cảm xúc ước đoán: "+RecognitionPack.emotion(r.getEmotion());State.changed();}}finally{stream.release();}
     }
     if(!active||RegionPicker.active||job.revision!=RegionPicker.revision)continue;
     engine="Âm thanh offline";
     if(mode.equals("hybrid")){HybridFusion.Choice choice=fusion.audio(source,job.time-pcm.length/16,job.time);source=choice.text;engine=choice.reason;}
     engine+=emotion;if(mode.equals("audio")&&SystemClock.elapsedRealtime()-job.time<2500&&sound.nonverbal&&source.trim().isEmpty()){if(voice!=null)voice.clearPending();State.recordId=-1;State.translated="";State.original="";State.changed();}

    if(active&&job.revision==RegionPicker.revision&&!RegionPicker.active&&!source.trim().isEmpty())offer(jobs,new Job(new Recognized(source,engine),job.time,pcm,job.revision));
   }
  }catch(InterruptedException ignored){}catch(Exception|LinkageError e){if(active){State.status("Bộ nghe dừng: "+State.error(e));if(mode.equals("audio"))State.main.post(this::stopSelf);}}
  finally{if(sounds!=null)sounds.close();if(asr!=null)asr.release();}
 }
 private void audio(){AudioRecord a=null;SpeechBoundary boundary=null;try{
  if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new SecurityException("Chưa cấp quyền thu âm");
  AudioPlaybackCaptureConfiguration config=new AudioPlaybackCaptureConfiguration.Builder(projection).addMatchingUsage(AudioAttributes.USAGE_MEDIA).addMatchingUsage(AudioAttributes.USAGE_GAME).addMatchingUsage(AudioAttributes.USAGE_UNKNOWN).excludeUid(android.os.Process.myUid()).build();
  int min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);if(min<=0)throw new IllegalStateException("Máy không hỗ trợ âm thanh 16 kHz");
  a=new AudioRecord.Builder().setAudioFormat(new AudioFormat.Builder().setSampleRate(16000).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build()).setBufferSizeInBytes(Math.max(min*4,32000)).setAudioPlaybackCaptureConfig(config).build();recorder=a;
  if(a.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("Không mở được luồng thu âm nội bộ");if(!active)return;a.startRecording();State.status("Đang nghe âm thanh nội bộ. Mở video web hoặc Telegram.");
  try{boundary=new SpeechBoundary(this);}catch(Exception|LinkageError e){State.status("Không mở được VAD, dùng ngắt câu dự phòng: "+State.error(e));}
  Segmenter segmenter=new Segmenter(pcm->{ReferenceVoice.update(pcm);if(!mode.equals("ocr"))enqueue(pcm);},live);short[] buffer=new short[320];long last=SystemClock.elapsedRealtime(),warn=0;
  while(active){int n=a.read(buffer,0,buffer.length,AudioRecord.READ_BLOCKING);if(n<0)throw new IllegalStateException("Mã thu âm "+n);if(n==0)continue;
   if(Segmenter.rms(buffer,n)>0.003)last=SystemClock.elapsedRealtime();if(SystemClock.elapsedRealtime()-last>10000&&SystemClock.elapsedRealtime()-warn>10000){warn=SystemClock.elapsedRealtime();State.status("Chưa nhận được tiếng. Ứng dụng có thể chặn thu âm; thử đọc phụ đề.");}Boolean neural=null;if(boundary!=null)try{neural=boundary.inspect(buffer,n);}catch(Exception|LinkageError e){boundary.close();boundary=null;State.status("VAD tạm lỗi, tiếp tục ngắt câu dự phòng");}segmenter.add(buffer,n,neural);
  }
 }catch(Exception e){if(active){State.status("Thu âm dừng: "+State.error(e));if(mode.equals("audio"))State.main.post(this::stopSelf);}}finally{if(boundary!=null)boundary.close();recorder=null;if(a!=null){try{a.stop();}catch(Exception ignored){}a.release();}}}
 private void addOverlay(){
  wm=getSystemService(WindowManager.class);overlay=new LinearLayout(this);overlay.setOrientation(LinearLayout.VERTICAL);overlay.setPadding(16,12,16,10);overlay.setBackgroundColor(0xEB102D37);
  caption=new TextView(this);caption.setTextColor(Color.WHITE);caption.setTextSize(16);caption.setMaxLines(4);caption.setText("VietVoice · đang chuẩn bị");overlay.addView(caption);
  LinearLayout row=new LinearLayout(this);Button stop=new Button(this);stop.setText("Dừng");stop.setOnClickListener(v->{State.status("Đã dừng");stopSelf();});row.addView(stop);
  if(!mode.equals("audio")){Button area=new Button(this);area.setText("Khoanh vùng");area.setOnClickListener(v->RegionPicker.show(this));row.addView(area);}overlay.addView(row);
  Button keep=new Button(this);keep.setText("Giữ giọng vừa nói");keep.setOnClickListener(v->{try{ReferenceVoice.keep(this);State.status("Đã giữ giọng vừa nói. Các câu tiếp theo dùng giọng này.");}catch(Exception e){State.status(State.error(e));}});overlay.addView(keep);
  int width=Math.min(getResources().getDisplayMetrics().widthPixels-32,(int)(350*getResources().getDisplayMetrics().density));WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;p.y=70;
  caption.setOnTouchListener(new View.OnTouchListener(){float y;int old;public boolean onTouch(View v,MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){y=e.getRawY();old=p.y;return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){p.y=Math.max(0,old+(int)(e.getRawY()-y));wm.updateViewLayout(overlay,p);}return true;}});wm.addView(overlay,p);
 }
 private void moveCrop(int delta){SharedPreferences p=getSharedPreferences("settings",0);int n=Math.max(0,Math.min(70,p.getInt("crop",65)+delta));SubtitleRegion.reset(this);p.edit().putInt("crop",n).apply();caption.setText("Vùng quét: "+n+"–"+(n+30)+"% chiều cao màn hình");}
 @Override public void onConfigurationChanged(android.content.res.Configuration c){super.onConfigurationChanged(c);if(screenReader!=null&&Build.VERSION.SDK_INT<34){android.util.DisplayMetrics m=new android.util.DisplayMetrics();getSystemService(WindowManager.class).getDefaultDisplay().getRealMetrics(m);screenReader.resize(m.widthPixels,m.heightPixels);}}
 @Override public void onDestroy(){
  active=false;RegionPicker.closePicker();ReferenceVoice.latest=null;State.running=false;State.listeners.remove(refresh);jobs.clear();audioJobs.clear();if(recognition!=null)recognition.interrupt();if(worker!=null)worker.interrupt();if(capture!=null)capture.interrupt();AudioRecord a=recorder;if(a!=null)try{a.stop();}catch(Exception ignored){}
  if(screenReader!=null)screenReader.close();if(sceneAnalyzer!=null)sceneAnalyzer.close();if(heavyAi!=null)heavyAi.close();sceneMemory.clear();dialogue.clear();State.scene("");if(projection!=null){if(callback!=null)projection.unregisterCallback(callback);projection.stop();}if(voice!=null)voice.close();if(overlay!=null)try{wm.removeView(overlay);}catch(Exception ignored){}stopForeground(STOP_FOREGROUND_REMOVE);State.changed();super.onDestroy();
 }
}
