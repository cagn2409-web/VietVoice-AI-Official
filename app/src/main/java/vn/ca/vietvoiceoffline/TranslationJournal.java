package vn.ca.vietvoiceoffline;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

/** App-private, bounded journal. Saved corrections are separate from history. */
final class TranslationJournal extends SQLiteOpenHelper {
 private static TranslationJournal instance;
 static synchronized TranslationJournal get(Context c) {
  if (instance == null) instance = new TranslationJournal(c.getApplicationContext());
  return instance;
 }
 private TranslationJournal(Context c) { super(c, "translations.db", null, 1); }
 @Override public void onCreate(SQLiteDatabase db) {
  db.execSQL("CREATE TABLE history (id INTEGER PRIMARY KEY AUTOINCREMENT, time INTEGER NOT NULL, language TEXT NOT NULL, source TEXT NOT NULL, initial TEXT NOT NULL, translation TEXT NOT NULL, engine TEXT NOT NULL)");
  db.execSQL("CREATE TABLE memory (id INTEGER PRIMARY KEY AUTOINCREMENT, language TEXT NOT NULL, source TEXT NOT NULL, translation TEXT NOT NULL, UNIQUE(language,source))");
 }
 @Override public void onUpgrade(SQLiteDatabase db, int old, int next) { }
 static final class Entry {
  final long id, time; final String language, source, initial, translation, engine;
  Entry(long i,long t,String l,String s,String a,String v,String e) {id=i;time=t;language=l;source=s;initial=a;translation=v;engine=e;}
 }
 synchronized long add(String language,String source,String translation,String engine) {
  ContentValues v=new ContentValues();v.put("time",System.currentTimeMillis());v.put("language",language);v.put("source",source);v.put("initial",translation);v.put("translation",translation);v.put("engine",engine);
  SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
  try {long id=db.insertOrThrow("history",null,v);db.execSQL("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY id DESC LIMIT 1000)");db.setTransactionSuccessful();return id;} finally {db.endTransaction();}
 }
 synchronized String lookup(String language,String source) {
  try(Cursor c=getReadableDatabase().query("memory",new String[]{"translation"},"language=? AND source=?",new String[]{language,PhraseKey.of(source)},null,null,null)) {return c.moveToFirst()?c.getString(0):null;}
 }
 synchronized void remember(String language,String source,String translation) {
  source=PhraseKey.of(source);translation=translation.trim();
  if(!PhraseKey.validLanguage(language)||source.isEmpty()||translation.isEmpty()||source.length()>2000||translation.length()>4000)throw new IllegalArgumentException("Chọn ngôn ngữ và nhập câu gốc, bản dịch (tối đa 2.000 / 4.000 ký tự).");
  SQLiteDatabase db=getWritableDatabase();
  if(lookup(language,source)==null && count(true)>=1000)throw new IllegalStateException("Đã nhớ 1.000 câu. Xóa bớt câu không dùng trước khi thêm.");
  ContentValues v=new ContentValues();v.put("language",language);v.put("source",source);v.put("translation",translation);if(db.insertWithOnConflict("memory",null,v,SQLiteDatabase.CONFLICT_REPLACE)<0)throw new SQLiteException("Không lưu được câu vào bộ nhớ.");
 }
 synchronized void edit(Entry entry,String translation,boolean remember) {
  translation=translation.trim();if(translation.isEmpty()||translation.length()>4000)throw new IllegalArgumentException("Bản dịch cần từ 1 đến 4.000 ký tự.");
  SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
  try {if(remember)remember(entry.language,entry.source,translation);ContentValues v=new ContentValues();v.put("translation",translation);if(db.update("history",v,"id=?",new String[]{Long.toString(entry.id)})!=1)throw new IllegalStateException("Câu này đã được xóa khỏi lịch sử.");db.setTransactionSuccessful();}finally{db.endTransaction();}
 }

 synchronized void refine(long id,String translation,String engine) {
  translation=translation==null?"":translation.trim();if(id<0||translation.isEmpty()||translation.length()>4000)return;ContentValues v=new ContentValues();v.put("translation",translation);v.put("engine",engine==null?"":engine);getWritableDatabase().update("history",v,"id=?",new String[]{Long.toString(id)});
 }
 synchronized int count(boolean memory) {try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+(memory?"memory":"history"),null)){c.moveToFirst();return c.getInt(0);}}
 synchronized List<Entry> list(boolean memory,String query,int limit) {
  String table=memory?"memory":"history";
  String pattern="%"+query.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
  String[] columns=memory?new String[]{"id","0 AS time","language","source","translation AS initial","translation","'Câu đã nhớ' AS engine"}:new String[]{"id","time","language","source","initial","translation","engine"};
  ArrayList<Entry> entries=new ArrayList<>();
  try(Cursor c=getReadableDatabase().query(table,columns,"source LIKE ? ESCAPE '\\' OR translation LIKE ? ESCAPE '\\'",new String[]{pattern,pattern},null,null,"id DESC",Integer.toString(Math.min(1000,Math.max(1,limit))))) {
   while(c.moveToNext())entries.add(new Entry(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6)));
  } return entries;
 }
 synchronized void deleteMemory(long id) {getWritableDatabase().delete("memory","id=?",new String[]{Long.toString(id)});}
 synchronized void clearHistory() {getWritableDatabase().delete("history",null,null);}
 synchronized String exportText() {
  List<Entry> rows=list(false,"",1000);Collections.reverse(rows);
  java.text.DateFormat date=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ROOT);
  StringBuilder out=new StringBuilder("VietVoice — lịch sử dịch\nThời gian ghi nhận trên điện thoại; không phải mốc thời gian của video.\n\n");
  for(Entry e:rows)out.append(date.format(new Date(e.time))).append(" · ").append(e.language).append(" → vi\n").append(e.source).append("\n→ ").append(e.translation).append("\n\n");
  return out.toString();
 }
}
