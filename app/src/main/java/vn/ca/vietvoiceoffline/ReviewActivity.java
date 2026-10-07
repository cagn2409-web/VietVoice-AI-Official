package vn.ca.vietvoiceoffline;

import android.app.*;
import android.content.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Review stays local; no network, external storage permission, or private app API. */
public class ReviewActivity extends Activity {
 private TranslationJournal journal;private LinearLayout rows;private TextView summary;
 private EditText search;private boolean memory;private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable update=this::reload;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 @Override public void onCreate(Bundle state){super.onCreate(state);journal=TranslationJournal.get(this);memory=state!=null&&state.getBoolean("memory");
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),dp(12));root.setBackgroundColor(0xfff4f7f6);setContentView(root);
  Button back=button(root,"← Về VietVoice",v->finish());
  RadioGroup group=new RadioGroup(this);group.setOrientation(LinearLayout.HORIZONTAL);RadioButton h=new RadioButton(this);h.setId(View.generateViewId());h.setText("Lịch sử");RadioButton m=new RadioButton(this);m.setId(View.generateViewId());m.setText("Câu đã nhớ");group.addView(h);group.addView(m);root.addView(group);group.check(memory?m.getId():h.getId());group.setOnCheckedChangeListener((g,id)->{memory=id==m.getId();reload();});
  summary=label(root,"",13);search=new EditText(this);search.setSingleLine(true);search.setHint("Tìm câu gốc hoặc bản dịch");root.addView(search);
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){handler.removeCallbacks(update);handler.postDelayed(update,250);}public void afterTextChanged(Editable e){}});
  LinearLayout actions=new LinearLayout(this);root.addView(actions);button(actions,"Thêm câu",v->editMemory(null));button(actions,"Xuất TXT",v->export());button(actions,"Xóa lịch sử",v->new AlertDialog.Builder(this).setTitle("Xóa lịch sử đã lưu?").setMessage("Các câu trong bộ nhớ dịch vẫn được giữ.").setNegativeButton("Giữ lại",null).setPositiveButton("Xóa",(d,w)->{journal.clearHistory();reload();}).show());
  ScrollView scroll=new ScrollView(this);rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));reload();
 }
 private TextView label(LinearLayout parent,String text,int size){TextView view=new TextView(this);view.setText(text);view.setTextSize(size);view.setTextColor(0xff102d37);view.setPadding(0,dp(5),0,dp(5));parent.addView(view);return view;}
 private Button button(LinearLayout parent,String text,View.OnClickListener click){Button b=new Button(this);b.setText(text);b.setAllCaps(false);parent.addView(b);b.setOnClickListener(click);return b;}
 private void reload(){if(rows==null||isFinishing())return;rows.removeAllViews();try{
  List<TranslationJournal.Entry> entries=journal.list(memory,search.getText().toString(),100);
  summary.setText(memory?"Chỉ dùng lại khi câu gốc trùng và cùng ngôn ngữ. Câu giống nhau ở ngữ cảnh khác vẫn có thể cần cách dịch khác.":"Lưu tối đa 1.000 câu trên máy. Chạm một câu để sửa và tùy chọn nhớ cách dịch.");
  label(rows,"Hiện "+entries.size()+" kết quả gần nhất · Tổng "+journal.count(memory)+" câu",12);
  java.text.DateFormat date=new java.text.SimpleDateFormat("dd/MM HH:mm",Locale.ROOT);
  for(TranslationJournal.Entry e:entries){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(10),dp(10),dp(10),dp(10));row.setBackgroundColor(0xffffffff);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);rows.addView(row,p);
   label(row,("ja".equals(e.language)?"Nhật":"zh".equals(e.language)?"Trung":"Hàn")+(memory?"":" · "+date.format(new Date(e.time))),12);TextView original=label(row,e.source,15);original.setMaxLines(4);original.setEllipsize(TextUtils.TruncateAt.END);TextView translated=label(row,"→ "+e.translation,17);translated.setMaxLines(5);translated.setEllipsize(TextUtils.TruncateAt.END);
   button(row,memory?"Sửa / quên câu này":"Sửa bản dịch",v->{if(memory)editMemory(e);else editHistory(e);});
  }
 }catch(Exception e){toast("Không đọc được lịch sử: "+State.error(e));}}
 private LinearLayout form(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(8),dp(20),dp(8));return box;}
 private EditText input(LinearLayout box,String hint,String value){EditText field=new EditText(this);field.setHint(hint);field.setText(value);field.setMinLines(2);field.setMaxLines(5);field.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);box.addView(field);return field;}
 private void editHistory(TranslationJournal.Entry entry){LinearLayout box=form();label(box,entry.source,16);EditText vi=input(box,"Bản dịch tiếng Việt",entry.translation);CheckBox remember=new CheckBox(this);remember.setText("Nhớ cho lần sau khi gặp đúng câu gốc này");box.addView(remember);label(box,"Chỉ nhớ câu bạn đã kiểm tra. Không tự sửa các câu khác chứa cùng một từ.",12);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Sửa bản dịch").setView(box).setNegativeButton("Hủy",null).setNeutralButton("Bản ban đầu",null).setPositiveButton("Lưu",null).create();dialog.setOnShowListener(d->{dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v->vi.setText(entry.initial));dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v->{try{String text=vi.getText().toString().trim();journal.edit(entry,text,remember.isChecked());if(State.recordId==entry.id){State.translated=text;State.changed();}dialog.dismiss();reload();}catch(Exception e){vi.setError(State.error(e));}});});dialog.show();
 }
 private void editMemory(TranslationJournal.Entry entry){LinearLayout box=form();Spinner language=new Spinner(this);language.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Tiếng Nhật","Tiếng Trung","Tiếng Hàn"}));String[] codes={"ja","zh","ko"};language.setSelection(entry==null?Math.max(0,Math.min(2,getSharedPreferences("settings",0).getInt("language",0))):Arrays.asList(codes).indexOf(entry.language));box.addView(language);
  EditText src=input(box,"Câu gốc",entry==null?"":entry.source);EditText vi=input(box,"Cách dịch tiếng Việt",entry==null?"":entry.translation);if(entry!=null){language.setEnabled(false);src.setEnabled(false);}label(box,"Khớp toàn câu, cùng ngôn ngữ. Không phải huấn luyện lại AI và không thay từ bên trong các câu khác.",12);
  AlertDialog.Builder b=new AlertDialog.Builder(this).setTitle("Câu đã nhớ").setView(box).setNegativeButton("Hủy",null).setPositiveButton("Lưu",null);if(entry!=null)b.setNeutralButton("Quên câu",(d,w)->{journal.deleteMemory(entry.id);reload();});AlertDialog dialog=b.create();dialog.setOnShowListener(d->dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v->{try{journal.remember(codes[language.getSelectedItemPosition()],src.getText().toString(),vi.getText().toString());dialog.dismiss();reload();}catch(Exception e){vi.setError(State.error(e));}}));dialog.show();
 }
 private void export(){if(journal.count(false)==0){toast("Chưa có lịch sử để xuất.");return;}Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain").putExtra(Intent.EXTRA_TITLE,"VietVoice-lich-su.txt");try{startActivityForResult(intent,20);}catch(ActivityNotFoundException e){toast("Máy chưa có ứng dụng chọn nơi lưu tệp.");}}
 @Override public void onActivityResult(int request,int code,Intent data){super.onActivityResult(request,code,data);if(request==20&&code==RESULT_OK&&data!=null&&data.getData()!=null){android.net.Uri uri=data.getData();new Thread(()->{try(java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new java.io.IOException("Không mở được tệp");out.write(journal.exportText().getBytes(StandardCharsets.UTF_8));runOnUiThread(()->toast("Đã xuất lịch sử. Mốc giờ là lúc ghi nhận, không phải thời gian trong video."));}catch(Exception e){runOnUiThread(()->toast("Chưa xuất được: "+State.error(e)));}},"export-journal").start();}}
 @Override public void onSaveInstanceState(Bundle out){out.putBoolean("memory",memory);super.onSaveInstanceState(out);}
 @Override public void onDestroy(){handler.removeCallbacks(update);super.onDestroy();}
 private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
}
