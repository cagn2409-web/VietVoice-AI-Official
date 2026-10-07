package vn.ca.vietvoiceoffline;
/** Conservative thresholds: retain speech/mixed/uncertain segments. Scores are not calibrated probabilities. */
final class AudioDecision {
 final boolean nonverbal;final String label;
 AudioDecision(boolean n,String l){nonverbal=n;label=l;}
 static final int[] SPEECH={0,1,2,3,5,6,9,10,12,24,25,27,28,29,30,31};
 static final int[] EVENTS={11,13,14,15,16,17,18,19,20,21,22,23,33,34,36,37,38,39,40,41,42,43};
 static AudioDecision decide(float[][] windows){
  if(windows.length==0)return new AudioDecision(false,"");int pure=0;float peakSpeech=0,best=0;int event=-1;
  for(float[] scores:windows){float speech=0,non=0;int idx=-1;for(int i:SPEECH)speech=Math.max(speech,scores[i]);for(int i:EVENTS)if(scores[i]>non){non=scores[i];idx=i;}
   peakSpeech=Math.max(peakSpeech,speech);if(non>=.65f&&speech<.2f)pure++;if(non>best){best=non;event=idx;}
  }
  boolean skip=peakSpeech<.35f&&pure>=Math.ceil(windows.length*.8);
  return new AudioDecision(skip,best>=.45f?label(event):"");
 }
 private static String label(int i){if(i>=13&&i<=18)return "tiếng cười";if(i>=19&&i<=21)return "tiếng khóc";if(i==22||i==33||i==34)return "tiếng rên / âm thanh không lời";if(i==23)return "tiếng thở dài";if(i==11)return "tiếng hét";if(i>=36&&i<=41)return "tiếng thở";return "âm thanh không lời";}
}
