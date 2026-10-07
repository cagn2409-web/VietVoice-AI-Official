# VietVoice AI 1.0 Official

- Khóa kiến trúc Official; không tiếp tục đổi tên 0.x theo từng thử nghiệm nhỏ.
- Scene AI on-device: Face + Pose + Image Labeling + motion, lấy mẫu khoảng 1 Hz.
- Giữ snapshot JPEG cảnh mới nhất; Heavy AI đa phương thức có thể nhìn ảnh cùng lời thoại.
- Context AI: tối đa 6 lượt thoại gần nhất, tự xóa khi đổi vùng phụ đề.
- Heavy Context AI qua LiteRT-LM 0.17.1; ưu tiên GPU+vision, fallback CPU/text-only.
- Heavy AI single-flight; không để inference nặng tạo backlog làm lệch video.
- Heavy output tối đa 128 token, thinking tắt để giảm thời gian/thermal.
- Fast Translation luôn hiện trước; Heavy AI chỉ tinh chỉnh chữ/lịch sử, không đọc lặp lại câu.
- Memory AI luôn ưu tiên hơn Heavy AI.
- Tối ưu OCR: ảnh toàn màn hình chỉ cấp cho Scene AI khoảng 1 Hz; các frame OCR còn lại chỉ copy vùng phụ đề.
- Bổ sung kiểm tra dung lượng trước khi nhập model nhiều GB.
- Sửa foreground service Android 14/15: mediaProjection + microphone khi có AudioRecord.
- Thêm workflow build release arm64.
