# VietVoice AI 1.0 Official

Đây là nhánh chính thức. Cách dùng ngắn: **Tải dữ liệu & hoàn thiện app → (tùy chọn) tải/nhập model `.litertlm` khuyến nghị → chọn Tự động: phụ đề + âm thanh → Bắt đầu → mở video**. Scene AI và Heavy AI được thiết kế bổ trợ; bộ dịch nhanh vẫn hoạt động nếu model nặng chưa có hoặc đang bận. Xem `OFFICIAL-1.0.md` để biết pipeline và model.

---

# VietVoice 0.8 — Multi-AI tự điều phối

Cài đè bản 0.7 với cùng mã ứng dụng/chữ ký khi đã build bằng keystore thử nghiệm đi kèm. Không gỡ app nếu muốn giữ dữ liệu và các gói đã tải.

- Bật **Multi-AI tự điều phối theo video và tải máy**: app dùng phụ đề khi đủ rõ, tự gọi Speech AI khi thiếu phụ đề hoặc tới lượt kiểm tra định kỳ; khi máy nóng/quá tải sẽ giảm tác vụ nghe nặng.
- Bật **Translation AI kiểm tra kết quả đáng ngờ**: nếu bản dịch có dấu hiệu lỗi rõ, app thử làm sạch câu OCR và dịch lại một lần. Nếu vẫn không chắc, app chỉ hiện chữ và không đọc sai thành tiếng.
- Câu đã sửa và chọn **Nhớ cho lần sau** tiếp tục có ưu tiên cao nhất (Memory AI).
- Bản 0.8 vẫn hoàn toàn không dùng Gemini/API key. Bộ dịch nền vẫn là ML Kit offline; đây chưa phải LLM dịch ngữ cảnh dài.

---

# VietVoice 0.7 — lịch sử và bộ nhớ dịch

Bản cập nhật tiếp nối 0.6, dùng cùng chữ ký để cài đè. Không cần nhập khóa API. Các gói AI đã tải trong dữ liệu app được giữ khi cài đè; không gỡ app trước nếu muốn giữ dữ liệu.

## Mới trong 0.7

1. **Lịch sử · sửa câu · bộ nhớ dịch:** mở nút này ở phần Bản dịch hiện tại. Lưu tối đa 1.000 câu trên điện thoại, vẫn còn khi đóng app. Tìm theo chữ gốc hoặc tiếng Việt; màn hình hiện 100 kết quả gần nhất.
2. Chọn **Sửa bản dịch**, nhập cách dịch đúng. Tích **Nhớ cho lần sau** nếu muốn gặp đúng câu gốc, cùng ngôn ngữ thì dùng lại ngay. Nút **Bản ban đầu** đưa nội dung lúc mới dịch về ô sửa; cần bấm Lưu để áp dụng.
3. Mục **Câu đã nhớ** cho thêm, sửa và quên từng câu, tối đa 1.000 câu. Câu được nhớ chỉ khớp toàn câu, không tự sửa các câu khác chứa cùng từ. Đây không phải huấn luyện lại AI. Cùng một câu trong ngữ cảnh khác vẫn có thể cần cách dịch khác.
4. **Xuất TXT** lưu tất cả lịch sử còn giữ bằng trình chọn tệp của Android. Giờ ghi nhận là thời gian trên máy, không phải mốc phát video. Xóa lịch sử không xóa các câu đã nhớ.
5. **Tốc độ giọng Việt** chỉnh 0,80–1,60×, áp dụng từ đoạn đọc tiếp theo. Bật bám nhịp có thể tăng/giảm thêm theo đoạn tham chiếu. Tăng tốc đọc không làm AI nhận dạng/dịch nhanh hơn.
6. **Phụ đề rõ: giảm xử lý nhận giọng** mặc định bật. Chế độ Tự động chỉ giảm ASR nếu phụ đề ổn định phủ gần hết đoạn âm thanh; vẫn lọc âm thanh, kiểm tra lời nghe định kỳ khoảng 15 giây, nghe bình thường khi thiếu phụ đề. Tắt công tắc để đối chiếu lời nghe mọi đoạn. Không đảm bảo luôn bắt được khác biệt giữa chữ và tiếng.

