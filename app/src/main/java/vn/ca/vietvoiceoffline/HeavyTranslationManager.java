package vn.ca.vietvoiceoffline;
import android.content.Context;
import java.util.concurrent.*;
/** One inference at a time. Quality mode waits for completion before speaking. */
final class HeavyTranslationManager implements AutoCloseable {
 interface Callback{void done(String result,long elapsedMs,String error);}
 private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"heavy-ai");t.setPriority(Thread.NORM_PRIORITY-1);return t;});
 private HyTranslator hy;private HeavyTranslator lite;
 private final CountDownLatch initialized=new CountDownLatch(1);
 private volatile boolean busy,closed,ready;private volatile String initError="";
 HeavyTranslationManager(Context context){Context c=context.getApplicationContext();executor.execute(()->{
  try{if(HyModelStore.ready(c))hy=new HyTranslator(HyModelStore.file(c).getAbsolutePath());else{lite=new HeavyTranslator(c,HeavyModelStore.model(c).getAbsolutePath());lite.initialize();}ready=!closed;}
  catch(Throwable t){initError=State.error(t);}finally{initialized.countDown();}
 });}
 boolean ready(){return ready&&!closed;}
 String status(){return ready?(hy!=null?"HY-MT chuyên dịch sẵn sàng":"Heavy AI sẵn sàng"):initError.isEmpty()?"Đang nạp bộ chuyên dịch offline":"Không nạp được bộ dịch: "+initError;}
 boolean awaitReady()throws InterruptedException{initialized.await(45,TimeUnit.SECONDS);return ready();}
 private String translate(String language,String source,String fastVi,String scene,String history,byte[] image){return hy!=null?hy.translate(language,source,history):lite.translate(language,source,fastVi,scene,history,image);}
 synchronized boolean refine(String language,String source,String fastVi,String scene,String history,byte[] image,Callback cb){
  if(closed||!ready||busy)return false;busy=true;long start=android.os.SystemClock.elapsedRealtime();
  executor.execute(()->{String out="",err="";try{out=translate(language,source,fastVi,scene,history,image);}catch(Throwable t){err=State.error(t);}finally{busy=false;}if(!closed&&cb!=null)cb.done(out,android.os.SystemClock.elapsedRealtime()-start,err);});return true;
 }
 String translateWaiting(String language,String source,String draft,String history)throws Exception{
  CompletableFuture<String> answer=new CompletableFuture<>();
  if(!refine(language,source,draft,"",history,null,(text,ms,error)->{if(error.isEmpty())answer.complete(text);else answer.completeExceptionally(new IllegalStateException(error));}))throw new IllegalStateException(status());
  // Native inference aborts at 18 s and rejects incomplete output. The UI thread never waits.
  return answer.get(20,TimeUnit.SECONDS);
 }
 @Override public synchronized void close(){if(closed)return;closed=true;ready=false;executor.execute(()->{if(hy!=null)hy.close();if(lite!=null)lite.close();});executor.shutdown();}
}
