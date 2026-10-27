# Hướng dẫn sử dụng — Director (Giám đốc)

## Xét duyệt đề nghị nhân sự và tự sửa thông tin — V24

Menu tài khoản → **Thông tin cá nhân** → **Chỉnh sửa thông tin cá nhân** cho phép tự sửa họ tên, căn cước, ngày sinh và giới tính; tên tài khoản/email giữ nguyên. Vai trò và khoa trong trang sửa là liên kết mở form đề nghị được duyệt, không trực tiếp thay quyền.

Mở **Duyệt thay đổi nhân sự** hoặc chuông khi có đơn mới. Chọn Phê duyệt/Từ chối, nhập mật khẩu cấp 2 và gửi. **Lý do từ chối chỉ hiện khi chọn Từ chối và bắt buộc nhập**. Phê duyệt chỉ gửi việc cho Admin, chưa gửi kết quả thành công cho nhân viên. Admin áp dụng xong nhân viên mới nhận kết quả; từ chối thì nhân viên nhận ngay kèm lý do. [Cập nhật V25](../verification/STAFF_PROFILE_AND_NOTICE_TIMING.md).

Vai trò tự đề nghị chỉ gồm Bác sĩ/Nhân viên lưu trữ, không cho xin quyền Admin/Giám đốc. Giám đốc không tự duyệt đơn của mình; không có Giám đốc khác hoạt động thì hệ thống không nhận đơn tự đề nghị của Giám đốc. Không áp dụng thay đổi làm mất Giám đốc hoạt động cuối cùng.

HTML: `templates/account/change-list.html`, `account/change-detail.html`, `account/profile-edit.html`; menu tại `templates/fragments/layout.html`. [Chi tiết](../verification/STAFF_SELF_SERVICE_AND_APPROVAL.md).

**Tài khoản hiện tại:** Tạ Đức Mạnh, sinh — `manh.td24102003@mrm.com`. Mật khẩu ban đầu tại `.secrets/operational-accounts.txt`. Khi đăng nhập, nhập mật khẩu mới và xác nhận; đồng thời tự tạo mật khẩu cấp 2 và xác nhận. Không có ô mật khẩu cũ ở lần thiết lập này. Hai mật khẩu phải khác nhau và khác mật khẩu ban đầu, mỗi mật khẩu 12–128 ký tự. Chưa hoàn thành thì không truy cập được chức năng nghiệp vụ. Mật khẩu cấp 2 từng cấp sẵn đã bị bỏ, Giám đốc phải tự tạo. Tên `director1` và Lê Văn Minh trong báo cáo cũ chỉ phản ánh dữ liệu trước đổi tên. Xem [hướng dẫn thiết lập ban đầu](../verification/INITIAL_CREDENTIAL_SETUP.md).

## Thông báo chung và dọn thông báo đã xử lý — V21,

Chuông mở mặc định tab **Đang chờ xử lý**: chỉ giữ những việc chưa kiểm tra/phê duyệt/xác nhận hoặc còn cần bổ sung. Mọi thẻ đều dùng bố cục người yêu cầu, hồ sơ và ngày giờ như Archivist; lịch đã xác nhận có ngày/phòng nhận. Chuông chỉ tính việc đang chờ chưa đọc. Đã đọc không đồng nghĩa đã xử lý.

Mở **Đã xử lý** để xem kết quả và dùng **Xóa thông báo**, hoặc **Xóa tất cả đã xử lý** để dọn hộp thư. Các thông báo đang chờ được giữ và không có nút xóa riêng; backend cũng chặn xóa. Xóa chỉ ảnh hưởng hộp thư của bạn, giữ yêu cầu và lịch sử nghiệp vụ. Admin vẫn không được nhận dữ liệu bệnh án.

HTML: `templates/notification/inbox.html`; service/controller: `NotificationService.java`, `NotificationController.java`. Xem [báo cáo và kiểm thử](../verification/UNIFIED_NOTIFICATIONS_AND_CLEANUP.md).

## Báo cáo và trang xét duyệt được tinh gọn —

