# Hướng dẫn sử dụng — Reader (Người đọc)

## Tự sửa thông tin và đề nghị đổi vai trò/khoa — V24

Menu tên nhân viên → **Thông tin cá nhân** → **Chỉnh sửa thông tin cá nhân** cho phép sửa họ tên, căn cước, ngày sinh và giới tính. Email không chỉnh sửa tại đây; tên tài khoản và email được giữ nguyên. Căn cước không được trùng nhân viên khác.

Trong trang chỉnh sửa thông tin cá nhân, nhấp mục **Vai trò** hoặc **Khoa/phòng** để mở form đề nghị đã có, chọn giá trị mong muốn, nhập lý do rồi gửi Giám đốc. Không còn nút đề nghị riêng trên trang thông tin hoặc menu tài khoản. Mỗi người có tối đa một đơn đang chờ. Giám đốc duyệt xong vẫn chờ Admin áp dụng; sau khi áp dụng, đăng nhập lại để nhận quyền mới.

Chuông chỉ gửi kết quả **được phê duyệt sau khi Admin đã áp dụng**, hoặc **bị từ chối ngay khi Giám đốc từ chối** (có lý do bên dưới). Không gửi email thật. Kết quả chưa đọc hiện ở tab đang chờ; đọc xong chuyển sang đã xử lý. [Cập nhật V25](../verification/STAFF_PROFILE_AND_NOTICE_TIMING.md).

HTML: `templates/account/profile.html`, `account/profile-edit.html`, `account/change-list.html`, `account/change-detail.html`. [Chi tiết](../verification/STAFF_SELF_SERVICE_AND_APPROVAL.md).

**Đăng nhập lần đầu — V23:** dùng mật khẩu ban đầu trong file tài khoản, sau đó nhập mật khẩu mới và xác nhận ở `/account/initial-setup` (không yêu cầu mật khẩu cũ). Mật khẩu 12–128 ký tự và khác mật khẩu ban đầu. Phải lưu thành công mới sử dụng các chức năng. Bác sĩ không phải tạo mật khẩu cấp 2. Đổi mật khẩu các lần sau tại menu cá nhân vẫn cần mật khẩu hiện tại. [Chi tiết](../verification/INITIAL_CREDENTIAL_SETUP.md).

**Tài khoản sau reset:** Nguyễn Văn An — `an.nv12041990@mrm.com`. Mật khẩu riêng tại `.secrets/operational-accounts.txt`. Các ví dụ `reader1`/`reader2` bên dưới là tên kiểm thử cũ đã xóa. 16 bệnh án được giữ, mã DEMO đã đổi; lịch sử mượn/thông báo thử đã dọn. Xem [báo cáo reset](../verification/OPERATIONAL_BASELINE_RESET.md).

## Thông báo chung và dọn thông báo đã xử lý — V21,

Chuông mở mặc định tab **Đang chờ xử lý**: chỉ giữ những việc chưa kiểm tra/phê duyệt/xác nhận hoặc còn cần bổ sung. Mọi thẻ đều dùng bố cục người yêu cầu, hồ sơ và ngày giờ như Archivist; lịch đã xác nhận có ngày/phòng nhận. Chuông chỉ tính việc đang chờ chưa đọc. Đã đọc không đồng nghĩa đã xử lý.

Mở **Đã xử lý** để xem kết quả và dùng **Xóa thông báo**, hoặc **Xóa tất cả đã xử lý** để dọn hộp thư. Các thông báo đang chờ được giữ và không có nút xóa riêng; backend cũng chặn xóa. Xóa chỉ ảnh hưởng hộp thư của bạn, giữ yêu cầu và lịch sử nghiệp vụ. Admin vẫn không được nhận dữ liệu bệnh án.

HTML: `templates/notification/inbox.html`; service/controller: `NotificationService.java`, `NotificationController.java`. Xem [báo cáo và kiểm thử](../verification/UNIFIED_NOTIFICATIONS_AND_CLEANUP.md).

## Menu tài khoản cá nhân —

Menu tên nhân viên gồm **Thông tin cá nhân**, **Thay đổi mật khẩu**, **Đăng xuất**. Trang thông tin chỉ có nút **Chỉnh sửa thông tin cá nhân**; trong trang sửa, nhấp Vai trò hoặc Khoa/phòng để mở đề nghị. Email không tự sửa; vai trò/khoa vẫn phải được Giám đốc duyệt và Admin áp dụng.

Đổi mật khẩu tại `/account/password`: nhập mật khẩu hiện tại, mật khẩu mới 12–128 ký tự và xác nhận. Phiên đang đổi tiếp tục dùng; các phiên khác hết hiệu lực ở lần truy cập tiếp theo. Không dùng lại mật khẩu cũ; Director không được dùng trùng mật khẩu cấp 2.

HTML: `templates/fragments/layout.html` (menu), `templates/account/profile.html` (cá nhân), `templates/account/password.html` (mật khẩu). Java: `PersonalAccountController.java`, `AccountService.java`. Menu/CSS: `static/js/portal.js`, `static/css/portal.css`.

Khi bản giấy đã được giao thực tế cho chính bạn (phiên Đang đọc), trang thông tin bệnh án ẩn hướng dẫn xin mượn tử vong, nút Mượn hồ sơ giấy và hướng dẫn mượn giấy ở cuối. Các thông tin hồ sơ vẫn giữ nguyên. HTML: `templates/records/basic-detail.html`; điều kiện do backend xác định, không dựa vào tham số URL.

Xem [báo cáo thay đổi và kiểm thử](../verification/PERSONAL_MENU_AND_DIRECTOR_DASHBOARD.md).

## Ngày và phòng nhận trong tiến trình —

Mở **Lịch sử yêu cầu → Theo dõi tiến trình**. Tại node **Xác nhận lịch nhận**, ngay dưới Người xử lý hiển thị **Ngày nhận** (dd/MM/yyyy) và **Phòng nhận**. Ngày lấy từ lịch nhận đã xác nhận; phòng/hướng dẫn lấy từ thông tin nhân viên lưu trữ điền khi xác nhận lịch, không phải vị trí kho. Thời gian bên dưới là thời điểm xử lý xác nhận, tách biệt ngày nhận. Nếu yêu cầu được sửa, không gán lịch mới cho node xác nhận thuộc phiên bản cũ.

HTML chỉnh nội dung: `src/main/resources/templates/reading/history.html`. Java chuẩn bị dữ liệu từng node: `src/main/java/mrm/workflow/ReadingProgress.java`. Không bổ sung trường hoặc thay đổi database.

## Nhận diện bệnh án tử và thông báo hủy —, V20

Các thông báo gắn với yêu cầu đã hủy không còn nút Mở yêu cầu; vẫn có thể đánh dấu đã đọc. Theo dõi lịch sử hủy từ **Lịch sử yêu cầu**.

