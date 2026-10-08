package vn.ca.vietvoiceoffline;
final class HyPrompt {
 static String build(String language,String source,String history){
  if(source==null||source.trim().isEmpty())throw new IllegalArgumentException("Thiếu lời gốc");
  if(source.length()>700)throw new IllegalArgumentException("Lời gốc quá dài");
  String s=plain(source.trim());String h=history==null?"":plain(history.trim());
  if(h.length()>900){h=h.substring(h.length()-900);int nl=h.indexOf('\n');if(nl>=0)h=h.substring(nl+1);}
  if(!h.isEmpty())return h+"\n参考上面的信息，把下面的文本翻译成越南语，注意不需要翻译上文，也不要额外解释：\n"+s;
  return ("zh".equals(language)?"将以下文本翻译为越南语，注意只需要输出翻译后的结果，不要额外解释：\n\n":"Translate the following segment into Vietnamese, without additional explanation.\n\n")+s;
 }
 private static String plain(String s){return s.replace("<｜","〈｜").replace("｜>","｜〉");}
}
