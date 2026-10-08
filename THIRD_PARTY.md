# Nguồn thư viện và mô hình

- sherpa-onnx 1.13.8 (Apache-2.0): https://github.com/k2-fsa/sherpa-onnx/tree/v1.13.8
  AAR chính thức: https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-1.13.8.aar
- Whisper / model xuất ONNX: https://github.com/openai/whisper và https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny/tree/65176e2deb88badc814a94058666cadccc29b61c
- Giọng Piper VAIS1000: https://huggingface.co/csukuangfj/vits-piper-vi_VN-vais1000-medium
  Model card ghi nguồn dữ liệu: https://ieee-dataport.org/documents/vais-1000-vietnamese-speech-synthesis-corpus và giấy phép dữ liệu CC BY 4.0: https://creativecommons.org/licenses/by/4.0/
  Nguồn được ghi công cho bộ dữ liệu VAIS-1000, dự án Piper và bản chuyển đổi sherpa-onnx; app không huấn luyện lại mô hình.
- eSpeak NG, phần chuyển chữ thành âm vị của bộ giọng: https://github.com/espeak-ng/espeak-ng (GPL-3.0-or-later). Xem giấy phép thành phần và yêu cầu phân phối khi sử dụng lại.
- ONNX Runtime: https://github.com/microsoft/onnxruntime (MIT).
- Google ML Kit: https://developers.google.com/ml-kit/terms và https://developers.google.com/ml-kit/terms/privacy-policy
- Apache Commons Compress 1.27.1: https://commons.apache.org/proper/commons-compress/ (Apache-2.0).
- Kotlin standard library 2.1.0: https://github.com/JetBrains/kotlin (Apache-2.0).

Dữ liệu lớn được tải trực tiếp từ nguồn nêu trên khi người dùng nhấn tải. Chữ ký SHA-256 của dữ liệu nhận dạng và giọng được kiểm tra trước khi sử dụng.

## Bổ sung 0.4
- OpenVoice V2 (MIT), MyShell / MIT: https://github.com/myshell-ai/OpenVoice
- ONNX export bởi TigreGotico: https://huggingface.co/TigreGotico/voiceclonnx-openvoice-v2/tree/34d010c192c97f763207f488f6057fd07fee42ad
  App tải encoder và converter FP32 từ revision cố định; SHA-256 trong ClonePack.java. Không dùng dịch vụ inference bên ngoài.
- ONNX Runtime Android 1.30.0 (MIT): https://github.com/microsoft/onnxruntime
  Bản 0.4.1 giữ ORT 1.28.2 gốc trong AAR sherpa dưới tên libonnxruntime_sherpa.so và sửa liên kết bằng patchelf; Java OpenVoice dùng ORT 1.30.0 riêng. Không sửa mã hoặc trọng số mô hình.

## Bổ sung 0.5
- SenseVoice / FunAudioLLM: https://github.com/FunAudioLLM/SenseVoice
- Bản ONNX k2-fsa: https://huggingface.co/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/tree/2365baeacb507f821a0c8120fcee3d484dba7a07
  URL, dung lượng và SHA-256 cố định trong RecognitionPack.java.
- YAMNet, Google/TensorFlow (Apache-2.0): https://github.com/tensorflow/models/tree/master/research/audioset/yamnet
  TFLite phân loại 521 âm thanh: https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/audio_classification/android/lite-model_yamnet_classification_tflite_1.tflite
  Danh sách lớp đóng trong assets/yamnet_class_map.csv.
- TensorFlow Lite 2.17.0 (Apache-2.0): https://github.com/tensorflow/tensorflow


## Silero VAD in 0.6
Upstream: https://github.com/snakers4/silero-vad (MIT). Android integration/export: https://github.com/k2-fsa/sherpa-onnx (Apache-2.0).
Model URL: https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx
SHA256: 9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6
Assets: silero_vad.onnx, licenses/silero-vad.txt, licenses/sherpa-onnx.txt. 16 kHz, windows of 512 samples, single CPU thread.

## Official 1.0 additions
- Google ML Kit Face Detection 16.1.7 — on-device visual hints.
- Google ML Kit Pose Detection 18.0.0-beta5 — on-device pose hints.
- Google ML Kit Image Labeling 17.0.9 — on-device scene labels.
- Google AI Edge LiteRT-LM 0.17.1 — optional local LLM runtime for user-selected `.litertlm` models.

## Offline quality translation
- HY-MT1.5-1.8B Q4_K_M by Tencent, downloaded from its official Hugging Face repository, revision 265b2e615a7dc9b06c435dc878829ad99a512ba2. License: app/src/main/assets/licenses/hy-mt.txt. Model bytes are not embedded in the APK.
- llama.cpp by ggml-org, MIT license, revision 988190680d5a89fce97de3c20df2c2813731fd61. License copied from the pinned source into APK assets by tools/prepare_build.py.
