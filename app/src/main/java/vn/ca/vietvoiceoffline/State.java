package vn.ca.vietvoiceoffline;
import android.os.*;
import java.util.concurrent.CopyOnWriteArrayList;
final class State {
 static final Handler main = new Handler(Looper.getMainLooper());
 static final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
 static volatile boolean running, downloading;
 static volatile long recordId=-1;
 static volatile String status="Sẵn sàng chuẩn bị", setup="Cần tải bộ dịch trước lần dùng đầu.", original="", translated="", timing="", engine="", audioMood="", sceneContext="";
 static final StringBuilder history=new StringBuilder();
 static void changed(){main.post(()->{for(Runnable r:listeners)r.run();});}
 static void status(String s){status=s;changed();}
 static void setup(String s){setup=s;changed();}
 static void scene(String s){sceneContext=s==null?"":s;changed();}
 static synchronized void result(String src,String vi,String timingText,String engineText,long id){
  recordId=id;
  original=src;translated=vi;timing=timingText;engine=engineText;
  history.insert(0,src+"\n→ "+vi+"\n\n");if(history.length()>12000)history.setLength(12000);changed();
 }
 static synchronized void refine(long id,String src,String vi,String engineText,String note){
  if(recordId!=id||!original.equals(src))return;translated=vi;engine=engineText;timing=timing+(timing.isEmpty()?"":" · ")+note;changed();
 }
 static synchronized String historyText(){return history.toString();}
 static String error(Throwable t){if(t==null)return "Lỗi chưa xác định";while(t.getCause()!=null)t=t.getCause();return t.getMessage()==null?t.getClass().getSimpleName():t.getMessage();}
}
