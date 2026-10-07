package vn.ca.vietvoiceoffline;

/**
 * Lightweight coordinator for the specialist AIs. It does not replace OCR/ASR/translation;
 * it decides when expensive modules should run so the phone can stay close to live video.
 */
final class AiRouter {
 static final class Decision {
  final boolean run;
  final String reason;
  Decision(boolean run,String reason){this.run=run;this.reason=reason;}
 }
 private long lastAsr=Long.MIN_VALUE;
 private double translateMs;

 synchronized Decision speech(boolean subtitleFirst,boolean covered,long now,int queueDepth,int skipped,int thermalStatus){
  if(!subtitleFirst||!covered){lastAsr=now;return new Decision(true,"Speech AI · cần lời nghe");}
  long interval=15000;
  if(thermalStatus>=4)interval=24000;
  else if(queueDepth>=2||translateMs>1800||skipped>=3)interval=20000;
  if(lastAsr==Long.MIN_VALUE||now<lastAsr||now-lastAsr>=interval){lastAsr=now;return new Decision(true,"Speech AI · kiểm tra định kỳ");}
  return new Decision(false,thermalStatus>=4?"Subtitle AI · giảm tải vì máy nóng":queueDepth>=2?"Subtitle AI · giảm tải vì hàng đợi":"Subtitle AI · phụ đề đang đủ rõ");
 }
 synchronized void translated(long elapsedMs){
  if(elapsedMs<0)return;
  translateMs=translateMs==0?elapsedMs:(translateMs*.75+elapsedMs*.25);
 }
 synchronized double averageTranslateMs(){return translateMs;}
}
