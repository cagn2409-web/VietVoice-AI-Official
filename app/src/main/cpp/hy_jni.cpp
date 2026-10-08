#include <jni.h>
#include "hy_core.hpp"
static void fail(JNIEnv *env,const char *s){env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),s);}
extern "C" JNIEXPORT jlong JNICALL Java_vn_ca_vietvoiceoffline_HyTranslator_open(JNIEnv *env,jclass,jstring path){
 const char *p=env->GetStringUTFChars(path,nullptr);if(!p)return 0;std::string name(p);env->ReleaseStringUTFChars(path,p);
 try{return reinterpret_cast<jlong>(new HyEngine(name.c_str()));}catch(const std::exception &e){fail(env,e.what());return 0;}
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_vn_ca_vietvoiceoffline_HyTranslator_infer(JNIEnv *env,jclass,jlong handle,jbyteArray prompt){
 try{auto *engine=reinterpret_cast<HyEngine*>(handle);if(!engine)throw std::runtime_error("HY-MT da dong");
 std::string p(env->GetArrayLength(prompt),'\0');env->GetByteArrayRegion(prompt,0,p.size(),reinterpret_cast<jbyte*>(p.data()));
 auto s=engine->generate(p);auto out=env->NewByteArray(s.size());if(out)env->SetByteArrayRegion(out,0,s.size(),reinterpret_cast<const jbyte*>(s.data()));return out;
 }catch(const std::exception &e){fail(env,e.what());return nullptr;}
}
extern "C" JNIEXPORT void JNICALL Java_vn_ca_vietvoiceoffline_HyTranslator_release(JNIEnv *,jclass,jlong handle){delete reinterpret_cast<HyEngine*>(handle);}