Trang báo cáo bỏ các đoạn diễn giải dài và dùng tiêu đề cột tiếng Việt: Người thao tác, Hành động, Loại đối tượng, Mã đối tượng, Kết quả, Nội dung, Thời điểm. Các ô dữ liệu hiển thị đúng nội dung. Bộ lọc/xuất CSV/in vẫn có; CSV giữ khóa cột kỹ thuật.

**Kiểm kê chỉ dành Archivist**. Director không còn mục Kiểm kê, loại báo cáo kiểm kê hoặc quyền mở qua URL. Trong trang xét duyệt đọc hồ sơ, hai khối Lịch nhận và giao–trả/Lịch sử xử lý đã được bỏ; giữ thông tin và form quyết định.

HTML: `report/list.html`, `reading/detail.html`, `fragments/layout.html`; nhãn cột: `ReportLabels.java`. [Báo cáo và kiểm thử](../verification/DIRECTOR_REPORTS_AND_NOTICE_PRESENTATION.md).

## Menu tài khoản cá nhân —

Menu tên nhân viên có Thông tin cá nhân, Thay đổi mật khẩu, Đổi mật khẩu cấp 2 và Đăng xuất. Không còn liên kết đề nghị riêng; mở từ mục Vai trò/Khoa trong trang chỉnh sửa thông tin cá nhân. Giám đốc không tự duyệt đơn của mình.

Đổi mật khẩu tại `/account/password`: nhập mật khẩu hiện tại, mật khẩu mới 12–128 ký tự và xác nhận. Phiên đang đổi tiếp tục dùng; các phiên khác hết hiệu lực ở lần truy cập tiếp theo. Không dùng lại mật khẩu cũ; Director không được dùng trùng mật khẩu cấp 2.

HTML: `templates/fragments/layout.html` (menu), `templates/account/profile.html` (cá nhân), `templates/account/password.html` (mật khẩu). Java: `PersonalAccountController.java`, `AccountService.java`. Menu/CSS: `static/js/portal.js`, `static/css/portal.css`.

Menu tài khoản của Giám đốc còn có **Đổi mật khẩu cấp 2**. Mục này được chuyển khỏi navigation và thẻ tác vụ nhanh; URL `/director/approval-secret` vẫn dùng được.

Ba ô thống kê trên trang chủ đã mở đúng bảng: **Chờ duyệt** (`/director/work?queue=pending`), **Đã duyệt** (`/director/work?queue=approved`, do chính bạn duyệt), **Điện tử đã công bố** (`/director/work?queue=published`). Danh sách khớp số thống kê, có liên kết mở yêu cầu/bệnh án. HTML: `templates/reading/director-work.html`; Java `DashboardService.java`/`HomeController.java`.

Căn cứ/lý do duyệt hiện vẫn nhập text theo xác nhận của bạn; chưa bổ sung danh mục công văn.

Xem [báo cáo thay đổi và kiểm thử](../verification/PERSONAL_MENU_AND_DIRECTOR_DASHBOARD.md).

## Thông báo yêu cầu đã hủy — V20,

Thông báo công việc chờ duyệt của yêu cầu mượn đã hủy được ẩn khỏi danh sách và số chuông chưa đọc; thông báo còn lại gắn với yêu cầu đã hủy không có nút Mở yêu cầu. Lịch sử xử lý trong database vẫn được giữ. Xem [báo cáo](../verification/ARCHIVIST_NOTICES_AND_DECEASED_RECORDS.md).

## Chỉ duyệt mượn lượt có bệnh án tử vong — (V19)

Đối với lượt đăng ký mượn mới, Giám đốc chỉ quyết định các lượt có ít nhất một hồ sơ tử vong. **Tất cả yêu cầu con** trong lượt này đều cần Giám đốc, kể cả hồ sơ thường. Nhân viên lưu trữ kiểm tra rồi trình từng yêu cầu; chuông báo và liên kết mở đúng trang quyết định. Kiểm tra lý do, chọn phê duyệt/từ chối, nhập căn cứ và mật khẩu cấp 2. Sau đó nhân viên lưu trữ xác nhận lịch và giao–trả.

