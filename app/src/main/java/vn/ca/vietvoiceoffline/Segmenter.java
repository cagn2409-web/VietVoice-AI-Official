package vn.ca.vietvoiceoffline;
import java.util.Arrays;
/** Input: 20ms / 320 samples at 16kHz. Six-second maximum, 600ms silence endpoint. */
final class Segmenter {
 interface Sink{void accept(float[] pcm);}
 private final Sink sink;private final float[] samples,pre=new float[3200];private final int pauseSamples;
 private int count,quiet,voices,preCount;private boolean heardNeural;
 Segmenter(Sink s){this(s,false);}
 Segmenter(Sink s,boolean live){sink=s;samples=new float[live?48000:96000];pauseSamples=live?5760:9600;}
 static double rms(short[] b,int n){double sum=0;for(int i=0;i<n;i++){double v=b[i]/32768.0;sum+=v*v;}return Math.sqrt(sum/Math.max(n,1));}
 void add(short[] b,int n){add(b,n,null);}
 void add(short[] b,int n,Boolean neural){
  boolean speech=rms(b,n)>0.009||Boolean.TRUE.equals(neural);
  if(count==0&&!speech){int keep=Math.min(preCount,pre.length-n);if(keep>0)System.arraycopy(pre,preCount-keep,pre,0,keep);for(int i=0;i<n;i++)pre[keep+i]=b[i]/32768f;preCount=keep+n;return;}
  if(count==0){System.arraycopy(pre,0,samples,0,preCount);count=preCount;preCount=0;}
  int take=Math.min(n,samples.length-count);for(int i=0;i<take;i++)samples[count++]=b[i]/32768f;
  if(Boolean.TRUE.equals(neural))heardNeural=true;
  if(speech)voices+=take;
  boolean continuing=heardNeural&&neural!=null?neural:speech;
  if(continuing)quiet=0;else quiet+=take;
  if(quiet>=pauseSamples||count==samples.length){if(voices>=4800)sink.accept(Arrays.copyOf(samples,count));count=quiet=voices=preCount=0;heardNeural=false;}
 }
}