Bệnh án tử được ghi rõ trong cột Ghi chú tra cứu; khi chọn mượn có nền hồng nhạt, viền đỏ và chữ “Hồ sơ tử vong”. Lý do vẫn bắt buộc khi có bất kỳ hồ sơ tử vong nào, mọi hồ sơ trong lượt đều cần Director duyệt. Mẫu tử vong DEMO-MRM-006/008 chỉ điện tử nên không có nút mượn giấy; mẫu DEMO-MRM-009 có bản giấy để thử quy trình này.

HTML thông báo: `templates/notification/inbox.html`; tra cứu: `templates/records/list.html`; bộ chọn: `templates/reading/new.html`, `static/js/borrow-record-picker.js`; màu: `static/css/portal.css`. [Báo cáo và kiểm thử](../verification/ARCHIVIST_NOTICES_AND_DECEASED_RECORDS.md).

## Duyệt mượn theo lượt đăng ký — (V19)

Quy tắc này thay thế hướng dẫn cũ yêu cầu mọi lượt mượn phải qua Giám đốc:

- Không có hồ sơ tử vong trong lượt: nhân viên lưu trữ kiểm tra và phê duyệt hoặc từ chối.
- Có ít nhất một hồ sơ tử vong: **mọi hồ sơ trong lượt**, kể cả hồ sơ thường, phải được Giám đốc duyệt sau khi nhân viên lưu trữ kiểm tra. Từng hồ sơ vẫn có lịch sử và quyết định riêng.
- Lý do bắt buộc khi chọn **trên 5 hồ sơ hoặc có hồ sơ tử vong**, kể cả chỉ chọn 1 hồ sơ tử vong. Tối đa 5 hồ sơ thường vẫn được để trống lý do.
- Bộ chọn ghi rõ “Hồ sơ tử vong”. Bỏ chọn hồ sơ tử vong trước khi gửi sẽ cập nhật yêu cầu nhập lý do. Sau khi gửi, tuyến duyệt của cả lượt được cố định; hủy một hồ sơ không hạ quyền duyệt của các hồ sơ còn lại.
- Sau khi được duyệt, nhân viên lưu trữ xác nhận ngày nhận trong giới hạn ngày đăng ký + 2 ngày lịch. Đọc tại phòng đọc, giao và trả vẫn được ghi nhận như trước.

Theo dõi tại **Lịch sử yêu cầu → Theo dõi tiến trình**. Lượt thường không có node Giám đốc. Yêu cầu cũ trước V19 giữ tuyến Giám đốc để không thay đổi lịch sử đang xử lý. Luồng bác sĩ khai báo bệnh án mới vẫn cần Giám đốc công bố, không thay đổi theo quy tắc mượn này.

HTML: `reading/new.html` (phiếu), `reading/history.html` (tiến trình), `reading/list.html` (lịch sử); bộ chọn: `static/js/borrow-record-picker.js`. Chi tiết [thay đổi và kiểm thử](../verification/BORROW_APPROVAL_ROUTING.md).

## Tách hồ sơ chính thức, khai báo và liên kết thống kê —

Mục yêu cầu của Reader đổi thành **Lịch sử yêu cầu** ở navigation, tiêu đề trang và breadcrumb; URL vẫn `/reader/requests`. Cột Trạng thái không còn mũi tên sắp xếp; chỉ giữ ▲/▼ Ngày đăng ký. Liên kết cũ yêu cầu sắp theo trạng thái được chuyển về ngày đăng ký mới nhất. Bộ lọc trạng thái riêng từng Reader giữ nguyên.

Trong **Tra cứu hồ sơ**, tab mặc định **Hồ sơ chính thức** chỉ chứa bệnh án đã nhập kho/đã công bố. Tab **Bệnh án đã khai báo** có bảng riêng gồm khai báo của chính bạn ở mọi giai đoạn (mới gửi, cần bổ sung, chờ duyệt, đã công bố, từ chối, hủy). Bệnh án chờ duyệt không xuất hiện trong bảng chính thức, dù dùng bộ lọc hay sửa URL. URL bảng khai báo là `/reader/records?scope=DECLARATIONS`; bộ lọc và phân trang giữ tab hiện tại. Sau khi được duyệt, bệnh án xuất hiện trong bảng chính thức và vẫn được lưu trong lịch sử khai báo. Không có bảng dữ liệu database mới hoặc sao chép bệnh án.

Các ô thống kê Reader trên trang chủ đã bấm được:

- **Đang mượn**: mở Lịch sử yêu cầu lọc Đang mượn trong toàn bộ thời gian, tránh bỏ sót phiên đã giao từ trước 7.
- **Chờ xử lý**: mở `/reader/work`, hai bảng gồm yêu cầu mượn đang xử lý và bệnh án khai báo chờ xét duyệt của bạn. Tổng số dòng hai bảng khớp số trên trang chủ; bấm để theo dõi/xem/bổ sung.
- **Bệnh án đã tạo**: mở tab Bệnh án đã khai báo của bạn.

File: `records/list.html` (hai tab/bảng), `reading/list.html` (lịch sử), `reading/work.html` (danh mục chờ), `home.html` (ô thống kê), `DashboardService.java` (đích và dữ liệu), `RecordRepository.java` (giới hạn danh sách tại backend). Navigation vẫn dùng `fragments/layout.html`. Phần này thay thế hướng dẫn cũ hiển thị khai báo chưa duyệt chung với hồ sơ chính thức và sắp xếp trạng thái.

## Nhập ngày sinh đầy đủ — V18,

Ở trang **Thêm hồ sơ bệnh án mới/Bổ sung**, thay ô năm sinh bằng ba ô **Ngày / Tháng / Năm** theo thứ tự trái sang phải, luôn cùng một hàng trong vị trí ô cũ. Nhập đủ ba phần; hệ thống kiểm tra ngày lịch thật, năm nhuận, không ở tương lai và không sau ngày điều trị. Phần xem trước và trang chi tiết hiển thị `dd/MM/yyyy`.

Ngày sinh đầy đủ lưu trong `medical_records.birth_date` (DATE); `birth_year` giữ năm tương ứng để bộ lọc năm sinh và phần nhập hồ sơ giấy cũ tiếp tục sử dụng. Khi sửa, ba ô được điền lại theo ngày đã lưu. Hồ sơ cũ chỉ có năm giữ `birth_date=NULL`, không tự gán 01/01; trang chi tiết ghi chỉ biết năm. Khi bổ sung/gửi lại khai báo cũ, cần nhập ngày/tháng chính xác, không tự đoán.

