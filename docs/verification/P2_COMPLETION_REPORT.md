# Báo cáo hoàn thiện P2 — MRM-UC-03

**Cập nhật sau P2:** UC-04 đã lưu snapshot/FK cùng hồ sơ; V7 đã áp dụng và bootstrap được kiểm tra đến V7. Đọc [báo cáo năm chức năng](FIVE_FEATURE_COMPLETION_REPORT.md) cho kết quả hiện tại. Các mốc V6 và 75 kiểm tra bên dưới là lịch sử lúc hoàn thiện riêng P2.

Ngày:, múi giờ Asia/Bangkok (+07:00).

## 1. Kết luận

Đã hoàn thiện phần triển khai P2 cho dự án học tập theo xác nhận của người dùng: danh mục năm khoa cố định, loại bệnh án, chính sách bảo quản theo phiên bản, phân quyền, audit, chống biểu mẫu cũ và khởi tạo môi trường trống. Đã áp dụng migration V6 trên database `mrm`; V1–V5 giữ nguyên.

Phân biệt ba việc: người dùng đã chốt quy tắc mô phỏng; phần mềm đã được kiểm tra kỹ thuật; nghiệm thu trực quan cuối bởi người dùng chưa diễn ra. Không ghi nhận thay hành động xác nhận của Director. Bốn chính sách mẫu ở DRAFT, chờ Director xác nhận trong ứng dụng trước khi chọn cho hồ sơ mới.

## 2. Mục đích của P2 và giá trị đối với hệ thống

P2 là nhóm công việc ưu tiên thứ hai trong PROJECT_CHECKLIST, tương ứng **MRM-UC-03 — Quản lý danh mục khoa, loại hồ sơ và chính sách lưu**. P2 không phải giai đoạn viết toàn bộ phần mềm lưu trữ; nó chuẩn bị dữ liệu tham chiếu và quy tắc thống nhất để các hồ sơ được nhập ở P3.

Ví dụ, khi nhập một hồ sơ, nhân viên cần chọn khoa điều trị, loại bệnh án và chính sách bảo quản. Nếu tự gõ các thông tin này cho từng hồ sơ, cùng một khoa có thể được ghi bằng nhiều tên, hoặc hai hồ sơ cùng nhóm lại được gán thời hạn khác nhau. P2 đưa các giá trị đó vào danh mục dùng chung và kiểm tra chúng tại backend.

| Thành phần | Mục đích | Sai sót cần ngăn ngừa |
|---|---|---|
| Danh mục khoa | Xác định hồ sơ thuộc khoa điều trị cuối nào; dùng chung khi nhập, lọc và thống kê | Tên khoa nhập tùy ý, khoa không tồn tại, phát sinh khoa ngoài phạm vi năm khoa đã chọn |
| Loại bệnh án | Phân loại hình thức/chuyên khoa của bệnh án, độc lập với khoa điều trị | Nhầm loại bệnh án với khoa, cờ tử vong hoặc nhóm thời hạn |
| Chính sách lưu | Lưu số năm, phạm vi/căn cứ, phiên bản và khoảng hiệu lực | Dùng chính sách chưa xác nhận, hết hiệu lực hoặc không có căn cứ |
| Phiên bản và snapshot | Giữ lại quy tắc đã áp dụng tại thời điểm nhập hồ sơ | Một thay đổi danh mục làm thay đổi âm thầm thời hạn/lịch sử hồ sơ cũ |
| Phân quyền và audit | Gắn thao tác với đúng vai trò, người và thời điểm | Người không có quyền sửa danh mục/xác nhận chính sách; thay đổi không có lịch sử |

Đầu ra của P2 gồm các bảng danh mục, màn hình quản lý, bộ kiểm tra lựa chọn và DTO bản chụp bất biến `CatalogSnapshot`. P2 chưa tạo hồ sơ bệnh án, chưa cấp số lưu trữ, chưa bố trí kho/kệ/ngăn và chưa xây dựng tra cứu bệnh án.

