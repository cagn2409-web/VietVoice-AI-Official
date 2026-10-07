# VietVoice AI 1.0 Official

Bản chính thức của ứng dụng dịch/lồng tiếng video Nhật · Trung · Hàn → Việt, ưu tiên chạy trên điện thoại và không cần Gemini/API key.

## Cách dùng ngắn nhất

1. Mở app → **Tải dữ liệu & hoàn thiện app**.
2. Giữ **Scene AI**, **Multi-AI Router** và **Quality Guard** bật.
3. Muốn chất lượng ngữ cảnh cao nhất: bấm **Mở trang tải model khuyến nghị**, tải file `.litertlm`, sau đó **Nhập model AI nặng**.
4. Chọn **Tự động: phụ đề + âm thanh** → **Bắt đầu dịch video** → cấp quyền chia sẻ màn hình.
5. Mở video. Nếu phụ đề chưa bắt đúng, dùng **Khoanh vùng phụ đề**.

## Official pipeline

`Video → Scene AI + OCR AI + Speech/Sound AI → Context AI → Fast Translation → Heavy Multimodal AI → Quality Guard → Voice AI`

- **Scene AI:** mặt, tư thế, chuyển động, nhãn cảnh; giữ ảnh cảnh mới nhất cho model vision.
- **Fast Translation:** hiện kết quả ngay để bám video.
- **Heavy AI:** đọc 6 lượt hội thoại gần nhất + Scene AI + ảnh cảnh nếu model hỗ trợ vision; single-flight nên không dồn hàng câu cũ.
- **Quality Guard:** kết quả đáng ngờ không được đọc thành tiếng.
- **Memory AI:** câu bạn đã sửa/nhớ có ưu tiên cao nhất.

Chi tiết kỹ thuật, model và giới hạn: xem [`OFFICIAL-1.0.md`](OFFICIAL-1.0.md).

## Model khuyến nghị

- 12 GB RAM trở lên: **Gemma 3n E4B** (~4.92 GB, vision/audio).
- 8 GB RAM trở lên: **Gemma 3n E2B** (~3.66 GB, vision/audio).
- RAM thấp hơn: **Gemma3 1B** (~0.58 GB); Scene AI ML Kit vẫn hoạt động nhưng model này nhẹ hơn.

Model vài GB không được nhúng vào APK để việc cài/cập nhật không trở nên bất khả thi. App tự kiểm tra dung lượng trống trước khi chép model.

## Build

- Android 10+ (`minSdk 29`), `arm64-v8a`.
- JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3, compile/target SDK 35.
- Workflow: `.github/workflows/build-official.yml` chạy test rồi tạo APK release.
- Keystore đi kèm chỉ dành cho bản cá nhân/checkpoint và giữ khả năng cài đè; không dùng làm khóa phát hành công khai.
