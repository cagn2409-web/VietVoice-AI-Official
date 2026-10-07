package vn.ca.vietvoiceoffline;
import org.junit.Test;import static org.junit.Assert.*;
public class AudioDecisionTest {
 @Test public void pureLaughIsNotSpeech(){float[][] s=new float[3][521];for(float[] f:s){f[13]=.9f;f[0]=.04f;}assertTrue(AudioDecision.decide(s).nonverbal);assertEquals("tiếng cười",AudioDecision.decide(s).label);}
 @Test public void mixedSpeechAndCryingIsKept(){float[][] s=new float[3][521];for(float[] f:s)f[19]=.9f;s[1][0]=.8f;assertFalse(AudioDecision.decide(s).nonverbal);}
 @Test public void uncertaintyIsNeverDiscarded(){float[][] s=new float[2][521];s[0][33]=.5f;s[1][33]=.5f;assertFalse(AudioDecision.decide(s).nonverbal);}
 @Test public void whisperSpeechIsKept(){float[][] s=new float[2][521];for(float[] f:s){f[12]=.8f;f[36]=.9f;}assertFalse(AudioDecision.decide(s).nonverbal);}
}
