package vn.ca.vietvoiceoffline;
/** Reject obvious expansions or prompt scaffolding. Not a semantic accuracy guarantee. */
final class SpecialistOutput {
 static boolean acceptable(String language,String source,String draft,String result){
  if(TranslationQuality.suspicious(language,source,result))return false;
  String r=result.trim(),d=draft==null?"":draft.trim();
  int sourceUnits=TranslationQuality.lettersDigits(source).codePointCount(0,TranslationQuality.lettersDigits(source).length());
  if(sourceUnits<=8&&r.length()>60)return false;
  if(!d.isEmpty()&&r.length()>Math.max(90,d.length()*2+30))return false;
  String lower=r.toLowerCase(java.util.Locale.ROOT);
  return !r.contains("=>")&&!r.contains("<｜")&&!lower.contains("translate the following")&&!lower.startsWith("ngữ cảnh:");
 }
}
