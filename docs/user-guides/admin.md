# Hướng dẫn sử dụng Admin Quản trị kỹ thuật

## Tạo, quản lý và xóa tài khoản — V26,

Ở **Tạo tài khoản**, nhập họ tên, căn cước 10 số, ngày sinh, giới tính, vai trò, **khoa bắt buộc** và email liên hệ nếu có. Hệ thống tự sinh tên đăng nhập theo công thức đã thống nhất, ví dụ `an.nv12041990@mrm.com`, và mật khẩu ngẫu nhiên riêng. Nếu trùng tên/ngày sinh của người khác, tên tài khoản thêm hậu tố `-2`, `-3` trước `@mrm.com`.

Sau khi tạo, hệ thống mở trang kết quả riêng chỉ hiển thị tên tài khoản và mật khẩu ban đầu **một lần**. Lưu để bàn giao cho nhân viên; mật khẩu này không được thêm vào file tài khoản cũ. Chọn **Đến trang tạo tài khoản** để tạo tài khoản kế tiếp. Nhân viên vẫn phải đổi mật khẩu lần đầu. Căn cước trùng được báo ngay trên form, giữ thông tin đã điền và không tạo tài khoản dở dang. Trong **Quản lý tài khoản**, nhấp **Họ tên** để chỉnh sửa; **Tên tài khoản** là cột riêng không có liên kết.

Tại **Quản lý tài khoản**, dùng **Cập nhật thay đổi vai trò/khoa** để xem các đề nghị Giám đốc đã duyệt và áp dụng. Đề nghị vẫn chưa có hiệu lực cho đến khi Admin bấm áp dụng. Mục này thay thế liên kết cũ **Thay đổi nhân sự đã duyệt**.

Để xóa, tìm nhân viên rồi bấm **Xóa tài khoản** và xác nhận. Tài khoản sẽ bị vô hiệu hóa, phiên đăng nhập cũ bị thu hồi và mục tài khoản biến khỏi danh sách. Vì tài khoản được tham chiếu bởi hồ sơ, yêu cầu và nhật ký, hệ thống giữ dấu vết tài khoản/nhân viên để lịch sử vẫn toàn vẹn; tên đăng nhập và căn cước cũ vẫn được dành riêng. Không thể xóa Admin đang đăng nhập, Admin/Giám đốc hoạt động cuối cùng, người đang có yêu cầu mượn/đề nghị/bệnh án điện tử chưa xử lý, người được giao yêu cầu cần kiểm tra hoặc người còn đợt kiểm kê đang mở. Hoàn tất các việc đó rồi thử lại.

Khi Giám đốc duyệt đề nghị đổi vai trò/khoa, Admin nhận thông báo qua chuông. Mở thông báo hoặc **Quản lý tài khoản → Cập nhật thay đổi vai trò/khoa**, đối chiếu rồi bấm **Áp dụng vai trò/khoa đã duyệt**. Chỉ lúc này quyền và khoa mới thay đổi; nhân viên cần đăng nhập lại. Nếu dữ liệu hiện tại đã khác thời điểm gửi đơn, hệ thống chặn ghi đè để Admin đối chiếu lại. Đơn chưa duyệt/bị từ chối không áp dụng được.

**V25:** Chỉ sau khi Admin áp dụng thành công, hệ thống mới gửi kết quả phê duyệt cho nhân viên. Bấm lại hoặc áp dụng lỗi không gửi kết quả thành công. Giám đốc từ chối vẫn gửi ngay lý do cho nhân viên. [Chi tiết](../verification/STAFF_PROFILE_AND_NOTICE_TIMING.md).

HTML: `templates/account/new.html`, `account/created.html`, `account/list.html`, `account/change-list.html`, `account/change-detail.html`; chuông tại `templates/notification/inbox.html`. Schema xóa mềm ở `V26__AddAccountArchival.java`; [báo cáo](../verification/ACCOUNT_ARCHIVAL_AND_ADMIN_QUEUE.md).

