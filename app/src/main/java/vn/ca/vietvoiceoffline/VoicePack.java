package vn.ca.vietvoiceoffline;
import android.content.Context;
import java.io.*;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
final class VoicePack{
 static final String NAME="vits-piper-vi_VN-vais1000-medium";
 static final String URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/"+NAME+".tar.bz2";
 static final String SHA="fa1367710767d36ed5cf13b4a449e20c35ffd12791c2e47c2e64142bfa55551a";
 static File dir(Context c){return new File(new File(c.getFilesDir(),"voice"),NAME);}
 static boolean ready(Context c){File d=dir(c);return new File(d,".ready").exists()&&new File(d,"vi_VN-vais1000-medium.onnx").length()==63149198L&&new File(d,"tokens.txt").length()>0&&new File(d,"espeak-ng-data/phondata").exists();}
 static void install(Context c)throws Exception{
  if(ready(c))return;File archive=new File(c.getFilesDir(),"voice.tar.bz2");ModelStore.fetch(archive,URL,67154040L,SHA,"Giọng Việt");State.setup("Đang giải nén giọng Việt offline…");
  File stage=new File(c.getFilesDir(),"voice-stage");remove(stage);stage.mkdirs();String root=stage.getCanonicalPath()+File.separator;long total=0;int files=0;
  try(TarArchiveInputStream tar=new TarArchiveInputStream(new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(archive))))){
   TarArchiveEntry e;byte[] b=new byte[65536];while((e=tar.getNextTarEntry())!=null){
    if(++files>3000||e.getSize()<0||(total+=e.getSize())>300000000)throw new IOException("Gói giọng vượt giới hạn");
    File out=new File(stage,e.getName());if(!out.getCanonicalPath().startsWith(root)||e.isSymbolicLink()||e.isLink())throw new IOException("Gói giọng không hợp lệ");
    if(e.isDirectory()){out.mkdirs();continue;}if(!e.isFile())continue;out.getParentFile().mkdirs();try(OutputStream stream=new FileOutputStream(out)){int n;while((n=tar.read(b))!=-1)stream.write(b,0,n);}
   }
  }
  File model=new File(stage,NAME+"/vi_VN-vais1000-medium.onnx");if(model.length()!=63149198L||!ModelStore.hash(model).equals("df1512ef3265609f147ae23726b8c8867c6d28e60acb9ffca3545e11783b809f"))throw new IOException("Mô hình giọng Việt chưa nguyên vẹn");
  File dest=new File(c.getFilesDir(),"voice");remove(dest);if(!stage.renameTo(dest))throw new IOException("Không lưu được giọng Việt");
  try(FileOutputStream out=new FileOutputStream(new File(dir(c),".ready"))){out.write(SHA.getBytes("UTF-8"));}archive.delete();
 }
 private static void remove(File f){if(f.isDirectory()){File[] children=f.listFiles();if(children!=null)for(File child:children)remove(child);}f.delete();}
}
