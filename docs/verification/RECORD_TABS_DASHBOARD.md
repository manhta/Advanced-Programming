# Phân tách bảng hồ sơ và liên kết thống kê —

## Kết quả

- Reader đổi Yêu cầu mượn hồ sơ thành Lịch sử yêu cầu ở tiêu đề/breadcrumb/navigation, URL `/reader/requests` giữ nguyên. Bỏ mũi tên Trạng thái; giữ sắp xếp ngày đăng ký. Liên kết sắp xếp trạng thái cũ ở web được chuyển về ngày đăng ký mới nhất.
- Giữ một mục Tra cứu, dùng các tab có URL độc lập: OFFICIAL mặc định (STORED/PUBLISHED); DECLARATIONS (khai báo của Reader hiện tại, hoặc các khai báo theo quyền staff); DRAFTS chỉ Archivist cho bản giấy nháp. Khai báo chưa duyệt không có trong bảng chính thức, kể cả khi sửa query string. Bộ lọc/phân trang giữ scope, đổi tab mở bảng riêng. Hồ sơ đã công bố vẫn xuất hiện trong lịch sử khai báo và trong danh sách chính thức, không nhân đôi dữ liệu lưu trữ.
- Ba ô thống kê Reader bấm được: Đang mượn mở lịch sử lọc BORROWED toàn thời gian; Chờ xử lý mở `/reader/work` với bảng yêu cầu mượn và bảng khai báo chưa xử lý xong; Bệnh án đã tạo mở DECLARATIONS của chính Reader. Tổng số dòng hai danh sách chờ khớp số thống kê. Chức năng tìm kiếm gần đây gửi scope bằng hidden field để GET không làm mất tab.
- Giữ thiết kế header, font nội dung từ 15px, cơ chế giao–trả và workflow duyệt. Không có migration hoặc sửa dữ liệu thật/Note trong phiên này.

## Xác minh

Maven offline package BUILD SUCCESS; **540/540 FunctionalRequirementsCheck đạt**, dùng schema riêng đã xóa sau kiểm thử. Bao gồm tách danh sách tại backend, giới hạn owner, Reader không vào nháp của Archivist, số liệu/đích của ô thống kê, hàng đợi không lẫn khai báo người khác, tên lịch sử và bỏ mũi tên trạng thái; các ca tạo bệnh án/ngày sinh/phân quyền/giao–trả cũ tiếp tục đạt.

Trình duyệt đã kiểm tra đăng nhập giả lập, ô Chờ xử lý mở hai bảng (29 yêu cầu + 11 khai báo = 40 trong dữ liệu kiểm thử), chuyển qua tab khai báo/chính thức, xem tên Lịch sử yêu cầu và header không còn sắp xếp trạng thái. Ảnh ở viewport mặc định; chưa kiểm tra mọi thiết bị.

Minh chứng: [trang chủ](evidence/record-tabs-2026-10-26/home.png), [khai báo](evidence/record-tabs-2026-10-26/declarations.png), [chính thức](evidence/record-tabs-2026-10-26/official.png), [lịch sử](evidence/record-tabs-2026-10-26/history.png), [log](evidence/record-tabs-2026-10-26/functional.txt).

## File chính

`records/list.html`, `reading/list.html`, `reading/work.html`, `home.html`, `fragments/layout.html`, `portal.css`; `RecordFilter`, `RecordRepository`, `RecordController`, `ElectronicRecordController`, `ReadingController`, `DashboardService`, `HomeController`. Hướng dẫn Reader/Archivist/Director đã cập nhật.

Khởi động lại ứng dụng và Ctrl+F5 để nạp bản mới. Không cần chạy SQL thủ công.
