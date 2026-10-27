# Menu tài khoản, danh mục Giám đốc và hồ sơ Reader đã nhận giấy

Ngày thực hiện:. Không thêm bảng/cột hoặc migration; sử dụng dữ liệu và cơ chế mật khẩu hiện có.

## Phạm vi theo xác nhận

Giữ căn cứ/lý do duyệt ở dạng text. Người dùng xác nhận hoãn bộ chọn công văn vì chưa có danh mục công văn trong dự án; không tạo công văn giả hoặc dùng chính sách lưu thay cho công văn.

## Menu dưới tên nhân viên

Reader, Archivist và Director nhấp tên/ảnh đại diện ở header để mở các mục **Thông tin cá nhân**, **Thay đổi mật khẩu**, **Đăng xuất**. Director có thêm **Đổi mật khẩu cấp 2**. Liên kết mật khẩu cấp 2 đã được bỏ khỏi navigation và thẻ tác vụ nhanh để tập trung tại menu tài khoản. Admin giữ giao diện đăng xuất hiện tại.

Menu dùng HTML details/summary nên vẫn mở được không cần JavaScript; JavaScript hỗ trợ đóng khi nhấp bên ngoài/Escape và trả focus về tên. Màu, vị trí header và navigation chính được giữ; menu có bố cục cho màn hình nhỏ.

- HTML header: `src/main/resources/templates/fragments/layout.html`.
- Đóng menu: `src/main/resources/static/js/portal.js`.
- CSS: `src/main/resources/static/css/portal.css`.

### Thông tin cá nhân

URL `/account/profile`; HTML `account/profile.html`; Java `PersonalAccountController.java` và `AccountService.ownProfile`. Hiển thị tên đăng nhập, họ tên nhân viên, căn cước, ngày sinh, giới tính, vai trò, khoa và email. Dữ liệu lấy từ app_users/employees/departments của tài khoản đã xác thực, không nhận ID người khác từ form/URL. Dữ liệu chưa có hiện Chưa cập nhật hoặc Chưa gán, không bịa thông tin cho tài khoản Demo. Trang chỉ xem; chỉnh nhân sự vẫn do Admin.

### Thay đổi mật khẩu đăng nhập

URL `/account/password`; HTML `account/password.html`. Nhập mật khẩu hiện tại, mật khẩu mới và xác nhận. Backend kiểm tra mật khẩu hiện tại, độ dài 12–128, hai ô mới trùng nhau, khác mật khẩu cũ; với Director còn phải khác mật khẩu cấp 2. Năm lần xác thực sai trong 15 phút tạm chặn thao tác. Mật khẩu được băm bằng PasswordEncoder hiện có; không lưu/in mật khẩu trong audit hoặc flash.

Thành công tăng version tài khoản. Phiên thực hiện được cập nhật version để tiếp tục dùng; các phiên khác hết hiệu lực ở lần truy cập tiếp theo. Giữ mật khẩu cấp 2 của Director. POST yêu cầu CSRF; chỉ ba role trên được dùng, không có đổi mật khẩu người khác.

## Ba ô thống kê Giám đốc

| Ô | URL | Danh sách |
|---|---|---|
| Chờ duyệt | `/director/work?queue=pending` | Hai bảng: yêu cầu mượn PENDING_APPROVAL và bệnh án khai báo PENDING_APPROVAL |
| Đã duyệt | `/director/work?queue=approved` | Các yêu cầu/bệnh án do chính Director đang đăng nhập phê duyệt, cùng điều kiện đếm của ô thống kê |
| Điện tử đã công bố | `/director/work?queue=published` | Các bệnh án điện tử đã được phê duyệt/công bố, tương ứng số tổng trên trang chủ |

Mỗi dòng mở yêu cầu hoặc bệnh án thật để xem/xử lý. Trang dùng cùng header, card và bảng với Reader/Archivist. Java: `DashboardService.directorWork`, `HomeController`; HTML: `reading/director-work.html`. Chỉ Director vào được danh mục, các role khác bị chặn.

## Reader đã được giao bản giấy

Backend xác định phiên READING thực sự có handed_at, chưa có returned_at và người yêu cầu là chính Reader đang đăng nhập. Khi đó `records/basic-detail.html` chỉ giữ thông tin hồ sơ và liên kết quay lại, ẩn ba phần cuối: hướng dẫn đăng ký tử vong, nút Mượn hồ sơ giấy, hướng dẫn quy trình mượn giấy. Không ẩn thông tin bệnh nhân hoặc trường tử vong; không áp dụng nhầm cho Reader khác hoặc phiên chỉ xác nhận lịch chưa giao.

Dữ liệu kiểm tra ở `RecordService.paperHandedToReader`, model `readerHasPaper` do `RecordController` chuẩn bị. Khi nhận trả, phiên không còn READING nên điều kiện ẩn kết thúc; khả năng đăng ký tiếp vẫn theo quy tắc hiện tại.

## Kiểm thử và nghiệm thu

**BUILD SUCCESS; 643/643 kiểm tra chức năng/HTTP đạt; JavaScript `node -check` đạt.** Tất cả thao tác mật khẩu kiểm thử dùng tài khoản giả trên schema tạm, không thay mật khẩu hoặc thông tin nhân sự thực tế. Schema tạm đã tự xóa; bản xem thử đã đóng.

Các kiểm tra liên quan gồm:

- Menu/trang cá nhân/mật khẩu của cả ba role trả đúng giao diện; mật khẩu cấp 2 không còn ở primary navigation. Admin và người chưa đăng nhập không vào được trang cá nhân mới theo phạm vi đã chọn.
- Hồ sơ cá nhân hiển thị đúng họ tên, căn cước, ngày sinh; tham số username giả không đổi chủ dữ liệu; không trả password/hash.
- Đổi mật khẩu yêu cầu CSRF và mật khẩu hiện tại; từ chối hai mật khẩu mới không khớp, quá ngắn, trùng mật khẩu cũ hoặc cấp 2. Sau đổi, phiên hiện tại tiếp tục dùng, phiên khác hết hiệu lực; đăng nhập bằng mật khẩu mới thành công và mật khẩu cũ thất bại. Năm lần sai bị tạm chặn, không lộ mật khẩu trong audit.
- Ba liên kết thống kê Director trả HTTP 200 và số dòng trong hai bảng khớp số đếm; role Reader không gọi được service danh mục Director.
- Sau giao bản giấy, trang Reader vẫn có dữ liệu hồ sơ nhưng không còn ba phần nhắc đăng ký mượn; điều kiện đúng người đang giữ bản giấy.
- Kiểm tra trình duyệt: menu Director có bốn lựa chọn, hồ sơ cá nhân hiện dữ liệu/fallback, trang chờ duyệt có bảng yêu cầu và bảng bệnh án riêng. Không thực hiện đổi mật khẩu thật trên trình duyệt.

Bằng chứng: [functional.txt](evidence/personal-menu-2026-10-26/functional.txt), [build.txt](evidence/personal-menu-2026-10-26/build.txt), [menu](evidence/personal-menu-2026-10-26/director-menu.png), [cá nhân](evidence/personal-menu-2026-10-26/profile.png), [chờ duyệt](evidence/personal-menu-2026-10-26/director-pending.png).

Để thử trên máy: khởi động lại ứng dụng, đăng nhập từng role, nhấp tên nhân viên để mở menu; kiểm tra thông tin cá nhân. Director bấm từng ô thống kê để đối chiếu danh sách. Reader mở một hồ sơ giấy đã được giao thực tế để kiểm tra ba phần nhắc mượn đã ẩn. Không cần chạy migration hoặc tạo lại tài khoản.
