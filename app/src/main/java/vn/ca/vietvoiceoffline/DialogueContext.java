package vn.ca.vietvoiceoffline;
import java.util.*;

/** Recent dialogue only; bounded by turns and characters for predictable on-device prompts. */
final class DialogueContext {
 static final class Turn{final long time;final String source,vi;Turn(long t,String s,String v){time=t;source=s;vi=v;}}
 private final ArrayDeque<Turn> turns=new ArrayDeque<>();
 synchronized void add(long time,String source,String vi){
  source=clean(source,700);vi=clean(vi,900);if(source.isEmpty()||vi.isEmpty())return;
  turns.addLast(new Turn(time,source,vi));while(turns.size()>6)turns.removeFirst();
 }
 synchronized String prompt(long now){
  StringBuilder b=new StringBuilder();
  for(Turn t:turns){if(now-t.time>45000)continue;if(b.length()>0)b.append('\n');b.append("- ").append(t.source).append(" => ").append(t.vi);}
  if(b.length()>2600)b.delete(0,b.length()-2600);return b.toString();
 }
 synchronized void refineLatest(String source,String vi){
  if(source==null||vi==null||turns.isEmpty())return;Turn last=turns.peekLast();if(last!=null&&last.source.equals(clean(source,700))){turns.removeLast();turns.addLast(new Turn(last.time,last.source,clean(vi,900)));}
 }
 synchronized void clear(){turns.clear();}
 private static String clean(String s,int max){if(s==null)return "";s=s.replace('\n',' ').replace('\r',' ').trim();return s.length()>max?s.substring(0,max):s;}
}