File giao diện [electronic/form.html](../../src/main/resources/templates/electronic/form.html), nhóm `.patient-birth-inputs` trong [portal.css](../../src/main/resources/static/css/portal.css), phần xem trước/kiểm tra lịch trong [record-form-preview.js](../../src/main/resources/static/js/record-form-preview.js). Backend vẫn kiểm tra khi JavaScript bị bỏ qua. Migration Java `V18__AddPatientBirthDate` tự chạy khi khởi động lại ứng dụng; không cần chạy SQL thủ công. Thay đổi này áp dụng form của bác sĩ; form nhập bản giấy của Archivist vẫn dùng độ chính xác năm sinh nếu nguồn giấy chỉ có năm.

## Trạng thái cá nhân và mũi tên sắp xếp —

**Trạng thái đăng ký** chỉ liệt kê các trạng thái đang có trong toàn bộ lịch sử yêu cầu của Reader đang đăng nhập (bao gồm Đang mượn/Hoàn thành tính theo giao–trả). Không lấy trạng thái của Reader khác; chưa có yêu cầu thì chỉ có Tất cả trạng thái. Danh sách không bị giới hạn theo trang hiện tại hay thay đổi theo từ khóa/khoảng.

Không còn ô Sắp xếp. Bấm **▲/▼** ở tiêu đề **Ngày đăng ký** để xem ngày tăng/giảm dần; mặc định mới nhất trước. Ở cột **Trạng thái**, ▲ nhóm theo thứ tự tiến trình, ▼ đảo thứ tự các nhóm. Trong cùng trạng thái luôn giữ ngày/thời điểm đăng ký tăng dần. Mũi tên đang áp dụng được tô xanh; bộ lọc từ khóa, ngày và trạng thái được giữ khi sắp xếp/phân trang, khi đổi cách sắp xếp trở về trang đầu. Gửi lại bộ lọc vẫn giữ cách sắp xếp đã chọn. Hướng dẫn này thay thế ô chọn Sắp xếp cũ ở các phần bên dưới.

## Ngày, trạng thái và sắp xếp yêu cầu mượn —

Ngày đăng ký hiện với mọi trạng thái, kể cả yêu cầu đã hủy. Ngày nhận để trống khi đang xét duyệt, bị từ chối hoặc đã hủy; chỉ hiện ngày đã xác nhận đối với yêu cầu đã xếp lịch/đang mượn/hoàn thành. Không còn dòng chờ xếp lịch trong ô ngày nhận. Tiêu đề các cột được căn giữa.

Bộ lọc **Trạng thái đăng ký** kết hợp được với tên/số lưu trữ và khoảng. Mặc định sắp xếp **Ngày đăng ký mới nhất**. Chọn **Trạng thái, đăng ký trước xử lý trước** để nhóm theo thứ tự Mới gửi → Cần bổ sung → Chờ duyệt → Đã duyệt chờ lịch → Đã xác nhận lịch → Đang mượn → Hoàn thành → Từ chối → Đã hủy; cùng trạng thái xếp ngày đăng ký tăng dần, cùng ngày dùng thời điểm tạo để yêu cầu gửi trước đứng trước. Phân trang giữ cả bộ lọc và cách sắp xếp.

Ghi chú chỉ hiện mục mở rộng **▶ Lý do mượn** khi yêu cầu còn đang xử lý (bao gồm chờ nhận theo lịch) và có lý do được nhập. Bấm để mở/đóng nội dung; không có lý do thì ô ghi chú để trống, không hiện thông báo mặc định Không cung cấp lý do. Sau khi nhân viên ghi nhận giao hồ sơ, trạng thái và ghi chú là **Đang mượn**, không hiện lý do. Khi đã nhận trả toàn bộ phiên cần thực hiện, trạng thái là **Hoàn thành**, ghi chú **Ngày trả DD/MM/YYYY** theo lần trả cuối ở giờ Việt Nam. Yêu cầu hủy ghi **Đã hủy**, không hiện lý do. Trạng thái Từ chối để trống ghi chú; căn cứ vẫn xem trong tiến trình.

Đang mượn/Hoàn thành là trạng thái hiển thị tính từ `reading_sessions`, không thay đổi mã `CONFIRMED` của quy trình trong `reading_requests`. Xác nhận lịch chưa có giao thực tế không được tính là đang mượn; còn phiên chưa trả/chưa thực hiện thì chưa tính hoàn thành. Phần này thay thế hướng dẫn cũ để trống ngày đăng ký khi hủy và ghi chú mở rộng lý do cho mọi trạng thái.

## Giao diện thêm bệnh án và yêu cầu mượn — **Thêm bệnh án mới/Bổ sung**: [electronic/form.html](../../src/main/resources/templates/electronic/form.html) chia ba nhóm nhận diện, đợt điều trị, thông tin bổ sung; bên phải có xem trước từ dữ liệu đang nhập. Mã hồ sơ chỉ được cấp khi server ghi nhận thành công, không dùng mã dự kiến giả. Ghi chú không bắt buộc. **Nhập lại** phục hồi giá trị lúc tải form (trang tạo mới trở về trống; trang bổ sung trở về thông tin ban đầu). Phần xem trước tại [record-form-preview.js](../../src/main/resources/static/js/record-form-preview.js) không lưu dữ liệu; bấm **Gửi kiểm tra** mới gửi form thật. Khoa/loại lấy từ database như trước.
- **Yêu cầu mượn**: [reading/list.html](../../src/main/resources/templates/reading/list.html) dùng bộ lọc ngày/từ khóa và bảng có nhãn trạng thái màu. Cột Tiến trình xử lý chứa **Theo dõi tiến trình**, cùng nút **Hủy yêu cầu** khi chưa có quyết định Giám đốc. Cột Ghi chú hiển thị lý do khi đang xử lý, Đang mượn khi đang giao đọc, Ngày trả khi hoàn thành hoặc Đã hủy khi hủy. Không bổ sung tên bệnh nhân dưới mã hồ sơ. Quy tắc phân trang 10, mặc định 7 ngày gần nhất và giữ ngày đăng ký, để trống ngày nhận của yêu cầu hủy. Trang này dùng chung với Archivist/Director; staff có liên kết **Mở và xử lý** thay cho thao tác của Reader.
- CSS hai trang nằm trong [portal.css](../../src/main/resources/static/css/portal.css), các nhóm `.record-entry-page`, `.request-statement-page`, `.entry-*`, `.request-*`. Nội dung tối thiểu 15px; header/navigation chính giữ nguyên. Nhãn ghi chú trạng thái nằm trong `ReadingLabels.requestHint()`; dữ liệu/trạng thái nghiệp vụ không đổi.

## Bố cục tra cứu và đăng ký mượn —

Trang tra cứu dùng nhãn **Điện tử**, **Giấy**, **Giấy và điện tử** theo hai cờ hình thức. Cột **Mượn hồ sơ** chỉ chứa nút mượn khi đủ điều kiện; thông báo đã đăng ký/đã xác nhận lịch/đã được mượn nằm ở cột **Ghi chú** bên phải. Đây là ghi chú về khả năng mượn, không phải ghi chú nội bộ của bệnh án. Bảng kết quả không còn cột trạng thái hoặc đợt điều trị; bộ lọc trạng thái vẫn sử dụng được.

