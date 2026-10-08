package vn.ca.vietvoiceoffline;
import org.junit.Test;
import static org.junit.Assert.*;
public class SpecialistOutputTest {
 @Test public void rejectsObservedContextLeak(){assertFalse(SpecialistOutput.acceptable("ja","もういい。","Được rồi.","Đủ rồi. Hai người đang cãi vã, và một người đang cố gắng chấm dứt cuộc trò chuyện này."));}
 @Test public void acceptsShortNaturalTranslation(){assertTrue(SpecialistOutput.acceptable("ja","もういい。","Được rồi.","Thôi, đủ rồi."));}
 @Test public void rejectsCopiedHistory(){assertFalse(SpecialistOutput.acceptable("ja","はい","Vâng","はい => Vâng"));}
}
