#include "hy_core.hpp"
#include <iostream>
int main(int argc,char **argv){
 if(argc!=2)return 2;
 try{HyEngine e(argv[1]);
 for(auto prompt:{"Translate the following segment into Vietnamese, without additional explanation.\n\n明日は雨が降るので、傘を持ってきてください。", "将以下文本翻译为越南语，注意只需要输出翻译后的结果，不要额外解释：\n\n别着急，我马上就回来。", "二人が口論し、一人が会話を終わらせようとしている。\n参考上面的信息，把下面的文本翻译成越南语，注意不需要翻译上文，也不要额外解释：\nもういい。"}){
 auto start=std::chrono::steady_clock::now();auto out=e.generate(prompt,60000);if(out.empty())throw std::runtime_error("Empty translation");
 std::cout<<"RESULT: "<<out<<"\nSECONDS: "<<std::chrono::duration<double>(std::chrono::steady_clock::now()-start).count()<<std::endl;
 }
 }catch(const std::exception &e){std::cerr<<e.what()<<std::endl;return 1;}
}
