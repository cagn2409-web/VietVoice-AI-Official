package vn.ca.vietvoiceoffline;

import java.text.Normalizer;
import java.util.Locale;

/** Conservative quality guard. It never invents a translation; it only flags obvious failures. */
final class TranslationQuality {
 private TranslationQuality(){}
 static boolean suspicious(String language,String source,String vi){
  if(source==null||vi==null)return true;
  String s=source.trim(),v=vi.trim();
  if(s.isEmpty()||v.isEmpty())return true;
  String ns=lettersDigits(s),nv=lettersDigits(v);
  if(ns.length()>3&&ns.equals(nv))return true;
  if(ns.length()>8&&nv.length()<2)return true;
  // A Vietnamese result that still consists largely of the source writing system is often a failed pass.
  int letters=0,foreign=0;
  for(int i=0;i<v.length();){int cp=v.codePointAt(i);i+=Character.charCount(cp);if(Character.isLetter(cp)){letters++;if(sourceScript(language,cp))foreign++;}}
  return letters>=6&&foreign*100/letters>=45;
 }
 static String retrySource(String source){
  if(source==null)return "";
  String s=Normalizer.normalize(source,Normalizer.Form.NFKC)
   .replace('\u00a0',' ').replaceAll("[\\t\\r\\n]+"," ").replaceAll(" {2,}"," ").trim();
  // OCR frequently leaves decorative bullets/separators around a line. Keep meaningful punctuation.
  return s.replaceAll("^[•·|｜◆◇■□▶▷]+\\s*","").replaceAll("\\s*[•·|｜◆◇■□▶▷]+$","").trim();
 }
 static String lettersDigits(String s){
  StringBuilder b=new StringBuilder();String n=Normalizer.normalize(s,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
  for(int i=0;i<n.length();){int cp=n.codePointAt(i);i+=Character.charCount(cp);if(Character.isLetterOrDigit(cp))b.appendCodePoint(cp);}return b.toString();
 }
 static boolean sourceScript(String language,int cp){
  Character.UnicodeScript sc=Character.UnicodeScript.of(cp);
  if("ja".equals(language))return sc==Character.UnicodeScript.HAN||sc==Character.UnicodeScript.HIRAGANA||sc==Character.UnicodeScript.KATAKANA;
  if("zh".equals(language))return sc==Character.UnicodeScript.HAN;
  if("ko".equals(language))return sc==Character.UnicodeScript.HANGUL||sc==Character.UnicodeScript.HAN;
  return false;
 }
}
