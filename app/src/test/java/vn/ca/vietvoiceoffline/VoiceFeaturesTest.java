package vn.ca.vietvoiceoffline;
import org.junit.Test;
import static org.junit.Assert.*;
public class VoiceFeaturesTest {
 @Test public void silenceSpectrogramHasEpsilonFloor(){float[][] s=VoiceFeatures.spectrogram(new float[4096]);assertEquals(16,s.length);for(float[] row:s)for(float f:row)assertEquals(.001f,f,1e-7f);}
 @Test public void periodicHannPreservesKnownTone(){float[] x=new float[4096];for(int i=0;i<x.length;i++)x[i]=(float)Math.sin(2*Math.PI*20*i/1024);float[][] s=VoiceFeatures.spectrogram(x);assertEquals(256f,s[3][20],.001f);assertEquals(128f,s[3][19],.001f);assertTrue(s[3][25]<.002f);}
 @Test public void resamplingPreservesDuration(){float[] x=new float[16000];java.util.Arrays.fill(x,.2f);float[] y=VoiceFeatures.resample(x,16000,22050);assertEquals(22050,y.length);assertEquals(.2f,y[22049],1e-6f);}
 @Test public void energyNeverAmplifiesIntoClipping(){float[] x=new float[2000],r=new float[3000];java.util.Arrays.fill(x,.95f);for(int i=1500;i<r.length;i++)r[i]=.8f;VoiceFeatures.applyEnergy(x,r);for(float f:x){assertTrue(Float.isFinite(f));assertTrue(Math.abs(f)<=.98f);}assertTrue(x[1900]>x[100]);}
}