Lượt chỉ gồm hồ sơ thường do nhân viên lưu trữ duyệt và không gửi thông báo trình Giám đốc. Yêu cầu cũ trước V19 giữ tuyến duyệt trước đây. Quy trình công bố bệnh án bác sĩ mới khai báo vẫn cần Giám đốc như cũ.

Giao diện quyết định: `templates/reading/detail.html`; tuyến duyệt được kiểm tra ở `ReadingService.java`, không phụ thuộc lựa chọn từ trình duyệt. Xem [báo cáo](../verification/BORROW_APPROVAL_ROUTING.md).

## Duyệt trong tra cứu chung — V17,

Không còn menu Hồ sơ điện tử riêng. Mở **Tra cứu hồ sơ bệnh án**, chọn Trạng thái xét duyệt **Chờ duyệt**, mở mã và ghi quyết định/mật khẩu cấp 2 như trước. Chuông thông báo dẫn tới cùng trang /director/records/{id}. Hồ sơ chỉ điện tử được công bố ngay sau duyệt, không cần giấy; nếu sau này có giấy, Archivist bổ sung vào đúng bản ghi.

Bảng dùng [records/list.html](../../src/main/resources/templates/records/list.html), phần duyệt nguồn bác sĩ dùng [electronic/detail.html](../../src/main/resources/templates/electronic/detail.html). Xem [mô hình dữ liệu](../database/UNIFIED_MEDICAL_RECORDS.md). Hướng dẫn này thay cách mở mục Hồ sơ điện tử riêng bên dưới.

## Giao diện bàn làm việc mới —

Các trang dùng thanh đầu trang xanh, tên nhân viên/vai trò, chuông thông báo thật và menu ☰ thu gọn. Bấm thẻ Tác vụ nhanh để vào chức năng; bảng gần đây và số liệu được truy vấn thật theo quyền. Reader chỉ thấy bệnh án điện tử của mình; Admin chỉ có dữ liệu tài khoản và nhật ký, không có dữ liệu bệnh nhân.

Vị trí chỉnh giao diện: [home.html](../../src/main/resources/templates/home.html) (banner, thẻ, bảng), [fragments/layout.html](../../src/main/resources/templates/fragments/layout.html) (header/menu dùng chung), [portal.css](../../src/main/resources/static/css/portal.css) (màu sắc/bố cục), [portal.js](../../src/main/resources/static/js/portal.js) (menu), `DashboardService.java` (tác vụ/số liệu theo role). Giữ các đường dẫn thật và form POST đăng xuất có CSRF khi chỉnh HTML. Thông báo đăng nhập 5 giây vẫn hoạt động.

Thông tin bệnh án điện tử mới có thêm khoa/loại, ngày điều trị, cờ tử vong, nguồn và ghi chú tùy chọn. Các trường được hiển thị tại [electronic/detail.html](../../src/main/resources/templates/electronic/detail.html) để kiểm tra/duyệt; quy trình công bố không thay đổi.


## Bổ sung — Duyệt và công bố hồ sơ điện tử,

Nhận thông báo khi nhân viên lưu trữ trình hồ sơ mới. Mở chuông hoặc **Hồ sơ điện tử** (`/director/electronic-records`), lọc Chờ duyệt, mở mã hồ sơ. Kiểm tra người bệnh/lịch sử, chọn Duyệt và công bố hoặc Từ chối; nhập căn cứ và mật khẩu cấp 2 đã thiết lập rồi ghi quyết định.

Duyệt thành công cập nhật **Đã công bố** và ngày công bố trong cùng giao dịch. Bác sĩ nhận thông báo, xem được kết quả ngay, không có bước xác nhận giấy hoặc nhập kho. Từ chối không công bố. Không được duyệt trước khi lưu trữ kiểm tra; bản đã quyết định không cho sửa/duyệt lặp qua chức năng này.

File HTML: [electronic/list.html](../../src/main/resources/templates/electronic/list.html), [electronic/detail.html](../../src/main/resources/templates/electronic/detail.html). Xem [luồng và dữ liệu](../database/ELECTRONIC_RECORDS.md).

## Thông báo đăng nhập — cập nhật

