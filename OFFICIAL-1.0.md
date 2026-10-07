# VietVoice AI 1.0 Official

Mục tiêu của nhánh Official là ổn định kiến trúc thay vì tăng số phiên bản thử nghiệm liên tục.

## Pipeline

`Video -> Scene AI + OCR AI + Speech AI -> Context window -> Fast Translation -> Heavy Context AI -> Quality Guard -> Voice AI`

### Scene AI
- Chạy on-device, lấy mẫu màn hình khoảng 1 lần/giây, tách khỏi OCR 2 lần/giây.
- ML Kit Face Detection: đếm khuôn mặt, tín hiệu cười/nhắm mắt ở mức thận trọng.
- ML Kit Pose Detection: chỉ suy ra cử chỉ tương đối chắc như giơ tay, hai tay gần nhau, thân nghiêng/ngang, đầu cúi thấp.
- ML Kit Image Labeling: lấy tối đa 4 nhãn cảnh có confidence >= 0.65.
- Giữ một ảnh JPEG nén mới nhất của cảnh. Khi model Heavy có vision (khuyến nghị Gemma 3n), ảnh này được gửi cùng câu thoại để model tự nhìn bối cảnh; nếu model từ chối ảnh, app tự retry bằng text-only.
- Motion estimator 32x18 phát hiện thay đổi cảnh/chuyển động ở mức thô.
- Ngữ cảnh ảnh tồn tại tối đa khoảng 9 giây. Không được coi là lời thoại và prompt cấm LLM thêm chi tiết không có trong câu gốc.

### Context AI
- Giữ tối đa 6 lượt thoại gần nhất, tối đa khoảng 2.600 ký tự trong prompt.
- Khi đổi vùng phụ đề, context và scene memory được xóa để tránh nhiễm cảnh cũ.

### Heavy Context AI
- Người dùng nhập một model `.litertlm` bằng Android file picker; model được chép vào app-private storage.
- Runtime: LiteRT-LM 0.17.1. Thứ tự thử: GPU + vision → CPU + vision → GPU text-only → CPU text-only.
- Output bị giới hạn 128 token và tắt thinking để tránh một câu dịch kéo dài làm nóng máy.
- Single-flight: chỉ 1 lượt inference nặng tại một thời điểm. Nếu đang bận, câu mới dùng bản dịch nhanh thay vì xếp hàng.
- Bản dịch nhanh hiển thị/đọc ngay. Nếu Heavy AI hoàn tất khi câu vẫn còn hiện tại, app cập nhật phần chữ và lịch sử nhưng không đọc lặp lại.
- Memory AI do người dùng sửa luôn có ưu tiên cao hơn Heavy AI.

### Realtime safety
- Hàng đợi OCR/translation và audio vẫn giữ giới hạn nhỏ; đoạn cũ bị bỏ khi quá tải.
- Scene AI và Heavy AI không được chặn luồng OCR/ASR chính.
- Quality Guard tiếp tục chặn đọc thành tiếng khi kết quả có dấu hiệu lỗi rõ.

## Model nặng

Bản Official không nhúng model vài GB vào APK. App có nút mở trang model khuyến nghị theo RAM máy, sau đó chọn `Nhập model AI nặng (.litertlm)` để chép model vào vùng riêng của ứng dụng. Điều này giữ APK có thể cài/cập nhật bình thường và cho phép thay model mà không build lại APK.

- >= 12 GB RAM: ưu tiên Gemma 3n E4B (~4.92 GB).
- >= 8 GB RAM: ưu tiên Gemma 3n E2B (~3.66 GB).
- Máy ít RAM hơn: Gemma3 1B (~0.58 GB); model này nhẹ hơn và Scene AI ML Kit vẫn cung cấp ngữ cảnh riêng.

Trước khi chép model, app kiểm tra dung lượng trống và chừa thêm khoảng 512 MB an toàn.

Nếu chưa nhập model nặng, toàn bộ OCR/ASR/Scene AI/dịch nhanh/giọng Việt vẫn hoạt động offline như trước.

## ABI

Official chỉ build `arm64-v8a`. Đây là lựa chọn chủ động vì runtime/model LLM hiện đại tập trung vào arm64 và giúp tránh đóng gói native 32-bit không cần thiết.

## Giới hạn cần kiểm thử trên thiết bị thật

- Độ chính xác Scene AI phụ thuộc góc quay, hoạt hình, cảnh tối và việc video có cho phép screen capture.
- Nhãn scene/pose là quan sát ước đoán, không phải nhận thức hoàn chỉnh nội dung phim.
- Tốc độ Heavy AI phụ thuộc model, RAM, GPU/driver và thermal throttling.
- Một số model `.litertlm` chỉ hỗ trợ backend nhất định; app fallback CPU nếu GPU init thất bại.


## Build

Source có workflow `.github/workflows/build-official.yml` chạy unit test và build APK release arm64 bằng Gradle 8.9/JDK 17. Release cá nhân dùng cùng keystore của các checkpoint trước để giữ khả năng cài đè trong nhánh này. Keystore nằm trong source chỉ phù hợp bản cá nhân/thử nghiệm, không nên dùng làm khóa phát hành công khai trên store.
