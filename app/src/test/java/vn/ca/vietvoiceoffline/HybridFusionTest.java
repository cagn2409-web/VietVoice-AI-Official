package vn.ca.vietvoiceoffline;
import org.junit.Test;import static org.junit.Assert.*;
public class HybridFusionTest {
 @Test public void alignedSubtitleWinsAndIsNotRepeated(){HybridFusion f=new HybridFusion();f.observe("こんにちは",1000);f.observe("こんにちは",1500);assertEquals("こんにちは",f.audio("こんにちは",800,2000).text);assertEquals("",f.fallback(4000).text);}
 @Test public void unrelatedOldSubtitleDoesNotReplaceAudio(){HybridFusion f=new HybridFusion();f.observe("昨日",100);assertEquals("今日",f.audio("今日",5000,7000).text);}
 @Test public void fallbackDoesNotWaitForever(){HybridFusion f=new HybridFusion();f.observe("你好",1000);assertEquals("",f.fallback(2000).text);assertEquals("你好",f.fallback(2900).text);assertEquals("",f.audio("你好",800,3000).text);}
 @Test public void successiveCaptionsRemainOrdered(){HybridFusion f=new HybridFusion();f.observe("안녕",1000);f.observe("친구",1700);assertEquals("안녕 친구",f.audio("안녕 친구",900,2200).text);}
 @Test public void regionResetDiscardsOldWords(){HybridFusion f=new HybridFusion();f.observe("旧字幕",1000);f.clear();assertEquals("",f.fallback(5000).text);}
 @Test public void punctuationDoesNotChangeSimilarity(){assertEquals(1,HybridFusion.similarity("你好！","你 好"),0);}
}