**Thiết lập mật khẩu lần đầu — V23:** tài khoản mới được tạo có cờ `must_change_password=TRUE`. Người dùng phải tự thay mật khẩu ban đầu trước khi dùng nghiệp vụ; Admin cấp lại mật khẩu cũng bật lại cờ này và thu hồi phiên cũ. Giám đốc phải tự tạo thêm mật khẩu cấp 2. `admin1` hiện có giữ nguyên và không bị ép thiết lập lại; Admin mới vẫn được hỗ trợ màn hình thiết lập lần đầu. Xem [chi tiết triển khai](../verification/INITIAL_CREDENTIAL_SETUP.md).

**Sau reset:** `admin1` giữ nguyên thông tin và mật khẩu. Ba tài khoản sử dụng mới theo công thức email đã thay các tài khoản demo; hồ sơ nhân viên liên kết nằm trong `employees`. Mật khẩu mới được lưu cục bộ tại `.secrets/operational-accounts.txt`. Không bật lại seed để tạo demo; dấu `data_reset_history` chặn tái tạo trên database này. Xem [báo cáo reset](../verification/OPERATIONAL_BASELINE_RESET.md).

## Thông báo chung và dọn thông báo đã xử lý — V21,

Chuông mở mặc định tab **Đang chờ xử lý**: chỉ giữ những việc chưa kiểm tra/phê duyệt/xác nhận hoặc còn cần bổ sung. Mọi thẻ đều dùng bố cục người yêu cầu, hồ sơ và ngày giờ như Archivist; lịch đã xác nhận có ngày/phòng nhận. Chuông chỉ tính việc đang chờ chưa đọc. Đã đọc không đồng nghĩa đã xử lý.

Mở **Đã xử lý** để xem kết quả và dùng **Xóa thông báo**, hoặc **Xóa tất cả đã xử lý** để dọn hộp thư. Các thông báo đang chờ được giữ và không có nút xóa riêng; backend cũng chặn xóa. Xóa chỉ ảnh hưởng hộp thư của bạn, giữ yêu cầu và lịch sử nghiệp vụ. Admin vẫn không được nhận dữ liệu bệnh án.

HTML: `templates/notification/inbox.html`; service/controller: `NotificationService.java`, `NotificationController.java`. Xem [báo cáo và kiểm thử](../verification/UNIFIED_NOTIFICATIONS_AND_CLEANUP.md).

## Mô hình bệnh án thống nhất — V17,

Dữ liệu giấy/điện tử chuyển về medical_records. electronic_records còn là view tương thích chỉ đọc, không phải bảng lưu bệnh án thứ hai. Quyền Admin không được mở rộng: dashboard vẫn quản lý tài khoản/nhật ký, không xem thông tin bệnh nhân. Chi tiết migration tại [UNIFIED_MEDICAL_RECORDS.md](../database/UNIFIED_MEDICAL_RECORDS.md).

## Giao diện bàn làm việc mới —

Các trang dùng thanh đầu trang xanh, tên nhân viên/vai trò, chuông thông báo thật và menu ☰ thu gọn. Bấm thẻ Tác vụ nhanh để vào chức năng; bảng gần đây và số liệu được truy vấn thật theo quyền. Reader chỉ thấy bệnh án điện tử của mình; Admin chỉ có dữ liệu tài khoản và nhật ký, không có dữ liệu bệnh nhân.

Vị trí chỉnh giao diện: [home.html](../../src/main/resources/templates/home.html) (banner, thẻ, bảng), [fragments/layout.html](../../src/main/resources/templates/fragments/layout.html) (header/menu dùng chung), [portal.css](../../src/main/resources/static/css/portal.css) (màu sắc/bố cục), [portal.js](../../src/main/resources/static/js/portal.js) (menu), `DashboardService.java` (tác vụ/số liệu theo role). Giữ các đường dẫn thật và form POST đăng xuất có CSRF khi chỉnh HTML. Thông báo đăng nhập 5 giây vẫn hoạt động.


## Thông báo đăng nhập — cập nhật

Sau đăng nhập thành công, thông báo nổi ở góc trên bên phải hiện trong **5 giây** rồi tự đóng; có thể bấm **×** để đóng sớm. Thông báo chỉ hiện một lần sau đăng nhập, không hiện lại khi tải lại/quay về trang chủ. Quy tắc dùng chung cả bốn vai trò.

