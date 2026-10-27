# Hướng dẫn sử dụng — Archivist (Nhân viên lưu trữ)

## Tự sửa thông tin và đề nghị đổi vai trò/khoa — V24

Menu tên nhân viên → **Thông tin cá nhân** → **Chỉnh sửa thông tin cá nhân** cho phép sửa họ tên, căn cước, ngày sinh và giới tính. Tên tài khoản/email giữ nguyên; căn cước không được trùng nhân viên khác. Nhấp mục Vai trò hoặc Khoa/phòng trong trang sửa để mở form đề nghị.

Tại form đề nghị, chọn vai trò/khoa mong muốn và nhập lý do. Giám đốc xét duyệt, sau đó Admin nhận việc và bấm áp dụng; trong lúc chờ vẫn dùng quyền và khoa hiện tại. Không còn mục đề nghị riêng trong menu tài khoản hoặc nút riêng trên trang thông tin cá nhân. Sau khi áp dụng, đăng nhập lại.

Nhân viên nhận kết quả phê duyệt **sau khi Admin áp dụng**; nếu Giám đốc từ chối, nhận ngay thông báo từ chối với lý do bên dưới. Không gửi email thật. Theo dõi đơn tại `/account/change-requests`. [Cập nhật V25](../verification/STAFF_PROFILE_AND_NOTICE_TIMING.md).

HTML: `templates/account/profile.html`, `account/profile-edit.html`, `account/change-list.html`, `account/change-detail.html`. [Chi tiết](../verification/STAFF_SELF_SERVICE_AND_APPROVAL.md).

**Đăng nhập lần đầu — V23:** dùng mật khẩu ban đầu trong file tài khoản, sau đó nhập mật khẩu mới và xác nhận ở `/account/initial-setup` (không yêu cầu mật khẩu cũ). Mật khẩu 12–128 ký tự và khác mật khẩu ban đầu. Phải lưu thành công mới xử lý nghiệp vụ. Nhân viên lưu trữ không phải tạo mật khẩu cấp 2. Đổi mật khẩu các lần sau tại menu cá nhân vẫn cần mật khẩu hiện tại. [Chi tiết](../verification/INITIAL_CREDENTIAL_SETUP.md).

**Tài khoản sau reset:** Trần Thị Linh — `linh.tt18091993@mrm.com`. Mật khẩu riêng tại `.secrets/operational-accounts.txt`. Các ví dụ `archivist1` bên dưới là tên kiểm thử cũ đã xóa. Kho/danh mục và 16 bệnh án được giữ, mã DEMO đã đổi; bắt đầu xử lý những đăng ký mới từ thời điểm reset. Xem [báo cáo reset](../verification/OPERATIONAL_BASELINE_RESET.md).

## Thông báo chung và dọn thông báo đã xử lý — V21,

Chuông mở mặc định tab **Đang chờ xử lý**: chỉ giữ những việc chưa kiểm tra/phê duyệt/xác nhận hoặc còn cần bổ sung. Mọi thẻ đều dùng bố cục người yêu cầu, hồ sơ và ngày giờ như Archivist; lịch đã xác nhận có ngày/phòng nhận. Chuông chỉ tính việc đang chờ chưa đọc. Đã đọc không đồng nghĩa đã xử lý.

Mở **Đã xử lý** để xem kết quả và dùng **Xóa thông báo**, hoặc **Xóa tất cả đã xử lý** để dọn hộp thư. Các thông báo đang chờ được giữ và không có nút xóa riêng; backend cũng chặn xóa. Xóa chỉ ảnh hưởng hộp thư của bạn, giữ yêu cầu và lịch sử nghiệp vụ. Admin vẫn không được nhận dữ liệu bệnh án.

HTML: `templates/notification/inbox.html`; service/controller: `NotificationService.java`, `NotificationController.java`. Xem [báo cáo và kiểm thử](../verification/UNIFIED_NOTIFICATIONS_AND_CLEANUP.md).

## Thông báo cần kiểm tra —

Thẻ “Có yêu cầu đọc cần kiểm tra” hiển thị **Người yêu cầu** (họ tên/tên đăng nhập), **Hồ sơ yêu cầu** (mã/người bệnh), **Ngày giờ yêu cầu** theo giờ Việt Nam. Không hiển thị mục đích hoặc UUID yêu cầu trong phần nội dung thẻ; mở yêu cầu để xét nội dung nghiệp vụ. Thông báo cũ cũng được trình bày theo cách này.

HTML: `templates/notification/inbox.html`; Java: `NotificationService.java`, DTO `WorkInfo`. [Báo cáo và kiểm thử](../verification/DIRECTOR_REPORTS_AND_NOTICE_PRESENTATION.md).

## Form xét duyệt gộp —

Mở một yêu cầu mới và chọn **Kết quả**:

- **Phê duyệt cho mượn**: ghi chú không bắt buộc; tích xác nhận, chọn ngày nhận và nhập phòng nhận, rồi bấm Xác nhận phê duyệt và lịch nhận. Toàn bộ được lưu cùng lúc.
- **Trả bổ sung**: ô đổi thành Cần bổ sung, phải nhập nội dung; ngày/phòng nhận được ẩn.
- **Từ chối**: phải nhập Lý do hủy / từ chối; ngày/phòng nhận được ẩn.
- **Lượt cần Director**: chỉ trình Giám đốc hoặc trả bổ sung, chưa có ngày/phòng nhận. Sau khi Director duyệt mới xếp lịch như trước.

Trang xét duyệt đã bỏ các khối lịch sử xử lý, giao–trả và hủy. Giao/nhận trả được chuyển sang **Quản lý giao–trả hồ sơ** trên yêu cầu đã xác nhận lịch; ô Chờ giao ở trang chủ cũng mở thẳng trang này. HTML xét duyệt `reading/detail.html`; giao–trả `reading/circulation.html`; tương tác chọn kết quả `static/js/request-review.js`. Xem [báo cáo](../verification/ARCHIVIST_REVIEW_FORM.md).

