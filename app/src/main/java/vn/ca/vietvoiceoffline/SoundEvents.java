package vn.ca.vietvoiceoffline;
import android.content.Context;
import java.io.*;
import java.nio.*;
import java.util.*;
import org.tensorflow.lite.Interpreter;
final class SoundEvents implements AutoCloseable {
 private final Interpreter model;private final int samples;
 SoundEvents(Context c)throws IOException{
  byte[] bytes;try(InputStream in=c.getAssets().open("yamnet.tflite");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)out.write(b,0,n);bytes=out.toByteArray();}
  ByteBuffer buf=ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());buf.put(bytes).rewind();Interpreter.Options options=new Interpreter.Options().setNumThreads(2);model=new Interpreter(buf,options);samples=model.getInputTensor(0).numElements();
  if(model.getOutputTensor(0).numElements()!=521)throw new IOException("Sai bộ phân biệt âm thanh");
 }
 AudioDecision inspect(float[] pcm){
  if(pcm.length<8000)return new AudioDecision(false,"");ArrayList<float[]> windows=new ArrayList<>();
  for(int offset=0;offset<pcm.length;offset+=samples){if(offset>0&&pcm.length-offset<samples/2)break;
   ByteBuffer in=ByteBuffer.allocateDirect(samples*4).order(ByteOrder.nativeOrder());for(int j=0;j<samples;j++)in.putFloat(offset+j<pcm.length?pcm[offset+j]:0);in.rewind();
   ByteBuffer out=ByteBuffer.allocateDirect(521*4).order(ByteOrder.nativeOrder());model.run(in,out);out.rewind();float[] score=new float[521];out.asFloatBuffer().get(score);windows.add(score);
  }return AudioDecision.decide(windows.toArray(new float[0][]));
 }
 public void close(){model.close();}
}