Các trang sau đăng nhập có breadcrumb **Trang chủ / Tên trang**; bấm Trang chủ để quay về bàn làm việc. Chỉnh fragment `breadcrumb(title)` trong [layout.html](../../src/main/resources/templates/fragments/layout.html), phần `.page-breadcrumb` trong [portal.css](../../src/main/resources/static/css/portal.css). Navigation chính vẫn giữ thiết kế chung hiện tại. Khi dùng HTML tham khảo mới chỉ áp dụng nội dung, không thay header/navigation chính theo bản mẫu.

Trang [reading/new.html](../../src/main/resources/templates/reading/new.html) dùng khối quy định, thẻ hồ sơ đã chọn, lý do và tóm tắt phiếu. Bấm **Chọn hồ sơ mượn…** để tìm hồ sơ giấy đang khả dụng; có thể mở **Thông tin hồ sơ**, bỏ từng hồ sơ bằng nút × hoặc **Xóa tất cả**. **Làm lại** xóa lựa chọn và lý do. Chọn trên 5 hồ sơ thì lý do bắt buộc; phần tóm tắt và cảnh báo cập nhật ngay. Nút gửi vẫn gửi form thật tới `/reader/requests`, giữ token chống gửi trùng, CSRF và kiểm tra ở backend. JavaScript tại [borrow-record-picker.js](../../src/main/resources/static/js/borrow-record-picker.js); CSS trang ở nhóm `.borrow-registration-page` trong `portal.css`. Quy trình không thay đổi: nhân viên kiểm tra → Giám đốc duyệt → nhân viên xác nhận lịch trong giới hạn 2 ngày lịch kể từ đăng ký → giao/nhận tại phòng đọc.

## Tra cứu thống nhất và mượn bản giấy — V17,

Cột **Mượn hồ sơ** phân biệt: **Đã đăng ký mượn** khi có yêu cầu đang xử lý; **Đã xác nhận lịch mượn** khi đã xếp lịch nhưng chưa giao; **Đã được mượn** khi nhân viên đã giao hồ sơ tại phòng đọc. **Chưa đủ điều kiện** dành cho các trường hợp khác như chất lượng không bảo đảm hoặc tạm không khả dụng. Sau khi nhận trả đầy đủ, hồ sơ đủ điều kiện sẽ hiện lại nút mượn. Trạng thái **Trong kho** vẫn đúng khi đã đăng ký/xếp lịch nhưng chưa giao.

Dữ liệu quản lý: `reading_requests` lưu yêu cầu, người yêu cầu (`requester_id` liên kết `app_users.id`), trạng thái và người duyệt; `reading_sessions` lưu lịch nhận, giao/nhận trả và trạng thái `READING` khi đang được mượn, `RETURNED` khi đã trả; `request_events` lưu lịch sử xử lý. `medical_records.physical_status` phản ánh trạng thái lưu chuyển của bản giấy. Các nhãn trên giao diện được tính từ các bảng này, không cần thêm bảng mới.

Không còn mục Hồ sơ điện tử riêng trên navigation. Mở **Tra cứu hồ sơ bệnh án**, lọc **Hình thức** (Giấy và điện tử/Chỉ điện tử) và **Trạng thái xét duyệt** nếu cần. Hồ sơ chưa duyệt của bạn cũng nằm trong danh sách; Reader khác không thấy chúng.

Bấm **Mã hồ sơ / số lưu trữ** để xem thông tin bệnh nhân của bệnh án đã công bố, không bị chuyển sang trang xin phép chỉ vì chưa đăng ký mượn. Cột **Mượn hồ sơ** có nút riêng khi bản giấy đủ điều kiện; bản chỉ điện tử để trống cột này. Khi hồ sơ đang có yêu cầu chưa hoàn tất hoặc không khả dụng, không cho gửi thêm. Bác sĩ mới tạo hồ sơ tại `/reader/records/new`; mã tự động và quy trình lưu trữ kiểm tra/Giám đốc duyệt vẫn giữ.

Các hướng dẫn cũ về menu Hồ sơ điện tử riêng hoặc phải được duyệt mượn trước khi xem metadata được thay thế bởi phần này. File bảng chung: [records/list.html](../../src/main/resources/templates/records/list.html); thông tin cơ bản: [records/basic-detail.html](../../src/main/resources/templates/records/basic-detail.html); form bác sĩ và trang xử lý vẫn ở templates/electronic nhưng dùng URL chung /records. Xem [mô hình dữ liệu](../database/UNIFIED_MEDICAL_RECORDS.md).

## Giao diện bàn làm việc mới —

Các trang dùng thanh đầu trang xanh, tên nhân viên/vai trò, chuông thông báo thật và menu ☰ thu gọn. Bấm thẻ Tác vụ nhanh để vào chức năng; bảng gần đây và số liệu được truy vấn thật theo quyền. Reader chỉ thấy bệnh án điện tử của mình; Admin chỉ có dữ liệu tài khoản và nhật ký, không có dữ liệu bệnh nhân.

Vị trí chỉnh giao diện: [home.html](../../src/main/resources/templates/home.html) (banner, thẻ, bảng), [fragments/layout.html](../../src/main/resources/templates/fragments/layout.html) (header/menu dùng chung), [portal.css](../../src/main/resources/static/css/portal.css) (màu sắc/bố cục), [portal.js](../../src/main/resources/static/js/portal.js) (menu), `DashboardService.java` (tác vụ/số liệu theo role). Giữ các đường dẫn thật và form POST đăng xuất có CSRF khi chỉnh HTML. Thông báo đăng nhập 5 giây vẫn hoạt động.

### Form thêm bệnh án điện tử mở rộng

Đã bổ sung **Khoa điều trị cuối, Loại bệnh án, Ngày vào viện, Ngày ra viện, Người bệnh tử vong, Nguồn dữ liệu/căn cứ đối chiếu và Ghi chú** bên cạnh các trường người bệnh hiện có. Mã bệnh án vẫn tự tạo. Không có số lưu trữ, ngày đầu lưu, ngăn/kho, chất lượng giấy hoặc chính sách lưu.

Ô **Ghi chú** không có `required`, được để trống; giới hạn ký tự. Nguồn dữ liệu tùy chọn tối đa 1000 ký tự. Khoa/loại và ngày điều trị có thể để Chưa xác định/trống như form giấy đang có; nếu nhập thì phải hợp lệ. Ngày điều trị không ở tương lai, ngày ra viện không trước ngày vào viện, năm sinh không sau năm vào viện. Chọn loại bệnh án đang hoạt động. Ghi chú do bác sĩ nhập được hiển thị cho chính bác sĩ, Archivist và Director phụ trách xử lý, không phải ghi chú nội bộ riêng của nhánh giấy.