## Menu tài khoản cá nhân —

Menu tên nhân viên gồm **Thông tin cá nhân**, **Thay đổi mật khẩu**, **Đăng xuất**. Dùng trang chỉnh sửa thông tin để sửa họ tên/căn cước/ngày sinh/giới tính hoặc nhấp Vai trò/Khoa để mở đề nghị; email không chỉnh sửa ở đây.

Đổi mật khẩu tại `/account/password`: nhập mật khẩu hiện tại, mật khẩu mới 12–128 ký tự và xác nhận. Phiên đang đổi tiếp tục dùng; các phiên khác hết hiệu lực ở lần truy cập tiếp theo. Không dùng lại mật khẩu cũ; Director không được dùng trùng mật khẩu cấp 2.

HTML: `templates/fragments/layout.html` (menu), `templates/account/profile.html` (cá nhân), `templates/account/password.html` (mật khẩu). Java: `PersonalAccountController.java`, `AccountService.java`. Menu/CSS: `static/js/portal.js`, `static/css/portal.css`.

Xem [báo cáo thay đổi và kiểm thử](../verification/PERSONAL_MENU_AND_DIRECTOR_DASHBOARD.md).

## Ô thống kê và thông báo hủy —, V20

Ba ô trên trang chủ đã có liên kết: **Chờ kiểm tra** mở hai bảng yêu cầu mượn/bệnh án mới khai báo; **Hồ sơ trong kho** mở tra cứu đã lọc hồ sơ đang trong kho; **Chờ giao** mở các phiên đã xác nhận lịch chưa giao, có ngày nhận và nút Mở yêu cầu.

Khi Reader hủy yêu cầu, chuông hiện “Yêu cầu với hồ sơ [mã] đã hủy”. Thông báo công việc cũ của cùng yêu cầu được ẩn, không tính vào số chưa đọc; vẫn giữ bản ghi database. Thông báo đã hủy không có nút Mở yêu cầu, vẫn đánh dấu đã đọc được. V20 áp dụng cả yêu cầu hủy cũ.

Ba mẫu tử vong đã tạo: DEMO-MRM-006, 008 (chỉ điện tử), 009 (giấy và điện tử, lưu 30 năm). Bộ chọn mượn dùng nền hồng/viền đỏ kèm chữ; cột ghi chú tra cứu có “Bệnh án tử”. Dùng 009 để thử mượn giấy và trình Director. [Báo cáo](../verification/ARCHIVIST_NOTICES_AND_DECEASED_RECORDS.md).

HTML danh mục mới: `templates/reading/archivist-work.html`; thông báo: `templates/notification/inbox.html`; ô thống kê dùng `templates/home.html` và dữ liệu `DashboardService.java`.

## Thẩm quyền duyệt mượn và bản giấy nháp — (V19)

Trong yêu cầu mượn mới, mở thông báo hoặc danh sách yêu cầu, chọn hồ sơ:

1. Lượt không có hồ sơ tử vong: chọn **Phê duyệt cho mượn**, **Từ chối** hoặc **Trả bổ sung**, nhập kết quả kiểm tra rồi lưu. Khi phê duyệt, hệ thống ghi chính tài khoản nhân viên lưu trữ là người quyết định và thông báo cho Reader; không trình Giám đốc.
2. Lượt có ít nhất một hồ sơ tử vong: **mọi yêu cầu con** chỉ có lựa chọn **Đủ điều kiện — trình Giám đốc** hoặc **Trả bổ sung**. Không tự duyệt hồ sơ thường nằm trong cùng lượt này. Giám đốc nhận thông báo cho từng yêu cầu được trình.
3. Sau quyết định phê duyệt, mở lại yêu cầu để xác nhận ngày nhận/phòng đọc; tiếp tục giao–nhận trả. Ngày nhận không quá ngày đăng ký + 2 ngày lịch, không được chọn ngày đã qua.

Yêu cầu cũ giữ tuyến Giám đốc. Màn hình cho biết tuyến duyệt của từng lượt. Việc khai báo/công bố bệnh án mới không đổi.

**Bản giấy nháp** (`Tra cứu hồ sơ → Bản giấy nháp`) là dữ liệu nhập tạm bằng thao tác Lưu nháp, chưa xác nhận nhập kho. Đây không phải bản giấy đã được mượn hoặc bản điện tử chờ Giám đốc công bố. Chỉ Archivist xem được; hoàn thiện trường bắt buộc và xác nhận nhập kho để chuyển sang Hồ sơ chính thức. Reader không thấy hoặc mượn nháp.

HTML: `reading/detail.html` (kiểm tra, duyệt, lịch/giao trả), `records/list.html` (tab nháp), `records/form.html` (nhập hồ sơ giấy). Xem [báo cáo](../verification/BORROW_APPROVAL_ROUTING.md).

## Kho và khai báo trong danh sách chung — V17,

Mở **Tra cứu hồ sơ bệnh án**. Dùng Hình thức và Trạng thái xét duyệt để tìm khai báo mới; mở mã để kiểm tra/trả bổ sung/trình Giám đốc ngay trong trang chi tiết. Không còn menu Hồ sơ điện tử riêng.

Khi nhận bản giấy của bệnh án đã có trên phần mềm, tìm mã trước, mở bệnh án đã được công bố rồi chọn **Bổ sung bản giấy**. Điền các thông tin nhập kho còn thiếu và lưu. Hệ thống dùng cùng một bản ghi, giữ mã và lịch sử; không nhập mới bệnh án đó lần nữa. Hồ sơ giấy hoàn toàn chưa có trên hệ thống vẫn nhập từ chức năng Nhập hồ sơ giấy cũ. Mã MRM-REC là mã hệ thống cho bác sĩ, không tự tạo một phiếu giấy thứ hai với mã đó.