Quy tắc 10/15/20/30 năm được người dùng đồng ý **cho mô phỏng học tập**, tham khảo tài liệu Khánh Sơn năm. Đó không phải kết luận về pháp luật hiện hành hoặc xác nhận nghiệp vụ của trung tâm. Việc người dùng chốt quy tắc dự án cũng khác với việc tài khoản Director xác nhận một phiên bản chính sách cụ thể trong phần mềm.

## 3. Đối chiếu các đầu việc P2

| Nội dung | Kết quả |
|---|---|
| Khoa/phòng | Cố định năm khoa đã seed; bỏ form thêm, POST trả 405; database trigger chặn thêm/sửa/xóa. Giữ ID và lịch sử tạo |
| Loại bệnh án | Sáu loại sẵn có; Archivist thêm/sửa tên/ngừng dùng; mã không đổi, không xóa cứng |
| Chống ghi đè | `record_types.version` tăng mỗi lần sửa; biểu mẫu cũ bị chặn, chỉ một trong hai luồng sửa cùng version được lưu |
| Chính sách bảo quản | Mã+phiên bản unique; số năm/ngày/căn cứ được kiểm tra; Archivist tạo dự thảo, Director xác nhận/ngừng dùng |
| Dữ liệu mô phỏng | GENERAL=10, ACCIDENT=15, PSYCHIATRIC=20, DECEASED=30 năm; v1, ngày hiệu lực dự kiến; nguồn Khánh Sơn và xác nhận dự án học tập |
| Quy tắc tính | Từ ngày đầu lưu; nhiều nhóm lấy dài nhất; gốc 29/2 chuyển ngày đích 28/2, kể cả năm đích nhuận. Không có tiêu hủy |
| Ràng buộc chọn danh mục | Chặn ID không có, mục ngừng dùng, policy DRAFT hoặc ngoài ngày hiệu lực; backend kiểm tra lại dữ liệu |
| Lịch sử/snapshot | Bộ chọn tạo bản chụp bất biến mã/tên/phiên bản/số năm/căn cứ/ngày gốc/ngày dự kiến. Đổi/ngừng danh mục không đổi bản chụp đã cấp |
| Audit và transaction | Tạo/sửa/xác nhận/ngừng dùng được ghi cùng giao dịch; lỗi audit rollback nghiệp vụ |
| Dựng môi trường trống | Profile `dev,seed-users`: migrate đến V3, bootstrap Archivist với password được cấp, migrate tiếp rồi tạo các demo user còn thiếu. Đã chạy ứng dụng thật trên schema trống cách ly |
| Kiểm thử | 49 mục PostgreSQL/HTTP + 26 mục P2 mở rộng đạt; schema và dữ liệu thử được dọn |

## 4. Quyền sử dụng và cách kiểm tra thủ công

1. Chạy ứng dụng với profile `dev` như bình thường; database hiện tại đã có V6, không cần chạy lại seed-users nếu tài khoản đã tồn tại.
2. Đăng nhập Archivist: xem **Quản lý khoa/phòng** (không còn thêm), **Quản lý loại bệnh án**, **Quản lý chính sách bảo quản**.
3. Đăng nhập Director: chọn **Xác nhận chính sách bảo quản**; xem bốn nhóm mô phỏng, căn cứ và ngày hiệu lực trước khi xác nhận các nhóm muốn sử dụng.
4. Reader và Admin không truy cập được danh mục nghiệp vụ qua URL Archivist/Director.
5. Để kiểm tra xung đột, mở cùng một loại ở hai tab: lưu tab đầu, sau đó lưu tab còn lại; thao tác thứ hai báo cần tải lại, không ghi đè.

Ngày hiệu lực không phải ngày đầu lưu hồ sơ: bộ chọn đối chiếu ngày đầu lưu với khoảng hiệu lực chính sách. Hồ sơ lịch sử trước cần một phiên bản có hiệu lực phù hợp; không tự gán bốn phiên bản mẫu cho mọi hồ sơ cũ.

