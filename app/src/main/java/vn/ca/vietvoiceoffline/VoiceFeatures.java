package vn.ca.vietvoiceoffline;
/** DSP matching OpenVoice's periodic Hann STFT, reflect padding, no log. */
final class VoiceFeatures {
 static float[] resample(float[] x,int from,int to){
  if(from==to)return x;int n=(int)((long)x.length*to/from);float[] y=new float[n];
  for(int i=0;i<n;i++){double p=(double)i*from/to;int j=(int)p;double f=p-j;y[i]=(float)(x[Math.min(j,x.length-1)]*(1-f)+x[Math.min(j+1,x.length-1)]*f);}return y;
 }
 static float[][] spectrogram(float[] x){
  if(x.length<1024)throw new IllegalArgumentException("Mẫu giọng quá ngắn");
  int frames=(x.length+768-1024)/256+1;float[][] out=new float[frames][513];
  double[] re=new double[1024],im=new double[1024];
  for(int t=0;t<frames;t++){
   for(int i=0;i<1024;i++){int j=t*256+i-384;if(j<0)j=-j;if(j>=x.length)j=2*x.length-2-j;re[i]=x[j]*(.5-.5*Math.cos(2*Math.PI*i/1024));im[i]=0;}
   fft(re,im);for(int k=0;k<=512;k++)out[t][k]=(float)Math.sqrt(re[k]*re[k]+im[k]*im[k]+1e-6);
  }return out;
 }
 private static void fft(double[] re,double[] im){
  int n=re.length;for(int i=1,j=0;i<n;i++){int bit=n>>1;for(;(j&bit)!=0;bit>>=1)j^=bit;j^=bit;if(i<j){double a=re[i];re[i]=re[j];re[j]=a;}}
  for(int len=2;len<=n;len<<=1){double ar=Math.cos(-2*Math.PI/len),ai=Math.sin(-2*Math.PI/len);for(int base=0;base<n;base+=len){double wr=1,wi=0;for(int j=0;j<len/2;j++){int u=base+j,v=u+len/2;double vr=re[v]*wr-im[v]*wi,vi=re[v]*wi+im[v]*wr;re[v]=re[u]-vr;im[v]=im[u]-vi;re[u]+=vr;im[u]+=vi;double next=wr*ar-wi*ai;wi=wr*ai+wi*ar;wr=next;}}}
 }
 static float rms(float[] x){double sum=0;for(float f:x)sum+=f*f;return (float)Math.sqrt(sum/Math.max(1,x.length));}
 // Transfer only a restrained energy contour. This is not emotion classification.
 static void applyEnergy(float[] output,float[] reference){
  if(output.length==0||reference==null||reference.length<1024)return;
  int bins=24;double[] level=new double[bins];double average=0;
  for(int b=0;b<bins;b++){int a=b*reference.length/bins,z=(b+1)*reference.length/bins;double sum=0;for(int i=a;i<z;i++)sum+=reference[i]*reference[i];level[b]=Math.sqrt(sum/Math.max(1,z-a));average+=level[b];}
  average/=bins;if(average<.003)return;
  for(int i=0;i<output.length;i++){double p=(double)i*(bins-1)/Math.max(1,output.length-1);int j=(int)p;double energy=level[j]*(1-(p-j))+level[Math.min(j+1,bins-1)]*(p-j);double gain=Math.max(.65,Math.min(1.3,.8+.2*energy/average));output[i]=(float)Math.max(-.98,Math.min(.98,output[i]*gain));}
 }
}