HTML tra cứu: [records/list.html](../../src/main/resources/templates/records/list.html); bổ sung giấy: [records/form.html](../../src/main/resources/templates/records/form.html); chi tiết kho: [records/detail.html](../../src/main/resources/templates/records/detail.html); kiểm tra khai báo chỉ điện tử: [electronic/detail.html](../../src/main/resources/templates/electronic/detail.html). Xem [mô hình dữ liệu](../database/UNIFIED_MEDICAL_RECORDS.md). Phần này thay hướng dẫn mở menu Hồ sơ điện tử riêng bên dưới.

## Giao diện bàn làm việc mới —

Các trang dùng thanh đầu trang xanh, tên nhân viên/vai trò, chuông thông báo thật và menu ☰ thu gọn. Bấm thẻ Tác vụ nhanh để vào chức năng; bảng gần đây và số liệu được truy vấn thật theo quyền. Reader chỉ thấy bệnh án điện tử của mình; Admin chỉ có dữ liệu tài khoản và nhật ký, không có dữ liệu bệnh nhân.

Vị trí chỉnh giao diện: [home.html](../../src/main/resources/templates/home.html) (banner, thẻ, bảng), [fragments/layout.html](../../src/main/resources/templates/fragments/layout.html) (header/menu dùng chung), [portal.css](../../src/main/resources/static/css/portal.css) (màu sắc/bố cục), [portal.js](../../src/main/resources/static/js/portal.js) (menu), `DashboardService.java` (tác vụ/số liệu theo role). Giữ các đường dẫn thật và form POST đăng xuất có CSRF khi chỉnh HTML. Thông báo đăng nhập 5 giây vẫn hoạt động.

Thông tin bệnh án điện tử mới có thêm khoa/loại, ngày điều trị, cờ tử vong, nguồn và ghi chú tùy chọn. Các trường được hiển thị tại [electronic/detail.html](../../src/main/resources/templates/electronic/detail.html) để kiểm tra/duyệt; quy trình công bố không thay đổi.


## Bổ sung — Kiểm tra hồ sơ điện tử do bác sĩ khai báo,

Mở chuông thông báo hoặc menu **Hồ sơ điện tử** (`/archivist/electronic-records`). Lọc **Mới gửi**, mở mã hồ sơ, đối chiếu thông tin người bệnh. Chọn **Xác nhận và trình Giám đốc** nếu đủ; chọn yêu cầu bổ sung nếu thiếu; nhập nội dung kiểm tra rồi gửi. Bác sĩ bổ sung sẽ đưa hồ sơ về Mới gửi để kiểm tra lại. Nhân viên lưu trữ không sửa thay thông tin bệnh nhân qua luồng này và không tự công bố.

Giám đốc duyệt là hồ sơ điện tử được công bố ngay: không yêu cầu nhận hồ sơ giấy, chọn ngăn, chính sách hoặc xác nhận lịch nhập kho. Chức năng **Nhập hồ sơ giấy chưa có trên hệ thống** vẫn giữ ở `/archivist/records/new`, sử dụng form, chính sách và vị trí hiện có. Không nhập hồ sơ điện tử mới như một hồ sơ giấy để hoàn tất duyệt.

File HTML luồng mới: [electronic/list.html](../../src/main/resources/templates/electronic/list.html), [electronic/detail.html](../../src/main/resources/templates/electronic/detail.html). Nhập giấy vẫn tại [records/form.html](../../src/main/resources/templates/records/form.html). Xem [luồng và dữ liệu](../database/ELECTRONIC_RECORDS.md).

## Thông báo đăng nhập — cập nhật

Sau đăng nhập thành công, thông báo nổi ở góc trên bên phải hiện trong **5 giây** rồi tự đóng; có thể bấm **×** để đóng sớm. Thông báo chỉ hiện một lần sau đăng nhập, không hiện lại khi tải lại/quay về trang chủ. Quy tắc dùng chung cả bốn vai trò.

Giao diện nằm tại `src/main/resources/templates/home.html`; JavaScript tại `src/main/resources/static/js/login-toast.js` (thời gian `5000` mili giây); CSS `.login-toast` trong `src/main/resources/static/css/app.css`. Cờ đăng nhập được đặt trong `SecurityConfig.java` và tiêu thụ một lần trong `HomeController.java`. Cần bật JavaScript để thấy thông báo nổi.

**Giữ chỗ hồ sơ,:** đăng ký SUBMITTED/NEEDS_INFO/PENDING_APPROVAL/APPROVED hoặc lịch CONFIRMED chưa kết thúc sẽ ẩn hồ sơ khỏi bộ chọn của mọi Reader. Yêu cầu quá hạn chưa đóng vẫn giữ chỗ; xử lý hủy/từ chối theo nghiệp vụ để giải phóng. Trả đầy đủ/không đến/hủy hết các phiên sẽ giải phóng nếu không còn yêu cầu giữ chỗ khác và hồ sơ còn khả dụng. Không cần xóa lịch sử. Xem [quy tắc](../verification/BORROW_PICKER_AVAILABILITY.md).

## Xếp ngày nhận — quy trình mới

Reader đăng ký không chọn ngày, có thể gửi nhiều hồ sơ trong một lần; mỗi hồ sơ là một yêu cầu độc lập. Bạn kiểm tra/trình như trước. Khi Director đã duyệt, mở chi tiết **Xác nhận lịch** và chọn **Ngày nhận do nhân viên lưu trữ chỉ định**, nhập hướng dẫn/phòng đọc rồi xác nhận.

Ngày nhận phải từ hôm nay, không trước ngày đăng ký và không sau ngày đăng ký + 2 ngày lịch, tính cả cuối tuần. Nếu hết hạn, cần hủy/đăng ký mới; không lùi ngày hoặc gia hạn bằng sửa mục đích. Hệ thống chặn xếp trùng hồ sơ/ngày và chặn giao ngoài ngày nhận đã xác nhận. Trước khi xếp ngày chưa có reading_sessions. Giao–trả/ngoại lệ giữ nghiệp vụ cũ.