Khi dựng một database phát triển mới, cấp DB_PASSWORD và bốn biến SEED_*_PASSWORD rồi chạy:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev,seed-users'
```

Không có password mặc định trong mã. Tài khoản đã tồn tại không bị đổi password. Cơ chế bootstrap này dành cho profile phát triển, không phải cấu hình triển khai vận hành.

## 5. Quá trình và kết quả kiểm thử chi tiết

### 5.1. Môi trường, công cụ và dữ liệu kiểm tra

| Nội dung | Giá trị sử dụng trong lượt kiểm chứng |
|---|---|
| Môi trường | Windows, Java 26.0.2.1, Spring Boot 4.1.1 |
| Database hiện có | PostgreSQL 18.6, `127.0.0.1:5433/mrm`, schema `public` |
| Migration | V1–V6; lượt P2 này chỉ thêm V6, không sửa checksum migration cũ |
| HTTP bản cuối | `http://localhost:53348`; cổng ngẫu nhiên do `-server.port=0`, không phải cổng chạy thông thường |
| Bộ kiểm tra danh mục | [CatalogIntegrationCheck.java](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/src/test/java/mrm/catalog/CatalogIntegrationCheck.java>) |
| Bộ kiểm tra P2 mở rộng | [CompleteCatalogCheck.java](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/src/test/java/mrm/catalog/CompleteCatalogCheck.java>) |
| Dữ liệu nghiệp vụ | Mã/tên thử có tiền tố hoặc UUID, ngày và chính sách giả lập; không dùng dữ liệu người bệnh thật |
| Tài khoản HTTP | Bốn tài khoản thử tương ứng bốn role, mật khẩu ngẫu nhiên trong bộ nhớ; xóa sau kiểm tra |
| Schema dựng mới | `p2_bootstrap_<UUID>` tách biệt; chạy ứng dụng thật rồi đóng và xóa schema thử |

Hai chương trình kiểm tra là Java có hàm `main`, dùng assertion tự kiểm tra kết quả và ném lỗi nếu điều kiện không đạt. Chúng **không phải 75 bài JUnit độc lập**, và không được Surefire tự chạy. Con số 75 là tổng số điểm kiểm tra đạt được từ hai chương trình trong lượt chạy cuối.

### 5.2. Trình tự thực hiện

1. **Build:** chạy `.\mvnw.cmd -o -B -ntp package` để biên dịch mã ứng dụng, mã kiểm tra và đóng gói JAR. Build thành công chỉ chứng minh việc biên dịch/đóng gói; chưa thay thế các bước kiểm thử sau.
2. **Áp dụng migration:** khởi động ứng dụng với profile `dev`, xác nhận V6 được áp dụng trên `mrm`. V6 thêm version loại bệnh án, khóa danh mục khoa và tạo bốn chính sách mô phỏng ở DRAFT.
3. **Kiểm tra dữ liệu ban đầu:** đối chiếu năm khoa, sáu loại bệnh án và bốn chính sách mô phỏng; chưa tự xác nhận các chính sách này bằng role Director.
4. **Kiểm tra service/database:** dùng Spring transaction và PostgreSQL thật để tạo/sửa dữ liệu thử, thử tình huống lỗi, kiểm tra audit. Các giao dịch thử chủ động rollback; riêng ca audit thất bại kiểm tra rollback tự động của service.
5. **Kiểm tra HTTP:** gửi GET/POST bằng Java HttpClient qua server đang chạy. Mỗi phiên có cookie riêng; lấy CSRF token từ HTML, đăng nhập, gọi URL và đối chiếu status code cùng dữ liệu trong database.
6. **Kiểm tra P2 mở rộng:** kiểm tra trigger khoa, bộ chọn danh mục, tính hạn, snapshot, biểu mẫu cũ và hai luồng sửa đồng thời.
7. **Kiểm tra dựng mới:** tạo schema thử có tên ngẫu nhiên đã kiểm tra định dạng; khởi động Spring Boot với `dev,seed-users`, trỏ datasource và Flyway vào schema đó. Xác nhận V1–V6, tài khoản và danh mục được khởi tạo đầy đủ.
8. **Dọn dữ liệu:** rollback/xóa đúng dữ liệu thử, đóng ứng dụng/schema thử, đối chiếu schema đã được xóa. Sau toàn bộ kiểm tra, dừng server kiểm chứng.

