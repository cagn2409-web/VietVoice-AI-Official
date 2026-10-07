package vn.ca.vietvoiceoffline;
import android.content.*;import android.graphics.Rect;
final class SubtitleRegion {
 static String key(int w,int h){return w>h?"roi_land_":"roi_port_";}
 static Rect get(Context c,int w,int h){SharedPreferences p=c.getSharedPreferences("settings",0);String k=key(w,h);float top=p.getInt("crop",65)/100f;int l=(int)(w*p.getFloat(k+"l",.03f)),t=(int)(h*p.getFloat(k+"t",top)),r=(int)(w*p.getFloat(k+"r",.97f)),b=(int)(h*p.getFloat(k+"b",Math.min(1f,top+.3f)));l=Math.max(0,Math.min(w-1,l));t=Math.max(0,Math.min(h-1,t));return new Rect(l,t,Math.max(l+1,Math.min(w,r)),Math.max(t+1,Math.min(h,b)));}
 static void save(Context c,Rect r,int w,int h){String k=key(w,h);c.getSharedPreferences("settings",0).edit().putFloat(k+"l",r.left/(float)w).putFloat(k+"t",r.top/(float)h).putFloat(k+"r",r.right/(float)w).putFloat(k+"b",r.bottom/(float)h).apply();}
 static void reset(Context c){SharedPreferences.Editor e=c.getSharedPreferences("settings",0).edit();for(String k:new String[]{"roi_land_","roi_port_"})for(String s:new String[]{"l","t","r","b"})e.remove(k+s);e.apply();}
}