Chỉnh tại [electronic/form.html](../../src/main/resources/templates/electronic/form.html); xem kết quả tại [electronic/detail.html](../../src/main/resources/templates/electronic/detail.html). Sửa/gửi lại giữ đầy đủ các trường và giữ nguyên mã. Sau kiểm tra/trình, Director duyệt vẫn công bố ngay như trước.


## Bổ sung — Bác sĩ khai báo hồ sơ điện tử mới,

Mở **Thêm bệnh án mới** trên menu/trang chủ hoặc `/reader/electronic-records/new`. Nhập họ tên, năm sinh, giới tính và mã người bệnh nếu có; bấm Gửi kiểm tra. Không phải nhập số hồ sơ, vị trí hay chính sách kho. Mã được sinh ở backend theo `MRM-REC-YYYY-000001`.

Mở **Hồ sơ điện tử** để tìm theo tên/mã, lọc trạng thái và xem hồ sơ của mình. Nhân viên lưu trữ kiểm tra rồi trình Giám đốc. Nếu cần bổ sung, mở hồ sơ → Bổ sung/gửi lại; mã không đổi. Chỉ sửa ở Mới gửi/Cần bổ sung; hủy được khi chưa có quyết định Giám đốc. Sau phê duyệt, trạng thái **Đã công bố**, ngày công bố xuất hiện ngay; không chờ nhận giấy hoặc xếp kho. Chuông báo kết quả và mở đúng hồ sơ điện tử.

Reader chỉ truy cập hồ sơ điện tử do mình khai báo; không được đoán ID để xem của bác sĩ khác. Danh sách này tách khỏi hồ sơ giấy/đăng ký mượn; không tự cấp quyền đọc bệnh án giấy. Các biểu mẫu hiện chỉ lưu thông tin nhận diện người bệnh, chưa có phần khám/chẩn đoán/điều trị hay file đính kèm.

File HTML: [electronic/form.html](../../src/main/resources/templates/electronic/form.html), [electronic/list.html](../../src/main/resources/templates/electronic/list.html), [electronic/detail.html](../../src/main/resources/templates/electronic/detail.html). Xem [luồng và dữ liệu](../database/ELECTRONIC_RECORDS.md).

## Thông báo đăng nhập — cập nhật

Sau đăng nhập thành công, thông báo nổi ở góc trên bên phải hiện trong **5 giây** rồi tự đóng; có thể bấm **×** để đóng sớm. Thông báo chỉ hiện một lần sau đăng nhập, không hiện lại khi tải lại/quay về trang chủ. Quy tắc dùng chung cả bốn vai trò.

Giao diện nằm tại `src/main/resources/templates/home.html`; JavaScript tại `src/main/resources/static/js/login-toast.js` (thời gian `5000` mili giây); CSS `.login-toast` trong `src/main/resources/static/css/app.css`. Cờ đăng nhập được đặt trong `SecurityConfig.java` và tiêu thụ một lần trong `HomeController.java`. Cần bật JavaScript để thấy thông báo nổi.

## Trang lịch sử và nút hủy — cập nhật

Trang trong ảnh **Lịch sử xử lý yêu cầu** nằm tại [reading/history.html](../../src/main/resources/templates/reading/history.html). Đường dẫn web là `/reader/requests/{id}`. Bạn sửa tiêu đề, bố cục node và nút thao tác tại file này; kiểu dáng `.request-timeline` nằm trong [app.css](../../src/main/resources/static/css/app.css). Các bước và tên người xử lý được chuẩn bị bởi `ReadingProgress.java` và `ReadingService.java`, chuyển vào HTML qua `ReadingController.java`.

Đã thêm **Hủy yêu cầu** ở đầu trang, phía trên các node. Nút chỉ hiện khi trạng thái **Mới gửi**, **Cần bổ sung**, **Chờ duyệt**. Bấm hủy sẽ lưu lịch sử và thông báo, quay về danh sách yêu cầu; trạng thái thành **Đã hủy**, giữ ngày đăng ký và để trống ngày nhận. Không được hủy sau khi Giám đốc đã quyết định; backend kiểm tra lại trạng thái, chủ yêu cầu và phiên bản để tránh hủy từ trang cũ. Không thêm chức năng sửa nội dung yêu cầu.

Quy định mới này thay thế các mô tả cũ bên dưới nói trang tiến trình không có nút hủy.

## Cập nhật — Hiển thị yêu cầu đã hủy

Trạng thái hủy hiển thị **Đã hủy**. Trên dòng yêu cầu đã hủy, **Ngày đăng ký** vẫn hiển thị, **Ngày nhận** để trống; không còn dòng chờ xếp lịch. Dữ liệu ngày và lịch sử xử lý vẫn được giữ, nên tìm kiếm theo ngày đăng ký thực tế vẫn hoạt động.

Thông báo hủy chỉ có nội dung **Yêu cầu đã hủy**, không hiển thị mã UUID, ngày nhận/lịch hoặc lý do dài. Quy tắc này cũng áp dụng khi xem thông báo cũ. Các trạng thái yêu cầu và đã đọc/chưa đọc vẫn hiển thị tiếng Việt; chức năng mở yêu cầu/đánh dấu đã đọc giữ nguyên.

## Cập nhật — Theo dõi tiến trình

Tại **Yêu cầu**, bấm **Theo dõi tiến trình** ở bất kỳ dòng nào, kể cả yêu cầu đã duyệt, xác nhận lịch, hủy hoặc từ chối. Trang mới có các node dọc: bước đã xử lý ghi người/thời gian/nội dung; bước đang chờ được đánh dấu; bước tương lai ghi vai trò phụ trách. Trang này không sửa nội dung; có nút hủy trước quyết định của Giám đốc như hướng dẫn mới nhất.

Khi Giám đốc duyệt và nhân viên lưu trữ xác nhận lịch, nút **Xem thông tin hồ sơ** hiện nếu quyền đọc còn hiệu lực. Các bước giao–trả tiếp tục được ghi trong tiến trình. Đã bỏ dòng tổng số kết quả trong bộ lọc và liên kết theo dõi ở cuối form đăng ký. Quy tắc tìm kiếm/phân trang, quyền xem và nút Hủy trong bảng giữ nguyên.

Nội dung này thay thế các hướng dẫn trước chỉ cho xem lịch sử khi yêu cầu đang chờ duyệt. Xem [báo cáo và các file giao diện](../verification/READER_PROGRESS_TIMELINE.md).

## Cập nhật — Danh sách yêu cầu và lý do mượn

