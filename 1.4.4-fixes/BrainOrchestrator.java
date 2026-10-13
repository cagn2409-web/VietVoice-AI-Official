package vn.ca.vietvoiceoffline;
import android.content.*;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import java.util.Locale;
/** Safe, deterministic action dispatcher under the optional large local model. */
final class BrainOrchestrator {
 static final class Reply {final String message,action;Reply(String m,String a){message=m;action=a;}}
 static String diagnostics(Context c){
  boolean online=false;
  try{ConnectivityManager cm=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);NetworkCapabilities n=cm.getNetworkCapabilities(cm.getActiveNetwork());online=n!=null&&n.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);}catch(Exception ignored){}
  return "Mạng="+(online?"có":"mất hoặc chưa xác minh")+"; dịch="+(State.running?"đang chạy":"đã dừng")+"; model="+(HeavyModelStore.ready(c)?"đã tải":"chưa tải")+"; DNS="+c.getSharedPreferences("vv_browser",0).getString("doh_provider","mặc định")+"; lỗi web="+c.getSharedPreferences("vv_browser",0).getString("last_web_error","không có")+"; lỗi player="+c.getSharedPreferences("video_boost",0).getString("last_error", "không có");
 }
 static Reply quick(Context c,String request){
  String s=request.toLowerCase(Locale.ROOT).trim();
  android.content.SharedPreferences settings=c.getSharedPreferences("settings",0),browser=c.getSharedPreferences("vv_browser",0);
  if((s.contains("bật")||s.contains("mở"))&&(s.contains("lồng tiếng")||s.contains("giọng việt"))){settings.edit().putBoolean("speak",true).apply();return new Reply("Đã bật đọc tiếng Việt cho phiên dịch. Nếu chưa chạy, bấm Bắt đầu dịch và chấp thuận quyền của Android.",null);}
  if((s.contains("tắt")||s.contains("không"))&&(s.contains("lồng tiếng")||s.contains("giọng việt"))){settings.edit().putBoolean("speak",false).apply();return new Reply("Đã tắt đọc tiếng Việt ở cấu hình. Phụ đề vẫn có thể được dịch.",null);}
  if(s.contains("bắt đầu")&&(s.contains("dịch")||s.contains("lồng tiếng")))return new Reply("Mình sẽ mở bước cấp quyền ghi màn hình/âm thanh của Android; bạn phải tự đồng ý.","start_translation");
  if((s.contains("dừng")||s.contains("ngừng"))&&(s.contains("dịch")||s.contains("lồng tiếng"))){c.stopService(new Intent(c,CaptureService.class));return new Reply("Đã gửi yêu cầu dừng phiên dịch.",null);}
  if((s.contains("bật")||s.contains("mở"))&&s.contains("chặn quảng cáo")){browser.edit().putBoolean("ad_block",true).apply();return new Reply("Đã bật lọc tên miền quảng cáo. Tải lại trang để áp dụng; một số quảng cáo nội tuyến vẫn có thể xuất hiện.","reload");}
  if(s.contains("tắt")&&s.contains("chặn quảng cáo")){browser.edit().putBoolean("ad_block",false).apply();return new Reply("Đã tắt chặn quảng cáo, tải lại trang để áp dụng.","reload");}
  if(s.contains("tua")&&(s.contains("lùi")||s.contains("về")||s.contains("trước")||s.contains("-10")))return new Reply("Sẵn sàng tua lùi 10 giây (nếu video hỗ trợ).","seek_back");
  if(s.contains("tua")&&(s.contains("tiến")||s.contains("tới")||s.contains("nhanh")||s.contains("+10")))return new Reply("Sẵn sàng tua tiến 10 giây (nếu video hỗ trợ).","seek_forward");
  if(s.contains("xoay")||s.contains("ngang"))return new Reply("Sẵn sàng xoay video sang chế độ ngang.","rotate");
  if(s.contains("toàn màn")||s.contains("full screen"))return new Reply("Sẵn sàng mở toàn màn hình nếu trình phát hỗ trợ.","fullscreen");
  if(s.contains("1080")||s.contains("full hd"))return new Reply("1080p chỉ có thể chọn khi nguồn video cung cấp chất lượng đó. Bộ phát sẽ ưu tiên mức tối đa 1080p.","fullhd");
  if(s.contains("mở")&&(s.contains("trình duyệt")||s.contains("web")))return new Reply("Sẵn sàng mở VietVoice Browser.","browser");
  if(s.contains("lỗi")||s.contains("đứng")||s.contains("không xem")||s.contains("mất tiếng")||s.contains("mạng"))return new Reply("Kết quả kiểm tra:\n"+diagnostics(c)+"\n\nBạn có thể thử tải lại trang hoặc khởi động lại trình phát, giữ nguyên vị trí video. Nếu trang báo ERR_CONNECTION_CLOSED hãy thử DNS hệ thống và kiểm tra cùng URL trong Chrome. Không xóa cache khi chưa hỏi.","reload");
  return null;
 }
 private BrainOrchestrator(){}
}