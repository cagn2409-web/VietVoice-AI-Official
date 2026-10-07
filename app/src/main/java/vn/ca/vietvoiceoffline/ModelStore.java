package vn.ca.vietvoiceoffline;
import android.content.Context;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.*;
import com.google.mlkit.nl.translate.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
final class ModelStore {
 static final String BASE="https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny/resolve/65176e2deb88badc814a94058666cadccc29b61c/";
 static final String[] NAMES={"tiny-encoder.int8.onnx","tiny-decoder.int8.onnx","tiny-tokens.txt"};
 static final long[] SIZES={12937772,89855401,816730};
 static final String[] HASH={"d24fb083ae3b1041fc24e97971d60e280c9342201fbb67b0ab428a8b4a51a434","d2fece8dd42771f1df975c6c0445770d0c292bf7547c2cae04a6c0cc57540925","b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126"};
 static File file(Context c,int i){File f=new File(c.getFilesDir(),"whisper-tiny");f.mkdirs();return new File(f,NAMES[i]);}
 static boolean asrPresent(Context c){for(int i=0;i<3;i++)if(file(c,i).length()!=SIZES[i])return false;return true;}
 static Translator translator(String lang){return Translation.getClient(new TranslatorOptions.Builder().setSourceLanguage(lang).setTargetLanguage("vi").build());}
 static boolean translationPresent(String lang)throws Exception{
  RemoteModelManager m=RemoteModelManager.getInstance();
  return Tasks.await(m.isModelDownloaded(new TranslateRemoteModel.Builder(lang).build()),15,TimeUnit.SECONDS)&&Tasks.await(m.isModelDownloaded(new TranslateRemoteModel.Builder("vi").build()),15,TimeUnit.SECONDS);
 }
 static void download(Context context,String lang,boolean audio){
  if(State.downloading||State.running)return;Context c=context.getApplicationContext();State.downloading=true;State.setup("Đang chuẩn bị tải. Giữ app mở và kết nối mạng.");
  new Thread(()->{
   try{
    if(audio)for(int i=0;i<3;i++)downloadFile(c,i);
    RecognitionPack.install(c);
    VoicePack.install(c);
    ClonePack.install(c);
    for(String language:new String[]{"ja","zh","ko"}){
     State.setup("Đang tải bộ dịch "+(language.equals("ja")?"Nhật":language.equals("zh")?"Trung":"Hàn")+" → Việt…");
     Translator t=translator(language);try{Tasks.await(t.downloadModelIfNeeded(new DownloadConditions.Builder().build()),15,TimeUnit.MINUTES);}finally{t.close();}
    }
    State.setup("Đã tải đủ bộ offline Nhật / Trung / Hàn và giọng Việt. Bấm Bắt đầu để dùng.");
   }catch(Exception e){State.setup("Tải chưa xong: "+State.error(e)+". Bấm tải lại để tiếp tục.");}
   finally{State.downloading=false;State.changed();}
  },"models").start();
 }
 private static void downloadFile(Context c,int i)throws Exception{
  fetch(file(c,i),BASE+NAMES[i],SIZES[i],HASH[i],"Bộ nghe "+(i+1)+"/3");
 }
 static void fetch(File dst,String url,long size,String expectedHash,String label)throws Exception{
  File part=new File(dst+".part");

  if(dst.length()==size&&hash(dst).equals(expectedHash))return;
  if(part.length()>=size)part.delete();long offset=part.length();
  HttpURLConnection conn=(HttpURLConnection)new URL(url).openConnection();conn.setConnectTimeout(30000);conn.setReadTimeout(45000);
  if(offset>0)conn.setRequestProperty("Range","bytes="+offset+"-");
  try{
   int code=conn.getResponseCode();if(code!=200&&code!=206)throw new IOException("Tải mô hình: HTTP "+code);
   if(code==200)offset=0;
   if(code==206&&(conn.getHeaderField("Content-Range")==null||!conn.getHeaderField("Content-Range").startsWith("bytes "+offset+"-")))throw new IOException("Sai gói tải tiếp");
   long done=offset,last=0;
   try(InputStream in=new BufferedInputStream(conn.getInputStream());FileOutputStream out=new FileOutputStream(part,offset>0)){
    byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){done+=n;if(done>size)throw new IOException("Sai dung lượng");out.write(b,0,n);if(System.currentTimeMillis()-last>800){last=System.currentTimeMillis();State.setup(label+": "+(done*100/size)+"% · "+(done/1048576)+" MB\nGiữ app mở. Tải ngắt có thể tiếp tục.");}}
   }
   if(part.length()!=size)throw new IOException("Tải gián đoạn");
   if(!hash(part).equals(expectedHash)){part.delete();throw new IOException("Kiểm tra dữ liệu thất bại");}
   if(dst.exists()&&!dst.delete())throw new IOException("Không thay được mô hình");if(!part.renameTo(dst))throw new IOException("Không lưu được mô hình");
  }finally{conn.disconnect();}
 }
 static String hash(File f)throws Exception{
  MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}
  StringBuilder s=new StringBuilder();for(byte b:md.digest())s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();
 }
}