Sau đăng nhập thành công, thông báo nổi ở góc trên bên phải hiện trong **5 giây** rồi tự đóng; có thể bấm **×** để đóng sớm. Thông báo chỉ hiện một lần sau đăng nhập, không hiện lại khi tải lại/quay về trang chủ. Quy tắc dùng chung cả bốn vai trò.

Giao diện nằm tại `src/main/resources/templates/home.html`; JavaScript tại `src/main/resources/static/js/login-toast.js` (thời gian `5000` mili giây); CSS `.login-toast` trong `src/main/resources/static/css/app.css`. Cờ đăng nhập được đặt trong `SecurityConfig.java` và tiêu thụ một lần trong `HomeController.java`. Cần bật JavaScript để thấy thông báo nổi.

## Đăng ký không ngày — cập nhật

Reader chọn nhiều hồ sơ và mục đích, không còn chọn; từng hồ sơ được trình/duyệt riêng. Director vẫn xử lý PENDING_APPROVAL bằng mật khẩu cấp 2. Sau duyệt, Archivist chỉ định ngày nhận không sau ngày đăng ký + 2 ngày lịch. Trước đó lịch hiển thị chờ nhân viên lưu trữ xếp.

Yêu cầu mới đã quá hạn nhận không được phê duyệt để mở quyền mới; cần từ chối/hủy và đăng ký mới theo nghiệp vụ. Lịch cũ LEGACY được giữ, không hồi tố áp giới hạn mới. Quyền xem chi tiết Reader vẫn bắt đầu từ phê duyệt hợp lệ, được giới hạn bởi hạn nhận/lịch hiện tại. Xem [CR-011](../changes/CR-011_borrowing-batches-and-pickup-window.md).


## Các file HTML để chỉnh giao diện Director / Giám đốc

Đối chiếu với code hiện tại. Các liên kết dưới đây mở file nguồn trong dự án. Gốc giao diện là `src/main/resources/templates/`; cấu trúc thư mục chia theo chức năng, không chia thành một thư mục riêng cho từng role.

### Màn hình của vai trò này

| File HTML | Đường dẫn website | Nội dung bạn có thể chỉnh |
|---|---|---|
| [records/list.html](../../src/main/resources/templates/records/list.html) | `/director/records` | Tra cứu hồ sơ đã công bố. |
| [records/basic-detail.html](../../src/main/resources/templates/records/basic-detail.html) | `/director/records/{id}` | Metadata phục vụ giám sát; không dùng trang nghiệp vụ kho của Archivist. |
| [reading/list.html](../../src/main/resources/templates/reading/list.html) | `/director/requests` | Danh sách yêu cầu, trạng thái trình duyệt và liên kết Theo dõi yêu cầu. |
| [reading/detail.html](../../src/main/resources/templates/reading/detail.html) | `/director/requests/{id}` | Form phê duyệt/từ chối bằng mật khẩu cấp 2 khi PENDING_APPROVAL; lịch sử xử lý. |
| [account/secret.html](../../src/main/resources/templates/account/secret.html) | `/director/approval-secret` | Thiết lập mật khẩu cấp 2. Đây là trang Director, không phải trang quản trị tài khoản Admin. |
| [catalog/retention-policies.html](../../src/main/resources/templates/catalog/retention-policies.html) | `/director/retention-policies` | Phần xác nhận/ngừng dùng chính sách; dùng chung file với Archivist. |
| [inventory/list.html](../../src/main/resources/templates/inventory/list.html) | `/director/inventories` | Danh sách đợt kiểm kê. Director xem kết quả tổng hợp qua báo cáo; không mở inventory/detail.html vì chứa vị trí chi tiết. |
| [report/list.html](../../src/main/resources/templates/report/list.html) | `/director/reports` | Báo cáo/kiểm kê tổng hợp theo quyền; bộ lọc, bảng, xuất và in. |

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

## Chuông chờ duyệt theo CR-008 —

Sau Archivist kiểm tra và trình, các Director ACTIVE nhận thông báo **Có yêu cầu đọc chờ phê duyệt**. Tải lại trang rồi bấm chuông → Mở yêu cầu. Tại PENDING_APPROVAL, nhập quyết định/lý do và mật khẩu cấp 2 để duyệt hoặc từ chối. Khi Reader mới gửi SUBMITTED, công việc thuộc Archivist trước, chưa có thông báo trình duyệt.

