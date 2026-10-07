# VietVoice 0.8 Multi-AI Router

## Thay đổi mã nguồn
- Thêm `AiRouter.java`: điều phối ASR dựa trên độ phủ phụ đề, hàng đợi, số đoạn bị bỏ, thời gian dịch gần đây và trạng thái nhiệt Android.
- Thêm `TranslationQuality.java`: kiểm tra lỗi dịch rõ ràng và chuẩn hóa OCR để thử lại an toàn.
- `CaptureService`: tích hợp Router, Quality Guard, Memory AI label; không phát giọng khi bản dịch vẫn bị đánh dấu chưa chắc.
- `MainActivity`: thêm hai công tắc Multi-AI Router/Translation AI Quality Guard; đổi nhãn phiên bản 0.8.
- Thêm kiểm thử `AiRouterTest` và `TranslationQualityTest`.
- `versionCode` 7, `versionName` `0.8-multi-ai`.

## Kiểm tra đã làm trong môi trường này
- Biên dịch và chạy trực tiếp hai lớp Java thuần `AiRouter` + `TranslationQuality`: đạt.
- Chưa thể assemble APK trong môi trường hiện tại vì không có Android SDK/Gradle và vùng build không tải được bộ công cụ.

## Không thay đổi
- Không thêm Gemini, API key hay máy chủ dịch.
- Không nhúng LLM lớn/TranslateGemma vào APK.
- Không thay chữ ký dự án hoặc applicationId.
