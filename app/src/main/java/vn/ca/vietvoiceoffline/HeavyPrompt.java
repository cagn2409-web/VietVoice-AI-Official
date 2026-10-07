package vn.ca.vietvoiceoffline;

/** Prompt contract for the local LLM. Kept pure-Java for unit testing. */
final class HeavyPrompt {
 static String build(String language,String source,String fastVi,String scene,String history){
  String lang="ja".equals(language)?"tiếng Nhật":"zh".equals(language)?"tiếng Trung":"tiếng Hàn";
  return "Bạn là AI dịch phim "+lang+" sang tiếng Việt.\n"
   +"QUY TẮC BẮT BUỘC:\n"
   +"1) Chỉ trả về đúng MỘT bản dịch tiếng Việt, không giải thích, không tiêu đề, không ngoặc kép.\n"
   +"2) Giữ đúng ý lời thoại; tuyệt đối không thêm hành động, tên, cảm xúc hay thông tin không có trong câu gốc.\n"
   +"3) Ngữ cảnh hình ảnh chỉ dùng để gỡ đại từ, sắc thái hoặc nghĩa mơ hồ. Nếu không chắc thì bỏ qua ngữ cảnh hình ảnh.\n"
   +"4) Ưu tiên tiếng Việt tự nhiên như phụ đề phim, giữ mức lịch sự/xưng hô nhất quán với các câu gần đây.\n"
   +"5) Bản dịch nhanh chỉ là tham khảo; sửa nếu sai nghĩa hoặc cứng.\n\n"
   +"NGỮ CẢNH CẢNH (có thể sai): "+safe(scene,1200)+"\n"
   +"HỘI THOẠI GẦN ĐÂY:\n"+safe(history,2800)+"\n\n"
   +"CÂU GỐC: "+safe(source,2200)+"\n"
   +"BẢN DỊCH NHANH: "+safe(fastVi,3000)+"\n"
   +"BẢN DỊCH CUỐI:";
 }
 static String cleanResponse(String text){
  if(text==null)return "";String s=text.trim();
  s=s.replaceFirst("(?i)^\\s*(bản dịch( cuối)?|translation)\\s*[:：-]\\s*","").trim();
  if((s.startsWith("\"")&&s.endsWith("\""))||(s.startsWith("“")&&s.endsWith("”")))s=s.substring(1,s.length()-1).trim();
  int fence=s.indexOf("```");if(fence>=0)s=s.replace("```","").trim();
  if(s.length()>4000)s=s.substring(0,4000).trim();return s;
 }
 private static String safe(String s,int max){if(s==null||s.trim().isEmpty())return "(không có)";s=s.replace('\r',' ').trim();return s.length()>max?s.substring(0,max):s;}
}