Các nhóm chạy tuần tự để việc so sánh số bản ghi trước/sau không bị ảnh hưởng bởi dữ liệu thử của nhóm khác. Kiểm tra tranh chấp sửa loại bệnh án là ngoại lệ có chủ đích: hai luồng chạy đồng thời trên cùng bản ghi thử.

### 5.3. Kịch bản và kết quả — nhóm service/database

Các dòng dưới đây nhóm những điểm kiểm tra liên quan, không phải mỗi dòng tương ứng đúng một assertion.

| Mã báo cáo | Đầu vào và thao tác | Kết quả mong đợi | Kết quả đã quan sát |
|---|---|---|---|
| P2-DB-01 | Kiểm tra form loại với mã `!`, tên chỉ có khoảng trắng | Bean Validation trả vi phạm | Đạt: tập vi phạm không rỗng |
| P2-DB-02 | Kiểm tra form policy với version=0, years=0, thiếu ngày hiệu lực và căn cứ trắng | Không hợp lệ | Đạt: tập vi phạm không rỗng; chưa đếm/assert riêng từng thông báo lỗi |
| P2-DB-03 | Thêm loại bằng mã chữ thường và tên tiếng Việt có khoảng trắng đầu/cuối | Mã uppercase, tên trim, giữ dấu tiếng Việt | Đạt: đọc lại tên đúng và tìm được bằng mã chuẩn hóa |
| P2-DB-04 | Sửa tên, chuyển active=false | Bản ghi còn tồn tại, trạng thái ngừng dùng | Đạt; có sự kiện tạo và cập nhật |
| P2-DB-05 | Tạo hai loại cùng mã | Chặn trùng | Đạt: `DuplicateKeyException` |
| P2-DB-06 | Tạo policy có ngày bắt đầu, kết thúc | Chặn ngày đảo | Đạt: `CatalogException`, không lưu phiên bản lỗi |
| P2-DB-07 | Tạo policy hợp lệ rồi Director xác nhận | Ban đầu DRAFT/inactive; sau xác nhận active và có người xác nhận | Đạt: đọc lại approver là `director1` trong giao dịch thử |
| P2-DB-08 | Ngừng dùng policy đã xác nhận | Giữ phiên bản/lịch sử, active=false | Đạt: trạng thái APPROVED vẫn được giữ |
| P2-DB-09 | Xác nhận hai phiên bản cùng mã có khoảng hiệu lực chồng nhau | Chặn phiên bản thứ hai | Đạt: `CatalogException` |
| P2-DB-10 | Xác nhận hai phiên bản cùng mã có khoảng liền nhau nhưng không giao nhau | Cho phép cả hai phiên bản | Đạt: hai phiên bản active, áp dụng ở những ngày khác nhau |
| P2-DB-11 | Tạo lại cùng mã+version policy | Chặn trùng | Đạt: `DuplicateKeyException` |
| P2-DB-12 | Gây lỗi audit bằng actor quá độ dài cho phép sau bước tạo loại | Toàn bộ transaction rollback | Đạt: ngoại lệ dữ liệu; số loại, policy và sự kiện trở về số ban đầu |

`P2-DB-01/02` kiểm tra rằng đầu vào tổng hợp có lỗi bị validator nhận diện; kết quả đó không được diễn giải thành đã kiểm tra độc lập mọi tổ hợp trường, độ dài và nội dung thông báo.

### 5.4. Kịch bản và kết quả — HTTP, phân quyền và biểu mẫu

