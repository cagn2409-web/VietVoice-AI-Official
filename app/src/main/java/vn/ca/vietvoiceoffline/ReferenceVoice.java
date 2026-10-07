package vn.ca.vietvoiceoffline;
import android.content.Context;
import java.io.*;
final class ReferenceVoice {
 static volatile float[] latest;
 static volatile long latestAt;
 static void update(float[] pcm){latest=pcm;latestAt=android.os.SystemClock.elapsedRealtime();}
 static float[] current(){float[] p=latest;return android.os.SystemClock.elapsedRealtime()-latestAt<=10000?p:null;}
 static synchronized void keep(Context c)throws IOException{
  float[] p=current();if(p==null||p.length<16000||VoiceFeatures.rms(p)<.003f)throw new IOException("Chờ nhân vật nói rõ ít nhất 1 giây rồi bấm lại");
  File file=new File(c.getFilesDir(),"character-voice.dat"),part=new File(file+".part");
  try(DataOutputStream d=new DataOutputStream(new FileOutputStream(part))){d.writeInt(p.length);for(float f:p)d.writeFloat(f);}
  if(!part.renameTo(file))throw new IOException("Không lưu được mẫu giọng");
 }
 static synchronized float[] read(Context c){
  try(DataInputStream d=new DataInputStream(new FileInputStream(new File(c.getFilesDir(),"character-voice.dat")))){
   int n=d.readInt();if(n<16000||n>192000)return null;float[] p=new float[n];for(int i=0;i<n;i++)p[i]=d.readFloat();return p;
  }catch(IOException e){return null;}
 }
 static synchronized void clear(Context c){new File(c.getFilesDir(),"character-voice.dat").delete();}
}
