# Đối chiếu hai APK và thay đổi VietVoice 0.7

Phân tích tĩnh các gói người dùng cung cấp, ngày 05/10/2026. Không chạy hai app, không truy cập máy chủ của chúng. Tên lớp, thư viện và nội dung giao diện chứng minh có thành phần/tính năng trong gói; không chứng minh mọi tính năng đang hoạt động, miễn phí hay dùng một mô hình cụ thể trong thực tế.

| Hạng mục | App dịch giọng nói | Subflow | VietVoice 0.7 |
|---|---|---|---|
| Gói đã kiểm tra | com.subtitle.voice 2.4.0 | com.subflowai.app 1.0.26 | vn.ca.vietvoiceoffline 0.7-thu-nghiem |
| Xử lý cục bộ | Có thư viện sherpa-onnx, ONNX Runtime, Vosk, ML Kit | Không thấy bộ ASR/dịch lớn trong các thư viện/asset đã kiểm tra | Giữ SenseVoice/Whisper, ML Kit, Piper của 0.6 |
| Xử lý online | Có lựa chọn nhiều engine và thông báo hạn ngạch online; có thành phần giao tiếp máy chủ | Có API dự án, ứng viên bản dịch, phiên bản dịch; giao diện tải video từ máy chủ và hạn ngạch lồng tiếng | Không thêm phụ thuộc máy chủ hoặc quota |
| Lịch sử | Có lưu, tìm kiếm, xuất | Có dự án, phụ đề chỉnh sửa, xuất | Thêm lịch sử lưu trên máy, tìm kiếm, sửa, xuất TXT |
| Điều chỉnh bản dịch | Nhiều lựa chọn engine | Có thuật ngữ, ứng viên và phiên bản dịch | Thêm bộ nhớ câu do người dùng sửa, khớp toàn câu và ngôn ngữ |
| Tốc độ đọc | Có cấu trúc SpeechRateVO và các cài đặt tốc độ | Có điều khiển tốc độ phát video | Thêm tốc độ giọng Việt 0,80–1,60× |
| Giảm công việc khi đã có phụ đề | Chưa xác định chính xác thuật toán thực tế | Thiên về xử lý dự án/video | Tự triển khai giảm ASR khi phụ đề ổn định phủ ít nhất 90% đoạn; đối chiếu lại định kỳ |
| Xuất video/SRT | Có giao diện xuất SRT, song ngữ, chèn phụ đề vào video | Có SRT, video và lồng tiếng | Chưa thêm xuất video/SRT; TXT không giả định biết mốc phát video ngoài app |

## Kết luận về bộ AI

App dịch giọng nói có định danh engine Qwen, DeepSeek, Hunyuan/HY_MT2, ChatGPT, Gemma, Gemini và các dịch vụ dịch khác. Đây là các lựa chọn trong mã khách, không phải bằng chứng tất cả trọng số AI nằm trong APK, và không xác định được engine người dùng đang dùng. Nhiều tên Whisper/SenseVoice trong thư viện có thể là chức năng chung của sherpa-onnx.

Subflow có các kiểu dữ liệu TranslationGlossaryTerm, TranslationCandidates và TranslationVersion. Không đủ bằng chứng xác định mô hình phía máy chủ. Không sao chép mã, tài nguyên, khóa hay kết nối máy chủ riêng của hai app vào VietVoice.

## Những phần tự triển khai

- Nhật ký SQLite riêng của app, giữ tối đa 1.000 câu gần nhất. Giao diện hiển thị tối đa 100 kết quả gần nhất theo tìm kiếm, xuất TXT toàn bộ lịch sử còn lưu.
- Sửa bản dịch; xem lại bản ban đầu; tùy chọn nhớ câu. Bộ nhớ tách khỏi lịch sử, tối đa 1.000 câu, có sửa/xóa từng câu. Xóa lịch sử không xóa bộ nhớ câu.
- Bộ nhớ chỉ bỏ khác biệt xuống dòng/khoảng trắng và chuẩn hóa Unicode NFC. Giữ dấu câu, chữ hoa/thường; tách Nhật/Trung/Hàn. Câu có cùng chữ nhưng khác ngữ cảnh có thể cần sửa lại. Không phải huấn luyện lại AI hay thuật ngữ áp dụng xuyên câu.
- Đọc nhanh/chậm áp dụng từ đoạn tiếp theo. Nếu bật bám nhịp, tốc độ còn được điều chỉnh theo tham chiếu. Tăng tốc đọc không tăng tốc nhận dạng hoặc dịch.
- Ở chế độ Tự động, phụ đề phải gần như liên tục và còn mới để giảm ASR. YAMNet vẫn phân loại âm thanh. ASR vẫn chạy khi thiếu phụ đề, lần đầu và sau khoảng 15 giây. Có công tắc tắt tối ưu để nhận giọng mọi đoạn. Tối ưu có thể bỏ lỡ khác biệt giữa phụ đề và lời nói ở đoạn không đối chiếu.

## Phạm vi kiểm tra

Kiểm tra build, lint, bài kiểm thử logic Java, ký APK và tương thích liên kết ONNX Runtime cho hai ABI. Chưa chạy bản 0.7 trên điện thoại thật hoặc đo độ trễ, chất lượng dịch/giọng thực tế. Không khẳng định chính xác tuyệt đối, giả giọng đủ cảm xúc hoặc thời gian thực trên mọi máy.