Giao diện nằm tại `src/main/resources/templates/home.html`; JavaScript tại `src/main/resources/static/js/login-toast.js` (thời gian `5000` mili giây); CSS `.login-toast` trong `src/main/resources/static/css/app.css`. Cờ đăng nhập được đặt trong `SecurityConfig.java` và tiêu thụ một lần trong `HomeController.java`. Cần bật JavaScript để thấy thông báo nổi.

## Các file HTML để chỉnh giao diện Admin / Quản trị viên

Đối chiếu với code hiện tại. Các liên kết dưới đây mở file nguồn trong dự án. Gốc giao diện là `src/main/resources/templates/`; cấu trúc thư mục chia theo chức năng, không chia thành một thư mục riêng cho từng role.

### Màn hình của vai trò này

| File HTML | Đường dẫn website | Nội dung bạn có thể chỉnh |
|---|---|---|
| [account/new.html](../../src/main/resources/templates/account/new.html) | `/admin/accounts/new` | Chỉ form tạo tài khoản và hồ sơ nhân viên, có chọn vai trò; không kèm bảng quản lý. |
| [account/created.html](../../src/main/resources/templates/account/created.html) | `/admin/accounts/created` | Kết quả tạo một lần; không nhúng form tạo tài khoản. Nút điều hướng mở riêng `/admin/accounts/new`. |
| [account/list.html](../../src/main/resources/templates/account/list.html) | `/admin/accounts` | Bảng quản lý; lọc tên, căn cước và role. Bấm tên để mở trang sửa. |
| [account/edit.html](../../src/main/resources/templates/account/edit.html) | `/admin/accounts/{id}/edit` | Sửa đúng một tài khoản: thông tin nhân viên, role, trạng thái, khoa/email, mật khẩu mới tùy chọn. |
| [account/change-list.html](../../src/main/resources/templates/account/change-list.html) | `/admin/accounts/changes` | Đề nghị đổi vai trò/khoa đã duyệt, đang chờ Admin áp dụng. |
| [report/list.html](../../src/main/resources/templates/report/list.html) | `/admin/reports` | Bộ lọc/bảng nhật ký kỹ thuật và xuất/in theo quyền Admin; không phải báo cáo bệnh án. |

### Thành phần dùng chung

| File HTML | Nơi sử dụng | Nội dung |
|---|---|---|
| [login.html](../../src/main/resources/templates/login.html) | `/login` | Đăng nhập chung; CSS và JavaScript của trang login nằm trực tiếp trong file này. |
| [home.html](../../src/main/resources/templates/home.html) | `/{role}/home` | Trang chủ dùng chung; sửa khối điều kiện role của vai trò này. {role} là reader, archivist, director hoặc admin. |
| [fragments/layout.html](../../src/main/resources/templates/fragments/layout.html) | `Các trang đã đăng nhập` | Header/navigation, logo, menu theo quyền, chuông, họ tên cạnh chuông và Đăng xuất; có cả fragment head/footer. |
| [notification/inbox.html](../../src/main/resources/templates/notification/inbox.html) | `/notifications` | Danh sách thông báo, bộ lọc, phân trang, nút mở yêu cầu và đánh dấu đã đọc. Người nhận/nội dung do backend quyết định. |
| [common/error.html](../../src/main/resources/templates/common/error.html) | `Khi gặp lỗi nghiệp vụ được controller xử lý` | Bố cục/thông báo lỗi dùng chung; không phải mọi lỗi 403/404 đều render bằng file này. |

### Chỉnh màu sắc, bố cục và dữ liệu hiển thị