**Hồ sơ** mở metadata; **Theo dõi yêu cầu** mở trang quyết định/lịch sử. Reader được mở chi tiết từ lúc duyệt đến hết ngày đọc cuối còn hiệu lực. Nội dung này thay thế mô tả chuông Director rỗng của phiên bản trước. Xem [báo cáo](../verification/WORKFLOW_ALERTS_AND_READER_ACCESS.md).


## Bổ sung — Thông báo quyết định

Duyệt/từ chối thành công tự tạo thông báo nội bộ cho Reader sở hữu yêu cầu, cùng giao dịch quyết định. Sai mật khẩu cấp 2 hoặc lỗi nghiệp vụ không tạo thông báo thành công. Không gửi email.

Chuông có trên trang Director, nhưng hộp thông báo nghiệp vụ hiện chỉ dành cho Reader. Director vẫn theo dõi tại danh sách yêu cầu. Xem [FR-11](../verification/FR11_INTERNAL_NOTIFICATIONS.md).

 Áp dụng cho phiên bản mở rộng FR.

## 1. Bạn được sử dụng những gì?

Director được đăng nhập, tra cứu metadata, xác nhận/ngừng chính sách, phê duyệt/từ chối yêu cầu đọc bằng mật khẩu cấp 2 và xem báo cáo/kiểm kê tổng hợp. Chỉ Director duyệt; chưa ủy quyền.

Trong bản hiện tại, Director không nhập/sửa/chuyển hồ sơ, không quản lý cây kho, không nhận vị trí kho chi tiết, ghi chú hoặc nhật ký vận hành của Archivist. Phê duyệt **chính sách bảo quản** khác với phê duyệt **yêu cầu đọc hồ sơ**; luồng yêu cầu đọc đã triển khai: xem mục 9 về mật khẩu cấp 2 và phê duyệt request.

## 2. Đăng nhập và điều hướng

1. Mở `http://localhost:8080/login` nếu ứng dụng chạy cùng máy bằng cấu hình mặc định; nếu được cấp địa chỉ khác thì dùng địa chỉ đó.
2. Nhập tài khoản Director và mật khẩu được cấp. Demo hiện có: `director1`; không có mật khẩu mặc định mới.
3. Bấm **Đăng nhập**, kiểm tra **Trang giám đốc**, vai trò **DIRECTOR** ở `/director/home`.
4. Từ trang chủ chọn **Xác nhận chính sách bảo quản** hoặc **Tra cứu hồ sơ bệnh án**.

## 3. Xem và kiểm tra chính sách bảo quản

1. Bấm **Xác nhận chính sách bảo quản** để tới `/director/retention-policies`.
2. Tìm chính sách theo mã/tên và phiên bản trong bảng.
3. Đọc đủ số năm, ngày hiệu lực từ/đến, căn cứ/phạm vi, trạng thái, người tạo và người xác nhận nếu đã có.

| Nội dung cần kiểm tra | Câu hỏi đối chiếu |
|---|---|
| Mã và tên | Nhóm hồ sơ áp dụng đã rõ chưa? |
| Phiên bản | Có phải phiên bản đang cần đưa vào sử dụng không? |
| Số năm | Có đúng quy tắc mô phỏng của dự án và căn cứ đã ghi không? |
| Hiệu lực | Có bao phủ ngày đầu lưu của hồ sơ dự định nhập không? |
| Căn cứ/phạm vi | Đủ để Archivist lựa chọn đúng nhóm chưa? |
| Bản cùng mã đang hoạt động | Có chồng khoảng hiệu lực với bản định xác nhận không? |

Khoảng hiệu lực bao gồm cả hai đầu. Ngày kết thúc trống nghĩa là chưa giới hạn ngày kết thúc. Bốn nhóm 10/15/20/30 năm là mô phỏng đã chọn cho bài tập, không khẳng định pháp luật hiện hành.

## 4. Xác nhận một bản dự thảo