| Mã báo cáo | Thao tác | Kết quả mong đợi | Kết quả đã quan sát |
|---|---|---|---|
| P2-WEB-01 | Người chưa đăng nhập mở danh mục loại | Chuyển tới luồng đăng nhập | HTTP 302 |
| P2-WEB-02 | Đăng nhập bốn tài khoản thử qua form thật có CSRF | Render login và xác thực được | GET 200; POST 302 tới `/home` cho cả bốn role |
| P2-WEB-03 | Archivist mở loại/policy; role khác mở URL Archivist | Archivist được truy cập; role khác bị chặn | Archivist 200; Reader/Director/Admin 403 ở URL loại được kiểm tra |
| P2-WEB-04 | Director mở trang policy Director; role khác gọi URL đó | Chỉ Director được truy cập | Director 200; Reader/Archivist/Admin 403 |
| P2-WEB-05 | Archivist POST thêm loại nhưng bỏ CSRF | Từ chối trước khi ghi dữ liệu | HTTP 403 |
| P2-WEB-06 | Gửi form loại sai mã và tên rỗng, có CSRF | Render lại form với lỗi | HTTP 200 và HTML có phần tử `role="alert"` |
| P2-WEB-07 | POST loại hợp lệ, sau đó sửa tên/ngừng dùng | Redirect sau lưu; database đúng | HTTP 302, active=false trong database |
| P2-WEB-08 | Gửi lại form sửa với version=0 sau khi bản ghi đã lên version=1 | Không ghi đè | HTTP 302; tên trong database vẫn là tên của lần lưu trước |
| P2-WEB-09 | Mở khoa cố định rồi POST thêm khoa với CSRF | Xem được danh sách, không thêm được | GET 200, không có form thêm; POST 405 |
| P2-WEB-10 | POST policy có số/version và khoảng ngày hợp lệ | Bind đúng kiểu dữ liệu và tạo dự thảo | HTTP 302; truy vấn được policy thử |
| P2-WEB-11 | Director xem danh sách policy có dữ liệu và POST xác nhận/ngừng dùng | Render đúng và xử lý đúng quyền | Các trang trả 200; thao tác trả 302 |
| P2-WEB-12 | Archivist POST trực tiếp URL xác nhận Director | Từ chối dù có CSRF của phiên Archivist | HTTP 403 |
| P2-WEB-13 | Kết thúc nhóm HTTP | Không giữ dữ liệu thử | Số bản ghi loại/policy/audit về mức trước chạy; tài khoản thử được xóa trong finally |

Các status code được kiểm tra với chế độ không tự theo redirect, nên phân biệt được phản hồi 302 và trang đích. Kiểm tra HTML xác nhận server render được form/danh sách; không đánh giá bằng mắt màu sắc, bố cục hay khả năng thao tác trên điện thoại.

### 5.5. Kịch bản và kết quả — quy tắc P2, snapshot và dựng mới

| Mã báo cáo | Thao tác/dữ liệu | Kết quả mong đợi | Kết quả đã quan sát |
|---|---|---|---|
| P2-EXT-01 | Đọc danh mục gốc | Có năm khoa và bốn policy mẫu DRAFT/inactive | Đạt |
| P2-EXT-02 | Gửi SQL qua Java để thêm, sửa, xóa khoa | Database cũng chặn, không chỉ giao diện | Cả ba thao tác phát sinh `DataIntegrityViolationException` |
| P2-EXT-03 | Chọn policy thử còn DRAFT | Chặn lựa chọn | `CatalogException` |
| P2-EXT-04 | Xác nhận các nhóm thử 10/15/20/30, chọn cùng lúc với ngày gốc | Lấy 30 năm, ngày đích | Đạt |
| P2-EXT-05 | Chỉ chọn nhóm 20 năm, ngày gốc |, dù năm nhuận, theo quy tắc học tập đã chốt | Đạt |
| P2-EXT-06 | Tra policy theo ngày trong hiệu lực và ngày trước hiệu lực | Chỉ trả mục hợp lệ theo ngày | Các ID thử xuất hiện ở ngày hợp lệ và bị loại khi tra ngày trước hiệu lực |
| P2-EXT-07 | Tạo snapshot rồi đổi tên/ngừng loại, ngừng các policy | Snapshot cũ giữ nguyên tên, 30 năm và bốn nhóm | Đạt; danh sách policy trong snapshot không cho sửa |
| P2-EXT-08 | Lấy danh sách loại mới và chọn lại sau khi loại ngừng dùng | Không hiện/chọn được loại ngừng | Đạt; thao tác chọn bị chặn |
| P2-EXT-09 | Gọi chọn với ID khoa/loại ngẫu nhiên không có | Từ chối dữ liệu tham chiếu không hợp lệ | `CatalogException` |
| P2-EXT-10 | Hai luồng cùng sửa một loại với version=0, được giải phóng đồng thời bằng CountDownLatch | Một thành công, một xung đột | Đạt: đúng một thành công, một `CatalogException`, version cuối=1 |
| P2-EXT-11 | Khởi động ứng dụng thật trên schema thử trống với `dev,seed-users` và mật khẩu thử | Không còn lỗi thiếu Archivist trước V4 | V1–V6 thành công; có bốn tài khoản, năm khoa, sáu loại, bốn policy |
| P2-EXT-12 | Đóng ứng dụng thử và dọn schema | Không giữ schema thử | Truy vấn information_schema xác nhận schema thử không còn |

