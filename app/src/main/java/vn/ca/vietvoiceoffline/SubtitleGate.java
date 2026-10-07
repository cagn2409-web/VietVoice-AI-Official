package vn.ca.vietvoiceoffline;
final class SubtitleGate {
 private String candidate="",last="";private int stable,empty;
 boolean isStable(String raw){return stable>=2&&candidate.equals(raw.replaceAll("\\s+"," ").trim());}
 String accept(String raw){
  String text=raw.replaceAll("\\s+"," ").trim();
  if(text.length()<2){candidate="";stable=0;if(++empty>=3)last="";return null;}
  empty=0;if(text.equals(candidate))stable++;else{candidate=text;stable=1;}
  if(stable>=2&&!text.equals(last)){last=text;return text;}return null;
 }
}
