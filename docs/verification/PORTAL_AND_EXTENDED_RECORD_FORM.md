# Hoàn thành form bệnh án chi tiết và giao diện chung

Ngày. Tham chiếu: ảnh và HTML mẫu người dùng cung cấp.

## Phạm vi đã thực hiện

Reader có đầy đủ các nhóm thông tin tương ứng form bệnh án giấy hiện tại, ngoại trừ phần kho: nhận diện (tên, năm sinh, giới tính, mã người bệnh), đợt điều trị (khoa, loại, vào/ra viện, tử vong), nguồn dữ liệu/căn cứ và ghi chú tùy chọn. Số bệnh án tự tạo thay ô nhập số thủ công; không có số lưu trữ, ngăn, ngày đầu lưu, chất lượng giấy hay chính sách. Các trường điều trị tùy chọn theo form giấy; nếu có dữ liệu thì kiểm tra hợp lệ. Note không có required trong HTML, backend chấp nhận trống và lưu null, tối đa ký tự. Nguồn tối đa 1000. Có thể xem/sửa toàn bộ trường ở các trạng thái cho phép, giữ mã và luồng Director duyệt công bố ngay.

Form/chi tiết tương ứng được cập nhật cho Reader/Archivist/Director. Java migration V16 mở rộng bảng electronic_records, giữ dữ liệu V15. Snapshot tên khoa/loại và event JSON giữ lịch sử. Phiếu HTTP không gửi checkbox tử vong vẫn mặc định false.

Giao diện được chuyển từ mẫu sang Thymeleaf: header xanh có logo trung tâm hiện hữu, hồ sơ người dùng, chuông và menu thật; trang chủ có banner, thống kê, các thẻ tác vụ, bảng gần đây, tìm kiếm đưa sang trang tìm kiếm thật. Dùng CSS/JS và SVG tại dự án, không cần Tailwind/Lucide CDN hay font mạng. Không sao chép số liệu 04/02/1,248, tài khoản hoặc thông báo giả trong mẫu. Đăng xuất tiếp tục dùng POST + CSRF; các tác vụ và dữ liệu theo role, không mở quyền mới cho Admin/Reader.

Header, trường nhập, bảng và card dùng phong cách chung trên các trang ứng dụng của bốn role. Trang đăng nhập có thiết kế riêng đã chốt trước, được giữ nguyên. Menu thu gọn dùng JavaScript, có aria-expanded, đóng bằng Escape/nhấn bên ngoài và vẫn truy cập được khi không bật JavaScript.

## Nguyên lý số liệu dashboard

- Reader: số phiên đang READING của mình; đăng ký mượn/khai báo điện tử còn chờ của mình; tổng bệnh án điện tử mình tạo. Bảng gần đây chỉ hồ sơ mình tạo.
- Archivist: đăng ký/khai báo mới cần kiểm tra; bệnh án giấy trong kho; phiên CONFIRMED chờ giao. Có thẻ nhập giấy, danh mục, vị trí và xử lý hồ sơ điện tử.
- Director: yêu cầu/khai báo chờ duyệt; hồ sơ/yêu cầu do mình đã duyệt; tổng điện tử đã công bố. Có thẻ duyệt, chính sách, báo cáo, mật khẩu cấp 2.
- Admin: tổng tài khoản, đang hoạt động, khóa/ngừng dùng; bảng tài khoản gần đây. Không truy vấn dữ liệu bệnh án cho nội dung dashboard.

Các số phản ánh trạng thái dữ liệu hiện có, không phải dữ liệu cố định và không phải lời khẳng định đã hoàn thành trả đối với mọi phiên CONFIRMED.

## File để chỉnh

- `templates/home.html`, `templates/fragments/layout.html`, `static/css/portal.css`, `static/js/portal.js`: giao diện chung.
- `mrm/authentication/DashboardService.java`: thống kê, tác vụ và danh sách gần đây theo quyền.
- `templates/electronic/form.html`, `templates/electronic/detail.html`: form và chi tiết.
- `mrm/electronic/ElectronicRecordForm.java`, `ElectronicRecordController.java`, `ElectronicRecordService.java`: nhận/kiểm tra/lưu dữ liệu, nạp danh mục.
- `mrm/database/migration/V16__ExtendElectronicRecordDetails.java`: thay đổi schema; không sửa V1–V15.

## Xác minh

- Maven offline package: BUILD SUCCESS; node -check portal.js: đạt.
- Chạy riêng FunctionalRequirementsCheck ở chế độ preview: **455/455 đạt**. Gồm kiểm thử cũ, lưu/sửa trường bổ sung, snapshot danh mục, ngày tương lai/ngày đảo, FK không tồn tại, giới hạn ghi chú, ghi chú rỗng, không có thuộc tính required, công bố vẫn đúng, bốn dashboard, số liệu thật, Admin không lộ bệnh án.
- ElectronicUpgradeCheck: **7/7 đạt**, từ V14 có bệnh án giấy/thông báo rồi lên V15 có hồ sơ điện tử và nâng V16; dữ liệu cũ giữ nguyên, trường mới có mặc định an toàn.
- Đã mở bằng trình duyệt và kiểm tra trực quan dashboard của Reader/Archivist/Director/Admin, form Reader và mở menu thu gọn ở viewport mặc định. Không đặt viewport giả để tạo ảnh; chưa kiểm tra riêng từng kích thước điện thoại.
- Đã quan sát toast xuất hiện sau đăng nhập và biến mất, không thay bằng alert chặn thao tác. Các ảnh có thể chụp trong lúc toast 5 giây còn hiển thị.
- Toàn bộ fixture là dữ liệu giả trong schema riêng; sau xem trước đã dừng máy chủ và xác nhận schema bị xóa. Không chạy V16 lên public trong lần làm việc này. Người dùng khởi động lại để Flyway áp dụng V16 vào database cấu hình.
- Lần kiểm tra đầu phát hiện checkbox tử vong vắng mặt gây lỗi binding cho phiếu cũ; đã chuẩn hóa Boolean null thành false và chạy lại toàn bộ thành công.

Bằng chứng: [hoàn tất 455 kiểm tra](evidence/portal-2026-10-25/completion.txt) (trích đoạn terminal, không phải toàn bộ log), [log nâng cấp](evidence/portal-2026-10-25/upgrade.txt), [Reader](evidence/portal-2026-10-25/reader-home.png), [form](evidence/portal-2026-10-25/reader-form.png), [Archivist](evidence/portal-2026-10-25/archivist-home.png), [Director](evidence/portal-2026-10-25/director-home.png), [Admin](evidence/portal-2026-10-25/admin-home.png).

## Nghiệm thu trên máy người dùng

Khởi động lại với profile dev như đang dùng, tải lại trình duyệt để nhận portal.css/portal.js. Reader tạo mới hoặc sửa phiếu chưa duyệt, nhập khoa/loại/ngày, để Ghi chú trống và gửi; thử lại có ghi chú. Archivist/Director kiểm tra toàn bộ thông tin rồi công bố. Đăng nhập từng role xem số liệu và thử thẻ tác vụ, menu, chuông, đăng xuất. Nhập hồ sơ giấy vẫn hoạt động theo đường dẫn cũ.
