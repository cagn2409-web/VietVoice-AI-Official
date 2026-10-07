package vn.ca.vietvoiceoffline;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class SegmenterTest{
 @Test public void ignoresSilenceAndBriefClicks(){List<float[]> chunks=new ArrayList<>();Segmenter s=new Segmenter(chunks::add);short[] silent=new short[320],loud=new short[320];Arrays.fill(loud,(short)6000);for(int i=0;i<100;i++)s.add(silent,320);s.add(loud,320);for(int i=0;i<40;i++)s.add(silent,320);assertEquals(0,chunks.size());}
 @Test public void endsUtteranceAfterPause(){List<float[]> chunks=new ArrayList<>();Segmenter s=new Segmenter(chunks::add);short[] b=new short[320];Arrays.fill(b,(short)6000);for(int i=0;i<50;i++)s.add(b,320);for(int i=0;i<30;i++)s.add(new short[320],320);assertEquals(1,chunks.size());assertEquals(25600,chunks.get(0).length);}
 @Test public void capsContinuousSpeechAtSixSeconds(){List<float[]> chunks=new ArrayList<>();Segmenter s=new Segmenter(chunks::add);short[] b=new short[320];Arrays.fill(b,(short)6000);for(int i=0;i<650;i++)s.add(b,320);assertEquals(2,chunks.size());for(float[] pcm:chunks)assertTrue(pcm.length<=96000);}
}