- [static/css/app.css](../../src/main/resources/static/css/app.css): màu, khoảng cách, font, bảng, form và bố cục màn hình cho đa số trang đã đăng nhập. Tìm `.site-header` và `nav` để chỉnh thanh điều hướng; CSS của login chủ yếu ở `login.html`.
- [Logo bệnh viện](../../src/main/resources/static/images/branding/hospital-logo.png): ảnh được header và login sử dụng.
- Muốn sửa menu hoặc phần họ tên cạnh chuông, mở `fragments/layout.html`. Header hiện được chèn vào trang bằng `<header th:replace="~{fragments/layout:: header}"></header>`; vì vậy thường không thấy mã menu đầy đủ trong HTML của từng màn hình.
- Họ tên cạnh chuông dùng `currentDisplayName`; dữ liệu này được chuẩn bị tại [WebModelAdvice.java](../../src/main/java/mrm/common/WebModelAdvice.java). Thay nhãn/chữ/màu trong HTML; muốn thay dữ liệu hoặc cách tính cần xem Java liên quan.
- Những file như `home.html`, `reading/detail.html` và `report/list.html` dùng chung nhiều role. Đọc các điều kiện `th:if` với `role`/`currentRole` trước khi sửa; đổi phần chung sẽ ảnh hưởng các role khác.
- Khi chỉ chỉnh giao diện, giữ nguyên tên input (`name`), `th:action`, `method`, dữ liệu `th:value`/`th:text`, ID tham chiếu bởi JavaScript và các trường ẩn version/token. Đây là liên kết tới backend; thay chúng có thể làm gửi form hoặc CSRF không hoạt động.
- Không mở HTML trực tiếp bằng đường dẫn file để kiểm tra Thymeleaf. Khởi động ứng dụng và mở URL website ở bảng. Nếu trang chưa cập nhật, khởi động lại ứng dụng rồi tải lại trình duyệt; chạy bằng JAR cần đóng gói lại sau khi sửa nguồn.

Phần này chỉ hướng dẫn vị trí chỉnh giao diện, không thay đổi quyền truy cập hoặc các quy tắc nghiệp vụ trong các mục sử dụng bên dưới.

**Tìm bằng căn cước:** tại Quản lý tài khoản, nhập đủ 10 chữ số vào ô Căn cước công dân rồi Tìm kiếm. Có thể để tên/role trống hoặc kết hợp cả ba; số căn cước được tìm chính xác, giữ số 0 đầu. Tài khoản chỉ tìm được theo căn cước sau khi Admin đã bổ sung hồ sơ nhân viên. Xóa bộ lọc để hiện tất cả.

## Tách trang tạo, quản lý và sửa — cập nhật

**Tạo tài khoản** ở `/admin/accounts/new` chỉ có form tạo. **Quản lý tài khoản** ở `/admin/accounts` gồm mã loại tài khoản, họ tên, tên tài khoản và vai trò tiếng Việt; bấm **họ tên** để mở `/admin/accounts/{id}/edit` và sửa ở trang riêng.

Tại Quản lý tài khoản, nhập tên đăng nhập hoặc họ tên ở ô **Tên**, chọn role ở **Loại tài khoản**, rồi Tìm kiếm. Có thể để trống một ô để tìm theo ô còn lại; cả hai trống là toàn bộ. Chọn Xóa bộ lọc để bỏ điều kiện. Các thông tin nhân viên và mật khẩu mới tùy chọn vẫn có ở trang sửa. Xem [chi tiết và kiểm chứng](../verification/ACCOUNT_PAGES_AND_SEARCH.md).

## Form chung và nhân viên — CR-010,

Thay thế hướng dẫn nút tạo riêng bên dưới: từ trang chủ bấm **Tạo tài khoản** hoặc menu **Tài khoản**, dùng một form và chọn **Bác sĩ** (READER) / **Nhân viên lưu trữ** (ARCHIVIST); Giám đốc và Quản trị viên vẫn thuộc bốn role cũ.

Khi tạo mới, điền tên nhân viên, căn cước đúng 10 chữ số (giữ số 0 đầu), ngày sinh, giới tính, role và khoa bắt buộc; email tùy chọn. Username và mật khẩu tự sinh theo V24. Tạo thành công có cả app_users và employees; trùng căn cước hoặc lỗi hồ sơ không để lại tài khoản tạo dở.

Tài khoản cũ chưa có hồ sơ nhân viên vẫn đăng nhập được. Trong danh sách tài khoản, bổ sung đủ căn cước/ngày sinh/giới tính rồi Lưu tài khoản; nếu chỉ sửa tài khoản thì có thể để trống cả ba. Sửa tên/role đồng bộ sang bảng nhân viên. Xem [thiết kế và kiểm chứng](../verification/EMPLOYEE_MANAGEMENT.md).


## Tạo Reader và Archivist — cập nhật