Ngày trong các ca trên là dữ liệu giả lập để thử thuật toán, không phải hồ sơ được nhập chính thức hoặc cho phép bỏ kiểm tra ngày tương lai của UC-04. Ca chặn ID ngẫu nhiên kiểm tra trường hợp khoa và loại đều không tồn tại; chưa phải kiểm tra riêng mọi tổ hợp chỉ một ID sai.

### 5.6. Kết quả tổng hợp và bằng chứng thời gian

| Bộ kiểm tra | Số điểm đạt | Điểm không đạt trong lượt cuối | Mã thoát |
|---|---|---|---|
| `CatalogIntegrationCheck` | 49 | 0 | 0 |
| `CompleteCatalogCheck` | 26 | 0 | 0 |
| **Tổng** | **75** | **0** | Hai chương trình kết thúc thành công |

Đây là kết quả của phạm vi ca kiểm tra đã thực hiện, không phải cam kết không còn lỗi trong mọi tình huống. Các lỗi tạm thời trong quá trình phát triển đã được sửa trước lượt cuối; số 0 trong bảng không phủ nhận các lần build thất bại trước đó.

- Build `.\mvnw.cmd -o -B -ntp package`: BUILD SUCCESS +07:00.
- V6 áp dụng trên `127.0.0.1:5433/mrm` thành công +07:00.
- Khởi động bản cuối +07:00: Flyway xác nhận schema version 6, không còn migration cần chạy.
- Bộ `CatalogIntegrationCheck`: 49 kiểm tra đạt, bao gồm form POST, lỗi validation, bốn role, CSRF, chặn form cũ và danh mục khoa cố định.
- Bộ `CompleteCatalogCheck`: 26 kiểm tra đạt, gồm ngày nhuận, max nhiều nhóm, lựa chọn hợp lệ, snapshot, hai thao tác đồng thời và khởi tạo V1–V6 trong schema trống.
- Cả hai bộ chạy tường minh; Maven package chỉ biên dịch chương trình kiểm tra, không tự chạy chúng. Lệnh chạy lại xem `docs/testing/CATALOG_VERIFICATION.md`.
- Đã dừng phiên ứng dụng kiểm tra sau khi hoàn tất; không để server thử tiếp tục chạy.

Lượt cuối bắt đầu nhóm danh mục +07:00; ứng dụng thử trên schema trống khởi động xong +07:00. Sau đó chương trình xác nhận dữ liệu khởi tạo, đóng ứng dụng và xóa schema thử thành công. Giờ chính xác của dòng kết thúc `PASSED` không được ghi trong log, nên không tự gán timestamp cho mốc này.

Không in password vào log; tài khoản/danh mục/sự kiện thử được xóa hoặc rollback. Schema thử có tên `p2_bootstrap_<UUID>` được kiểm tra đúng mẫu trước khi tạo/xóa; không xóa schema public.

### 5.7. Những phần chưa được chứng minh bởi lượt kiểm tra này

