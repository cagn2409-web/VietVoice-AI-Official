package vn.ca.vietvoiceoffline;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class LivePipelineTest {
 private short[] loud(){short[] b=new short[320];Arrays.fill(b,(short)6000);return b;}
 @Test public void liveBoundsContinuousAudioToThreeSeconds(){List<float[]> out=new ArrayList<>();Segmenter s=new Segmenter(out::add,true);for(int i=0;i<300;i++)s.add(loud(),320);assertEquals(2,out.size());assertEquals(48000,out.get(0).length);assertEquals(48000,out.get(1).length);}
 @Test public void neuralPauseEndsSpeechEvenOverBackgroundMusic(){List<float[]> out=new ArrayList<>();Segmenter s=new Segmenter(out::add,true);for(int i=0;i<50;i++)s.add(loud(),320,true);for(int i=0;i<17;i++)s.add(loud(),320,false);assertEquals(0,out.size());s.add(loud(),320,false);assertEquals(1,out.size());assertEquals(21760,out.get(0).length);}
 @Test public void nonverbalAudioStillReachesEventClassifier(){List<float[]> out=new ArrayList<>();Segmenter s=new Segmenter(out::add,true);for(int i=0;i<150;i++)s.add(loud(),320,false);assertEquals(1,out.size());}
 @Test public void quietSpeechDetectedByNeuralVadIsPreserved(){List<float[]> out=new ArrayList<>();Segmenter s=new Segmenter(out::add,true);short[] b=new short[320];Arrays.fill(b,(short)100);for(int i=0;i<50;i++)s.add(b,320,true);for(int i=0;i<18;i++)s.add(b,320,false);assertEquals(1,out.size());}
 @Test public void liveSubtitlesNeedNotWaitForRecognizer(){HybridFusion f=new HybridFusion();f.observe("こんにちは",1000);assertEquals("",f.fallback(1399,400).text);assertEquals("こんにちは",f.fallback(1400,400).text);assertEquals("",f.audio("こんにちは",800,3500).text);}
 @Test public void oldSubtitleCannotBeReplayedAfterLongStall(){HybridFusion f=new HybridFusion();f.observe("旧字幕",1000);assertEquals("",f.fallback(8000,400).text);}
 @Test public void voiceChunksKeepWordsAndPunctuation(){String text="Xin chào bạn. Hôm nay chúng ta cùng xem video và học tiếng Việt nhé!";List<String> parts=SpeechChunks.split(text,22);assertEquals(text,String.join(" ",parts));assertEquals("Xin chào bạn.",parts.get(0));for(String part:parts)assertTrue(part.length()<=22);}
 @Test public void blankTextCreatesNoVoiceJob(){assertTrue(SpeechChunks.split("  ",100).isEmpty());}
}