Yêu cầu mượn mặc định hiển thị 7 ngày gần nhất, 10 yêu cầu mỗi trang, có chuyển trang. Tìm theo tên người bệnh/số lưu trữ và khoảng ngày đăng ký, dùng riêng hoặc kết hợp. Chọn khoảng ngày khác để xem lịch sử cũ. Không còn tên người bệnh dưới số lưu trữ. Ngày đăng ký có cột riêng, chỉ hiện khi đã duyệt/xác nhận lịch theo quy ước giao diện; dữ liệu thực tế vẫn được lưu ngay khi gửi.

Hộp Hồ sơ đăng ký hiển thị số đã chọn, ẩn số lượng tối đa. **Lý do mượn chỉ bắt buộc khi chọn trên 5 hồ sơ**. Các hướng dẫn cũ yêu cầu mọi lần gửi đều có mục đích không còn áp dụng cho form mới. Tra cứu bệnh án chỉ còn một trạng thái tiếng Việt.

Đã xóa hai đăng ký mẫu cũ 007/012 của reader1; bệnh án gốc vẫn còn. Xem [chi tiết, file chỉnh giao diện và kiểm thử](../verification/READER_REQUEST_STATEMENT.md).

## Yêu cầu mượn: trạng thái, hủy và lịch sử —

Hướng dẫn này thay thế các mô tả trước về sửa/hủy tại trang theo dõi:

- Mở **Yêu cầu** để xem toàn bộ đăng ký của mình. Trạng thái hiển thị bằng tiếng Việt; không còn đoạn chú giải mã trạng thái cuối bảng.
- Với **Mới gửi**, **Cần bổ sung**, **Chờ duyệt**: nút **Hủy yêu cầu** nằm dưới trạng thái; liên kết **Theo dõi yêu cầu** mở lịch sử xử lý. Trang theo dõi có nút hủy trước quyết định của Giám đốc theo cập nhật mới nhất; không có form sửa.
- Với **Đã duyệt, chờ lịch**, **Từ chối**, **Đã xác nhận lịch**, **Đã hủy phần chưa thực hiện**: không hiện nút hủy và liên kết theo dõi trong bảng. Hủy không xóa dữ liệu hay lịch sử.
- Sau khi hủy, hệ thống trở lại danh sách. Nếu yêu cầu vừa được Giám đốc quyết định hoặc người khác cập nhật, hệ thống báo lỗi và yêu cầu tải lại, không hủy nhầm phiên bản.
- Số lưu trữ vẫn mở thông tin hồ sơ theo quyền đã cấp. Chuông thông báo có thể mở trang lịch sử kể cả sau khi được duyệt; trang này vẫn chỉ đọc.

Các file chỉnh giao diện: `src/main/resources/templates/reading/list.html` (bảng và nút hủy), `src/main/resources/templates/reading/history.html` (lịch sử Reader). `reading/detail.html` dành cho quy trình xử lý của nhân viên lưu trữ/Giám đốc. Nhãn tiếng Việt nằm trong `src/main/java/mrm/workflow/ReadingLabels.java`.

Xem [kết quả kiểm thử](../verification/READER_REQUEST_HISTORY.md).


## Hồ sơ còn trống và thông tin mở rộng —

Menu: **Trang chủ → Tra cứu hồ sơ → Đăng ký mượn hồ sơ**. Sau chọn hồ sơ, bấm dấu mở rộng **Thông tin hồ sơ** để xem số bệnh án, khoa, loại bệnh án và ngày vào/ra viện. Dữ liệu cần Director duyệt không được mở thêm qua bộ chọn.

Danh sách chỉ có hồ sơ đủ điều kiện và chưa có đăng ký đang hoạt động của bất kỳ Reader nào; đang mượn/chờ xử lý đều bị ẩn. Sau hủy/từ chối hoặc kết thúc toàn bộ lượt đọc, hồ sơ xuất hiện lại nếu còn khả dụng. Yêu cầu quá hạn chưa đóng vẫn giữ chỗ, cần hủy/đóng trước. Hai người gửi cùng hồ sơ sẽ chỉ một đăng ký thành công. Xem [quy tắc và bằng chứng](../verification/BORROW_PICKER_AVAILABILITY.md).

## Quy trình hiện tại —: Đăng ký mượn, không chọn ngày

Thay thế hướng dẫn chọn 1–5 ngày bên dưới: mở **Đăng ký mượn hồ sơ** từ menu/trang chủ → **＋ Chọn hồ sơ** → tìm và đánh dấu nhiều hồ sơ → **Xong** → nhập mục đích → **Gửi đăng ký mượn**. Giữ được lựa chọn khi tìm kiếm/chuyển trang, có thể Bỏ chọn từng hồ sơ; tối đa 20 hồ sơ trong một lần gửi. Đây là chọn hồ sơ đã lưu trong hệ thống, không tải file từ máy.

Reader không chọn ngày nhận. Sau kiểm tra và Director duyệt, Archivist chỉ định một ngày nhận cho từng hồ sơ trong khoảng ngày đăng ký đến hết ngày đăng ký + 2 ngày lịch (có cuối tuần). Ví dụ gửi 24/10 thì nhận muộn nhất 26/10. Quá hạn phải hủy/đăng ký mới; sửa mục đích không gia hạn.

Mỗi hồ sơ có yêu cầu/trạng thái riêng. Theo dõi tại Yêu cầu; chuông báo kết quả và ngày nhận. Sau duyệt nhưng chưa xếp lịch, quyền xem metadata tạm có hiệu lực đến hạn nhận; khi có lịch thì theo ngày nhận. Sửa/hủy làm mất hiệu lực của yêu cầu đó. Giao–trả tại phòng đọc vẫn như trước. Lịch đã có trước V14 được giữ, không tự đổi.

File chính: [reading/new.html](../../src/main/resources/templates/reading/new.html), [borrow-record-picker.js](../../src/main/resources/static/js/borrow-record-picker.js), [reading/detail.html](../../src/main/resources/templates/reading/detail.html). Bộ chọn ngày reading-dates của mốc trước không còn được sử dụng trong form Reader. Xem [báo cáo đầy đủ](../verification/BORROWING_REGISTRATION.md).


## Chọn ngày đọc — cập nhật

Tại form xin phép hoặc sửa yêu cầu, bấm ô ngày để chọn bằng lịch. Dùng **Thêm ngày** nếu cần nhiều ngày, tối đa 5; **Xóa ngày** để bỏ ô không dùng. Nhãn mới là **Ngày đọc (Số ngày mượn tối đa: 5)**, không cần gõ chuỗi ngày cách nhau dấu phẩy. Phạm vi ngày được backend cho phép vẫn giữ nguyên, không thay đổi thời hạn đăng ký.

Giao diện chọn ngày dùng chung tại [fragments/reading-dates.html](../../src/main/resources/templates/fragments/reading-dates.html); thao tác thêm/xóa nằm trong [static/js/reading-dates.js](../../src/main/resources/static/js/reading-dates.js). Hai màn hình sử dụng là reading/new.html và reading/detail.html; CSS ở static/css/app.css.