Bộ dịch nền vẫn là ML Kit như 0.6. Chưa thêm máy chủ AI, dịch nguyên video trước khi xem, xuất video lồng tiếng hoặc SRT có thời gian chính xác. Chưa đo tốc độ/giọng trên điện thoại thật. Xem `DOI-CHIEU-HAI-APP.md` trong mã nguồn để biết kết quả đối chiếu hai APK.

---

## Hướng dẫn nền tảng từ 0.6 (các tính năng còn giữ)

# VietVoice 0.6 — giảm trễ

Cài đè bản 0.4.1/0.5, không gỡ ứng dụng hoặc xóa dữ liệu. Giữ nguyên mã ứng dụng và khóa ký.

Bật **Ưu tiên theo kịp video** (mặc định bật), chọn **Tự động: phụ đề + âm thanh**, chọn tiếng Nhật/Trung/Hàn đúng video và khoanh sát vùng chữ.

- Silero VAD được đóng trong APK để nhận biết ngắt lời; không phải bộ dịch ngôn ngữ. Không loại bỏ âm thanh không lời chỉ dựa vào VAD: YAMNet vẫn phân loại khóc/cười/rên.
- Chế độ nhanh gom tối đa 3 giây âm thanh, ngắt sau khoảng nghỉ 360 ms. Đây không phải cam kết tổng độ trễ.
- Nhận dạng lời và dịch chữ ở hai luồng riêng. Phụ đề ổn định đợi khoảng 400 ms thay vì 1,8 giây trước khi dịch dự phòng; OCR vẫn cần xác nhận chữ ổn định.
- Hàng đợi âm thanh một đoạn, bỏ kết quả cũ khi quá tải. Có thể bỏ sót nội dung hoặc ngắt lời đang đọc để theo kịp video.
- Chế độ nhanh dùng giọng Việt thường, chia cụm từ ngắn. Muốn giả giọng: tắt Ưu tiên theo kịp video, bật Mô phỏng giọng, dừng rồi bắt đầu lại.
- Thời gian hiện trên màn hình tính từ lúc đoạn âm thanh/chữ được đưa vào hàng đợi, chưa phải toàn bộ độ trễ đến loa.

Bộ dịch vẫn là **ML Kit offline**, không có TranslateGemma trong APK này. Mô hình lớn hơn cần kiểm tra tốc độ và chất lượng trước khi tích hợp. Không gọi Gemini, không khóa API, không quota.

Chưa kiểm thử trên điện thoại thật; chưa xác nhận lỗi phát âm trước đó đã được khắc phục. Không cam kết dịch đúng mọi ngữ cảnh hoặc mô phỏng đầy đủ cảm xúc.

Nguồn Silero: https://k2-fsa.github.io/sherpa/onnx/vad/silero-vad.html
Giới hạn bộ dịch: https://developers.google.com/ml-kit/language/translation
Ứng viên TranslateGemma, chưa tích hợp: https://blog.google/innovation-and-ai/technology/developers-tools/translategemma/

---
Hướng dẫn bản trước:

# VietVoice 0.5 — Nhật / Trung / Hàn → Việt

APK thử nghiệm Android 10 trở lên, ARM64 và ARMv7. Mã ứng dụng `vn.ca.vietvoiceoffline`, cài riêng với VietVoice cũ.

