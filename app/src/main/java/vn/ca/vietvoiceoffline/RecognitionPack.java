package vn.ca.vietvoiceoffline;
import android.content.Context;import java.io.File;
final class RecognitionPack {
 static final String BASE="https://huggingface.co/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/resolve/2365baeacb507f821a0c8120fcee3d484dba7a07/";
 static final String[] NAMES={"model.int8.onnx","tokens.txt"};static final long[] SIZES={239233841,315894};
 static final String[] HASH={"c71f0ce00bec95b07744e116345e33d8cbbe08cef896382cf907bf4b51a2cd51","f449eb28dc567533d7fa59be34e2abca8784f771850c78a47fb731a31429a1dc"};
 static File file(Context c,int i){File d=new File(c.getFilesDir(),"sensevoice");d.mkdirs();return new File(d,NAMES[i]);}
 static boolean enhanced(Context c){return c.getSharedPreferences("settings",0).getInt("asrEngineV2",0)==0;}
 static boolean ready(Context c){if(!enhanced(c))return ModelStore.asrPresent(c);for(int i=0;i<2;i++)if(file(c,i).length()!=SIZES[i])return false;return true;}
 static void install(Context c)throws Exception{for(int i=0;i<2;i++)ModelStore.fetch(file(c,i),BASE+NAMES[i],SIZES[i],HASH[i],"AI lời nói + cảm xúc "+(i+1)+"/2");}
 static String emotion(String raw){String e=raw.toUpperCase(java.util.Locale.ROOT);if(e.contains("HAPPY"))return "vui";if(e.contains("SAD"))return "buồn";if(e.contains("ANGRY"))return "giận";if(e.contains("FEAR"))return "sợ";if(e.contains("SURPRISE"))return "ngạc nhiên";if(e.contains("DISGUST"))return "khó chịu";if(e.contains("NEUTRAL"))return "trung tính";return "chưa rõ";}
}
