#pragma once
#include "llama.h"
#include <string>
#include <vector>
#include <stdexcept>
#include <chrono>
#include <mutex>
#include <algorithm>
struct HyEngine {
    llama_model *model=nullptr;
    llama_context *ctx=nullptr;
    std::chrono::steady_clock::time_point deadline;
    static bool abort(void *p) { return std::chrono::steady_clock::now() >= static_cast<HyEngine*>(p)->deadline; }
    explicit HyEngine(const char *path) {
        static std::once_flag once;
        std::call_once(once, []{ llama_backend_init(); });
        auto mp=llama_model_default_params(); mp.n_gpu_layers=0; mp.load_mode=LLAMA_LOAD_MODE_MMAP;
        model=llama_model_load_from_file(path,mp);
        if(!model) throw std::runtime_error("Khong nap duoc model HY-MT");
        auto cp=llama_context_default_params(); cp.n_ctx=1536; cp.n_batch=128; cp.n_ubatch=128;
        cp.n_threads=2; cp.n_threads_batch=2;
        ctx=llama_init_from_model(model,cp);
        if(!ctx){llama_model_free(model);model=nullptr;throw std::runtime_error("Khong du RAM cho HY-MT");}
    }
    ~HyEngine(){if(ctx)llama_free(ctx);if(model)llama_model_free(model);}
    std::string generate(const std::string &prompt,int budgetMs=18000) {
        deadline=std::chrono::steady_clock::now()+std::chrono::milliseconds(budgetMs);
        llama_set_abort_callback(ctx,abort,this);
        llama_memory_clear(llama_get_memory(ctx),true);
        auto *vocab=llama_model_get_vocab(model);
        std::string wrapped="<｜hy_begin▁of▁sentence｜><｜hy_User｜>"+prompt+"<｜hy_Assistant｜>";
        std::vector<llama_token> tokens(1344);
        int n=llama_tokenize(vocab,wrapped.data(),wrapped.size(),tokens.data(),tokens.size(),false,true);
        if(n<=0||n>1344)throw std::runtime_error("Doan qua dai cho bo nho dich");
        tokens.resize(n);
        for(int i=0;i<n;i+=128){auto b=llama_batch_get_one(tokens.data()+i,std::min(128,n-i));if(abort(this)||llama_decode(ctx,b)!=0)throw std::runtime_error("HY-MT het thoi gian xu ly");}
        auto *sampler=llama_sampler_init_greedy();
        std::string result; bool ended=false;
        try {
            for(int i=0;i<192;i++){
                if(abort(this))throw std::runtime_error("HY-MT het thoi gian xu ly");
                auto token=llama_sampler_sample(sampler,ctx,-1);
                if(llama_vocab_is_eog(vocab,token)){ended=true;break;}
                char piece[512];int len=llama_token_to_piece(vocab,token,piece,sizeof(piece),0,false);
                if(len<0){std::vector<char> big(-len);len=llama_token_to_piece(vocab,token,big.data(),big.size(),0,false);if(len>0)result.append(big.data(),len);}
                else if(len>0)result.append(piece,len);
                auto b=llama_batch_get_one(&token,1);
                if(llama_decode(ctx,b)!=0)throw std::runtime_error("HY-MT het thoi gian xu ly");
            }
            if(!ended)throw std::runtime_error("Ban dich chua hoan tat");
        }catch(...){llama_sampler_free(sampler);throw;}
        llama_sampler_free(sampler);return result;
    }
};
