package vn.ca.vietvoiceoffline;
import java.util.*;

/** Keeps only a short, de-duplicated scene window so old visual context cannot leak into new dialogue. */
final class SceneMemory {
 private final ArrayDeque<SceneContext> items=new ArrayDeque<>();
 synchronized void add(SceneContext c){
  if(c==null||!c.useful())return;
  SceneContext last=items.peekLast();
  if(last!=null&&last.summary.equals(c.summary)){items.removeLast();}
  items.addLast(c);
  while(items.size()>5)items.removeFirst();
 }
 synchronized SceneContext latest(long now){
  SceneContext c=items.peekLast();
  return c!=null&&now-c.timeMs<=7000?c:SceneContext.EMPTY;
 }
 synchronized byte[] latestImage(long now){SceneContext c=latest(now);return c.imageJpeg;}
 synchronized String prompt(long now){
  StringBuilder out=new StringBuilder();
  for(SceneContext c:items){
   long age=now-c.timeMs;if(age<0||age>9000)continue;
   if(out.length()>0)out.append(" | ");out.append(c.summary);
  }
  return out.toString();
 }
 synchronized void clear(){items.clear();}
}
