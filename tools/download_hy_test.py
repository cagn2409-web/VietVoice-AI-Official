import hashlib, pathlib, urllib.request
path=pathlib.Path('/tmp/hy-test.gguf')
url='https://huggingface.co/tencent/HY-MT1.5-1.8B-GGUF/resolve/265b2e615a7dc9b06c435dc878829ad99a512ba2/HY-MT1.5-1.8B-Q4_K_M.gguf'
urllib.request.urlretrieve(url,path)
h=hashlib.sha256()
with path.open('rb') as f:
    for chunk in iter(lambda:f.read(1024*1024),b''):h.update(chunk)
assert path.stat().st_size==1133080512
assert h.hexdigest()=='4383ac0c3c8e476de98ff979c2a3f069f8c4fb385e7860cf2d28da896cc477c7'
