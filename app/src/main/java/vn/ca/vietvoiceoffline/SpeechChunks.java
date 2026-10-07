package vn.ca.vietvoiceoffline;
import java.util.*;
final class SpeechChunks {
 static List<String> split(String text,int limit){List<String> out=new ArrayList<>();StringBuilder part=new StringBuilder();
  for(String word:text.trim().split("\s+")){if(word.isEmpty())continue;if(part.length()>0&&part.length()+1+word.length()>limit){out.add(part.toString());part.setLength(0);}if(part.length()>0)part.append(' ');part.append(word);if(word.matches(".*[.!?。！？]$")){out.add(part.toString());part.setLength(0);}}
  if(part.length()>0)out.add(part.toString());return out;
 }
}
