package vn.ca.vietvoiceoffline;
import android.content.*;
import android.app.ActivityManager;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.os.StatFs;
import java.io.*;

/** Stores a user-selected LiteRT-LM model in app-private storage. */
final class HeavyModelStore {
 static File dir(Context c){return new File(c.getFilesDir(),"heavy_ai");}
 static File model(Context c){return new File(dir(c),"official-model.litertlm");}
 static boolean ready(Context c){if(HyModelStore.ready(c))return true;File f=model(c);return f.isFile()&&f.length()>64L*1024*1024;}
 static String status(Context c){if(HyModelStore.ready(c))return "HY-MT 1.8B · bộ chuyên dịch offline đã tải (1,13 GB)";File f=model(c);return ready(c)?String.format(java.util.Locale.ROOT,"AI nặng: %.2f GB · sẵn sàng",f.length()/1073741824.0):"AI nặng: chưa nhập model .litertlm";}
 static void remove(Context c){File f=model(c);if(f.exists()&&!f.delete())throw new IllegalStateException("Không xóa được model AI nặng");}
 static long totalMemory(Context c){try{ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();c.getSystemService(ActivityManager.class).getMemoryInfo(m);return m.totalMem;}catch(Throwable ignored){return 0;}}
 static String recommendation(Context c){double gb=totalMemory(c)/1073741824.0;if(gb>=11.5)return "Khuyến nghị máy này: Gemma 3n E4B · khoảng 4.92 GB · đa phương thức.";if(gb>=7.5)return "Khuyến nghị máy này: Gemma 3n E2B · khoảng 3.66 GB · đa phương thức.";return "RAM máy thấp cho Gemma 3n nặng: dùng Gemma3 1B (~0.58 GB) ổn định hơn; Scene AI vẫn hoạt động riêng.";}
 static String modelPage(Context c){double gb=totalMemory(c)/1073741824.0;if(gb>=11.5)return "https://huggingface.co/google/gemma-3n-E4B-it-litert-lm";if(gb>=7.5)return "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm";return "https://huggingface.co/litert-community/Gemma3-1B-IT";}
 static long sourceSize(Context c,Uri uri){
  try(Cursor cur=c.getContentResolver().query(uri,new String[]{OpenableColumns.SIZE},null,null,null)){if(cur!=null&&cur.moveToFirst()){int i=cur.getColumnIndex(OpenableColumns.SIZE);if(i>=0&&!cur.isNull(i))return cur.getLong(i);}}catch(Throwable ignored){}return -1;
 }
 static void importUri(Context c,Uri uri) throws IOException{
  File d=dir(c);if(!d.exists()&&!d.mkdirs())throw new IOException("Không tạo được thư mục model");
  File tmp=new File(d,"official-model.part"),dst=model(c);long copied=0,lastNotice=0,expected=sourceSize(c,uri);
  long reserve=512L*1024*1024,free=new StatFs(d.getAbsolutePath()).getAvailableBytes();
  if(expected>0&&free<expected+reserve)throw new IOException(String.format(java.util.Locale.ROOT,"Không đủ bộ nhớ trống. Cần khoảng %.1f GB nữa để nhập model an toàn.",(expected+reserve-free)/1073741824.0));
  try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new BufferedOutputStream(new FileOutputStream(tmp),4*1024*1024)){
   if(in==null)throw new FileNotFoundException("Không mở được file model");byte[] buf=new byte[4*1024*1024];int n;
   while((n=in.read(buf))>0){out.write(buf,0,n);copied+=n;if(copied-lastNotice>=128L*1024*1024){lastNotice=copied;State.setup(String.format(java.util.Locale.ROOT,"Đang chép AI nặng… %.1f GB",copied/1073741824.0));if(new StatFs(d.getAbsolutePath()).getAvailableBytes()<128L*1024*1024)throw new IOException("Bộ nhớ gần đầy trong lúc nhập model");}}
  }catch(Throwable t){tmp.delete();if(t instanceof IOException)throw (IOException)t;throw new IOException(t);}
  if(copied<64L*1024*1024){tmp.delete();throw new IOException("File quá nhỏ, không giống model .litertlm");}
  if(dst.exists()&&!dst.delete()){tmp.delete();throw new IOException("Không thay được model cũ");}
  if(!tmp.renameTo(dst)){try(InputStream in=new FileInputStream(tmp);OutputStream out=new FileOutputStream(dst)){byte[] b=new byte[1024*1024];for(int n;(n=in.read(b))>0;)out.write(b,0,n);}tmp.delete();}
 }
}
