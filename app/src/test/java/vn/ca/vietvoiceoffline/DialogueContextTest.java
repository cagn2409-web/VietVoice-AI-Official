package vn.ca.vietvoiceoffline;
import org.junit.Test;
import static org.junit.Assert.*;
public class DialogueContextTest{
 @Test public void refinedTurnReplacesFastTranslation(){DialogueContext d=new DialogueContext();d.add(1000,"もういい","Được rồi");d.refineLatest("もういい","Thôi, đủ rồi.");assertTrue(d.prompt(2000).contains("Thôi, đủ rồi."));}
}
