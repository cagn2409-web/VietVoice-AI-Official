package vn.ca.vietvoiceoffline;
import android.content.Context;
import ai.onnxruntime.*;
import java.nio.*;
import java.util.*;
final class VoiceClone implements AutoCloseable {
 private final OrtEnvironment env;private final OrtSession encoder,converter;
 VoiceClone(Context c)throws Exception{
  if(!ClonePack.ready(c))throw new IllegalStateException("Chưa tải bộ giả giọng");
  for(int i=0;i<2;i++)if(!ModelStore.hash(ClonePack.file(c,i)).equals(ClonePack.HASH[i]))throw new IllegalStateException("Bộ giả giọng hỏng, bấm tải lại");
  env=OrtEnvironment.getEnvironment();env.setTelemetry(false);
  try(OrtSession.SessionOptions options=new OrtSession.SessionOptions()){
   options.setIntraOpNumThreads(2);options.setInterOpNumThreads(1);
   encoder=env.createSession(ClonePack.file(c,0).getAbsolutePath(),options);
   try{converter=env.createSession(ClonePack.file(c,1).getAbsolutePath(),options);}catch(Exception e){encoder.close();throw e;}
  }
 }
 private float[] embedding(float[][] spec)throws Exception{
  float[] flat=new float[spec.length*513];for(int i=0;i<spec.length;i++)System.arraycopy(spec[i],0,flat,i*513,513);
  try(OnnxTensor t=OnnxTensor.createTensor(env,FloatBuffer.wrap(flat),new long[]{1,spec.length,513});OrtSession.Result r=encoder.run(Collections.singletonMap("spec",t))){return ((float[][])r.get(0).getValue())[0].clone();}
 }
 float[] convert(float[] source,float[] reference)throws Exception{
  if(reference.length<22050||VoiceFeatures.rms(reference)<.003f)throw new IllegalArgumentException("Chưa có mẫu giọng rõ ít nhất 1 giây");
  float[][] spec=VoiceFeatures.spectrogram(source);float[] src=embedding(spec),dst=embedding(VoiceFeatures.spectrogram(reference));int frames=spec.length;
  float[] flat=new float[513*frames];for(int t=0;t<frames;t++)for(int f=0;f<513;f++)flat[f*frames+t]=spec[t][f];
  try(OnnxTensor a=OnnxTensor.createTensor(env,FloatBuffer.wrap(flat),new long[]{1,513,frames});
      OnnxTensor length=OnnxTensor.createTensor(env,LongBuffer.wrap(new long[]{frames}),new long[]{1});
      OnnxTensor s=OnnxTensor.createTensor(env,FloatBuffer.wrap(src),new long[]{1,256,1});
      OnnxTensor d=OnnxTensor.createTensor(env,FloatBuffer.wrap(dst),new long[]{1,256,1})){
   Map<String,OnnxTensor> in=new HashMap<>();in.put("spec",a);in.put("spec_lengths",length);in.put("src_g",s);in.put("tgt_g",d);
   try(OrtSession.Result r=converter.run(in)){float[] pcm=((float[][][])r.get(0).getValue())[0][0];for(int i=0;i<pcm.length;i++){if(!Float.isFinite(pcm[i]))throw new IllegalStateException("Âm thanh giả giọng không hợp lệ");pcm[i]=Math.max(-.98f,Math.min(.98f,pcm[i]));}return pcm;}
  }
 }
 public void close(){try{encoder.close();}catch(Exception ignored){}try{converter.close();}catch(Exception ignored){}}
}
