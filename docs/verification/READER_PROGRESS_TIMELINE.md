# Reader — Tiến trình xử lý dạng node

Cập nhật tiếp ngày: theo yêu cầu mới, trang tiến trình có thêm nút **Hủy yêu cầu** phía trên các node, chỉ ở Mới gửi/Cần bổ sung/Chờ duyệt; không có form sửa nội dung. Sau hủy trở lại danh sách. Hướng dẫn mới tại [Reader guide](../user-guides/reader.md) thay thế mô tả chỉ đọc bên dưới. Build thành công; bộ FR mới đạt **359/359**. Log: [timeline-cancel-.txt](evidence/timeline-cancel-2026-10-25.txt).

Hoàn thành.

## Thay đổi

- Bỏ dòng tổng số kết quả/10 yêu cầu mỗi trang trong bộ lọc. Quy tắc 10 dòng, phân trang và mặc định 7 ngày vẫn hoạt động.
- Bỏ liên kết Theo dõi yêu cầu đã gửi ở cuối trang đăng ký.
- Trong bảng Yêu cầu, mọi trạng thái đều có liên kết **Theo dõi tiến trình**, bao gồm đã duyệt, đã xác nhận lịch, từ chối và hủy. Chỉ các yêu cầu thuộc tài khoản đăng nhập được xem. Quy tắc nút Hủy không thay đổi: trước quyết định của Giám đốc.
- Trang theo dõi có các node dọc theo sự kiện thật: gửi, kiểm tra, trình duyệt, quyết định, xác nhận lịch, các sự kiện giao–trả/ngoại lệ nếu đã phát sinh. Mỗi node có tên người xử lý, thời gian dd/MM/yyyy HH:mm (Asia/Bangkok), nội dung. Tên lấy từ employees.full_name rồi app_users.display_name, cuối cùng dùng username nếu thiếu.
- Các bước chưa thực hiện được thêm bên dưới: bước đang chờ nổi bật, bước tiếp theo màu nhẹ. Chưa có người thực hiện thì chỉ hiển thị vai trò phụ trách, không tự gán tên nhân viên. Hủy/từ chối dừng tiến trình; không thêm bước phê duyệt sau đó. Các sự kiện của phiên bản trước vẫn giữ đúng lịch sử.
- Hoàn tất trong màn hình này là **hoàn tất duyệt và xác nhận lịch nhận**. Khi yêu cầu CONFIRMED và quyền xem hồ sơ còn hiệu lực, hiện nút **Xem thông tin hồ sơ**. Giao–trả thực tế vẫn tiếp tục được ghi nhận ở các node sau. Nếu quá hạn quyền xem, không hiện nút; không mở rộng thời hạn hoặc vượt phân quyền cũ. Yêu cầu APPROVED chưa xếp lịch chưa có nút hoàn tất.
- Trang tiến trình vẫn chỉ đọc, không thêm lại form sửa/hủy. Không sửa schema hay dữ liệu ứng dụng.

## File liên quan

- `src/main/resources/templates/reading/history.html`: giao diện node và nút mở hồ sơ.
- `src/main/resources/templates/reading/list.html`: liên kết theo dõi và bộ lọc.
- `src/main/resources/templates/reading/new.html`: bỏ liên kết cuối form.
- `src/main/resources/static/css/app.css`: đường nối/node, màu trạng thái, bố cục màn hình hẹp.
- `src/main/java/mrm/workflow/ReadingProgress.java`: dựng node thật và các bước tiếp theo.
- `src/main/java/mrm/workflow/ReadingController.java`: kiểm tra điều kiện hiện nút theo quyền đang có.
- `src/main/java/mrm/workflow/ReadingService.java`: truy vấn tên người xử lý.

## Kiểm thử

Maven offline package: BUILD SUCCESS. Chạy riêng FunctionalRequirementsCheck: **343/343 đạt**, trên PostgreSQL schema riêng tự xóa. Không coi Maven package là tự chạy bộ kiểm tra main này.

Kiểm tra mới qua HTTP: yêu cầu đã duyệt/hủy vẫn có link tiến trình; yêu cầu mới có bước chờ và vai trò xử lý; bỏ dòng tổng số kết quả và link cuối form; APPROVED chưa có nút hoàn tất; CONFIRMED có quyền còn hạn hiện đúng link hồ sơ; hết hạn ẩn nút; Reader khác nhận 404 khi truy cập tiến trình không thuộc mình; ghi chú HTML được escape; không có form sửa/hủy trong trang theo dõi. Bộ kiểm tra cũng xác minh các luồng tìm kiếm/phân trang/duyệt/giao–trả trước đó.

Log: [reader-progress-.txt](evidence/reader-progress-2026-10-25.txt). Chưa kiểm tra trực quan bằng trình duyệt trong lần này. Cần khởi động lại ứng dụng và tải lại trang để nhận HTML/CSS mới.