## Nâng cấp 0.5
1. Cài đè bản 0.4.1, giữ dữ liệu. Bấm **Tải dữ liệu & hoàn thiện app** để tải thêm SenseVoice khoảng 240 MB. YAMNet khoảng 4 MB được đóng trong APK. Không khóa API/Gemini.
2. Chọn **Tự động: phụ đề + âm thanh** và **SenseVoice · lời nói và cảm xúc**. Chọn tiếng Nhật/Trung/Hàn phù hợp video.
3. Bật bảng dịch nổi, bắt đầu chia sẻ toàn màn hình rồi mở video. Bấm **Khoanh vùng** trên bảng nổi, kéo quanh vùng phụ đề và chọn **Dùng vùng này**. Vùng ngang/dọc được lưu riêng; khoanh lại sau khi thay cách hiển thị video. Đặt bảng nổi ngoài vùng chữ để tránh che phụ đề.
4. Hai nguồn được ghép theo thời gian: phụ đề ổn định ưu tiên; khi không có phụ đề khớp thì dùng lời nghe. Sau khoảng 1,8 giây, phụ đề có thể được dịch trước để không chờ quá lâu. Các câu đã đọc từ phụ đề không đọc lại từ âm thanh. So khớp chữ không phải xác minh ngữ nghĩa.
5. YAMNet phân loại khóc/cười/rên/thở và các âm thanh khác. Chỉ bỏ nhận dạng lời khi nhiều cửa sổ cùng cho kết quả âm thanh không lời rõ và điểm lời nói thấp. Nếu có lời trộn với khóc/cười hoặc kết quả chưa rõ thì vẫn nhận dạng lời. Đây là ngưỡng thực nghiệm, có thể nhầm khi nhạc lớn hoặc nhiều người nói.
6. SenseVoice hiển thị **cảm xúc ước đoán** như vui/buồn/giận/trung tính. Không suy ra cảm xúc từ hình ảnh khuôn mặt. Chưa có mô hình tiếng Việt tái hiện trọn cảm xúc; giọng đọc vẫn dùng Piper/OpenVoice và mô phỏng nhịp/âm lượng cơ bản.
7. Chế độ Whisper Tiny nhẹ hơn vẫn có sẵn nhưng không trả nhãn cảm xúc. Bộ dịch văn bản vẫn là ML Kit offline; bản này cải thiện nhận đầu vào, không phải LLM dịch ngữ cảnh dài. Không cam kết bản dịch sát nghĩa tuyệt đối.
8. Bộ phân biệt âm thanh và cảm xúc chạy ở chế độ Tự động hoặc Chỉ nghe. Chỉ đọc phụ đề không thể suy ra chắc chắn âm thanh/cảm xúc từ chữ.

## Kiểm tra 0.5
- 21 kiểm thử JVM về chọn nguồn theo thời gian, chống lặp, lọc âm thanh thận trọng, phân đoạn và xử lý tín hiệu.
- SenseVoice chạy được trên mẫu Nhật/Trung/Hàn chính thức ở Linux; YAMNet nhận các mẫu đó là lời nói. Đầu vào YAMNet xác minh 15.600 mẫu float32/16 kHz, đầu ra 521 lớp.
- Kiểm tra phiên bản symbol ORT trên cả ARM64 và ARMv7 giữ bản sửa 0.4.1.
- Chưa thử thao tác khoanh vùng, phát âm, độ trễ hoặc độ chính xác cảm xúc trên điện thoại thật. Lỗi phát âm người dùng báo trước đó chưa đủ thông tin để xác nhận đã khắc phục.

## Bản sửa 0.4.1
- Sửa lỗi `dlopen failed: cannot locate symbol OrtGetApiBase` được báo trên điện thoại với bản 0.4.
- Cài đè lên bản 0.4; không gỡ app, không xóa dữ liệu. Giữ nguyên mô hình đã tải và mẫu giọng đã lưu.
- Không cần tải lại mô hình đã tải đủ. Bộ dữ liệu nào chưa hoàn tất thì tiếp tục bằng nút tải.
- Kiểm tra hồi quy liên kết native tái hiện được lỗi của APK 0.4; bản mới kiểm tra đúng phiên bản các hàm ORT trên cả ARM64 và ARMv7.



Kiểm tra bản dựng 0.6: assembleDebug, lintDebug, 29 kiểm thử JVM đạt; chữ ký giữ nguyên; kiểm tra phiên bản liên kết ORT đạt trên ARM64 và ARMv7. Chưa chạy trên điện thoại thật.
