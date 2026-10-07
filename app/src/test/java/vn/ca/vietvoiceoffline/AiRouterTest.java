package vn.ca.vietvoiceoffline;
import org.junit.Test;
import static org.junit.Assert.*;
public class AiRouterTest {
 @Test public void uncoveredAudioAlwaysRuns(){AiRouter r=new AiRouter();assertTrue(r.speech(true,false,1000,3,10,5).run);}
 @Test public void clearSubtitlesSkipUntilPeriodicCheck(){AiRouter r=new AiRouter();assertTrue(r.speech(true,true,1000,0,0,0).run);assertFalse(r.speech(true,true,4000,0,0,0).run);assertTrue(r.speech(true,true,16000,0,0,0).run);}
 @Test public void heatExtendsPeriodicInterval(){AiRouter r=new AiRouter();assertTrue(r.speech(true,true,1000,0,0,4).run);assertFalse(r.speech(true,true,20000,0,0,4).run);assertTrue(r.speech(true,true,25000,0,0,4).run);}
 @Test public void translationEwmaUpdates(){AiRouter r=new AiRouter();r.translated(1000);r.translated(2000);assertEquals(1250.0,r.averageTranslateMs(),0.01);}
}
