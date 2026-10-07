package vn.ca.vietvoiceoffline;
import org.junit.Test;
import static org.junit.Assert.*;
public class HeavyPromptTest{
 @Test public void promptForbidsInventedDialogue(){String p=HeavyPrompt.build("ja","もういい","Được rồi","1 khuôn mặt; đầu cúi thấp","- trước => câu trước");assertTrue(p.contains("tuyệt đối không thêm"));assertTrue(p.contains("CÂU GỐC"));}
 @Test public void cleansWrapper(){assertEquals("Thôi, đủ rồi.",HeavyPrompt.cleanResponse("Bản dịch cuối: “Thôi, đủ rồi.”"));}
}
