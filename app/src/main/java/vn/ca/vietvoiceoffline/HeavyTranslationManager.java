package vn.ca.vietvoiceoffline;
import android.content.Context;
import java.util.concurrent.*;

/** Single-flight LLM queue: if the heavy model is busy, current realtime translation falls back immediately. */
final class HeavyTranslationManager implements AutoCloseable{
 interface Callback{void done(String result,long elapsedMs,String error);}
 private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"heavy-ai");t.setPriority(Thread.NORM_PRIORITY-1);return t;});
 private final HeavyTranslator translator;private volatile boolean busy,closed,ready,initializing=true;private volatile String initError="";
 HeavyTranslationManager(Context c){translator=new HeavyTranslator(c.getApplicationContext(),HeavyModelStore.model(c).getAbsolutePath());executor.execute(()->{try{translator.initialize();ready=!closed;}catch(Throwable t){initError=State.error(t);}finally{initializing=false;if(closed)try{translator.close();}catch(Throwable ignored){}}});}
 boolean ready(){return ready&&!closed;}
 String status(){return ready?"Heavy AI sẵn sàng":initError.isEmpty()?"Heavy AI đang nạp model":"Heavy AI lỗi: "+initError;}
 synchronized boolean refine(String language,String source,String fastVi,String scene,String history,byte[] sceneImage,Callback cb){
  if(closed||!ready||busy)return false;busy=true;long start=android.os.SystemClock.elapsedRealtime();
  executor.execute(()->{String out="",err="";try{out=translator.translate(language,source,fastVi,scene,history,sceneImage);}catch(Throwable t){err=State.error(t);}finally{busy=false;if(closed)try{translator.close();}catch(Throwable ignored){}}if(!closed&&cb!=null)cb.done(out,android.os.SystemClock.elapsedRealtime()-start,err);});return true;
 }
 @Override public synchronized void close(){closed=true;ready=false;executor.shutdownNow();if(!busy&&!initializing)try{translator.close();}catch(Throwable ignored){}}
}