## Các file HTML để chỉnh giao diện Reader / Bác sĩ

Đối chiếu với code hiện tại. Các liên kết dưới đây mở file nguồn trong dự án. Gốc giao diện là `src/main/resources/templates/`; cấu trúc thư mục chia theo chức năng, không chia thành một thư mục riêng cho từng role.

### Màn hình của vai trò này

| File HTML | Đường dẫn website | Nội dung bạn có thể chỉnh |
|---|---|---|
| [records/list.html](../../src/main/resources/templates/records/list.html) | `/reader/records` | Tra cứu: ô lọc, bảng kết quả, phân trang và liên kết số lưu trữ. |
| [records/basic-detail.html](../../src/main/resources/templates/records/basic-detail.html) | `/reader/records/{id}` | Metadata chi tiết sau khi có phê duyệt còn hiệu lực. Chưa có quyền thì chuyển tới trang xin phép, không render file này. |
| [reading/new.html](../../src/main/resources/templates/reading/new.html) | `/reader/requests/new` (recordId tùy chọn) | Đăng ký mượn nhiều hồ sơ bằng bộ chọn và mục đích; Reader không chọn. |
| [reading/list.html](../../src/main/resources/templates/reading/list.html) | `/reader/requests` | Danh sách đăng ký, liên kết Hồ sơ và Theo dõi yêu cầu. |
| [reading/detail.html](../../src/main/resources/templates/reading/detail.html) | `/reader/requests/{id}` | Tiến trình, lịch đọc, thông báo kết quả; form bổ sung/sửa/hủy dành cho Reader. |

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

## Quy tắc mới theo CR-008 —

Bấm số lưu trữ ở Tra cứu hoặc Yêu cầu/lịch đọc: chưa có quyền được duyệt còn hiệu lực sẽ chuyển tới **Xin phép đọc hồ sơ**; gửi mục đích/ngày đọc tại đó. Muốn xem tiến trình đã gửi, chọn **Theo dõi yêu cầu**. Sau Director duyệt, số lưu trữ mở metadata chi tiết, không mở lại yêu cầu.

Quyền có hiệu lực ngay sau duyệt đến hết ngày đọc cuối; sửa/hủy yêu cầu hoặc hết hạn làm mất quyền từ yêu cầu đó. Không dùng quyền của Reader khác. Có thể còn quyền từ một yêu cầu hợp lệ khác của chính mình. Không mở vị trí kho, ghi chú nội bộ hay bản quét bệnh án. Quy tắc này thay thế hướng dẫn xem chi tiết trước khi xin phép trong các mục cũ. Xem [CR-008](../changes/CR-008_workflow-alerts-and-reader-access.md).


## Bổ sung — Chuông thông báo

1. Quan sát chuông ở đầu trang; số đỏ là số chưa đọc (`99+` nếu trên 99).
2. Bấm chuông để xem kết quả bổ sung, duyệt/từ chối, xác nhận lịch và hủy yêu cầu của mình.
3. Chọn **Chưa đọc** để lọc; mỗi trang có 20 thông báo. Dùng **Đánh dấu đã đọc** hoặc **Đánh dấu tất cả đã đọc** để cập nhật.
4. Bấm **Mở yêu cầu** để xem trạng thái/lịch hiện tại và đánh dấu thông báo đã đọc. Đọc thông báo không đồng nghĩa nhận/trả bệnh án.
5. Tải lại trang để thấy kết quả mới. Không gửi email hoặc thông báo đẩy. Thông báo cũ giữ nội dung lịch sử, nên mở yêu cầu để xem lịch mới nhất.

Chỉ thấy thông báo của mình; không tạo bù kết quả trước triển khai. Xem [FR-11](../verification/FR11_INTERNAL_NOTIFICATIONS.md).

 Áp dụng cho phiên bản mở rộng FR. Tài liệu dành cho người dùng thao tác trên web, không yêu cầu biết Java hoặc SQL.

## 1. Bạn được sử dụng những gì?

Reader được đăng nhập, tra cứu và xem metadata hồ sơ chính thức, đăng ký lịch đọc, bổ sung/sửa/hủy yêu cầu của mình và theo dõi kết quả/lịch.

Bạn không được xem bản nháp, vị trí kho chi tiết, ghi chú/nguồn đối soát nội bộ hoặc lịch sử vận hành. Bạn không nhập/sửa/chuyển hồ sơ hay xác nhận chính sách. Đã có đăng ký đọc hồ sơ giấy tại phòng đọc; không xem bản quét/toàn văn bệnh án trong app.

## 2. Đăng nhập

1. Đảm bảo ứng dụng đã được người phụ trách khởi động.
2. Mở trình duyệt tới `http://localhost:8080/login` nếu chạy trên cùng máy bằng cấu hình mặc định. Nếu được cấp địa chỉ khác, dùng địa chỉ được cấp; localhost luôn chỉ máy đang mở trình duyệt.
3. Nhập tài khoản Reader và mật khẩu được cấp. Tài khoản demo hiện có là `reader1`; mật khẩu là giá trị đã đặt khi khởi tạo, không có mật khẩu mặc định trong hướng dẫn.
4. Bấm **Đăng nhập**.
5. Kiểm tra trang **Trang người đọc**, vai trò **READER**; địa chỉ trang chủ là `/reader/home`.

Nếu sai mật khẩu hoặc tài khoản không được phép đăng nhập, trang login hiển thị lỗi. Nhập lại đúng thông tin hoặc liên hệ người phụ trách; hiện chưa có chức năng tự khôi phục mật khẩu.

## 3. Tìm hồ sơ

1. Tại trang chủ, bấm **Tra cứu hồ sơ bệnh án**, hoặc chọn **Tra cứu hồ sơ** trên thanh điều hướng.
2. Trong ô **Số lưu trữ / số bệnh án / mã người bệnh / tên**, nhập một trong các thông tin bạn biết.
3. Bổ sung bộ lọc nếu cần, rồi bấm **Tìm kiếm**.

| Thông tin tìm | Cách nhập |
|---|---|
| Số lưu trữ | Nhập đầy đủ số được cấp, ví dụ `-000001`; số thực tế có thể khác |
| Số bệnh án | Nhập đầy đủ số nguồn của đợt điều trị |
| Mã người bệnh | Nhập đầy đủ mã; có thể tìm được nhiều hồ sơ của nhiều đợt |
| Họ tên | Nhập toàn bộ hoặc một phần tên, có hoặc không dấu; không phân biệt hoa/thường |
| Năm sinh | Nhập năm để phân biệt người trùng tên |
| Khoa / loại bệnh án | Chọn giá trị tương ứng hoặc để Tất cả |
| Ra viện từ ngày / đến ngày | Lọc theo ngày ra viện, không phải ngày nhập hồ sơ vào phần mềm |
| Trạng thái lưu chuyển | Chọn Trong kho, Tạm không khả dụng hoặc Đang giao đọc nếu cần |