Các yêu cầu LEGACY trước V14 vẫn giữ ngày cũ. Đọc [báo cáo](../verification/BORROWING_REGISTRATION.md) trước khi thay giao diện reading/detail.html; ô chọn ngày nhận chỉ render cho Archivist.


## Các file HTML để chỉnh giao diện Archivist / Nhân viên lưu trữ

Đối chiếu với code hiện tại. Các liên kết dưới đây mở file nguồn trong dự án. Gốc giao diện là `src/main/resources/templates/`; cấu trúc thư mục chia theo chức năng, không chia thành một thư mục riêng cho từng role.

### Màn hình của vai trò này

| File HTML | Đường dẫn website | Nội dung bạn có thể chỉnh |
|---|---|---|
| [records/list.html](../../src/main/resources/templates/records/list.html) | `/archivist/records` | Tra cứu hồ sơ chính thức/nháp theo quyền. |
| [records/form.html](../../src/main/resources/templates/records/form.html) | `/archivist/records/new; /archivist/records/{id}/edit` | Form nhập mới hoặc sửa: metadata, chính sách, vị trí và đối soát. |
| [records/detail.html](../../src/main/resources/templates/records/detail.html) | `/archivist/records/{id}` | Chi tiết nghiệp vụ, snapshot, lịch sử và form chuyển vị trí/đổi khả dụng. |
| [storage/list.html](../../src/main/resources/templates/storage/list.html) | `/archivist/locations` | Cây Kho–Kệ–Ngăn; tạo/sửa vị trí và sức chứa. |
| [department/list.html](../../src/main/resources/templates/department/list.html) | `/archivist/departments` | Danh sách khoa cố định trong dự án. |
| [catalog/record-types.html](../../src/main/resources/templates/catalog/record-types.html) | `/archivist/record-types` | Danh mục loại bệnh án và các form quản lý. |
| [catalog/retention-policies.html](../../src/main/resources/templates/catalog/retention-policies.html) | `/archivist/retention-policies` | Danh sách và form tạo chính sách dự thảo; phần duyệt thuộc Director. |
| [reading/list.html](../../src/main/resources/templates/reading/list.html) | `/archivist/requests` | Danh sách yêu cầu đọc cần xử lý. |
| [reading/detail.html](../../src/main/resources/templates/reading/detail.html) | `/archivist/requests/{id}` | Kiểm tra/trình/trả bổ sung, xác nhận lịch, giao–trả, ngoại lệ và hủy theo trạng thái. |
| [inventory/list.html](../../src/main/resources/templates/inventory/list.html) | `/archivist/inventories` | Danh sách và form tạo đợt kiểm kê. |
| [inventory/detail.html](../../src/main/resources/templates/inventory/detail.html) | `/archivist/inventories/{id}` | Đối soát từng hồ sơ, ghi chênh lệch và chốt đợt. |
| [report/list.html](../../src/main/resources/templates/report/list.html) | `/archivist/reports` | Bộ lọc/bảng báo cáo, nút xuất CSV và in. |

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

## Chuông công việc mới theo CR-008 —

Reader gửi hoặc gửi lại yêu cầu sẽ tạo thông báo cho các Archivist ACTIVE. Tải lại trang, bấm chuông → Mở yêu cầu để kiểm tra. Chọn **Đủ điều kiện — trình Director** sẽ báo chuông cho Director. Đánh dấu đã đọc chỉ là đã xem thông báo, chưa thay thao tác kiểm tra/trình.

Cột **Hồ sơ** mở chi tiết nghiệp vụ hồ sơ; cột **Theo dõi yêu cầu** mở quy trình/lịch và thao tác xử lý. Mỗi nhân viên có hộp thông báo riêng. Nội dung này thay thế mô tả hộp Archivist rỗng của phiên bản chuông trước. Xem [báo cáo](../verification/WORKFLOW_ALERTS_AND_READER_ACCESS.md).


## Bổ sung — Thông báo kết quả

Trả bổ sung, xác nhận lịch hoặc hủy được phép tự tạo thông báo cho Reader sở hữu yêu cầu, cùng giao dịch nghiệp vụ. Thao tác thất bại không tạo thông báo thành công. Reader tải lại trang rồi bấm chuông để xem; không cần gửi email hay nhập người nhận.

Chuông có trên trang Archivist, nhưng hộp thông báo nghiệp vụ hiện chỉ dành cho Reader; tiếp tục theo dõi công việc tại danh sách yêu cầu. Xem [FR-11](../verification/FR11_INTERNAL_NOTIFICATIONS.md).

 Áp dụng cho phiên bản mở rộng FR. Mục tiêu: chuẩn bị danh mục/vị trí, nhập hoặc đối soát hồ sơ giấy, tra cứu, sửa và theo dõi chuyển kho.

## 1. Phạm vi của bạn và trình tự làm việc

Bạn được xem năm khoa, quản lý loại bệnh án, tạo chính sách dự thảo, quản lý vị trí, nhập/sửa/đối soát hồ sơ, tra cứu chi tiết nghiệp vụ và chuyển vị trí. Bạn còn được kiểm tra/trình yêu cầu, xác nhận lịch, giao–trả, ghi ngoại lệ, kiểm kê và xuất báo cáo; không phê duyệt request hoặc chính sách thay Director.

Trình tự lần đầu: **đăng nhập → kiểm tra danh mục → Director xác nhận chính sách → tạo Kho–Kệ–Ngăn → nhập hồ sơ → tra cứu/đối soát → chuyển vị trí khi cần → đăng xuất**.

Các lần sau dùng lại danh mục và vị trí đã chuẩn bị; không tạo bản trùng cho mỗi hồ sơ. Ứng dụng hiện quản lý thông tin mô tả và vị trí hồ sơ giấy, không có tải lên bản quét; luồng yêu cầu đọc/giao–trả đã bổ sung ở mục 13.

## 2. Đăng nhập và mở các màn hình