1. Đăng nhập Admin rồi chọn **Tạo tài khoản**, dùng form chung và chọn vai trò phù hợp.
2. Nhập họ tên, căn cước 10 chữ số, ngày sinh, giới tính; không nhập tên đăng nhập hoặc mật khẩu.
3. Bắt buộc chọn khoa; email liên hệ tùy chọn, không gửi email thật.
4. Bấm **Tạo tài khoản**, lưu username/mật khẩu ban đầu từ phần bàn giao để chuyển cho nhân viên. Nếu trùng căn cước, form hiện lỗi và giữ dữ liệu để sửa.
5. Dùng cửa sổ/profile trình duyệt riêng để đăng nhập bằng mật khẩu vừa cấp, hoàn thành thiết lập mật khẩu lần đầu rồi vào trang đúng vai trò. Không thử bằng tab dùng chung phiên Admin nếu cần giữ đồng thời hai tài khoản.

Mật khẩu được băm, không hiển thị lại trong danh sách hoặc lưu vào audit. Tên người gửi trong reading_requests lấy username của tài khoản mới khi họ gửi yêu cầu. Backend chỉ cho ADMIN tạo tài khoản; không cần sửa database hoặc chạy seed-users. Quyền quản trị Director/Admin đã có trước vẫn được giữ trong form chung.

## Cập nhật CR-008 —

Thông báo công việc đã mở cho Archivist và Director; Admin vẫn không nhận/xem thông báo bệnh án của họ. Thông báo chỉ đọc được khi đúng tài khoản nhận và role hiện tại khớp role lúc gửi. Đổi role không chuyển giao thông báo nghiệp vụ cũ. Xem [CR-008](../changes/CR-008_workflow-alerts-and-reader-access.md).


## Bổ sung — Phạm vi chuông

Admin có chuông; từ V24 hộp thư nhận công việc áp dụng đề nghị nhân sự đã được Giám đốc duyệt. Thông báo kết quả đọc bệnh án chỉ dành cho Reader sở hữu yêu cầu; Admin không được đọc thông báo bệnh án của người khác. Không cần SMTP. Xem [FR-11](../verification/FR11_INTERNAL_NOTIFICATIONS.md) và [V24](../verification/STAFF_SELF_SERVICE_AND_APPROVAL.md).

Cập nhật ****, phiên bản mở rộng FR. Admin quản lý tài khoản và xem/xuất nhật ký kỹ thuật; không mặc nhiên có quyền bệnh án.

## 1. Đăng nhập

1. Mở `http://localhost:8080/login` khi chạy cùng máy/cổng mặc định hoặc dùng địa chỉ được cấp.
2. Nhập tài khoản Admin và password đã được cấp; demo `admin1`, không có password mặc định mới.
3. Bấm Đăng nhập; kiểm tra vai trò ADMIN và `/admin/home`.
4. Menu có **Tài khoản**, **Báo cáo / nhật ký** và Đăng xuất.

## 2. Tạo tài khoản

1. Mở **Tạo tài khoản** (`/admin/accounts/new`).
2. Nhập họ tên, căn cước 10 chữ số, ngày sinh và giới tính.
3. Chọn đúng **một** role và bắt buộc chọn khoa trong danh mục.
4. Email liên hệ tùy chọn. Username và mật khẩu tự sinh; Admin không nhập hai trường này.
5. Bấm Tạo tài khoản. Trang kết quả riêng hiển thị thông tin bàn giao một lần; lưu rồi chuyển cho đúng người nhận. Chọn **Đến trang tạo tài khoản** nếu muốn mở form riêng để tạo tài khoản tiếp theo. Nhân viên tự thay mật khẩu lần đầu; không đưa mật khẩu vào tài liệu/Git.

Mật khẩu được băm; danh sách không hiển thị password/hash. Người dùng Director tự thiết lập mật khẩu cấp 2 bằng mật khẩu đăng nhập hiện tại; Admin không biết/không đặt trực tiếp secret qua màn hình này.

## 3. Sửa, khóa, đổi role hoặc đặt lại mật khẩu