Các bộ lọc cùng được áp dụng. Ví dụ: tên `nguyen van an`, năm sinh ``, khoa Nội sẽ thu hẹp kết quả hơn chỉ tìm tên.

Tìm bằng mã là tìm chính xác, không phải tìm một phần mã. Tên có thể tìm theo chuỗi; không dùng ký tự `%` hoặc `_` làm ký hiệu đại diện.

## 4. Đọc danh sách và xem chi tiết

1. Đọc tổng số hồ sơ tìm thấy.
2. Đối chiếu **họ tên, năm sinh, mã người bệnh, số bệnh án và đợt điều trị**. Không kết luận hai dòng cùng tên là cùng người hoặc cùng hồ sơ.
3. Chọn số dòng 10/20/50/100 và cách sắp xếp nếu cần; bấm **Tìm kiếm** để áp dụng.
4. Dùng **Trang trước / Trang sau** để chuyển trang, các bộ lọc vẫn được giữ.
5. Bấm liên kết số lưu trữ để xem chi tiết cơ bản.
6. Dùng **Tra cứu hồ sơ** trên thanh điều hướng để quay lại tìm kiếm, hoặc nút Back của trình duyệt để trở lại danh sách trước đó.

Chi tiết cơ bản gồm nhận diện hồ sơ/người bệnh, khoa/loại, đợt điều trị và trạng thái. Đây là metadata phục vụ tra cứu; không phải nội dung toàn văn hồ sơ bệnh án.

| Mã có thể xuất hiện | Ý nghĩa |
|---|---|
| STORED | Hồ sơ đã xác nhận nhập kho |
| IN_STORAGE | Trong kho, đang khả dụng |
| UNAVAILABLE | Tạm không khả dụng; liên hệ Archivist để được giải thích/xử lý |
| READING | Đang giao đọc; luồng giao–trả đầy đủ thuộc giai đoạn tiếp theo |
| MALE / FEMALE / UNKNOWN | Nam / Nữ / Chưa xác định |

## 5. Khi không tìm thấy hồ sơ hoặc gặp lỗi

| Hiện tượng | Cách xử lý |
|---|---|
| Không có kết quả | Bấm **Xóa bộ lọc**, thử lại một thông tin chính; kiểm tra mã đã nhập đủ và khoảng ngày |
| Biết có hồ sơ nhưng không thấy | Có thể hồ sơ còn nháp hoặc chưa được nhập; nhờ Archivist đối chiếu |
| Khoảng ngày bị báo lỗi | Đặt ngày bắt đầu không sau ngày kết thúc |
| 403 / không được phép | Trở về trang chủ; bạn có thể đang mở URL của vai trò khác |
| 404 / không tìm thấy hồ sơ | Quay lại danh sách và chọn lại; nhờ Archivist kiểm tra số lưu trữ, không tự thay UUID trong URL |
| Bị đưa về login | Đăng nhập lại bằng tài khoản của mình |
| Không mở được website | Kiểm tra địa chỉ và nhờ người phụ trách kiểm tra ứng dụng đã chạy |

## 6. Kết thúc sử dụng

1. Trở về trang chủ hoặc trang tra cứu.
2. Bấm **Đăng xuất** trên thanh điều hướng.
3. Kiểm tra đã trở về trang login. Đóng tab không thay thế thao tác đăng xuất.

## 7. Bài thực hành ngắn

- [ ] Đăng nhập và xác nhận đúng vai trò READER.
- [ ] Tìm một hồ sơ giả đã được Archivist nhập chính thức.
- [ ] Tìm tên không dấu rồi kết hợp năm sinh/khoa.
- [ ] Mở chi tiết, đối chiếu đúng đợt điều trị; không có vị trí/ghi chú nội bộ.
- [ ] Xóa bộ lọc và đăng xuất.

Các ô trên để bạn tự đánh dấu khi thực hành, không phải kết quả nghiệm thu đã ghi sẵn.

Xem thêm: [mục lục tài liệu](../README.md), [báo cáo năm chức năng](../verification/FIVE_FEATURE_COMPLETION_REPORT.md).

## 8. Đăng ký và theo dõi đọc hồ sơ

1. Tra cứu và mở hồ sơ chính thức không tử vong → bấm **Đăng ký đọc**.
2. Nhập mục đích cụ thể; nhập 1–5 ngày từ hôm nay đến 30 ngày tới, dạng `yyyy-MM-dd`, cách nhau dấu phẩy. Bấm **Gửi yêu cầu**.
3. Mở **Yêu cầu / lịch đọc** trên menu. Chỉ các yêu cầu của bạn xuất hiện.
4. Theo dõi trạng thái: SUBMITTED mới gửi → NEEDS_INFO nếu cần bổ sung, hoặc PENDING_APPROVAL chờ Director → APPROVED đã duyệt nhưng chờ Archivist xác nhận → CONFIRMED mới có lịch chính thức. REJECTED là từ chối.
5. Đọc kết quả kiểm tra, căn cứ quyết định và hướng dẫn phòng đọc ngay trên trang chi tiết. **Chưa gửi email** theo phạm vi đã thống nhất.
6. Nếu cần sửa trước lần giao đầu, nhập lại mục đích và toàn bộ tập ngày mong muốn ở mục Bổ sung/sửa. Sửa làm mất hiệu lực quyết định/lịch cũ và phải trình duyệt lại; lịch sử phiên bản vẫn được giữ.
7. Đến phòng đọc theo lịch CONFIRMED; nhận và trả hồ sơ giấy trong ngày, Archivist ghi nhận từng ngày. Không nhận hồ sơ chỉ vì request đã APPROVED mà chưa có lịch xác nhận.
8. Nếu muốn hủy phần chưa làm, ghi lý do. Không hủy khi đang có phiên READING chưa trả. Sau khi đã giao một ngày, không sửa nội dung; hủy phần chưa làm rồi tạo yêu cầu mới nếu cần.

Phiên CONFIRMED = chờ đọc; READING = đã giao/chưa trả; RETURNED = đã nhận trả; NO_SHOW = không đến; CANCELLED = ngày đã hủy. Nhiều ngày là nhiều phiên riêng, không giữ giấy qua đêm. Hồ sơ tử vong xin khai thác ngoài phần mềm.

Checklist thực hành bổ sung: đăng ký dữ liệu giả → nhận trả bổ sung → sửa → theo dõi quyết định/lịch → đối chiếu thời điểm giao/trả → hủy phần chưa làm khi phù hợp. Xem [báo cáo FR](../verification/FR_COMPLETION_REPORT.md).