1. Mở `http://localhost:8080/login` nếu chạy cùng máy bằng cấu hình mặc định; nếu được cấp địa chỉ khác thì dùng địa chỉ đó.
2. Nhập tài khoản Archivist và mật khẩu được cấp. Demo: `archivist1`; dùng mật khẩu đã đặt khi khởi tạo, không có mật khẩu mặc định mới.
3. Bấm **Đăng nhập**, kiểm tra **Trang nhân viên lưu trữ**, vai trò **ARCHIVIST** tại `/archivist/home`.

| Từ trang chủ | Địa chỉ tương ứng |
|---|---|
| Nhập kho / đối soát hồ sơ | `/archivist/records/new` |
| Tra cứu và xem chi tiết | `/archivist/records` |
| Quản lý vị trí | `/archivist/locations` |
| Quản lý khoa/phòng | `/archivist/departments` |
| Quản lý loại bệnh án | `/archivist/record-types` |
| Quản lý chính sách bảo quản | `/archivist/retention-policies` |

Các địa chỉ trong bảng là phần sau tên máy/cổng; nên bấm liên kết trên web. Trang danh mục có liên kết trở về trang nhân viên lưu trữ; các trang nghiệp vụ có thanh điều hướng chung.

## 3. Chuẩn bị danh mục

### 3.1. Khoa/phòng

1. Mở **Quản lý khoa/phòng** để kiểm tra năm khoa đã thiết lập.
2. Khi nhập hồ sơ, chọn khoa điều trị cuối phù hợp với nguồn giấy.
3. Danh mục này đang cố định theo lựa chọn của dự án học tập, không có thao tác thêm/sửa/xóa khoa trên web.

### 3.2. Loại bệnh án

1. Mở **Quản lý loại bệnh án**, kiểm tra loại đã có trước khi thêm.
2. Nếu thiếu, nhập **Mã loại** và **Tên loại bệnh án**, bấm **Thêm loại bệnh án**.
3. Để sửa tên, tìm dòng tương ứng trong **Danh sách và chỉnh sửa**, đổi **Tên**, bấm **Lưu thay đổi**.
4. Để ngừng dùng, chọn **Ngừng hoạt động** trong dòng đó rồi lưu. Có thể chọn lại Đang hoạt động khi cần.

Mã được giữ nguyên sau tạo; dùng mã chữ Latin như `INTERNAL_MEDICINE`, không dùng tên tiếng Việt làm mã. Loại đã ngừng không hợp lệ cho nhập mới, dù một số màn hình vẫn hiển thị để đối chiếu lịch sử. Ngừng loại không xóa hoặc viết lại hồ sơ cũ.

### 3.3. Chính sách bảo quản

1. Mở **Quản lý chính sách bảo quản**, đọc các phiên bản đã có.
2. Nếu cần bản mới, điền **Mã chính sách**, **Tên**, **Phiên bản**, **Số năm**, **Hiệu lực từ ngày**, ngày kết thúc nếu có, **Căn cứ và phạm vi áp dụng**.
3. Bấm **Lưu dự thảo**; kết quả là bản DRAFT, chưa dùng nhập kho.
4. Thông báo Director kiểm tra và xác nhận trong tài khoản của họ. Ứng dụng chưa tự gửi thông báo/email.
5. Quay lại kiểm tra trạng thái **Đã xác nhận — cho phép dùng khi đúng ngày hiệu lực** trước khi nhập chính thức.

Không sửa trực tiếp nội dung một phiên bản đã tạo; thay đổi quy tắc cần phiên bản mới. Mã và số phiên bản không được trùng cặp đã có.

Quy tắc học tập đã thống nhất: các nhóm mẫu 10/15/20/30 năm; nhiều nhóm lấy số năm dài nhất; tính từ ngày đầu lưu, ngày gốc 29/2 chuyển về 28/2 năm đích. Đây là mô phỏng trong dự án, không phải hướng dẫn pháp luật hiện hành.

**Quan trọng khi nhập hồ sơ cũ:** ngày đầu lưu phải thuộc hiệu lực chính sách. Bản mẫu hiệu lực từ không áp dụng cho ngày đầu lưu trước đó. Chuẩn bị chính sách có hiệu lực phù hợp và nhờ Director xác nhận; giữ nguyên ngày đầu lưu thật.

## 4. Chuẩn bị vị trí: Kho → Kệ → Ngăn

1. Mở **Quản lý vị trí**.
2. Tạo Kho: mã `KHO_A`, tên `Kho A`, loại **Kho**, cha để trống, sức chứa để trống; bấm **Thêm vị trí**.
3. Tạo Kệ: mã `KE_01`, tên `Kệ 01`, loại **Kệ**, cha chọn Kho A; sức chứa để trống.
4. Tạo Ngăn: mã `NGAN_01`, tên `Ngăn 01`, loại **Ngăn**, cha chọn Kệ 01; có thể nhập sức chứa, ví dụ 50.
5. Tạo thêm ngăn khác nếu cần. Nếu mã ví dụ đã tồn tại thì sử dụng vị trí đó hoặc chọn mã mới cho vị trí thực sự khác.

Một ngăn chứa nhiều hồ sơ. Sức chứa để trống nghĩa là không giới hạn; nếu nhập phải là số nguyên dương. Hồ sơ chỉ được gán vào ngăn, không gán trực tiếp vào kho/kệ.

Trong danh sách, dùng phần **Cập nhật** của từng dòng để sửa tên, trạng thái hoặc sức chứa ngăn rồi bấm **Lưu**. Mã, loại và vị trí cha giữ nguyên sau tạo; không có nút xóa cứng.

Không ngừng vị trí còn hồ sơ hoặc còn con hoạt động; không giảm sức chứa thấp hơn số đang chứa. Muốn ngừng một kệ/kho, xử lý hồ sơ ở ngăn trước rồi ngừng con từ dưới lên. Đổi tên không viết lại đường dẫn đã ghi trong lịch sử chuyển trước đây.