- Chưa nghiệm thu trực quan bằng người dùng trên trình duyệt/điện thoại.
- Chưa có bảng hồ sơ UC-04 nên chưa kiểm tra lưu snapshot, FK và lịch sử cùng hồ sơ bệnh án qua nhiều lần khởi động.
- Đã kiểm tra tranh chấp sửa loại bằng hai luồng; chưa thử hai tiến trình cùng xác nhận policy hoặc tải nhiều người dùng.
- Chưa chạy độc lập tất cả biên ngày hiệu lực, ngày hết hạn, giá trị độ dài cực đại hoặc mọi trường hợp missing/invalid/null.
- Kiểm tra snapshot chứng minh đối tượng Java bất biến và không đổi sau thay đổi danh mục trong kịch bản thử; không chứng minh dữ liệu snapshot đã được lưu bền vững trong bảng hồ sơ.
- Kết quả khởi tạo từ trống áp dụng cho schema cách ly trong PostgreSQL phát triển với cấu hình đã nêu, không thay thế kiểm chứng triển khai production hoặc tạo database bằng tài khoản hạn chế quyền.

## 6. Tác động của P2 đến P3 — MRM-UC-04

### 6.1. P3 nhận được những gì từ P2?

P3 là **Nhập kho, cập nhật metadata và đối soát dữ liệu ban đầu**. P2 chuẩn bị danh mục và quy tắc; P3 sử dụng chúng để tạo một hồ sơ bệnh án cụ thể.

| Đầu ra P2 | P3 sử dụng như thế nào? | Việc P3 còn phải làm |
|---|---|---|
| Năm khoa, ID ổn định | Nạp vào ô chọn khoa điều trị cuối | Lưu `department_id`, tạo FK và kiểm tra tồn tại tại lúc lưu |
| Loại bệnh án active | Nạp vào ô chọn loại | Lưu `record_type_id`, kiểm tra lại khi submit, không chỉ tin danh sách đã hiển thị |
| Policy APPROVED/active/đúng ngày | Hiển thị lựa chọn theo ngày đầu lưu | Chặn DRAFT/ngừng dùng/ngoài hiệu lực, báo lỗi dễ hiểu |
| `CatalogSelectionService.select` | Kiểm tra bộ ID và tính hạn theo quy tắc đã chốt | Gọi trong transaction nhập kho bao ngoài, xử lý ngoại lệ để không lưu dở hồ sơ |
| `CatalogSnapshot` | Nhận bản chụp loại/khoa/policy, số năm áp dụng và ngày dự kiến | Thiết kế và lưu snapshot bền vững trong bảng hồ sơ/bảng chi tiết policy |
| Audit danh mục | Truy nguyên thay đổi danh mục | Bổ sung audit nhập/sửa hồ sơ; không dùng audit danh mục thay audit hồ sơ |
| Bootstrap đã kiểm tra | Dựng lại môi trường phát triển để kiểm thử P3 | Thêm migration tiếp theo cho hồ sơ và vị trí, không sửa V1–V6 |

### 6.2. Ảnh hưởng tới thiết kế dữ liệu và giao diện nhập kho

Form P3 sẽ dùng ô chọn khoa/loại/policy thay cho các ô nhập tên tự do. Ngày đầu lưu kho phải có trước khi xác định policy đủ điều kiện. Một loại bệnh án không tự suy ra một nhóm thời hạn: P3 vẫn cần quy tắc xác định nhóm dựa trên thông tin hồ sơ hoặc lựa chọn đã kiểm tra của Archivist.

Do cho phép nhiều nhóm, dữ liệu hồ sơ phải giữ được danh sách các policy đã áp dụng và nhóm/giá trị quyết định thời hạn dài nhất. Chỉ lưu một con số `30` sẽ không đủ giải thích vì sao một hồ sơ được áp dụng 30 năm.

Ví dụ mô phỏng: hồ sơ có ngày đầu lưu, đủ điều kiện áp dụng nhóm thường 10 năm và nhóm tử vong 30 năm. Sau khi các phiên bản liên quan đã được Director xác nhận, bộ chọn trả thời hạn 30 năm và ngày dự kiến. P3 phải lưu cả hai nhóm và bản chụp căn cứ, không chỉ lưu ngày dự kiến.

Ngày hiệu lực mẫu là: hồ sơ có ngày đầu lưu trước ngày này sẽ không tự được chọn bốn phiên bản mẫu. Muốn nhập dữ liệu lịch sử, cần chuẩn bị phiên bản có hiệu lực phù hợp hoặc quy trình đối soát; không sửa ngày gốc để vượt kiểm tra.