1. Thực hiện kiểm tra ở mục 3, bảo đảm bản dự thảo đúng nội dung cần dùng.
2. Bấm **Xác nhận** tại dòng chính sách Dự thảo.
3. Kiểm tra thông báo **Đã xác nhận chính sách** và trạng thái chuyển sang đã xác nhận/cho phép dùng khi đúng hiệu lực.
4. Kiểm tra tên người xác nhận tương ứng tài khoản của mình.
5. Thông báo Archivist có thể dùng bản này cho hồ sơ phù hợp; hiện chưa có thông báo/email tự động.

Nếu nội dung sai, chưa bấm xác nhận; trao đổi Archivist tạo phiên bản đúng. Hiện chưa có nút sửa nội dung, trả bổ sung hoặc từ chối kèm lý do trên màn hình policy. Không dùng tài khoản Archivist để thay vai trò của họ.

## 5. Ngừng dùng hoặc thay phiên bản

1. Xác định đúng bản cần ngừng và ảnh hưởng đến các hồ sơ chuẩn bị nhập.
2. Với chính sách đang hoạt động, bấm **Ngừng dùng**.
3. Kiểm tra thông báo và trạng thái **Đã ngừng dùng**.
4. Nếu cần bản thay thế, phối hợp Archivist tạo phiên bản mới và kiểm tra/xác nhận theo mục 4.

Ngừng dùng chặn lựa chọn mới; không xóa phiên bản và không tự tính lại hạn hồ sơ đã lưu. Snapshot là bản chụp căn cứ áp dụng của hồ sơ cũ, vẫn được giữ.

Không xác nhận hai bản cùng mã đang hoạt động với khoảng hiệu lực chồng nhau. Nếu thay bản cũ bằng bản mới có thời gian chồng, chỉ ngừng bản cũ sau khi đã xác định phương án thay thế. Không có nút bật lại một bản APPROVED đã ngừng; nếu cần tiếp tục sử dụng thì tạo phiên bản mới phù hợp.

## 6. Tra cứu và xem chi tiết hồ sơ

1. Từ trang chính sách bấm **← Trang giám đốc**, rồi chọn **Tra cứu hồ sơ bệnh án**; hoặc dùng thanh điều hướng ở trang chủ.
2. Tại `/director/records`, nhập số lưu trữ/số bệnh án/mã người bệnh đầy đủ hoặc tên có/không dấu.
3. Nếu cần, thêm năm sinh, khoa, loại, khoảng ngày ra viện hoặc trạng thái lưu chuyển.
4. Bấm **Tìm kiếm**; chọn số dòng/sắp xếp và chuyển trang.
5. Bấm số lưu trữ để xem metadata cơ bản; đối chiếu năm sinh/mã/đợt điều trị để tránh nhầm người trùng tên.
6. Bấm **Xóa bộ lọc** để tìm lại từ đầu.

Chỉ hồ sơ STORED được hiển thị. IN_STORAGE nghĩa là trong kho/khả dụng; UNAVAILABLE là tạm không khả dụng; READING là đang giao đọc. Trang chi tiết không cung cấp vị trí kho, ghi chú, snapshot policy hay nhật ký nội bộ.

## 7. Xử lý tình huống thường gặp

| Tình huống | Cách xử lý |
|---|---|
| Không thấy nút Xác nhận | Kiểm tra đúng tài khoản Director và dòng còn DRAFT; bản đã xác nhận không duyệt lại bằng nút này |
| Bị chặn do chồng hiệu lực | Đối chiếu các bản cùng mã; thống nhất bản còn dùng và bản cần ngừng trước khi thực hiện lại |
| Chính sách đúng nhưng hồ sơ cũ không dùng được | Đối chiếu ngày đầu lưu với hiệu lực, không đổi ngày hồ sơ để vượt kiểm tra |
| Cần sửa số năm/căn cứ | Yêu cầu Archivist tạo phiên bản mới; không sửa trực tiếp bản đã tạo |
| Không tìm thấy hồ sơ | Xóa bộ lọc, tra mã chính xác; nhờ Archivist kiểm tra đã nhập chính thức chưa |
| 403 khi mở URL kho/nhập | Đây là nghiệp vụ Archivist; quay về trang chủ Director |
| 404 chi tiết | Chọn lại từ danh sách; hồ sơ nháp không nằm trong phạm vi Director |

