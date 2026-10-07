package vn.ca.vietvoiceoffline;
import android.content.Context;
import android.graphics.*;
import android.hardware.display.*;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import java.util.*;

final class ScreenReader implements AutoCloseable{
 interface Sink{void accept(String text,boolean fresh);}
 interface FrameSink{void accept(Bitmap frame,long timeMs);}
 private final Context context;private final MediaProjection projection;private final TextRecognizer ocr;private final Sink sink;private final FrameSink frameSink;
 private final HandlerThread thread=new HandlerThread("subtitle-ocr");private final Handler handler;private SubtitleGate gate=new SubtitleGate();private Rect lastRegion;
 private ImageReader reader;private VirtualDisplay display;private boolean stopped,busy;private long next,lastText=SystemClock.elapsedRealtime(),nextScene;private int width,height;
 private java.nio.ByteBuffer cropBytes;
 ScreenReader(Context c,MediaProjection p,String lang,Sink s){this(c,p,lang,s,null);}
 ScreenReader(Context c,MediaProjection p,String lang,Sink s,FrameSink f){context=c;projection=p;sink=s;frameSink=f;ocr=lang.equals("ja")?TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build()):lang.equals("ko")?TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build()):TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());thread.start();handler=new Handler(thread.getLooper());}
 void start(){
  DisplayMetrics m=new DisplayMetrics();context.getSystemService(WindowManager.class).getDefaultDisplay().getRealMetrics(m);width=m.widthPixels;height=m.heightPixels;
  reader=makeReader(width,height);display=projection.createVirtualDisplay("VietVoice subtitles",width,height,m.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),new VirtualDisplay.Callback(){},handler);
 }
 private ImageReader makeReader(int w,int h){ImageReader r=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2);r.setOnImageAvailableListener(this::frame,handler);return r;}
 void resize(int w,int h){handler.post(()->{if(stopped||display==null||w<=0||h<=0||(w==width&&h==height))return;ImageReader old=reader;width=w;height=h;reader=makeReader(w,h);display.setSurface(null);display.resize(w,h,context.getResources().getDisplayMetrics().densityDpi);display.setSurface(reader.getSurface());old.close();});}
 private void frame(ImageReader source){
  Image image=null;Bitmap padded=null,crop=null,visible=null;
  try{
   image=source.acquireLatestImage();long now=SystemClock.elapsedRealtime();if(image==null||stopped||busy||RegionPicker.active||now<next)return;next=now+500;busy=true;
   Image.Plane plane=image.getPlanes()[0];int w=image.getWidth(),h=image.getHeight(),pixel=plane.getPixelStride(),row=plane.getRowStride();if(pixel!=4)throw new IllegalStateException("Định dạng màn hình không hỗ trợ");
   int paddedWidth=row/pixel;java.nio.ByteBuffer pixels=plane.getBuffer();pixels.rewind();
   Rect region=SubtitleRegion.get(context,w,h);if(!region.equals(lastRegion)){gate=new SubtitleGate();lastRegion=new Rect(region);}
   boolean sampleScene=frameSink!=null&&now>=nextScene;
   if(sampleScene){
    nextScene=now+1000;padded=Bitmap.createBitmap(paddedWidth,h,Bitmap.Config.ARGB_8888);padded.copyPixelsFromBuffer(pixels);
    crop=Bitmap.createBitmap(padded,region.left,region.top,region.width(),region.height());
    visible=Bitmap.createBitmap(padded,0,0,w,h);int target=Math.min(640,Math.max(320,w));int th=Math.max(180,Math.round(h*(target/(float)w)));Bitmap scene=Bitmap.createScaledBitmap(visible,target,th,true);try{frameSink.accept(scene,now);}catch(Throwable t){if(!scene.isRecycled())scene.recycle();throw new RuntimeException(t);}
   }else crop=copyCrop(plane,region);
   Bitmap owned=crop;crop=null;
   ocr.process(InputImage.fromBitmap(owned,0)).addOnCompleteListener(r->handler.post(r),task->{
    try{
     if(stopped||RegionPicker.active)return;if(!task.isSuccessful()){State.status("Chưa đọc được chữ: "+State.error(task.getException()));return;}
     ArrayList<Text.Line> lines=new ArrayList<>();for(Text.TextBlock block:task.getResult().getTextBlocks())lines.addAll(block.getLines());
     lines.sort(Comparator.comparingInt(l->l.getBoundingBox()==null?0:l.getBoundingBox().top));StringBuilder sb=new StringBuilder();
     for(Text.Line line:lines){String s=line.getText();if(s.matches("(?s).*[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsHangul}].*"))sb.append(s).append(' ');}
     String raw=sb.toString().trim();if(!raw.isEmpty())lastText=SystemClock.elapsedRealtime();else if(SystemClock.elapsedRealtime()-lastText>12000)State.status("Chưa thấy phụ đề. Chỉnh vùng quét hoặc kiểm tra video chặn chụp màn hình.");
     String text=gate.accept(raw);if(raw.length()<=700&&gate.isStable(raw))sink.accept(raw,text!=null);else sink.accept("",false);
    }finally{owned.recycle();busy=false;if(stopped)thread.quitSafely();}
   });
  }catch(Exception e){busy=false;if(!stopped)State.status("Lỗi đọc phụ đề: "+State.error(e));}
  finally{if(image!=null)image.close();if(crop!=null&&!crop.isRecycled())crop.recycle();if(visible!=null&&!visible.isRecycled())visible.recycle();if(padded!=null&&!padded.isRecycled())padded.recycle();}
 }

 private Bitmap copyCrop(Image.Plane plane,Rect region){
  int rw=region.width(),rh=region.height(),bytes=rw*rh*4;
  if(cropBytes==null||cropBytes.capacity()<bytes)cropBytes=java.nio.ByteBuffer.allocateDirect(bytes);
  cropBytes.clear();java.nio.ByteBuffer src=plane.getBuffer().duplicate();int rowStride=plane.getRowStride(),pixelStride=plane.getPixelStride(),rowBytes=rw*pixelStride;
  if(pixelStride!=4)throw new IllegalStateException("Định dạng màn hình không hỗ trợ");
  for(int y=region.top;y<region.bottom;y++){int start=y*rowStride+region.left*pixelStride;src.position(start);src.limit(start+rowBytes);cropBytes.put(src);src.limit(src.capacity());}
  cropBytes.flip();Bitmap out=Bitmap.createBitmap(rw,rh,Bitmap.Config.ARGB_8888);out.copyPixelsFromBuffer(cropBytes);return out;
 }
 public void close(){handler.post(()->{stopped=true;if(display!=null)display.release();if(reader!=null)reader.close();ocr.close();if(!busy)thread.quitSafely();});}
}
