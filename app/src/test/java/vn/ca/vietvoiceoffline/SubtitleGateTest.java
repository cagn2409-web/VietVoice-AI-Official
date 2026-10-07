package vn.ca.vietvoiceoffline;
import org.junit.Test;
import static org.junit.Assert.*;
public class SubtitleGateTest{
 @Test public void waitsForStableText(){SubtitleGate g=new SubtitleGate();assertNull(g.accept("안녕하세요"));assertEquals("안녕하세요",g.accept("안녕하세요"));assertNull(g.accept("안녕하세요"));}
 @Test public void collapsesWhitespaceAndStopsRepeatedVoice(){SubtitleGate g=new SubtitleGate();assertNull(g.accept("你好\n世界"));assertEquals("你好 世界",g.accept("你好  世界"));assertNull(g.accept("你好 世界"));}
 @Test public void requiresStabilityAfterOcrNoise(){SubtitleGate g=new SubtitleGate();assertNull(g.accept("今日は"));assertNull(g.accept("今日わ"));assertNull(g.accept("今日は"));assertEquals("今日は",g.accept("今日は"));}
 @Test public void repeatsAreAllowedAfterDisappearance(){SubtitleGate g=new SubtitleGate();g.accept("안녕");g.accept("안녕");g.accept("");g.accept("");g.accept("");assertNull(g.accept("안녕"));assertEquals("안녕",g.accept("안녕"));}
}
