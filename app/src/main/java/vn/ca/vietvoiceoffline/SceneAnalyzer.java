package vn.ca.vietvoiceoffline;

import android.graphics.*;
import android.os.SystemClock;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.*;
import com.google.mlkit.vision.label.*;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.pose.*;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * On-device scene hints for translation. Deliberately conservative: outputs observations,
 * not invented story events. The translation LLM is instructed to ignore uncertain hints.
 */
final class SceneAnalyzer implements AutoCloseable {
 interface Sink{void accept(SceneContext context);}
 private final ImageLabeler labels;
 private final FaceDetector faces;
 private final PoseDetector pose;
 private final Sink sink;
 private final AtomicBoolean busy=new AtomicBoolean(false);
 private volatile boolean closed;
 private long lastAccepted;
 private int[] previousLuma;

 SceneAnalyzer(Sink sink){
  this.sink=sink;
  labels=ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
  faces=FaceDetection.getClient(new FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
    .setMinFaceSize(0.08f).build());
  pose=PoseDetection.getClient(new PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build());
 }

 void analyze(Bitmap bitmap,long timeMs){
  if(bitmap==null){return;}if(closed){bitmap.recycle();return;}
  if(timeMs-lastAccepted<900||!busy.compareAndSet(false,true)){bitmap.recycle();return;}lastAccepted=timeMs;
  try{
   float motion=motion(bitmap);InputImage image=InputImage.fromBitmap(bitmap,0);
   Task<List<ImageLabel>> lt=labels.process(image);Task<List<Face>> ft=faces.process(image);Task<Pose> pt=pose.process(image);
   Tasks.whenAllComplete(lt,ft,pt).addOnCompleteListener(done->{
    try{
     if(closed)return;List<ImageLabel> ls=lt.isSuccessful()?lt.getResult():Collections.emptyList();List<Face> fs=ft.isSuccessful()?ft.getResult():Collections.emptyList();Pose p=pt.isSuccessful()?pt.getResult():null;
     SceneContext c=build(timeMs,ls,fs,p,motion);if(c.useful()&&sink!=null){byte[] shot=jpeg(bitmap);sink.accept(new SceneContext(c.timeMs,c.summary,c.confidence,shot));}
    }finally{busy.set(false);if(!bitmap.isRecycled())bitmap.recycle();}
   });
  }catch(Throwable t){busy.set(false);if(!bitmap.isRecycled())bitmap.recycle();}

 }

 private SceneContext build(long time,List<ImageLabel> ls,List<Face> fs,Pose p,float motion){
  ArrayList<String> obs=new ArrayList<>();float confidence=0.35f;
  if(fs!=null&&!fs.isEmpty()){
   obs.add(fs.size()==1?"1 khuôn mặt":"nhiều khuôn mặt ("+fs.size()+")");confidence=Math.max(confidence,0.7f);
   int smiles=0,closedEyes=0;
   for(Face f:fs){Float s=f.getSmilingProbability(),l=f.getLeftEyeOpenProbability(),r=f.getRightEyeOpenProbability();if(s!=null&&s>0.72f)smiles++;if(l!=null&&r!=null&&l<0.25f&&r<0.25f)closedEyes++;}
   if(smiles>0)obs.add(smiles==1?"có người đang cười":"có nhiều người đang cười");
   if(closedEyes>0)obs.add("có người nhắm mắt");
  }
  if(p!=null){String action=poseSummary(p);if(!action.isEmpty()){obs.add(action);confidence=Math.max(confidence,0.68f);}}
  if(motion>0.22f){obs.add("chuyển động mạnh");confidence=Math.max(confidence,0.55f);}else if(motion>0.10f)obs.add("có chuyển động");
  if(ls!=null&&!ls.isEmpty()){
   ArrayList<String> top=new ArrayList<>();for(ImageLabel l:ls){if(l.getConfidence()<0.65f)continue;String t=l.getText();if(t==null||t.trim().isEmpty()||generic(t))continue;top.add(t.trim());if(top.size()>=4)break;}
   if(!top.isEmpty()){obs.add("nhãn cảnh: "+String.join(", ",top));confidence=Math.max(confidence,0.5f);}
  }
  return new SceneContext(time,String.join("; ",obs),confidence);
 }