### 6.3. Trình tự xử lý cần có trong P3

```text
Archivist gửi form nhập kho
 → kiểm tra quyền và dữ liệu hồ sơ
 → bắt đầu transaction nhập kho
 → kiểm tra trùng số lưu trữ và vị trí hợp lệ
 → gọi CatalogSelectionService.select với ngày đầu lưu và các ID
 → nhận CatalogSnapshot và ngày hạn dự kiến
 → lưu hồ sơ + snapshot + sự kiện nhập kho/lịch sử vị trí + audit
 → commit cùng nhau
```

Nếu một bước thất bại, rollback toàn bộ, hiển thị lỗi và giữ dữ liệu form để sửa. Ràng buộc unique/FK trong database vẫn cần thiết dù đã kiểm tra bằng Java.

- UC-04 phải gọi `CatalogSelectionService.select` **bên trong cùng transaction nhập hồ sơ**, lưu toàn bộ snapshot vào database hồ sơ và thêm FK khi có bảng hồ sơ. P2 chưa tạo bản ghi bệnh án nên chưa có kiểm thử lưu/đọc snapshot xuyên suốt với UC-04.
- Bộ chọn dùng khóa chia sẻ để danh mục không bị ngừng dùng giữa kiểm tra và lưu khi được gọi trong transaction bao ngoài. Không kiểm tra ở một giao dịch riêng rồi tin kết quả ở giao dịch khác.
- Người dùng còn kiểm tra bố cục/thao tác và nghiệm thu cuối; kết quả HTTP không thay thế xác nhận này.
- Quy tắc mô phỏng không xác nhận pháp luật hiện hành hoặc việc trung tâm đã phê duyệt chính sách. D-19/CR-004 ghi rõ giới hạn.

### 6.4. Các ca P3 phải bổ sung để hoàn tất tích hợp

| Ca cần làm ở P3 | Kết quả cần chứng minh |
|---|---|
| Nhập hồ sơ với danh mục và vị trí hợp lệ | Lưu đủ dữ liệu, FK, snapshot và lịch sử; đọc lại được |
| Chọn danh mục sai/ngừng dùng hoặc policy DRAFT | Không tạo hồ sơ chính thức |
| Mở form khi loại đang hoạt động, ngừng loại trước khi submit | Backend phát hiện tại thời điểm lưu, không chấp nhận chỉ vì form cũ còn lựa chọn |
| Hồ sơ nhiều nhóm chính sách | Giữ đầy đủ nhóm, áp dụng số năm dài nhất đúng quy tắc |
| Ngừng/đổi danh mục sau khi hồ sơ đã lưu | Hồ sơ cũ vẫn đọc được và giữ snapshot/thời hạn cũ |
| Lỗi khi lưu snapshot, audit hoặc lịch sử nhập kho | Không để lại hồ sơ thiếu dữ liệu liên quan |
| Hai người cùng nhập một số lưu trữ | Chỉ một bản ghi chính thức được lưu |
| Nhập hồ sơ lịch sử thiếu ngày gốc hoặc ngoài hiệu lực mẫu | Chặn công bố/đưa vào đối soát theo thiết kế; không tự bịa ngày |

### 6.5. Điều kiện thực tế trước khi triển khai P3

P2 đã cung cấp nền tảng danh mục, nhưng P3 vẫn cần chốt trường bắt buộc, quy tắc số lưu trữ/khóa duy nhất, vị trí kho tối thiểu, trạng thái hồ sơ và cách xử lý dữ liệu lịch sử. Cần có ít nhất một policy phù hợp đã được Director xác nhận để thử nhập hồ sơ thành công qua toàn bộ luồng.

Thứ tự đề xuất: chốt form/khóa P3 → chuẩn bị vị trí tối thiểu và policy dùng thử → thiết kế bảng hồ sơ/snapshot → migration mới → service/controller/form → kiểm thử các ca ở mục 6.4.

Các giới hạn này được theo dõi ở P3 và giai đoạn 13 của checklist, không bị đánh dấu như đã triển khai hồ sơ hoặc đã nghiệm thu toàn hệ thống.

