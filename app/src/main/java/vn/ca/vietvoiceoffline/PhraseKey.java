package vn.ca.vietvoiceoffline;

import java.text.Normalizer;

/** Exact phrase memory: keep punctuation and case because they may change meaning. */
final class PhraseKey {
 static String of(String text) {
  return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFC)
    .replaceAll("[\\s\\p{Z}]+", " ").trim();
 }
 static boolean validLanguage(String language) {
  return "ja".equals(language) || "zh".equals(language) || "ko".equals(language);
 }
}
