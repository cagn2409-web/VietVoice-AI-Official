package vn.ca.vietvoiceoffline;
import org.junit.Test;
import static org.junit.Assert.*;
public class SceneMemoryTest{
 @Test public void ignoresOldScene(){SceneMemory m=new SceneMemory();m.add(new SceneContext(1000,"giơ tay",.8f));assertEquals("",m.prompt(12000));}
 @Test public void deduplicatesLatest(){SceneMemory m=new SceneMemory();m.add(new SceneContext(1000,"giơ tay",.8f));m.add(new SceneContext(2000,"giơ tay",.9f));assertEquals("giơ tay",m.prompt(2500));}
}