## 8. Đăng xuất và bài thực hành

Trở về trang chủ hoặc trang tra cứu có thanh điều hướng, bấm **Đăng xuất** và kiểm tra đã về login.

Với dữ liệu mô phỏng đã thống nhất cho bài thực hành:

- [ ] Đăng nhập đúng DIRECTOR.
- [ ] Đọc một chính sách dự thảo và kiểm tra căn cứ/hiệu lực.
- [ ] Xác nhận bản phù hợp, đối chiếu trạng thái và người xác nhận.
- [ ] Tra cứu một hồ sơ giả đã nhập chính thức và xem chi tiết cơ bản.
- [ ] Nếu cần thử ngừng dùng, chọn đúng chính sách dành cho bài thử và phối hợp Archivist để tránh gián đoạn nhập.
- [ ] Đăng xuất.

Các ô dành cho người dùng tự thực hành. Xem thêm [hướng dẫn Archivist](archivist.md), [mục lục tài liệu](../README.md), [báo cáo năm chức năng](../verification/FIVE_FEATURE_COMPLETION_REPORT.md).

## 9. Mật khẩu cấp 2 và duyệt yêu cầu đọc

1. Trên menu mở **Mật khẩu cấp 2**. Nhập mật khẩu đăng nhập hiện tại, đặt secret riêng 12–128 ký tự khác password đăng nhập. App không đặt secret mặc định cho bạn.
2. Mở **Yêu cầu / lịch đọc**, chọn PENDING_APPROVAL; đọc mục đích, từng ngày, kết quả Archivist và phiên bản nội dung.
3. Chọn phê duyệt/từ chối, nhập căn cứ/lý do và mật khẩu cấp 2; bấm ghi quyết định. Mỗi quyết định phải nhập lại secret.
4. Sai secret không ghi quyết định; năm lần sai trong cửa sổ 15 phút tạm chặn duyệt. Không gửi secret cho người khác hoặc vào nội dung lý do.
5. Không tự duyệt request của mình, kể cả tài khoản đã đổi role từ Reader. Chưa có ủy quyền, chỉ Director duyệt theo quyết định hiện tại.
6. APPROVED chưa phải đã giao; Archivist còn phải xác nhận lịch và thực hiện giao–trả từng ngày. Người yêu cầu theo dõi trong app, chưa có email.
7. Nếu Reader sửa trước giao, revision tăng và quyết định/lịch cũ mất hiệu lực; phải được kiểm tra và duyệt lại.

## 10. Báo cáo và kiểm kê tổng hợp

Mở **Báo cáo / nhật ký** để xem danh mục/thời hạn, tổng hợp kho, yêu cầu/quyết định, sổ giao–trả, nhật ký được phép, kết quả kiểm kê hoặc thống kê xử lý yêu cầu. Chọn bộ lọc rồi xem, xuất CSV hoặc In/lưu PDF qua trình duyệt.

Director không được báo cáo lịch sử vị trí hoặc đường dẫn ngăn; báo cáo audit hồ sơ đã lược chi tiết nội bộ. Mở **Kiểm kê** để xem danh sách đợt/số đã kiểm; chi tiết vị trí thuộc Archivist, kết quả tổng hợp xem trong báo cáo.

Phê duyệt request đọc và xác nhận chính sách bảo quản là hai thao tác khác nhau. Xem [báo cáo FR](../verification/FR_COMPLETION_REPORT.md) để đối chiếu toàn luồng.

## Tách bảng tra cứu —

Tra cứu mặc định chỉ có Hồ sơ chính thức (STORED/PUBLISHED). Chọn tab Xét duyệt khai báo (`/director/records?scope=DECLARATIONS`) để mở bảng riêng theo dõi và xét duyệt khai báo của bác sĩ; có thể lọc Chờ duyệt. Hồ sơ chưa công bố không trộn vào bảng chính thức. Template chung là `records/list.html`; liên kết chi tiết vẫn mở trang xử lý theo quyền Director. Mục yêu cầu của Director giữ danh sách xét duyệt; Reader dùng tên Lịch sử yêu cầu.