## 5. Nhập hồ sơ chính thức

### 5.1. Chuẩn bị trước khi nhập

Kiểm tra hồ sơ giấy và nguồn bàn giao. Xác định đúng người bệnh, đợt điều trị, khoa cuối, loại, ngày đầu lưu và vị trí thực tế. Nếu nguồn còn thiếu dữ liệu cần đối soát, chuyển sang quy trình lưu nháp ở mục 6.

### 5.2. Điền biểu mẫu

1. Mở **Nhập kho / đối soát hồ sơ** ở trang chủ, hoặc **Nhập hồ sơ** trên thanh điều hướng.
2. Điền các trường sau:

| Trường | Cách điền |
|---|---|
| Số lưu trữ | Để trống để hệ thống cấp lúc nhập chính thức, hoặc nhập số nguồn hợp lệ và duy nhất |
| Số bệnh án | Bắt buộc khi nhập chính thức; lấy theo đợt điều trị |
| Mã người bệnh | Nhập nếu nguồn có; không tự tạo để lấp chỗ thiếu |
| Họ tên | Bắt buộc cả khi lưu nháp |
| Năm sinh | Bắt buộc chính thức; không sau năm vào viện |
| Giới tính | Chọn Nam/Nữ; Chưa xác định nếu nguồn chưa xác định |
| Khoa điều trị cuối / loại bệnh án | Chọn đúng danh mục |
| Ngày vào / ra viện | Lấy từ nguồn giấy; ngày ra không trước ngày vào |
| Tử vong | Tích đúng thực tế; nếu tích thì phải chọn nhóm DECEASED; không tích thì không chọn nhóm đó |
| Ngày đầu lưu kho | Ngày thực tế đầu lưu, không mặc định bằng ngày nhập phần mềm; từ ngày ra viện trở đi |
| Ngăn lưu trữ | Chọn ngăn hoạt động, cha hoạt động, còn chỗ nếu có giới hạn |
| Chất lượng hồ sơ giấy | Bình thường hoặc Hư hỏng theo kiểm tra thực tế |
| Chính sách | Chọn đủ nhóm áp dụng, đã xác nhận và có hiệu lực tại ngày đầu lưu |
| Nguồn dữ liệu / căn cứ đối soát | Bắt buộc chính thức; ghi sổ/đợt bàn giao/căn cứ đã kiểm tra, tối đa 1.000 ký tự |
| Ghi chú nội bộ | Thông tin phục vụ lưu trữ, tối đa 2.000 ký tự; chỉ Archivist xem |

Các ngày điều trị/đầu lưu không được sau ngày hiện tại. Nếu chọn chất lượng **Thiếu/chưa hoàn thiện** cho hồ sơ mới thì lưu nháp, chưa nhập chính thức. Hồ sơ Hư hỏng được nhập ở trạng thái Tạm không khả dụng.

Số lưu trữ tự cấp dạng `YYYY-MM-NNNNNN`, theo tháng của ngày đầu lưu. Số nguồn cho phép chữ Latin không dấu, chữ số, `/`, `_`, `-`, dài 2–64 ký tự; hệ thống chuyển chữ hoa. Số có thể không liên tiếp; không suy ra số hồ sơ trong kho từ số thứ tự cuối.

### 5.3. Xác nhận và kiểm tra kết quả

1. Rà soát các trường, bấm **Xác nhận nhập kho / lưu hồ sơ chính thức**.
2. Nếu có lỗi, đọc danh sách **Chưa thể lưu**, sửa thông tin và gửi lại; không coi là lưu thành công khi form vẫn báo lỗi.
3. Khi thành công, trang chi tiết mở ra và có thông báo đã nhập kho.
4. Kiểm tra số lưu trữ, thông tin đợt điều trị, ngăn, ngày đầu lưu, số năm và hạn dự kiến.
5. Kiểm tra **Snapshot chính sách đã áp dụng** và **Lịch sử vị trí** có lần nhập đầu.

Một người bệnh có nhiều đợt thì tạo nhiều hồ sơ tương ứng, không gộp theo tên/mã người bệnh. Nếu hệ thống báo nghi trùng số bệnh án + mã người bệnh + ngày vào/ra, đối chiếu nguồn trước; không thay mã/ngày tùy ý để vượt kiểm tra.

## 6. Lưu nháp và đối soát dữ liệu thiếu

1. Mở form nhập, điền họ tên và các thông tin đã biết chính xác; để trống phần chưa biết.
2. Có thể ghi rõ phần cần đối soát ở nguồn/ghi chú; chọn chất lượng phù hợp.
3. Bấm **Lưu nháp để đối soát**.
4. Nháp có trạng thái DRAFT, chưa công bố cho Reader/Director, chưa có hạn lưu chính thức. Lựa chọn vị trí trên form nháp không phải xác nhận hồ sơ đã được xếp vào ngăn.
5. Để tìm lại: vào tra cứu, chọn **Trạng thái nhập → Bản nháp**, bấm Tìm kiếm; nháp chưa có số sẽ hiện liên kết **Bản nháp chưa có số**.
6. Mở nháp → **Sửa / đối soát**, bổ sung dữ liệu từ nguồn đã kiểm tra, chọn lại ngăn và chính sách phù hợp.
7. Nhập **Lý do sửa / xác nhận đối soát**, bấm xác nhận nhập kho khi đã đủ dữ liệu; hoặc tiếp tục lưu nháp nếu chưa đủ.
8. Kiểm tra đã chuyển STORED và có lần nhập kho đầu/snapshot.

Không tạo ngày hoặc năm sinh giả để đủ trường. Nháp chưa đủ điều kiện nhập chính thức không đồng nghĩa dữ liệu đã mất.

## 7. Tra cứu và xem chi tiết nghiệp vụ

