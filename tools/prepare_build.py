"""Fetch bundled binary dependencies for CI and verify model checksums."""
import hashlib, pathlib, subprocess, urllib.request, json
root = pathlib.Path(__file__).resolve().parents[1]
def download(url, path, digest=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    urllib.request.urlretrieve(url, path)
    if digest and hashlib.sha256(path.read_bytes()).hexdigest() != digest:
        path.unlink()
        raise RuntimeError('Checksum mismatch: ' + str(path))
download('https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx', root/'app/src/main/assets/silero_vad.onnx', '9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6')
download('https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/audio_classification/android/lite-model_yamnet_classification_tflite_1.tflite', root/'app/src/main/assets/yamnet.tflite', '10c95ea3eb9a7bb4cb8bddf6feb023250381008177ac162ce169694d05c317de')
url='https://api.github.com/repos/k2-fsa/sherpa-onnx/releases/tags/v1.13.8'
with urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent':'VietVoice-build'})) as response:
    release=json.load(response)
asset=next(a for a in release['assets'] if a['name']=='sherpa-onnx-1.13.8.aar')
digest=asset.get('digest') or ''
original=root/'app/libs/sherpa-original.aar'
download(asset['browser_download_url'], original, digest.removeprefix('sha256:') if digest.startswith('sha256:') else None)
subprocess.run(['python3',str(root/'tools/isolate_sherpa_runtime.py'),str(original),str(root/'app/libs/sherpa-onnx-1.13.8.aar'),'patchelf'],check=True)
original.unlink()

# Pinned llama.cpp runtime for the bundled HY-MT translator.
llama=root/'third_party/llama.cpp'
if not llama.exists():
    llama.parent.mkdir(parents=True,exist_ok=True)
    subprocess.run(['git','init',str(llama)],check=True)
    subprocess.run(['git','-C',str(llama),'remote','add','origin','https://github.com/ggml-org/llama.cpp.git'],check=True)
subprocess.run(['git','-C',str(llama),'fetch','--depth','1','origin','988190680d5a89fce97de3c20df2c2813731fd61'],check=True)
subprocess.run(['git','-C',str(llama),'checkout','--detach','FETCH_HEAD'],check=True)
(root/'app/src/main/assets/licenses/llama-cpp.txt').write_bytes((llama/'LICENSE').read_bytes())
