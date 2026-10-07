package vn.ca.vietvoiceoffline;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
public class MainActivity extends Activity{
 private LinearLayout root;private TextView status,setup,source,result,timing,cropLabel,heavyModel;private Spinner language,mode;
 private Button start,download;private Switch read,overlay;private SharedPreferences prefs;private OfflineVoice tester;
 private final Runnable refresh=this::refresh;private boolean checking;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("settings",0);getWindow().setStatusBarColor(Color.rgb(16,45,55));
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(244,247,246));root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(28));scroll.addView(root);setContentView(scroll);
  TextView brand=text(root,"VIETVOICE AI  /  1.0 OFFICIAL",12,0xff147D71);brand.setLetterSpacing(.15f);
  TextView title=text(root,"Xem video.\nNghe tiếng Việt.",30,0xff102D37);title.setTypeface(null,Typeface.BOLD);
  text(root,"Nhật · Trung · Hàn → Việt\nScene AI + Speech AI + Context AI + Heavy AI chạy trên thiết bị.",15,0xff526870);
  LinearLayout prep=card();text(prep,"01   CHUẨN BỊ MỘT LẦN",12,0xff147D71);setup=text(prep,State.setup,15,0xff102D37);
  download=button(prep,"Tải dữ liệu & hoàn thiện app",v->ModelStore.download(this,"ja",true));
  text(prep,"Gồm bộ nghe, đọc phụ đề, dịch và giọng Việt. Dùng Wi-Fi và giữ app mở khi tải. Chừa khoảng 2 GB trống. Gói giả giọng tải thêm khoảng 131 MB. Nếu bị ngắt, bấm tải lại.",12,0xff60747B);
  LinearLayout official=card();text(official,"02   AI NGỮ CẢNH CHÍNH THỨC",12,0xff147D71);
  Switch sceneAi=new Switch(this);sceneAi.setText("Scene AI · hiểu hoạt động và bối cảnh video");sceneAi.setChecked(prefs.getBoolean("sceneAi",true));official.addView(sceneAi);sceneAi.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("sceneAi",on).apply());
  text(official,"Chạy trên máy: nhận khuôn mặt, tư thế, chuyển động và nhãn cảnh. Chỉ đưa tín hiệu ngắn cho bộ dịch để gỡ nghĩa mơ hồ; không được tự thêm lời thoại.",12,0xff60747B);
  Switch heavyAi=new Switch(this);heavyAi.setText("Heavy Context AI · tinh chỉnh dịch bằng LLM cục bộ");heavyAi.setChecked(prefs.getBoolean("heavyAi",true));official.addView(heavyAi);heavyAi.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("heavyAi",on).apply());
  heavyModel=text(official,HeavyModelStore.status(this),13,0xff102D37);
  text(official,HeavyModelStore.recommendation(this),12,0xff526870);
  button(official,"Mở trang tải model khuyến nghị",v->safeOpen(new Intent(Intent.ACTION_VIEW,Uri.parse(HeavyModelStore.modelPage(this)))));
  button(official,"Nhập model AI nặng (.litertlm)",v->pickHeavyModel());
  button(official,"Xóa model AI nặng",v->{if(State.running){toast("Dừng phiên dịch trước khi thay model.");return;}try{HeavyModelStore.remove(this);State.setup("Đã xóa model AI nặng. App vẫn dịch bằng AI nhanh offline.");refresh();}catch(Exception e){State.status(State.error(e));}});
  text(official,"Model nặng là tùy chọn nhưng được ưu tiên khi có. App vẫn hiện bản dịch nhanh ngay; Heavy AI chỉ tinh chỉnh khi model rảnh, nên model chậm không làm dồn hàng câu cũ.",12,0xff60747B);
  LinearLayout controls=card();text(controls,"03   CHỌN NỘI DUNG",12,0xff147D71);
  text(controls,"Ngôn ngữ video",14,0xff102D37);language=spinner(controls,new String[]{"Tiếng Nhật","Tiếng Trung","Tiếng Hàn"},prefs.getInt("language",0));
  text(controls,"Lấy lời thoại bằng",14,0xff102D37);mode=spinner(controls,new String[]{"Tự động: phụ đề + âm thanh","Chỉ đọc phụ đề trên hình","Chỉ nghe âm thanh bên trong máy"},prefs.getInt("inputModeV2",0));
  text(controls,"Tự động ưu tiên phụ đề ổn định và đối chiếu lời nghe theo thời gian. Không ghép tùy ý hai câu khác nhau. Tiếng khóc/cười/rên rõ, không có lời: giữ tiếng gốc, không dịch thành chữ.",12,0xff60747B);
  Switch smartRouter=new Switch(this);smartRouter.setText("Multi-AI tự điều phối theo video và tải máy");smartRouter.setChecked(prefs.getBoolean("smartRouter",true));controls.addView(smartRouter);smartRouter.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("smartRouter",on).apply());
  text(controls,"Router tự chọn lúc dùng Subtitle AI, Speech AI và Sound AI; khi phụ đề đã đủ rõ hoặc máy nóng/quá tải, nó giảm tác vụ nặng nhưng vẫn kiểm tra lời nghe định kỳ.",12,0xff60747B);
  Switch qualityGuard=new Switch(this);qualityGuard.setText("Translation AI kiểm tra kết quả đáng ngờ");qualityGuard.setChecked(prefs.getBoolean("qualityGuard",true));controls.addView(qualityGuard);qualityGuard.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("qualityGuard",on).apply());
  text(controls,"Nếu kết quả dịch có dấu hiệu lỗi rõ (giữ nguyên chữ nguồn, OCR nhiễu...), app thử làm sạch và dịch lại. Nếu vẫn đáng ngờ, chỉ hiện chữ và không đọc sai thành tiếng.",12,0xff60747B);
  Switch economical=new Switch(this);economical.setText("Phụ đề rõ: giảm xử lý nhận giọng");economical.setChecked(prefs.getBoolean("subtitleFirst",true));controls.addView(economical);economical.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("subtitleFirst",on).apply());
  text(controls,"Ở chế độ Tự động: vẫn nghe khi thiếu phụ đề, kiểm tra lại lời nghe định kỳ khoảng 15 giây và tiếp tục lọc khóc/cười. Tắt để nhận giọng mọi đoạn. Áp dụng ngay.",12,0xff60747B);
  text(controls,"Bộ nhận dạng lời nói",14,0xff102D37);
  Spinner asrChoice=spinner(controls,new String[]{"SenseVoice · lời nói và cảm xúc","Whisper Tiny · chế độ nhẹ"},prefs.getInt("asrEngineV2",0));asrChoice.setOnItemSelectedListener(selection("asrEngineV2"));
  text(controls,"SenseVoice tải thêm khoảng 240 MB. Nhãn cảm xúc là ước đoán, chưa đồng nghĩa giọng đọc tái hiện đúng cảm xúc. Dừng rồi bắt đầu lại sau khi đổi bộ AI.",12,0xff60747B);
  button(controls,"Khoanh vùng phụ đề trên video",v->pickRegion());
  button(controls,"Đặt lại vùng phụ đề mặc định",v->{SubtitleRegion.reset(this);State.status("Đã đặt lại vùng phụ đề.");});
  text(controls,"Mở video rồi dùng nút Khoanh vùng trên bảng nổi. Kéo quanh chữ, chọn Dùng vùng này. Vùng ngang/dọc được lưu riêng.",12,0xff60747B);
  cropLabel=text(controls,"",13,0xff102D37);SeekBar crop=new SeekBar(this);crop.setMax(70);crop.setProgress(prefs.getInt("crop",65));controls.addView(crop);updateCrop(crop.getProgress());
  crop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int n,boolean user){updateCrop(n);if(user){SubtitleRegion.reset(MainActivity.this);prefs.edit().putInt("crop",n).apply();}}});
  read=new Switch(this);read.setText("Đọc bản dịch bằng giọng Việt");read.setChecked(prefs.getBoolean("speak",true));read.setPadding(0,dp(8),0,dp(8));controls.addView(read);read.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("speak",on).apply());
  Switch fast=new Switch(this);fast.setText("Ưu tiên theo kịp video");fast.setChecked(prefs.getBoolean("live",true));controls.addView(fast);
  text(controls,"Bật: đoạn nghe tối đa 3 giây, phụ đề dịch độc lập, dùng giọng Việt thường để giảm trễ. Tắt để dùng mô phỏng giọng. Dừng rồi bắt đầu lại sau khi đổi.",12,0xff60747B);
  Switch clone=new Switch(this);clone.setText("Mô phỏng giọng nhân vật (thử nghiệm)");clone.setChecked(prefs.getBoolean("clone",true));controls.addView(clone);clone.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("clone",on).apply());clone.setEnabled(!fast.isChecked());fast.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("live",on).apply();clone.setEnabled(!on);});
  TextView rateLabel=text(controls,"",14,0xff102D37);SeekBar rate=new SeekBar(this);rate.setMax(80);rate.setProgress(Math.max(0,Math.min(80,prefs.getInt("voiceRate",112)-80)));rateLabel.setText("Tốc độ giọng Việt: "+String.format(java.util.Locale.ROOT,"%.2f×",(rate.getProgress()+80)/100f));controls.addView(rate);rate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int n,boolean user){rateLabel.setText("Tốc độ giọng Việt: "+String.format(java.util.Locale.ROOT,"%.2f×",(n+80)/100f));if(user)prefs.edit().putInt("voiceRate",n+80).apply();}});
  text(controls,"Áp dụng từ đoạn đọc tiếp theo. Bám nhịp có thể điều chỉnh thêm tốc độ quanh mức này. Tăng tốc đọc không làm AI dịch nhanh hơn.",12,0xff60747B);
  Switch expression=new Switch(this);expression.setText("Bám nhịp và độ mạnh lời gốc");expression.setChecked(prefs.getBoolean("expression",true));controls.addView(expression);expression.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("expression",on).apply());
  text(controls,"Dừng rồi bắt đầu lại sau khi đổi lựa chọn. Tự lấy mẫu giọng từ âm thanh video, kể cả khi đọc phụ đề. Mẫu có nhạc nền hoặc nhiều người nói dễ bị sai giọng. Chưa tái hiện đầy đủ cảm xúc khóc/cười/thì thầm.",12,0xff60747B);
  button(controls,"Giữ giọng vừa nói",v->{try{ReferenceVoice.keep(this);State.status("Đã giữ giọng. Các câu tiếp theo dùng mẫu này.");}catch(Exception e){State.status(State.error(e));}});
  button(controls,"Xóa giọng đã giữ · tự theo nhân vật",v->{ReferenceVoice.clear(this);State.status("Đã xóa mẫu đã giữ. Tự lấy giọng từ đoạn video tiếp theo.");});
  overlay=new Switch(this);overlay.setText("Hiện bản dịch nổi trên video");overlay.setChecked(prefs.getBoolean("overlay",false));controls.addView(overlay);overlay.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("overlay",on).apply();if(on&&!Settings.canDrawOverlays(this))safeOpen(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));});
  start=button(controls,"Bắt đầu dịch video",v->begin());button(controls,"Dừng dịch",v->{stopService(new Intent(this,CaptureService.class));State.status("Đã dừng");});
  text(controls,"Android sẽ hỏi quyền chia sẻ mỗi lần bắt đầu. Để chuyển giữa web và Telegram, chọn toàn bộ màn hình. Nội dung chặn chụp/thu âm có thể không dịch được.",12,0xff60747B);
  LinearLayout live=card();text(live,"04   BẢN DỊCH HIỆN TẠI",12,0xff147D71);status=text(live,State.status,14,0xff147D71);source=text(live,"Lời gốc sẽ hiện ở đây",15,0xff60747B);result=text(live,"Bản dịch tiếng Việt",20,0xff102D37);result.setTypeface(null,Typeface.BOLD);timing=text(live,"",12,0xff60747B);
  button(live,"Mở Telegram",v->{Intent intent=getPackageManager().getLaunchIntentForPackage("org.telegram.messenger");if(intent==null)intent=getPackageManager().getLaunchIntentForPackage("org.telegram.messenger.web");if(intent!=null)startActivity(intent);else toast("Mở Telegram của bạn từ màn hình chính.");});
  button(live,"Mở trình duyệt",v->safeOpen(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com"))));
  button(live,"Thử giọng Việt",v->testVoice());button(live,"Lịch sử · sửa câu · bộ nhớ dịch",v->startActivity(new Intent(this,ReviewActivity.class)));
  text(root,"Official pipeline · AI trên thiết bị · Không khóa Gemini/API bắt buộc · Không quota dịch\nMạng chỉ cần cho tải dữ liệu/model và nội dung video trực tuyến.",13,0xff147D71);
  text(root,"Scene AI là ngữ cảnh hỗ trợ, không phải sự thật tuyệt đối. Heavy AI chỉ được dùng để gỡ mơ hồ và làm tiếng Việt tự nhiên hơn; tốc độ phụ thuộc model và phần cứng máy.",12,0xff60747B);
  language.setOnItemSelectedListener(selection("language"));mode.setOnItemSelectedListener(selection("inputModeV2"));
 }
 private AdapterView.OnItemSelectedListener selection(String key){return new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int n,long id){prefs.edit().putInt(key,n).apply();}};}
 private void updateCrop(int n){cropLabel.setText("Vùng phụ đề: từ "+n+"% đến "+(n+30)+"% chiều cao màn hình");}
 private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(16),dp(16),dp(16));GradientDrawable d=new GradientDrawable();d.setColor(Color.WHITE);d.setCornerRadius(dp(18));c.setBackground(d);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(18);root.addView(c,p);return c;}
 private TextView text(LinearLayout p,String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(5),0,dp(7));t.setTextIsSelectable(true);p.addView(t);return t;}
 private Button button(LinearLayout p,String title,View.OnClickListener click){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setMinHeight(dp(48));b.setTextColor(0xff147D71);p.addView(b,new LinearLayout.LayoutParams(-1,-2));b.setOnClickListener(click);return b;}
 private Spinner spinner(LinearLayout p,String[] names,int n){Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names);s.setAdapter(a);s.setSelection(Math.min(n,names.length-1));p.addView(s,new LinearLayout.LayoutParams(-1,dp(48)));return s;}
 private String language(){return new String[]{"ja","zh","ko"}[language.getSelectedItemPosition()];}
 private void refresh(){if(isFinishing())return;setup.setText(State.setup);status.setText(State.status);source.setText(State.original.isEmpty()?"Lời gốc sẽ hiện ở đây":State.original);result.setText(State.translated.isEmpty()?"Bản dịch tiếng Việt":State.translated);timing.setText((State.sceneContext.isEmpty()?"":"Scene AI: "+State.sceneContext+"\n")+State.audioMood+"\n"+State.engine+"\n"+State.timing);if(heavyModel!=null)heavyModel.setText(HeavyModelStore.status(this));start.setEnabled(!State.running&&!State.downloading&&!checking);download.setEnabled(!State.downloading&&!State.running);language.setEnabled(!State.running&&!State.downloading);mode.setEnabled(!State.running&&!State.downloading);download.setText(State.downloading?"Đang tải dữ liệu…":"Tải dữ liệu & hoàn thiện app");if(State.downloading||checking)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
 @Override public void onResume(){super.onResume();State.listeners.addIfAbsent(refresh);refresh();}
 @Override public void onPause(){State.listeners.remove(refresh);super.onPause();}
 @Override public void onDestroy(){if(tester!=null)tester.close();super.onDestroy();}
 private void begin(){
  if(State.running||State.downloading||checking)return;
  if((mode.getSelectedItemPosition()!=1||prefs.getBoolean("clone",true)||prefs.getBoolean("expression",true))&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1);return;}
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},2);return;}
  checking=true;refresh();String lang=language();boolean audio=mode.getSelectedItemPosition()!=1;
  new Thread(()->{
   String error=null;try{if(!ModelStore.translationPresent(lang))error="Chưa có bộ dịch. Bấm Tải dữ liệu & hoàn thiện app trước.";else if(!prefs.getBoolean("live",true)&&prefs.getBoolean("clone",true)&&!ClonePack.ready(this))error="Chưa tải bộ giả giọng. Bấm Tải dữ liệu & hoàn thiện app.";else if(audio&&!RecognitionPack.ready(this))error="Chưa có bộ nghe. Bấm Tải dữ liệu trước.";}catch(Exception e){error=State.error(e);}
   String problem=error;runOnUiThread(()->{checking=false;refresh();if(isFinishing()||isDestroyed())return;if(problem!=null){State.status(problem);return;}if(tester!=null){tester.close();tester=null;}
    MediaProjectionManager m=getSystemService(MediaProjectionManager.class);Intent request=Build.VERSION.SDK_INT>=34?m.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay()):m.createScreenCaptureIntent();startActivityForResult(request,10);
   });
  },"check-models").start();
 }
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==1){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)begin();else State.status("Cần quyền âm thanh để nghe hoặc mô phỏng giọng. Có thể tắt mô phỏng giọng và bám nhịp để chỉ đọc phụ đề.");}else if(request==2){launchAfterNotification();}}
 private void launchAfterNotification(){ // Do not re-prompt if notifications were denied.
  checking=true;String lang=language();boolean audio=mode.getSelectedItemPosition()!=1;
  new Thread(()->{String error=null;try{if(!ModelStore.translationPresent(lang)||(audio&&!RecognitionPack.ready(this))||(!prefs.getBoolean("live",true)&&prefs.getBoolean("clone",true)&&!ClonePack.ready(this)))error="Bấm Tải dữ liệu trước khi bắt đầu.";}catch(Exception e){error=State.error(e);}String message=error;runOnUiThread(()->{checking=false;refresh();if(isFinishing()||isDestroyed())return;if(message!=null){State.status(message);return;}MediaProjectionManager m=getSystemService(MediaProjectionManager.class);startActivityForResult(Build.VERSION.SDK_INT>=34?m.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay()):m.createScreenCaptureIntent(),10);});}).start();
 }
 @Override public void onActivityResult(int request,int resultCode,Intent data){super.onActivityResult(request,resultCode,data);
  if(request==30){if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null){checking=true;refresh();Uri uri=data.getData();new Thread(()->{String err=null;try{State.setup("Đang chép model AI nặng vào ứng dụng…");HeavyModelStore.importUri(this,uri);}catch(Exception e){err=State.error(e);}String error=err;runOnUiThread(()->{checking=false;State.setup(error==null?HeavyModelStore.status(this):"Lỗi model AI nặng: "+error);refresh();});},"import-heavy-model").start();}return;}
  if(request==10){if(resultCode==RESULT_OK&&data!=null){if(tester!=null){tester.close();tester=null;}Intent s=new Intent(this,CaptureService.class).putExtra("language",language()).putExtra("mode",new String[]{"hybrid","ocr","audio"}[mode.getSelectedItemPosition()]).putExtra("overlay",overlay.isChecked()).putExtra("consent",data);startForegroundService(s);toast("Bây giờ mở video ở web hoặc Telegram.");}else State.status("Bạn chưa cho phép chia sẻ màn hình.");}
 }
 private void pickHeavyModel(){
  if(State.running){toast("Dừng phiên dịch trước khi thay model.");return;}Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,30);
 }
 private void pickRegion(){if(!Settings.canDrawOverlays(this)){State.status("Cấp quyền hiển thị trên ứng dụng khác rồi bấm Khoanh vùng lại.");safeOpen(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));return;}moveTaskToBack(true);State.main.postDelayed(()->RegionPicker.show(getApplicationContext()),350);}
 private void testVoice(){if(State.running){toast("Dừng phiên dịch trước khi thử giọng.");return;}if(tester!=null)tester.close();tester=new OfflineVoice(this,ok->{if(ok)tester.speak("Xin chào. VietVoice đã sẵn sàng đọc bản dịch tiếng Việt.");else State.status("Chưa có giọng Việt. Bấm Tải dữ liệu & hoàn thiện app.");});}
 private void safeOpen(Intent i){try{startActivity(i);}catch(Exception e){toast("Không mở được mục này trên máy.");}}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