1. Tìm tài khoản trong danh sách, đối chiếu username.
2. Sửa họ tên/khoa/role nếu đã có căn cứ phân công.
3. Chọn ACTIVE để hoạt động, LOCKED để khóa, DISABLED để vô hiệu hóa.
4. Password mới để trống để giữ nguyên; nếu đặt lại thì nhập 12–128 ký tự và bàn giao đúng người. Đặt lại password hoặc đổi role làm mất secret cấp 2 cũ; Director cần thiết lập lại.
5. Bấm **Lưu tài khoản**. Phiên cũ của tài khoản bị sửa sẽ bị thu hồi ở request kế tiếp; người dùng cần đăng nhập lại.

Không tự khóa/đổi role tài khoản Admin đang dùng; không làm mất Admin hoạt động cuối cùng. Nút **Xóa tài khoản** lưu trữ mềm: tài khoản bị vô hiệu hóa và ẩn khỏi danh sách nhưng lịch sử, danh tính và căn cước được giữ để tham chiếu hồ sơ. Username/CCCD không được tái sử dụng sau khi xóa. Không thể xóa người còn yêu cầu mượn, đề nghị nhân sự hoặc đợt kiểm kê chưa xử lý; hoàn tất các việc còn mở trước.

## 4. Nhật ký kỹ thuật

1. Mở **Báo cáo / nhật ký** (`/admin/reports`).
2. Admin chỉ được loại Nhật ký kiểm toán với sự kiện AUTH/ACCOUNT: login thành công/thất bại, logout, tạo/sửa tài khoản.
3. Lọc khoảng ngày, username ở ô người thao tác, action như LOGIN/ACCOUNT_UPDATE và ID đối tượng khi biết; bấm Xem.
4. Xuất CSV để mở bằng Excel hoặc In/lưu PDF qua trình duyệt khi cần. Không có chức năng xóa/sửa log trong app.

Admin không được dữ liệu hồ sơ, request đọc, vị trí, kiểm kê chi tiết hoặc xuất báo cáo lâm sàng. Gõ URL của role khác nhận 403 là hành vi đúng.

## 5. Hỗ trợ lỗi và vận hành

| Hiện tượng | Cách xử lý |
|---|---|
| Website không mở | Kiểm tra địa chỉ/cổng và ứng dụng đã khởi động; localhost chỉ máy hiện tại |
| Không login | Kiểm tra username/status; có thể đặt lại password bằng chức năng tài khoản nếu được giao trách nhiệm |
| Phiên tự thoát sau sửa tài khoản | Hành vi thu hồi phiên cũ; đăng nhập lại |
| Lỗi nghiệp vụ hồ sơ/lịch | Ghi thời điểm/màn hình/thông báo, chuyển Archivist hoặc Director xử lý; không tự sửa bệnh án qua database |
| Báo version cũ | Tải lại, đối chiếu thay đổi mới rồi lưu |
| Director quên cấp 2 | Có thể đặt lại password đăng nhập theo trách nhiệm được giao; secret cũ bị xóa, Director tự thiết lập lại bằng password mới |

Không yêu cầu người dùng gửi password/secret cấp 2 vào hội thoại hỗ trợ. Quyền Admin website không tự cấp quyền Windows/PostgreSQL. Chưa có màn hình backup/restore hoặc quản lý cấu hình máy chủ; đó là công việc vận hành riêng.

Build/chạy/kiểm thử xem [README](../../README.md) và [báo cáo FR](../verification/FR_COMPLETION_REPORT.md). Bộ kiểm thử có tạo/dọn dữ liệu/schema tạm, chỉ dùng môi trường phát triển.

## 6. Kết thúc và tự thực hành

Bấm **Đăng xuất**, kiểm tra về login. Đóng tab không thay thế logout.

- [ ] Tạo tài khoản thử theo đúng role, kiểm tra login bằng tài khoản đã được cấp.
- [ ] Sửa/khóa tài khoản thử, kiểm tra phiên bị thu hồi và version cũ không ghi đè.
- [ ] Xem AUTH/ACCOUNT audit, thử bộ lọc và CSV.
- [ ] Xác nhận Admin bị chặn dữ liệu bệnh án; đăng xuất.

Các ô dành cho tự thực hành, không phải nghiệm thu đã ghi sẵn. [Mục lục hướng dẫn](../README.md).
