package vn.ca.vietvoiceoffline;
import android.content.Context;
import com.k2fsa.sherpa.onnx.*;
/** VAD only adjusts endpoints; it never discards nonverbal sound events. */
final class SpeechBoundary implements AutoCloseable {
 private final Vad vad;private final float[] window=new float[512];private int at;private boolean speech;
 SpeechBoundary(Context c){SileroVadModelConfig model=new SileroVadModelConfig();model.setModel("silero_vad.onnx");model.setWindowSize(512);VadModelConfig config=new VadModelConfig();config.setSileroVadModelConfig(model);config.setSampleRate(16000);config.setNumThreads(1);config.setProvider("cpu");vad=new Vad(c.getAssets(),config);}
 boolean inspect(short[] pcm,int n){for(int i=0;i<n;i++){window[at++]=pcm[i]/32768f;if(at==window.length){speech=vad.compute(window)>=.5f;at=0;}}return speech;}
 public void close(){vad.release();}
}
