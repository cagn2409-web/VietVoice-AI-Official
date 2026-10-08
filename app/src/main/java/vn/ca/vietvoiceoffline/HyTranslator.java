package vn.ca.vietvoiceoffline;
import java.nio.charset.StandardCharsets;
final class HyTranslator implements AutoCloseable {
 static {System.loadLibrary("vietvoice_hy");}
 private long handle;
 HyTranslator(String path){handle=open(path);}
 synchronized String translate(String lang,String source,String history){
  if(handle==0)throw new IllegalStateException("HY-MT đã đóng");
  return new String(infer(handle,HyPrompt.build(lang,source,history).getBytes(StandardCharsets.UTF_8)),StandardCharsets.UTF_8).trim();
 }
 public synchronized void close(){if(handle!=0){release(handle);handle=0;}}
 private static native long open(String path);
 private static native byte[] infer(long handle,byte[] prompt);
 private static native void release(long handle);
}
