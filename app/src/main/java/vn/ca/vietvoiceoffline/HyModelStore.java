package vn.ca.vietvoiceoffline;
import android.content.Context;
import android.os.StatFs;
import java.io.*;
final class HyModelStore {
 static final long SIZE=1133080512L;
 static final String HASH="4383ac0c3c8e476de98ff979c2a3f069f8c4fb385e7860cf2d28da896cc477c7";
 static final String URL="https://huggingface.co/tencent/HY-MT1.5-1.8B-GGUF/resolve/265b2e615a7dc9b06c435dc878829ad99a512ba2/HY-MT1.5-1.8B-Q4_K_M.gguf";
 static File file(Context c){return new File(c.getFilesDir(),"hy-mt/model.gguf");}
 static boolean ready(Context c){return file(c).length()==SIZE;}
 static void install(Context c)throws Exception{
  File f=file(c);if(!f.getParentFile().isDirectory()&&!f.getParentFile().mkdirs())throw new IOException("Không tạo được thư mục bộ dịch");
  long left=SIZE-new File(f+".part").length();
  if(!ready(c)&&new StatFs(f.getParent()).getAvailableBytes()<left+512L*1024*1024)throw new IOException("Cần thêm bộ nhớ trống để tải bộ dịch 1,13 GB");
  ModelStore.fetch(f,URL,SIZE,HASH,"Bộ chuyên dịch offline HY-MT");
 }
}
