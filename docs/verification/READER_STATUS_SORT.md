# Trạng thái của Reader và sắp xếp tại tiêu đề —

## Thay đổi

- Bộ lọc trạng thái lấy DISTINCT trạng thái hiển thị từ yêu cầu của Reader đã xác thực, không dùng danh sách trạng thái chung trong HTML. Nguồn là toàn bộ lịch sử của người đó, không giới hạn theo trang/khoảng ngày/từ khóa; giữ thứ tự tiến trình quen thuộc. Không có yêu cầu thì chỉ có Tất cả trạng thái.
- Bỏ select Sắp xếp; giữ giá trị sort trong input hidden để gửi lại bộ lọc không làm mất thứ tự hiện tại.
- Ngày đăng ký có ▲ cũ trước và ▼ mới trước (mặc định). Trạng thái có ▲ theo tiến trình, ▼ đảo nhóm; cùng trạng thái luôn đăng ký cũ trước. Cùng ngày dùng thời điểm tạo và UUID làm thứ tự ổn định.
- Liên kết ở tiêu đề giữ q/from/to/status, quay về trang đầu khi đổi thứ tự. Phân trang giữ sort. Mũi tên hiện hành tô xanh, có aria-label, title và aria-sort trên tiêu đề.
- Bốn giá trị sort hợp lệ: registered, registered_asc, status, status_desc; SQL chọn nhánh cố định. Quyền xem, lịch giao–trả, hủy, ghi chú và ngày giữ nguyên.
- Bộ lọc/mũi tên dành cho Reader; danh sách xử lý của Archivist/Director vẫn render dùng chung template với tiêu đề thường.

## Xác minh

Maven package offline thành công. Bộ kiểm thử FunctionalRequirementsCheck trong schema tạm xác minh trạng thái của Reader không lẫn tài khoản khác, bộ lọc ổn định khi không có kết quả trong khoảng ngày/từ khóa, hai chiều ngày, hai chiều nhóm trạng thái với tie-break cũ trước, phân trang giữ bộ lọc/sort và HTML không còn dropdown sắp xếp. Tiếp tục kiểm tra phân quyền, giao–trả và template ba role như trước.

Trình duyệt xác nhận tài khoản thử chỉ có SUBMITTED thì menu chỉ có Tất cả trạng thái/Mới gửi; bấm ▲ ngày đảo hai yêu cầu cùng ngày theo thời điểm tạo; bấm ▼ trạng thái giữ liên kết và giao diện đúng. Kiểm tra tại viewport mặc định.

Minh chứng: [ảnh](evidence/reader-status-sort-2026-10-25/statement.png), [log kiểm thử với tổng số kiểm tra](evidence/reader-status-sort-2026-10-25/functional.txt).

Không đổi dữ liệu thật/schema hoặc Note. App thử đã dừng và schema tạm được xóa. Hướng dẫn Reader đã cập nhật. Khởi động lại app và Ctrl+F5 để nạp bản mới.