1. Mở **Tra cứu và xem chi tiết**.
2. Tìm mã chính xác hoặc tên có/không dấu; thêm năm sinh, khoa, loại, ngày ra viện, trạng thái lưu chuyển/nhập nếu cần.
3. Bấm **Tìm kiếm**; dùng số dòng/sắp xếp và **Trang trước / Trang sau**.
4. Bấm số lưu trữ hoặc liên kết bản nháp để mở chi tiết.

Bạn xem được metadata, vị trí, chất lượng/khả dụng, snapshot chính sách, nguồn/ghi chú, người và thời gian tạo/sửa, lịch sử vị trí và lịch sử thao tác. Trong lịch sử, mở **Dữ liệu trước / Dữ liệu sau** để đối chiếu thay đổi. Bản chụp là thông tin lưu tại thời điểm thao tác, không phải biểu mẫu để chỉnh trực tiếp.

## 8. Sửa thông tin hồ sơ

1. Mở chi tiết → **Sửa / đối soát**.
2. Đối chiếu nguồn và sửa trường được phép.
3. Nhập lý do rõ ràng, ví dụ `Đối chiếu lại họ tên theo sổ bàn giao mẫu`.
4. Bấm nút lưu hồ sơ chính thức hoặc lưu nháp tùy trạng thái hiện tại.
5. Kiểm tra nội dung mới và lịch sử trước/sau.

Với hồ sơ đã nhập chính thức: không đổi số lưu trữ/ngày đầu lưu, không chuyển ngược thành nháp; chuyển ngăn bằng chức năng riêng. Giữ nguyên phân loại thì snapshot chính sách cũ được giữ kể cả danh mục đã ngừng; chủ động đổi khoa/loại/nhóm chính sách sẽ yêu cầu kiểm tra lại tính hợp lệ.

Nếu báo **Hồ sơ đã được người khác sửa**, tải lại chi tiết, đọc thay đổi mới rồi mở lại form; không bấm gửi liên tục từ tab cũ. Hồ sơ READING cần được nhận trả từ phiên đang đọc trong Yêu cầu / lịch đọc trước khi sửa.

## 9. Chuyển vị trí

1. Kiểm tra hồ sơ đang STORED và IN_STORAGE, xác định ngăn đích thực tế.
2. Mở chi tiết, tại **Chuyển vị trí** chọn **Ngăn đích** khác ngăn hiện tại.
3. Nhập lý do, ví dụ `Sắp xếp lại hồ sơ theo đợt lưu trữ`.
4. Bấm **Xác nhận chuyển**, thực hiện/đối chiếu việc xếp hồ sơ giấy với thông tin vừa ghi nhận.
5. Kiểm tra vị trí hiện tại và dòng mới trong **Lịch sử vị trí**: ngăn cũ/mới, người, thời điểm và lý do.

Hệ thống không tự di chuyển hồ sơ giấy. Nếu chuyển bị từ chối do ngăn đầy/ngừng, trạng thái không phù hợp hoặc dữ liệu đã đổi, đọc lỗi và kiểm tra lại cả vị trí thực tế lẫn bản ghi trước khi thao tác tiếp.

## 10. Xác nhận khả dụng sau kiểm tra vật lý

1. Mở chi tiết hồ sơ đã nhập, tìm **Xác nhận khả dụng**.
2. Nếu hồ sơ tạm không sử dụng được, chọn **Tạm không khả dụng**, nhập căn cứ kiểm tra rồi bấm **Lưu xác nhận**.
3. Khi đã kiểm tra hồ sơ thực tế và đủ điều kiện dùng lại, bảo đảm chất lượng là Bình thường; nếu cần thì sửa/đối soát chất lượng trước.
4. Chọn **Trong kho / khả dụng**, nhập căn cứ và lưu; kiểm tra lịch sử.

Sửa chất lượng về Bình thường không tự mở lại khả dụng. Không dùng mục này để giả lập đã nhận trả hồ sơ đang READING. Hồ sơ UNAVAILABLE vẫn được tính đang chiếm vị trí ngăn.

| Mã hiển thị | Ý nghĩa |
|---|---|
| DRAFT / STORED | Nháp / Đã xác nhận nhập kho |
| NOT_STORED / IN_STORAGE | Chưa nhập kho / Trong kho, khả dụng |
| UNAVAILABLE / READING | Tạm không khả dụng / Đang giao đọc |
| NORMAL / DAMAGED / INCOMPLETE | Bình thường / Hư hỏng / Thiếu hoặc chưa hoàn thiện |
| DRAFT / APPROVED ở chính sách | Chưa xác nhận / Đã xác nhận; còn cần kiểm tra hoạt động và hiệu lực |

## 11. Lỗi thường gặp

| Thông báo/hiện tượng | Cách xử lý |
|---|---|
| Chưa đủ thông tin nhập kho | Bổ sung trường được liệt kê hoặc lưu nháp |
| Chính sách chưa xác nhận/ngoài hiệu lực | Đối chiếu ngày đầu lưu và phiên bản; nhờ Director xác nhận bản đúng |
| Không có chính sách chọn được | Kiểm tra danh sách chính sách, phối hợp Director; không tự sửa ngày hồ sơ |
| Nghi trùng đợt hoặc số lưu trữ trùng | Tra hồ sơ đã có, đối soát nguồn; không tạo bản thứ hai chỉ vì bấm lại |
| Ngày không hợp lệ | Kiểm tra vào ≤ ra ≤ đầu lưu ≤ hôm nay và năm sinh |
| Ngăn đầy/ngừng hoặc sai cấp | Chọn ngăn đủ điều kiện; không sửa sức chứa sai thực tế để vượt kiểm tra |
| Không thể ngừng vị trí | Kiểm tra hồ sơ đang chứa và các vị trí con còn hoạt động |
| Biểu mẫu/vị trí đã thay đổi | Tải lại trang và đối chiếu dữ liệu mới trước khi lưu |
| 403 khi gửi form cũ | Mở lại trang bằng tài khoản đúng, nhập/gửi lại từ form mới nếu chưa lưu; kiểm tra danh sách trước để tránh tạo lại |
| Chưa biết lần bấm vừa rồi đã lưu chưa | Tra số/mã/đợt và xem lịch sử trước khi mở phiếu nhập mới |

