package vn.ca.vietvoiceoffline;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReviewPipelineTest {
 @Test public void memoryMatchesLineWrappingAndUnicodeComposition(){assertEquals(PhraseKey.of(" 안녕\n 친구 "),PhraseKey.of("안녕\u00a0친구"));assertEquals(PhraseKey.of("Tiếng Việt"),PhraseKey.of("Tiếng Việt"));}
 @Test public void memoryDoesNotEraseMeaningfulPunctuationOrCase(){assertNotEquals(PhraseKey.of("いい？"),PhraseKey.of("いい。"));assertNotEquals(PhraseKey.of("US"),PhraseKey.of("us"));assertNotEquals(PhraseKey.of("잘 가"),PhraseKey.of("잘가"));}
 @Test public void memoryIsRestrictedToConfiguredLanguages(){assertTrue(PhraseKey.validLanguage("ja"));assertTrue(PhraseKey.validLanguage("zh"));assertTrue(PhraseKey.validLanguage("ko"));assertFalse(PhraseKey.validLanguage("vi"));assertFalse(PhraseKey.validLanguage(null));}
 @Test public void oldOrMissingSubtitlesNeverCoverAudio(){HybridFusion f=new HybridFusion();assertFalse(f.covers(1000,4000));f.observe("字幕",1000);assertFalse(f.covers(1000,4000));f.observe("",4000);assertFalse(f.covers(1000,4000));}
 @Test public void continuousSubtitlesCoverAudioButGapsDoNot(){HybridFusion f=new HybridFusion();for(int t=1000;t<=4000;t+=500)f.observe("字幕",t);assertTrue(f.covers(1000,4000));assertFalse(f.covers(1000,5000));assertFalse(f.covers(3000,3000));HybridFusion gaps=new HybridFusion();gaps.observe("第一",1000);gaps.observe("第二",3500);gaps.observe("第二",4000);assertFalse(gaps.covers(1000,4000));}
 @Test public void adjacentCuesCoverWithoutDoubleCountingOverlap(){HybridFusion f=new HybridFusion();f.observe("第一",1000);f.observe("第一",1500);f.observe("第二",2000);f.observe("第二",2500);assertTrue(f.covers(1000,2500));assertFalse(f.covers(0,2500));}
 @Test public void subtitleFirstStillChecksPeriodicallyAndFallsBackImmediately(){AsrSchedule s=new AsrSchedule();assertTrue(s.shouldRecognize(true,true,1000));assertFalse(s.shouldRecognize(true,true,4000));assertTrue(s.shouldRecognize(true,false,5000));assertFalse(s.shouldRecognize(true,true,8000));assertTrue(s.shouldRecognize(true,true,20000));}
 @Test public void disablingOptimizationKeepsEveryAudioSegment(){AsrSchedule s=new AsrSchedule();assertTrue(s.shouldRecognize(false,true,1000));assertTrue(s.shouldRecognize(false,true,2000));assertTrue(s.shouldRecognize(true,true,500));}
}
