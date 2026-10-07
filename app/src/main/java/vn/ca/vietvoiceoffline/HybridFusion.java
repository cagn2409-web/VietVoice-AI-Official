package vn.ca.vietvoiceoffline;
import java.util.*;
final class HybridFusion {
 static final class Cue{final String text;final long start;long end;boolean read;Cue(String s,long t){text=s;start=t;end=t;}}
 static final class Choice{final String text,reason;Choice(String t,String r){text=t;reason=r;}}
 private final ArrayList<Cue> cues=new ArrayList<>();private Cue current;
 synchronized void clear(){cues.clear();current=null;}
 synchronized boolean observe(String text,long now){cues.removeIf(c->now-c.end>30000);while(cues.size()>40)cues.remove(0);if(text==null||text.trim().length()<2){current=null;return false;}if(current!=null&&current.text.equals(text)&&now-current.end<1400){current.end=now;return false;}current=new Cue(text,now);cues.add(current);return true;}
 synchronized Choice fallback(long now){return fallback(now,1800);}
 synchronized Choice fallback(long now,long wait){StringBuilder s=new StringBuilder();for(Cue c:cues)if(!c.read&&now-c.start>=wait&&now-c.end<=6000){add(s,c.text);c.read=true;}return new Choice(s.toString(),"Phụ đề ổn định · chưa có lời nghe để đối chiếu");}
 synchronized Choice audio(String asr,long start,long end){StringBuilder s=new StringBuilder(),all=new StringBuilder();boolean matched=false;for(Cue c:cues){long overlap=Math.min(end,c.end+500)-Math.max(start,c.start-500);if(overlap<250)continue;matched=true;add(all,c.text);if(!c.read){add(s,c.text);c.read=true;}}if(matched)return new Choice(s.toString(),similarity(all.toString(),asr)>=.55?"Phụ đề + lời nghe tương đồng":"Ưu tiên phụ đề ổn định · lời nghe khác hoặc chưa rõ");return new Choice(asr,"Âm thanh · không có phụ đề cùng thời điểm");}
 /** Require near-complete, recently observed subtitle coverage, not just one old line. */
 synchronized boolean covers(long start,long end){
  if(end<=start||current==null||end-current.end>800)return false;
  long covered=0,until=start;
  for(Cue c:cues){long a=Math.max(start,c.start-250),b=Math.min(end,c.end+250);if(b>a&&b>until){covered+=b-Math.max(a,until);until=b;}}
  return covered>=.90*(end-start);
 }
 static String normalize(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]","");}
 static double similarity(String a,String b){a=normalize(a);b=normalize(b);if(a.isEmpty()||b.isEmpty())return 0;int[] row=new int[b.length()+1];for(int j=0;j<row.length;j++)row[j]=j;for(int i=1;i<=a.length();i++){int prev=row[0];row[0]=i;for(int j=1;j<=b.length();j++){int old=row[j];row[j]=Math.min(Math.min(row[j]+1,row[j-1]+1),prev+(a.charAt(i-1)==b.charAt(j-1)?0:1));prev=old;}}return 1-(double)row[b.length()]/Math.max(a.length(),b.length());}
 private static void add(StringBuilder s,String t){if(s.length()>0)s.append(' ');s.append(t);}
}