## 12. Kết thúc và tự thực hành

Trở về trang chủ hoặc trang nghiệp vụ có thanh điều hướng, bấm **Đăng xuất**, kiểm tra đã về login.

Dùng dữ liệu giả cho bài thực hành sau:

- [ ] Kiểm tra danh mục, phối hợp Director xác nhận policy phù hợp.
- [ ] Tạo Kho–Kệ–hai Ngăn có mã chưa trùng.
- [ ] Nhập một hồ sơ hoàn chỉnh; kiểm tra số, snapshot và lần nhập đầu.
- [ ] Tạo một nháp chỉ có tên rồi đối soát để nhập chính thức.
- [ ] Tìm theo tên không dấu/năm sinh và kiểm tra đúng đợt.
- [ ] Sửa metadata kèm lý do; xem trước/sau.
- [ ] Chuyển ngăn; đối chiếu vị trí và lịch sử.
- [ ] Xác nhận tạm không khả dụng rồi khả dụng sau kiểm tra phù hợp; đăng xuất.

Đây là danh sách tự thực hành, chưa đánh dấu thay người sử dụng. Xem thêm [hướng dẫn Director](director.md), [mục lục tài liệu](../README.md), [báo cáo nguyên lý và kiểm thử](../verification/FIVE_FEATURE_COMPLETION_REPORT.md).

## 13. Kiểm tra, trình và xác nhận lịch đọc

1. Mở **Yêu cầu / lịch đọc**, chọn SUBMITTED; xem mục đích và từng ngày.
2. Trong Kiểm tra và trình, ghi nội dung; chọn **Trả bổ sung** nếu chưa đủ, hoặc **Đủ điều kiện — trình Director**. Mỗi request xử lý một lần, danh sách/báo cáo giúp tổng hợp các request cần làm.
3. Khi Director duyệt thành APPROVED, mở lại và ghi phòng đọc/hướng dẫn nhận, bấm xác nhận. CONFIRMED là lịch chính thức; lịch trùng hoặc ngày đã qua bị chặn.
4. Reader theo dõi kết quả trong app; không có email tự động.

## 14. Giao, nhận trả và ngoại lệ từng ngày

1. Mở request CONFIRMED và phiên của hôm nay. Đối chiếu đúng người Reader đang hoạt động và tình trạng giấy.
2. Nhập tình trạng khi giao, bấm **Giao hồ sơ tại phòng đọc hôm nay**. Kiểm tra hồ sơ thành READING; không thể giao trùng/chuyển vị trí bằng luồng kho lúc này.
3. Khi nhận giấy, chọn ngăn cất lại, chất lượng và tình trạng thực tế, bấm **Nhận trả và cất lại**. Kiểm tra RETURNED; NORMAL trở về IN_STORAGE, DAMAGED/INCOMPLETE là UNAVAILABLE.
4. Nếu khác ngày, ghi ngoại lệ trước khi nhận trả và giữ thời điểm trả thực tế. Không sửa ngày để che việc trả muộn.
5. Nếu chưa trả, ghi ngoại lệ và xử lý thực tế, không tự xác nhận đã trả. Nếu người đọc không đến và chưa giao, chọn không đến. Lượt đã giao không được đánh dấu NO_SHOW.
6. Rà soát **Báo cáo / nhật ký → Sổ giao nhận**, state `READING` để thấy lượt chưa trả; lọc ngày đọc nếu cần. Không dùng mục đổi khả dụng để thay thao tác nhận trả.

## 15. Kiểm kê và báo cáo

1. Mở **Kiểm kê**, nhập tên và chọn kho/kệ/ngăn hoặc toàn kho rồi tạo đợt. Danh sách chụp lúc tạo, không gồm hồ sơ đang giao đọc.
2. Với từng dòng chọn đủ đúng vị trí, chưa tìm thấy, sai vị trí hoặc hư hỏng; mô tả chênh lệch, chọn ngăn quan sát nếu sai vị trí. Ghi kết quả.
3. Chỉ chốt sau khi mọi dòng đã kiểm. Chốt không tự chuyển/sửa hồ sơ; xử lý chênh lệch bằng nghiệp vụ sửa/chuyển riêng có lý do.
4. **Báo cáo / nhật ký**: chọn một trong tám nhóm, tìm/lọc ngày và trạng thái, bấm Xem. Nhật ký có thêm bộ lọc người/hành động/ID đối tượng.
5. Xuất CSV mở Excel hoặc In/lưu PDF bằng trình duyệt. Cùng bộ lọc và quyền được áp dụng khi xuất; yêu cầu xuất/in được ghi nhật ký.
6. Báo cáo tổng hợp kho là số liệu hiện tại. Phạm vi lọc ngày mỗi loại được giải thích ngay trên màn hình; không coi báo cáo đó là tồn kho lịch sử tại ngày bất kỳ.

Xem [báo cáo FR](../verification/FR_COMPLETION_REPORT.md) cho trạng thái, quyền và các ca kiểm thử mới.

## Tách bảng tra cứu —

Tra cứu mặc định chỉ có Hồ sơ chính thức (STORED/PUBLISHED). Chọn tab Xét duyệt khai báo (`/archivist/records?scope=DECLARATIONS`) để kiểm tra khai báo của bác sĩ ở bảng riêng. Bản giấy chưa nhập kho được giữ trong tab Bản giấy nháp (`scope=DRAFTS`). Mọi bảng dùng template `records/list.html` với các nhánh theo scope; quyền tại backend vẫn được kiểm tra, không chỉ ẩn dòng HTML. Mục yêu cầu của nhân viên lưu trữ vẫn là danh sách xử lý, còn Reader đổi nhãn thành Lịch sử yêu cầu.
