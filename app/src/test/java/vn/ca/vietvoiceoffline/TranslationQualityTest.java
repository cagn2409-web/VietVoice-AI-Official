package vn.ca.vietvoiceoffline;
import org.junit.Test;
import static org.junit.Assert.*;
public class TranslationQualityTest {
 @Test public void identicalForeignTextIsSuspicious(){assertTrue(TranslationQuality.suspicious("ja","こんにちは","こんにちは"));assertTrue(TranslationQuality.suspicious("ko","안녕하세요","안녕하세요"));}
 @Test public void normalVietnamesePasses(){assertFalse(TranslationQuality.suspicious("ja","こんにちは","Xin chào"));assertFalse(TranslationQuality.suspicious("zh","你好吗？","Bạn khỏe không?"));}
 @Test public void retrySourceCleansOcrNoise(){assertEquals("こんにちは。",TranslationQuality.retrySource(" ◆  こんにちは。  | "));}
}
