# Reader — Sao kê yêu cầu và giao diện đăng ký

Ngày hoàn thành:.

## Cách sử dụng

Trong **Yêu cầu mượn hồ sơ**, danh sách mặc định lọc ngày đăng ký từ hôm nay trừ 6 ngày đến hôm nay (7 ngày lịch, múi giờ Asia/Bangkok), mới nhất trước. Mỗi trang tối đa 10 yêu cầu; ít hơn 10 hiển thị tất cả. Có trang trước/sau nếu nhiều hơn 10. Không còn dòng tên người bệnh dưới số lưu trữ.

Nhập tên người bệnh hoặc số lưu trữ, chọn Ngày đăng ký từ/Đến ngày rồi Tìm kiếm. Có thể tìm riêng theo tên, riêng theo khoảng ngày hoặc kết hợp. Khoảng ngày lọc dựa trên ngày đăng ký thực tế kể cả khi cột ngày đăng ký đang để trống do chưa duyệt. Khoảng ngày tự chọn cho phép xem lịch sử ngoài 7 ngày mặc định. Nút 7 ngày gần nhất xóa bộ lọc về mặc định. Ngày đầu/cuối đều được tính. Tham số được truyền qua SQL có bind và ký tự %/_ được coi là văn bản khi tìm kiếm. Chỉ thấy yêu cầu của tài khoản đăng nhập.

**Ngày đăng ký** tách riêng **Ngày nhận**. Theo yêu cầu giao diện, ngày đăng ký chỉ hiện ở APPROVED/CONFIRMED; các trạng thái còn lại để trống. Dữ liệu ngày đăng ký thực tế vẫn giữ nguyên để tính hạn nhận và lọc lịch sử. Không thay ngày đăng ký bằng ngày phê duyệt. Ngày nhận tiếp tục lấy lịch do nhân viên lưu trữ xác nhận.

Tra cứu hồ sơ hiển thị một trạng thái tiếng Việt: Chưa nhập kho, Trong kho, Đang giao đọc hoặc Tạm không khả dụng. Không còn hai dòng mã trạng thái. Bộ lọc dùng bốn cột ở màn hình rộng, ô nhập ngày và select cùng chiều cao 48px; tự xuống hai/một cột ở màn hình hẹp.

Form đăng ký có nhãn Hồ sơ đăng ký và hộp viền tương tự ô Lý do mượn. Vẫn chọn hồ sơ bằng hộp thoại và mở rộng thông tin nhận diện. Chỉ hiển thị số đã chọn, không hiển thị /20. Từ 1–5 hồ sơ không bắt buộc lý do; trên 5 bắt buộc cả JavaScript và backend. Lý do tối đa ký tự. Với lý do bỏ trống hợp lệ, database lưu nhãn hệ thống Không cung cấp lý do (đăng ký tối đa 5 hồ sơ), không giả làm lý do do người dùng nhập. Giới hạn bảo vệ kỹ thuật 20 hồ sơ mỗi lần vẫn giữ, không hiển thị bộ đếm tối đa.

## Dữ liệu cũ đã dọn

Đã kiểm tra và xóa đúng **hai đăng ký LEGACY của reader1** cho DEMO-MRM-007 và DEMO-MRM-012. Đã xóa 2 reading_requests, 4 reading_sessions, 6 request_events và 4 notifications liên quan. Không xóa medical_records. Giữ audit cũ và thêm hai audit CLEANUP_LEGACY để ghi nhận lần dọn.

Bản sao lưu JSON đầy đủ trước xóa nằm ở `target/verification/legacy-reading-backup-369444de-0a33-47b1-b0c7-01dc21fa51ab.json` (không đưa lên Git). Helper `src/test/java/mrm/features/LegacyReadingCleanup.java` chỉ kiểm tra nếu không có -apply; khi áp dụng, nó khóa bản ghi, giới hạn đúng tên mẫu/Reader/LEGACY, chặn nếu có giao–trả và thực hiện trong một transaction. Không chạy tự động khi ứng dụng khởi động. Đây là dọn dữ liệu mẫu theo yêu cầu, không phải chức năng xóa yêu cầu thông thường.

## Kết quả xác minh

- Maven offline package: BUILD SUCCESS.
- Chạy trực tiếp FunctionalRequirementsCheck: **333/333 đạt**. Bao gồm các kiểm thử luồng nghiệp vụ cũ và kiểm thử mới về 10 dòng/trang, không trùng trang, 7 ngày bao gồm ngày đầu/cuối, truy vấn lịch sử cũ, tên kết hợp ngày, không kết quả, khoảng ngày sai, HTML cột ngày đăng ký, lý do tùy chọn và bắt buộc trên 5, gửi lỗi không tạo batch dở dang, trạng thái tra cứu tiếng Việt.
- `node -check src/main/resources/static/js/borrow-record-picker.js`: đạt.
- Kiểm thử ứng dụng trên schema PostgreSQL riêng, tự xóa sau kết thúc. Lần dọn hai đăng ký là thay đổi thật trong public, đã sao lưu riêng như trên. Không có migration schema mới.
- Đã kiểm tra HTML bằng HTTP thật. Chưa xác minh trực quan CSS bằng trình duyệt trong lần này.

Log kiểm thử: [reader-statement-.txt](evidence/reader-statement-2026-10-25.txt).

## Các file chính để chỉnh tiếp

- `src/main/resources/templates/reading/list.html`: bộ lọc và bảng yêu cầu.
- `src/main/resources/templates/reading/new.html`: form đăng ký.
- `src/main/resources/templates/records/list.html`: tra cứu bệnh án.
- `src/main/resources/static/css/app.css`: căn hàng và hộp chọn hồ sơ.
- `src/main/resources/static/js/borrow-record-picker.js`: số lượng chọn và lý do bắt buộc.
- `src/main/java/mrm/workflow/ReadingService.java`: lọc/phân trang và kiểm tra lý do.

Sau khi khởi động lại ứng dụng, kiểm tra bằng Reader: lọc một tên/khoảng ngày, chuyển trang, mở form chọn 5 rồi 6 hồ sơ để kiểm tra thuộc tính bắt buộc; mở Tra cứu hồ sơ để kiểm tra vị trí các ô và trạng thái.
