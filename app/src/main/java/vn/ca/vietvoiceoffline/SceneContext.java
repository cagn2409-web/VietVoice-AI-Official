package vn.ca.vietvoiceoffline;

/** Conservative visual context. Never treated as dialogue; only a hint for disambiguation. */
final class SceneContext {
 static final SceneContext EMPTY=new SceneContext(0,"",0f);
 final long timeMs;
 final String summary;
 final float confidence;
 final byte[] imageJpeg;
 SceneContext(long timeMs,String summary,float confidence){this(timeMs,summary,confidence,null);}
 SceneContext(long timeMs,String summary,float confidence,byte[] imageJpeg){this.timeMs=timeMs;this.summary=summary==null?"":summary.trim();this.confidence=Math.max(0f,Math.min(1f,confidence));this.imageJpeg=imageJpeg;}
 boolean useful(){return !summary.isEmpty()&&confidence>=0.35f;}
 @Override public String toString(){return summary;}
}
