package vn.ca.vietvoiceoffline;
import android.content.Context;
import android.media.*;
import android.os.SystemClock;
import com.k2fsa.sherpa.onnx.*;
import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
final class OfflineVoice implements AutoCloseable{
 private static final Object nativeLock=new Object();
 interface Init{void done(boolean ready);}
 private final boolean live;private volatile int epoch;private volatile boolean closed,ready;private volatile AudioTrack track;private final Thread thread;
 private static class Speech{final String text;final long time;final float[] reference;final int epoch;Speech(String t,float[] r,int e,long born){text=t;reference=r;epoch=e;time=born;}}
 private final ArrayBlockingQueue<Speech> queue=new ArrayBlockingQueue<>(1);
 OfflineVoice(Context c,Init callback){live=c.getSharedPreferences("settings",0).getBoolean("live",true);Context app=c.getApplicationContext();thread=new Thread(()->run(app,callback),"vietnamese-offline");thread.start();}
 boolean ready(){return ready&&!closed;}
 void speak(String text){speak(text,null);}
 void speak(String text,float[] reference){speak(text,reference,SystemClock.elapsedRealtime());}
 void speak(String text,float[] reference,long born){if(!closed){queue.poll();queue.offer(new Speech(text,reference,epoch,born));}}
 void clearPending(){epoch++;queue.clear();AudioTrack a=track;if(a!=null)try{a.pause();a.flush();}catch(Exception ignored){}}
 private void run(Context c,Init callback){OfflineTts tts=null;VoiceClone cloner=null;boolean reported=false;
  try{
   if(!VoicePack.ready(c))throw new IllegalStateException("Chưa tải giọng Việt");File dir=VoicePack.dir(c);
   OfflineTtsVitsModelConfig vits=new OfflineTtsVitsModelConfig();vits.setModel(new File(dir,"vi_VN-vais1000-medium.onnx").getAbsolutePath());vits.setTokens(new File(dir,"tokens.txt").getAbsolutePath());vits.setDataDir(new File(dir,"espeak-ng-data").getAbsolutePath());
   OfflineTtsModelConfig model=new OfflineTtsModelConfig();model.setVits(vits);model.setNumThreads(2);model.setProvider("cpu");OfflineTtsConfig config=new OfflineTtsConfig();config.setModel(model);config.setMaxNumSentences(1);
   synchronized(nativeLock){tts=new OfflineTts(null,config);}if(closed)return;ready=true;reported=true;State.main.post(()->{if(!closed)callback.done(true);});
   while(!closed){Speech s=queue.take();if(s.epoch!=epoch||SystemClock.elapsedRealtime()-s.time>(live?6000:15000))continue;
    for(String part:SpeechChunks.split(s.text,live?100:350)){if(closed||s.epoch!=epoch||(live&&!queue.isEmpty()))break;
     GeneratedAudio audio;synchronized(nativeLock){if(closed)break;audio=tts.generate(part,0,1f);}
      if(closed||s.epoch!=epoch||(live&&SystemClock.elapsedRealtime()-s.time>8000))break;float[] pcm=audio.getSamples();int rate=audio.getSampleRate();
      float[] reference=ReferenceVoice.read(c);if(reference==null)reference=s.reference;
      boolean clone=!live&&c.getSharedPreferences("settings",0).getBoolean("clone",true);
      if(clone&&reference!=null&&reference.length>=16000){
       try{
        if(cloner==null)cloner=new VoiceClone(c);
        float[] ref=VoiceFeatures.resample(reference,16000,22050);
        float[] base=VoiceFeatures.resample(pcm,rate,22050);
        // Limit each conversion to 4 seconds to bound peak memory on phones.
        java.util.ArrayList<float[]> blocks=new java.util.ArrayList<>();int total=0;
        for(int offset=0;offset<base.length&&!closed;offset+=88200){
         int end=Math.min(base.length,offset+88200);float[] block=java.util.Arrays.copyOfRange(base,offset,end);
         if(block.length>=1024)block=cloner.convert(block,ref);blocks.add(block);total+=block.length;
        }
        pcm=new float[total];int at=0;for(float[] block:blocks){System.arraycopy(block,0,pcm,at,block.length);at+=block.length;}rate=22050;
        State.status("Đang đọc bằng giọng mô phỏng nhân vật · thử nghiệm");
       }catch(Exception|LinkageError e){State.status("Không giả giọng được, dùng giọng Việt thường: "+State.error(e));}
      }else if(clone)State.status("Chưa có mẫu giọng: đang đọc giọng Việt thường. Cần video có âm thanh được phép thu.");
      float speed=1f;
      if(c.getSharedPreferences("settings",0).getBoolean("expression",true)&&s.reference!=null&&s.reference.length>=16000){
       VoiceFeatures.applyEnergy(pcm,s.reference);
       speed=Math.max(.85f,Math.min(1.25f,((float)pcm.length/rate)/((float)s.reference.length/16000)));
      }
      speed=Math.max(.7f,Math.min(1.8f,speed*c.getSharedPreferences("settings",0).getInt("voiceRate",112)/100f));
      if(!closed&&s.epoch==epoch){if(SystemClock.elapsedRealtime()-s.time<(live?8000:30000))play(pcm,rate,speed,s.epoch,s.time);else State.status("Giả giọng xử lý quá chậm: bỏ câu cũ. Có thể tắt mô phỏng giọng để giảm trễ.");}
    }
   }
  }catch(InterruptedException ignored){}catch(Exception|LinkageError e){if(!closed)State.status("Giọng Việt chưa sẵn sàng: "+State.error(e));}
  finally{if(cloner!=null)cloner.close();ready=false;if(!reported)State.main.post(()->{if(!closed)callback.done(false);});if(tts!=null)synchronized(nativeLock){tts.release();}}
 }
 private void play(float[] pcm,int rate,float speed,int speechEpoch,long born)throws InterruptedException{
  if(pcm.length==0)return;int min=AudioTrack.getMinBufferSize(rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_FLOAT);
  AudioTrack local=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_FLOAT).build()).setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(Math.max(8192,min*2)).build();track=local;
  try{local.setPlaybackParams(new PlaybackParams().setPitch(1f).setSpeed(speed));local.play();int at=0;while(at<pcm.length&&!closed&&speechEpoch==epoch&&(!live||SystemClock.elapsedRealtime()-born<8000)&&(!live||queue.isEmpty()||SystemClock.elapsedRealtime()-born<4500)){int n=local.write(pcm,at,Math.min(2048,pcm.length-at),AudioTrack.WRITE_BLOCKING);if(n<0)throw new IllegalStateException("Lỗi phát giọng "+n);if(n==0){Thread.sleep(10);continue;}at+=n;}long until=SystemClock.elapsedRealtime()+3000;while(!closed&&speechEpoch==epoch&&local.getPlaybackHeadPosition()<at&&SystemClock.elapsedRealtime()<until)Thread.sleep(20);}
  finally{track=null;try{local.stop();}catch(Exception ignored){}local.release();}
 }
 public void close(){closed=true;ready=false;queue.clear();thread.interrupt();AudioTrack a=track;if(a!=null)try{a.pause();a.flush();}catch(Exception ignored){}}
}
