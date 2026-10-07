package vn.ca.vietvoiceoffline;
import android.content.Context;
import java.io.File;
final class ClonePack {
 static final String BASE="https://huggingface.co/TigreGotico/voiceclonnx-openvoice-v2/resolve/34d010c192c97f763207f488f6057fd07fee42ad/";
 static final String[] NAMES={"tone_ref_encoder.onnx","tone_converter.onnx"};
 static final long[] SIZES={3259275,128051288};
 static final String[] HASH={"3dd4918cab90e1acf7fa5c6f7539c27710e7a3cdfba550468c5ea49399178bf7","7d7ee834c230037ead5cd6b64d44fcd842fbab5cd4cf6fe3ab381fad763325e9"};
 static File file(Context c,int i){File d=new File(c.getFilesDir(),"openvoice-v2");d.mkdirs();return new File(d,NAMES[i]);}
 static boolean ready(Context c){for(int i=0;i<2;i++)if(file(c,i).length()!=SIZES[i])return false;return true;}
 static void install(Context c)throws Exception{for(int i=0;i<2;i++)ModelStore.fetch(file(c,i),BASE+NAMES[i],SIZES[i],HASH[i],"Bộ giả giọng "+(i+1)+"/2");}
}