 private static boolean generic(String s){String x=s.toLowerCase(Locale.ROOT);return x.equals("person")||x.equals("human")||x.equals("photograph")||x.equals("image")||x.equals("snapshot")||x.equals("event");}

 private static String poseSummary(Pose p){
  PoseLandmark ls=get(p,PoseLandmark.LEFT_SHOULDER),rs=get(p,PoseLandmark.RIGHT_SHOULDER),lh=get(p,PoseLandmark.LEFT_HIP),rh=get(p,PoseLandmark.RIGHT_HIP),lw=get(p,PoseLandmark.LEFT_WRIST),rw=get(p,PoseLandmark.RIGHT_WRIST),nose=get(p,PoseLandmark.NOSE);
  ArrayList<String> out=new ArrayList<>();
  if(good(ls)&&good(rs)){
   float shoulder=Math.max(20f,dist(ls,rs));
   boolean leftUp=good(lw)&&lw.getPosition().y<ls.getPosition().y-0.10f*shoulder;
   boolean rightUp=good(rw)&&rw.getPosition().y<rs.getPosition().y-0.10f*shoulder;
   if(leftUp&&rightUp)out.add("giơ hai tay");else if(leftUp||rightUp)out.add("giơ một tay");
   if(good(lw)&&good(rw)&&dist(lw,rw)<0.55f*shoulder)out.add("hai tay ở gần nhau");
   if(good(lh)&&good(rh)){
    PointF sm=mid(ls,rs),hm=mid(lh,rh);float dx=Math.abs(sm.x-hm.x),dy=Math.abs(sm.y-hm.y);if(dx>dy*1.25f&&dx>shoulder*0.45f)out.add("thân người nghiêng/ngang");
    if(good(nose)){float torso=Math.max(30f,dist(sm,hm));if(nose.getPosition().y>sm.y-0.20f*torso)out.add("đầu cúi thấp");}
   }
  }
  return String.join(", ",out);
 }
 private static PoseLandmark get(Pose p,int type){return p.getPoseLandmark(type);}
 private static boolean good(PoseLandmark l){return l!=null&&l.getInFrameLikelihood()>=0.55f;}
 private static float dist(PoseLandmark a,PoseLandmark b){return dist(a.getPosition(),b.getPosition());}
 private static float dist(PointF a,PointF b){float x=a.x-b.x,y=a.y-b.y;return (float)Math.sqrt(x*x+y*y);}
 private static PointF mid(PoseLandmark a,PoseLandmark b){return new PointF((a.getPosition().x+b.getPosition().x)/2f,(a.getPosition().y+b.getPosition().y)/2f);}

 private synchronized float motion(Bitmap source){
  Bitmap small=Bitmap.createScaledBitmap(source,32,18,false);int[] px=new int[32*18];small.getPixels(px,0,32,0,0,32,18);small.recycle();int[] luma=new int[px.length];
  for(int i=0;i<px.length;i++){int c=px[i];luma[i]=(Color.red(c)*3+Color.green(c)*6+Color.blue(c))/10;}
  if(previousLuma==null){previousLuma=luma;return 0f;}long d=0;for(int i=0;i<luma.length;i++)d+=Math.abs(luma[i]-previousLuma[i]);previousLuma=luma;return Math.min(1f,d/(255f*luma.length));
 }

 private static byte[] jpeg(Bitmap bitmap){
  try{ByteArrayOutputStream out=new ByteArrayOutputStream(96*1024);if(bitmap.compress(Bitmap.CompressFormat.JPEG,68,out))return out.toByteArray();}catch(Throwable ignored){}return null;
 }

 @Override public void close(){closed=true;try{labels.close();}catch(Exception ignored){}try{faces.close();}catch(Exception ignored){}try{pose.close();}catch(Exception ignored){}}
}
