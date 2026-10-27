# Báo cáo xây dựng năm chức năng chính của MRM

Ngày thực hiện: ****, múi giờ Asia/Bangkok. Phạm vi: website Java quản lý metadata và vị trí hồ sơ bệnh án giấy cho dự án học tập.

Tài liệu này giải thích kết quả triển khai hiện tại, cách đọc code, cách sử dụng và cách kiểm chứng. Các đặc tả tổng thể trước đây vẫn phục vụ giai đoạn tiếp theo; những lựa chọn tối giản đang chạy được ghi tại [CR-005](../changes/CR-005_five-feature-implementation.md). Đây không phải biên bản nghiệm thu của bệnh viện.

## 1. Kết quả và phạm vi

| Nhóm | Use case | Chức năng đã xây dựng |
|---|---|---|
| P1 | MRM-UC-01 | Đăng nhập, đăng xuất, xác thực và điều hướng theo một vai trò/tài khoản |
| P2 | MRM-UC-03 | Năm khoa cố định; quản lý loại bệnh án; tạo, xác nhận, ngừng dùng phiên bản chính sách lưu |
| P3 | MRM-UC-04 | Nhập hồ sơ chính thức, lưu nháp để đối soát, sửa metadata, kiểm tra trùng và lưu snapshot |
| P4 | MRM-UC-05 | Tra cứu, lọc, phân trang, xem chi tiết theo quyền |
| P5 | MRM-UC-06 | Cây Kho–Kệ–Ngăn, sức chứa, chuyển vị trí và lịch sử |

Chuỗi sử dụng đã được kiểm thử qua HTTP: **đăng nhập → nhập hồ sơ → xem/tìm → sửa → chuyển vị trí → đối chiếu lịch sử → đăng xuất**. Kiểm thử cũng gọi trực tiếp Service/Repository trên PostgreSQL để kiểm tra transaction, snapshot và cạnh tranh dữ liệu.

Các chức năng yêu cầu đọc, phê duyệt yêu cầu đọc, lịch, giao–trả, xuất Excel/PDF, quản trị tài khoản đầy đủ, sao lưu/khôi phục và báo cáo toàn hệ thống chưa thuộc đợt này. Trạng thái READING đã có điều kiện chặn để chuẩn bị tích hợp giao–trả; chưa có màn hình đưa hồ sơ vào trạng thái này.

## 2. Kiểm kê dự án trước và sau

### 2.1. Nền tảng và thành phần đã có trước đợt này

- Maven Wrapper: `mvnw`, `mvnw.cmd`, `.mvn/wrapper`; `pom.xml` dùng Spring Boot 4.1.1, Java 26.
- Package gốc `mrm`; điểm khởi động `MedicalRecordApplication`.
- Spring MVC + Thymeleaf cho giao diện server render; Spring Security cho xác thực/phân quyền; JDBC/JdbcTemplate cho database; Bean Validation cho biểu mẫu; Flyway Java migration.
- Các module `authentication`, `config`, `database/migration`, `devtools`, `department`, `catalog`.
- Trang login đã có logo và màu sắc theo yêu cầu; danh mục và trang chủ theo role đã tồn tại. Phần nhận diện login được giữ lại.
- Tài liệu phân tích, danh mục mandatory/optional, thiết kế, checklist, báo cáo P2 và thư mục Note đã có.
- Bộ kiểm tra danh mục `CatalogIntegrationCheck`, `CompleteCatalogCheck` đã có; đây là chương trình Java main chạy tường minh, không phải test JUnit được Maven tự chạy.

Kiểm kê trực tiếp database trước triển khai: **mrm/public, PostgreSQL 18.6 tại 127.0.0.1:5433**; Flyway V1–V6 thành công; 4 vai trò, 4 tài khoản, 5 khoa, 6 loại bệnh án, 4 chính sách và 15 sự kiện danh mục. Chưa có các bảng hồ sơ/vị trí của V7. Không đọc hoặc công bố dữ liệu mật khẩu.

Kết quả server 16.5 trong biên bản ngày 18/10 là lịch sử của phiên kết nối khác; nó không thay kết quả kiểm tra kết nối hiện tại ở cổng 5433.

### 2.2. Thành phần bổ sung và cấu trúc hiện tại

```text
Code/
├── pom.xml, mvnw, mvnw.cmd, requirement.txt, PROJECT_CHECKLIST.md, README.md
├──.secrets/ # file riêng trên máy, đã thêm vào.gitignore
├── Note/ # giữ nguyên trong đợt này
├── scripts/
│ └── verify-features.ps1 # chạy các bộ kiểm tra và lưu log vào target
├── src/main/java/mrm/
│ ├── MedicalRecordApplication.java
│ ├── authentication/ # login controller, đọc tài khoản PostgreSQL
│ ├── config/ # Security, bootstrap development
│ ├── database/migration/ # V1 đến V7, SQL được viết trong Java
│ ├── devtools/ # kiểm tra kết nối, khởi tạo demo theo profile
│ ├── department/ # năm khoa cố định
│ ├── catalog/ # loại bệnh án, chính sách, snapshot
│ ├── common/ # model chung, lỗi nghiệp vụ
│ ├── record/ # form/filter/DTO/repository/service/controller hồ sơ
│ └── storage/ # cây vị trí, sức chứa, quản lý kho
├── src/main/resources/
│ ├── application.properties, application-dev.properties
│ ├── static/css/app.css # giao diện chung cho các màn nghiệp vụ
│ ├── static/images/ # logo và tài nguyên hình hiện có
│ └── templates/
│ ├── login.html, home.html
│ ├── fragments/layout.html # header, menu, logout, footer chung
│ ├── common/error.html
│ ├── department/, catalog/ # màn hình đã có
│ ├── records/ # list, form, detail, basic-detail
│ └── storage/list.html
├── src/test/java/mrm/
│ ├── catalog/ # hai bộ kiểm tra P2/hồi quy
│ └── features/ # FiveFeatureCheck, ProjectInventoryCheck
└── docs/ # đặc tả, thiết kế, quyết định và báo cáo này
```

`record` chứa nghiệp vụ hồ sơ, `storage` chứa nghiệp vụ vị trí; `database/migration` chỉ thay đổi cấu trúc database. Các thư mục này không thay thế nhau. Không chuyển test vào `src/main/java`, vì như vậy test có thể được đóng gói/chạy cùng ứng dụng.

### 2.3. Thay đổi database thật

Đã áp dụng **V7__CreateMedicalRecordStorage** trên `mrm`,. Không sửa nội dung các migration đã áp dụng V1–V6 và không đặt lại mật khẩu hoặc trạng thái chính sách của người dùng.

| Bảng mới | Vai trò |
|---|---|
| `storage_locations` | Kho, kệ, ngăn; quan hệ cha; trạng thái; sức chứa; version |
| `archive_counters` | Bộ đếm số lưu trữ theo tháng, cập nhật nguyên tử |
| `medical_records` | Metadata, trạng thái, vị trí hiện tại, snapshot tổng hợp, người và thời gian tạo/sửa |
| `medical_record_policies` | Bản chụp chi tiết từng chính sách áp dụng cho một hồ sơ |
| `record_events` | Nhật ký tạo, sửa, xem chi tiết, chuyển vị trí, đổi khả dụng; dữ liệu trước/sau |
| `location_events` | Nhật ký tạo/sửa vị trí |
| `record_location_history` | Lần nhập đầu và từng lần chuyển: nguồn/đích, đường dẫn lúc đó, lý do, người, thời gian |

V7 còn thêm FK/unique/check/index, hàm `mrm_search_key` phục vụ tìm tên tiếng Việt và trigger kiểm tra cây vị trí. Hàm tìm kiếm không yêu cầu cài extension PostgreSQL. Dữ liệu nghiệp vụ phải được thao tác qua ứng dụng để thực thi đầy đủ các quy tắc Service; các CHECK trong database không thay thế toàn bộ Service. Xem [ERD/schema hiện thực](../database/implemented-schema.md).

Không tự tạo vị trí thật hoặc hồ sơ bệnh nhân thật. Các bản ghi thử có tiền tố UUID và được dọn sau kiểm thử. Bộ đếm số lưu trữ có thể đã tăng do thử giao dịch thật rồi dọn bản ghi; khoảng trống số không phải hồ sơ bị mất.

## 3. Nguyên lý chung: trình duyệt, Java và PostgreSQL liên kết thế nào?

### 3.1. Luồng đọc một trang

1. Trình duyệt gửi `GET /archivist/records` cùng cookie phiên nếu đã đăng nhập.
2. Spring Security kiểm tra đăng nhập và role ARCHIVIST cho URL đó.
3. Spring MVC tìm method có `@GetMapping` tương ứng trong `RecordController`.
4. Controller nhận bộ lọc từ query string, gọi `RecordService.search`.
5. Service gọi `RecordRepository`; Repository dùng JDBC truyền SQL có tham số sang PostgreSQL.
6. PostgreSQL trả các dòng dữ liệu; Repository ánh xạ thành `RecordSummary` và `RecordPage`.
7. Controller đặt các đối tượng vào `Model`, trả tên view `records/list`.
8. Thymeleaf ghép prefix mặc định `classpath:/templates/` + tên view + suffix `.html`, đọc `templates/records/list.html`.
9. `th:each` lặp danh sách, `th:text` ghi giá trị có escape, `th:href` tạo liên kết. Trình duyệt nhận HTML đã render, không chạy Java.

Với Maven, `src/main/resources` được copy vào classpath khi build; trong JAR, template nằm dưới `BOOT-INF/classes/templates`. Vì vậy controller không cần trả đường dẫn vật lý trong ổ C:.

Ví dụ đọc code thật:

```java
model.addAttribute("result", service.search(filter, archivist(auth)));
return "records/list";
```

```html
<tr th:each="item: ${result.items}">
 <td th:text="${item.patientName}"></td>
</tr>
```

`${...}` đọc thuộc tính trong Model; không phải truy vấn trực tiếp database từ HTML.

### 3.2. Luồng gửi biểu mẫu

1. `th:object="${recordForm}"` chọn đối tượng dữ liệu; `th:field="*{patientName}"` tạo tên input và giá trị.
2. Người dùng bấm nút; form gửi POST tới `th:action`.
3. Spring chuyển các trường chuỗi/UUID/ngày/số thành `RecordForm`.
4. `@Valid` kiểm tra các annotation của form; `BindingResult` giữ lỗi để render lại trang và giữ nội dung vừa nhập.
5. Service kiểm tra nghiệp vụ và thực hiện transaction; Repository ghi SQL.
6. Thành công trả redirect đến trang chi tiết. Mẫu POST–redirect–GET giúp refresh trang chi tiết không lặp POST.
7. Ngoài redirect, mã thao tác và fingerprint xử lý việc gửi lại cùng POST; optimistic version phát hiện biểu mẫu sửa đã cũ.

Spring Security tự đưa CSRF token vào các form POST được render qua Thymeleaf/Spring. POST thiếu token bị 403. Các thao tác thay đổi dữ liệu dùng POST; GET tra cứu không nhập/sửa hồ sơ, nhưng xem chi tiết có ghi nhật ký VIEW.

### 3.3. Trách nhiệm từng lớp

| Thành phần | Việc đảm nhận | Ví dụ |
|---|---|---|
| Form/Filter | Dữ liệu gửi vào và kiểm tra hình thức | `RecordForm`, `RecordFilter`, `LocationForm` |
| Controller | URL, đọc form, Model, thông báo, redirect | `RecordController`, `StorageController` |
| Service | Quy tắc và biên transaction | `RecordService.create`, `.move` |
| Repository | SQL, khóa và ánh xạ dòng dữ liệu | `RecordRepository.write`, `StorageRepository.lockTree` |
| DTO | Chỉ mang các dữ liệu được phép trả về | `RecordSummary` cho Reader/Director |
| HTML | Bố cục và hiển thị Model | `records/basic-detail.html` |
| Migration | Thay đổi schema theo phiên bản | `V7__CreateMedicalRecordStorage` |

Không dùng JPA trong phiên bản này. Java thực hiện nghiệp vụ; PostgreSQL lưu dữ liệu và hỗ trợ ràng buộc/khóa; Spring Boot cấu hình và khởi động các thành phần; Maven tải dependency, biên dịch và đóng gói.

## 4. P1 — MRM-UC-01: đăng nhập, đăng xuất

### 4.1. Mục đích và người sử dụng

Xác định người dùng là ai, có một role nào, sau đó mở đúng trang và bảo vệ quyền ở backend. Bốn role đều dùng một form login chung, không có bốn form mật khẩu riêng.

| Role | Sau đăng nhập | Quyền trong nhóm năm chức năng |
|---|---|---|
| READER | `/reader/home` | Tra cứu và xem metadata hồ sơ chính thức |
| ARCHIVIST | `/archivist/home` | Danh mục thuộc trách nhiệm lưu trữ, nhập/sửa/đối soát, tra cứu, vị trí/chuyển, lịch sử nội bộ |
| DIRECTOR | `/director/home` | Tra cứu metadata cơ bản; xác nhận/ngừng dùng chính sách lưu |
| ADMIN | `/admin/home` | Trang kỹ thuật; không mặc nhiên xem bệnh án hoặc thực hiện nghiệp vụ Archivist |

### 4.2. Nguyên lý và vị trí code

- `config/SecurityConfig.java`: cấu hình URL, form login, password encoder, logout và CSRF.
- `authentication/DatabaseUserDetailsService.java`: truy vấn tài khoản và vai trò từ PostgreSQL, trả đối tượng UserDetails.
- Mật khẩu được băm PBKDF2; khi đăng nhập, encoder so mật khẩu nhập với hash, không giải mã hash thành mật khẩu gốc.
- `authentication/HomeController.java`: GET `/login` trả `login`; `/home` chuyển theo quyền tới trang chủ role.
- POST `/login` và POST `/logout` được bộ lọc Spring Security xử lý; không cần method `@PostMapping` cho hai URL này trong HomeController.
- Thành công chuyển `/home`; sai mật khẩu/tài khoản không hợp lệ chuyển `/login?error`; logout chuyển `/login?logout`.
- Cookie phiên giúp request sau vẫn biết người đã đăng nhập. Phần quản lý phiên nâng cao vẫn hoãn theo xác nhận của người dùng.

`spring.thymeleaf.servlet.produce-partial-output-while-processing=false` được giữ trong properties để tránh gửi một phần response trước khi Thymeleaf/Security cần xử lý form/phiên.

### 4.3. Cách sử dụng và kiểm thử thủ công

1. Mở `/login`, nhập tài khoản đã có: `reader1`, `archivist1`, `director1` hoặc `admin1`.
2. Mật khẩu là giá trị bạn đã cấp lúc seed; không có mật khẩu mặc định mới. Seed lại không đổi password tài khoản đã tồn tại.
3. Kiểm tra URL sau login đúng role.
4. Dùng Reader mở `/archivist/records/new`: phải bị 403, kể cả tự gõ URL.
5. Nhập sai password: ở lại login kèm lỗi, không mở trang bảo vệ.
6. Đăng xuất bằng nút trên header, sau đó mở lại URL bảo vệ: phải yêu cầu login.

Kiểm thử tự động đã bao gồm đủ bốn role, tài khoản bị khóa khi đăng nhập mới, sai mật khẩu, thiếu xác thực, URL trái quyền, logout và truy cập lại. Chưa thực hiện chính sách khóa sau N lần sai, forced password change hay reset password vì đã hoãn.

## 5. P2 — MRM-UC-03: danh mục khoa, loại bệnh án, chính sách lưu

### 5.1. Mục đích

Chuẩn hóa dữ liệu được chọn khi nhập hồ sơ: không để mỗi người gõ tên khoa/loại khác nhau hoặc tự nhập số năm bảo quản. Phân biệt rõ khoa điều trị, loại bệnh án và nhóm chính sách lưu; chúng không phải cùng một danh mục.

Năm khoa đã cố định theo CR-004; trigger chặn thêm/sửa/xóa trong database. Thông tin `created_by`, `created_at` giữ nguồn gốc tạo danh mục. Loại bệnh án vẫn được thêm/sửa tên/ngừng dùng. Chính sách đã tạo không sửa trực tiếp nội dung; khi thay đổi tạo phiên bản mới.

### 5.2. Nguyên lý

1. Archivist tạo chính sách với mã, tên, phiên bản, số năm, ngày hiệu lực và căn cứ.
2. Bản mới là DRAFT/inactive, chưa dùng nhập kho.
3. Director kiểm tra và xác nhận; hệ thống ghi người duyệt, audit và chuyển sang APPROVED/active.
4. Chặn hai phiên bản active cùng mã có thời gian hiệu lực chồng nhau; khóa theo mã chính sách xử lý phê duyệt đồng thời.
5. Khi nhập kho, `CatalogSelectionService.select` kiểm tra khoa/loại/chính sách tồn tại, trạng thái và ngày đầu lưu có nằm trong hiệu lực.
6. Nếu chọn nhiều nhóm, lấy số năm lớn nhất. Quy tắc mô phỏng là 10/15/20/30 năm, ngày gốc 29/2 chuyển về 28/2 năm đích kể cả năm đích nhuận.
7. `CatalogSnapshot` được chụp và lưu cùng hồ sơ; sau này ngừng chính sách không tự tính lại hạn hồ sơ cũ.

Các file cần đọc: `CatalogController` → `CatalogService` → `CatalogRepository`; `CatalogSelectionService`, `CatalogSnapshot`; template trong `templates/catalog`. Báo cáo lịch sử riêng P2: [P2_COMPLETION_REPORT.md](P2_COMPLETION_REPORT.md).

### 5.3. Cách sử dụng trên web

1. Login Archivist; mở **Loại bệnh án** (`/archivist/record-types`) để xem/thêm/sửa/ngừng loại.
2. Mở **Chính sách lưu** (`/archivist/retention-policies`), xem bốn bản mô phỏng đã có hoặc tạo bản dự thảo.
3. Logout rồi login Director; mở `/director/retention-policies`, kiểm tra và bấm xác nhận chính sách bạn muốn áp dụng.
4. Với ngày đầu lưu trước, các bản mẫu có hiệu lực từ không áp dụng. Hãy tạo phiên bản/nhóm mô phỏng với hiệu lực phù hợp sau khi đối chiếu dữ liệu, rồi Director xác nhận; không sửa ngày thật của hồ sơ để vượt validation.
5. Login lại Archivist để nhập hồ sơ; chỉ dùng chính sách đã xác nhận và có hiệu lực cho ngày đầu lưu.

Đợt triển khai này không tự thay người dùng xác nhận các chính sách thật trên giao diện. Chính sách tạm phục vụ test được tạo riêng và dọn riêng.

### 5.4. Kiểm thử và liên hệ P3

Đã thử validation, trùng mã/phiên bản, khoảng hiệu lực, quyền xác nhận, CSRF, version cũ, hai luồng sửa đồng thời, trigger khoa cố định, quy tắc ngày nhuận và tính bất biến snapshot. Đã boot thật với schema trống qua V1–V7 và đủ bốn tài khoản demo bằng password test riêng.

P3 sử dụng trực tiếp các ID/snapshot này. Không có chính sách hợp lệ thì P3 cho lưu nháp, từ chối nhập kho chính thức. P3 hiện đã hoàn thành phần lưu snapshot vào bảng hồ sơ và FK mà báo cáo P2 cũ để lại cho giai đoạn kế tiếp.

## 6. P3 — MRM-UC-04: nhập kho, sửa metadata, đối soát

### 6.1. Chức năng là gì?

Archivist tạo một bản ghi đại diện cho **một hồ sơ giấy của một đợt điều trị**. Bản ghi cho biết hồ sơ của ai, thuộc khoa/loại nào, thời gian điều trị, đang ở đâu, bảo quản theo chính sách nào và ai đã nhập/sửa. Không lưu toàn văn bệnh án hoặc chẩn đoán chi tiết trong lát cắt này.

Hồ sơ cũ thiếu dữ liệu được lưu nháp. Sau khi kiểm tra sổ/hồ sơ giấy, Archivist bổ sung và xác nhận nhập kho; không điền ngày giả để làm đủ form.

### 6.2. Các trường và ý nghĩa

| Nhóm/trường | Yêu cầu và cách hiểu |
|---|---|
| UUID | Khóa kỹ thuật nội bộ, tự sinh; không phải số bệnh án |
| Số lưu trữ | Duy nhất toàn ứng dụng; có thể nhập số nguồn hoặc để hệ thống cấp lúc nhập chính thức |
| Số bệnh án | Số nguồn của hồ sơ/đợt điều trị; bắt buộc nhập chính thức, không mặc định unique toàn đời người bệnh |
| Mã người bệnh | Tùy nguồn; một mã có nhiều đợt điều trị |
| Họ tên | Bắt buộc cả nháp; tối đa 200 ký tự; không là khóa gộp hồ sơ |
| Năm sinh/giới tính | Năm sinh bắt buộc khi nhập chính thức; giới tính Nam/Nữ/Chưa xác định |
| Khoa cuối, loại bệnh án | FK danh mục; kiểm tra trạng thái khi nhập/chọn phân loại mới |
| Ngày vào, ra, đầu lưu | Bắt buộc chính thức; dùng ngày thực tế; vào ≤ ra ≤ đầu lưu ≤ hôm nay |
| Tử vong | Cờ độc lập với loại/khoa; phải khớp có nhóm DECEASED |
| Chính sách | Một hoặc nhiều nhóm hợp lệ; lưu toàn bộ snapshot, lấy thời hạn dài nhất |
| Vị trí | Ngăn đang hoạt động, cha hoạt động, còn chỗ nếu có giới hạn |
| Chất lượng | NORMAL: bình thường; DAMAGED: hư hỏng; INCOMPLETE: thiếu/chưa hoàn thiện |
| Nguồn đối soát | Bắt buộc chính thức: sổ, đợt bàn giao hoặc căn cứ kiểm tra; chỉ Archivist nhận |
| Ghi chú nội bộ | Không gửi cho Reader/Director |
| Lý do sửa | Bắt buộc khi sửa/đối soát; lưu với trước/sau |
| created/updated by/at | Hệ thống tự ghi người và thời gian thao tác, khác ngày đầu lưu thực tế |

Số lưu trữ tự cấp dạng `-000001`: năm/tháng từ ngày đầu lưu; phần cuối là bộ đếm của tháng, không phải số dòng đang có trong bảng. Số nguồn được trim/chuyển hoa, cho ký tự A–Z, 0–9, `/`, `_`, `-`, dài 2–64. Không tự gộp hồ sơ trùng tên.

### 6.3. Tách ba loại trạng thái

| Loại | Giá trị | Ý nghĩa |
|---|---|---|
| Bản ghi | DRAFT / STORED | Chưa công bố / đã xác nhận nhập kho |
| Lưu chuyển | NOT_STORED / IN_STORAGE / UNAVAILABLE / READING | Chưa nhập / trong kho / tạm không khả dụng / đang giao đọc |
| Chất lượng | NORMAL / DAMAGED / INCOMPLETE | Mức hoàn thiện hồ sơ giấy, không phải vị trí |

Nháp không có current location hoặc hạn lưu chính thức. Nhập mới INCOMPLETE bị chặn, cần lưu nháp. DAMAGED có thể nhập với trạng thái UNAVAILABLE. Hồ sơ đã lưu được phát hiện thiếu/hư hỏng sẽ chuyển UNAVAILABLE khi sửa chất lượng; sau khi sửa lại NORMAL cần xác nhận khả dụng riêng kèm lý do.

### 6.4. Nguyên lý nhập kho từng bước

1. GET `/archivist/records/new` tạo `RecordForm.empty` với mã phiếu UUID mới và version 0; tải danh mục/vị trí để render form.
2. POST `/archivist/records` nhận form cùng `mode=DRAFT` hoặc `STORE`.
3. Validation kiểm tra hình thức; `RecordService.create` kiểm tra ngày và điều kiện bắt buộc tùy mode.
4. Khóa mã phiếu, kiểm tra fingerprint SHA-256 của nội dung và người tạo. Phiếu đã dùng với cùng nội dung trả ID cũ; nội dung khác bị từ chối.
5. Với STORE, khóa nghiệp vụ kho trong transaction, kiểm tra vị trí/sức chứa và nghi trùng đợt điều trị sau khi đã lấy khóa. Điều này ngăn hai form đồng thời cùng vượt bước kiểm tra.
6. Bộ chọn danh mục xác minh dữ liệu nguồn, tạo snapshot và hạn lưu. Kiểm tra cờ tử vong khớp nhóm bảo quản.
7. Nếu chưa có số lưu trữ, bộ đếm PostgreSQL cấp số nguyên tử; UNIQUE là lớp chặn trùng cuối.
8. Repository ghi `medical_records`, `medical_record_policies`, lịch sử nhập vị trí và audit trong cùng transaction.
9. Thành công commit rồi redirect đến chi tiết; lỗi rollback, không để hồ sơ đã nhập nhưng mất snapshot/lịch sử.

`@Transactional` được Spring áp dụng qua Service bean. Controller không tự mở nhiều transaction rời cho từng bảng. Không gọi trực tiếp `new RecordService(...)` trong ứng dụng để bỏ qua proxy transaction.

### 6.5. Sửa và đối soát hoạt động thế nào?

1. Mở chi tiết → **Sửa / đối soát**, hệ thống tải form với version hiện tại.
2. POST sửa khóa dòng hồ sơ; so version. Nếu người khác đã thay đổi, trả lỗi và yêu cầu tải lại.
3. Hồ sơ STORED không đổi ngược thành DRAFT, không đổi số lưu trữ hoặc ngày đầu lưu; đổi vị trí phải qua chức năng chuyển.
4. Chưa thay khoa/loại/nhóm bảo quản thì giữ snapshot lịch sử, kể cả danh mục đã ngừng dùng. Đổi phân loại thì kiểm tra lại danh mục/chính sách hợp lệ.
5. Ghi before/after dạng JSONB, lý do, người, thời gian; tăng version.
6. Nháp được bổ sung đủ và chọn STORE sẽ tạo lần nhập vị trí đầu, snapshot và trạng thái chính thức.

Nghi trùng được xét bằng số bệnh án + mã người bệnh + ngày vào/ra. Đây là hàng rào đối soát, không phải hệ thống định danh bệnh nhân; sai khác dữ liệu nguồn vẫn cần kiểm tra thủ công.

### 6.6. Hướng dẫn nhập một hồ sơ mẫu

1. Chuẩn bị chính sách đã xác nhận và một ngăn hoạt động (xem P2/P5).
2. Login `archivist1` → **Nhập hồ sơ**.
3. Để trống số lưu trữ; nhập số bệnh án `DEMO-001`, mã người bệnh `DEMO-P001`, họ tên giả `Nguyễn Văn An`, năm sinh ``, giới tính Nam.
4. Chọn khoa Nội tổng hợp và loại bệnh án Nội; chọn ngày vào ``, ra ``, đầu lưu `` cho buổi thử. Khi thử ngày khác, chọn ngày thực tế không vượt hôm nay.
5. Chọn ngăn và chính sách GENERAL đã được xác nhận, có hiệu lực vào ngày đầu lưu. Không tích tử vong cho ví dụ này.
6. Chất lượng Bình thường; nguồn: `Dữ liệu giả phục vụ kiểm thử — sổ bàn giao mẫu`.
7. Bấm xác nhận nhập kho; phải tới trang chi tiết, có số lưu trữ, hạn lưu, snapshot và một dòng lịch sử nhập đầu.
8. Mở Sửa, đổi thông tin cần đối soát, nhập lý do; lưu và xem lịch sử trước/sau.
9. Thử hồ sơ khác chỉ có tên, chọn Lưu nháp; bổ sung sau bằng Sửa rồi xác nhận nhập kho. Reader không được nhìn thấy nháp.

## 7. P4 — MRM-UC-05: tra cứu và xem chi tiết

### 7.1. Mục đích và trường được xem

Tra đúng hồ sơ giữa nhiều người trùng tên hoặc một người nhiều lần điều trị. Danh sách hiển thị số lưu trữ/số bệnh án, họ tên/năm sinh/giới tính/mã người bệnh, khoa/loại, đợt điều trị và trạng thái.

Archivist xem đủ thông tin vận hành, snapshot, vị trí, ghi chú, nguồn và lịch sử. Reader/Director dùng `RecordSummary` và template chi tiết cơ bản riêng: query chỉ chọn metadata được phép, không tải full detail rồi chỉ che bằng CSS. Reader/Director chỉ nhận hồ sơ STORED; đoán UUID nháp nhận 404. Admin bị chặn truy cập hồ sơ.

### 7.2. Cơ chế tìm kiếm

1. GET tới `/archivist/records`, `/reader/records` hoặc `/director/records`.
2. `RecordFilter` nhận ô tìm, khoa, loại, năm sinh, ngày ra viện từ/đến, trạng thái, trang, số dòng, cách sắp xếp.
3. Ba mã được so chính xác không phân biệt hoa thường; tên so chứa chuỗi sau chuẩn hóa Unicode NFD, bỏ dấu kết hợp và đổi đ thành d.
4. Ví dụ `nguyen dang thi anh` tìm được `Nguyễn Đặng Thị Ánh`; tên vẫn giữ nguyên dấu trong database/HTML.
5. Các bộ lọc được kết hợp AND; điều kiện mã/tên bên trong ô tìm được kết hợp OR.
6. SQL dùng tham số; ký tự `%`, `_`, `!` trong ô tên được escape thành ký tự thật. Cách sắp xếp chỉ nhận name/archive/discharge/newest, không ghép tùy ý nội dung từ request vào ORDER BY.
7. Query COUNT tính tổng; query LIMIT/OFFSET lấy trang. Cột UUID làm khóa phụ để thứ tự ổn định khi tên/ngày trùng nhau.
8. Giới hạn 1–100 dòng; giao diện cung cấp 10/20/50/100. Trang tính từ 0 trong URL, hiển thị từ 1 cho người dùng.

Tên dạng `%chuỗi%` phù hợp dữ liệu bài tập; chưa có trigram/full-text index và chưa kết luận đạt chỉ tiêu hiệu năng lớn. Các index hiện có hỗ trợ mã/quan hệ và một số bộ lọc.

### 7.3. Cách sử dụng và tự đối chiếu

1. Login theo role, mở **Tra cứu hồ sơ**.
2. Nhập số lưu trữ vừa được cấp để tìm đúng một hồ sơ; thử mã người bệnh để xem nhiều đợt.
3. Thử tên có dấu rồi không dấu, kết hợp năm sinh và khoa để tách người trùng tên.
4. Lọc khoảng ngày ra viện; từ > đến phải nhận thông báo lỗi, không chạy truy vấn sai.
5. Đổi số dòng/sắp xếp, chuyển trang; liên kết phân trang giữ các bộ lọc.
6. Mở chi tiết bằng tên/số lưu trữ. Với Reader/Director kiểm tra không có vị trí ngăn, ghi chú hoặc lịch sử nội bộ.
7. Archivist lọc DRAFT để tìm dữ liệu cần đối soát. Reader/Director tự thêm `state=DRAFT` trong URL vẫn không được công bố nháp.

## 8. P5 — MRM-UC-06: vị trí, sức chứa, chuyển hồ sơ

### 8.1. Mục đích

Biết hồ sơ giấy đang ở ngăn nào và truy lại đã chuyển qua đâu, thay vì chỉ ghi một chuỗi vị trí có thể bị ghi đè. Chỉ Archivist được quản lý vị trí và chuyển.

### 8.2. Cây vị trí và nguyên lý quản lý

- WAREHOUSE: kho, không có cha.
- SHELF: kệ, cha phải là kho.
- COMPARTMENT: ngăn, cha phải là kệ; chỉ ngăn được gán vào hồ sơ.
- Mã duy nhất trong cùng cha; mã kho gốc duy nhất toàn cây. Cùng mã ngăn ở hai kệ khác nhau vẫn phân biệt bằng đường dẫn.
- Sau tạo, không đổi mã/loại/cha; chỉ sửa tên, trạng thái và sức chứa ngăn. Cấu trúc bất biến tránh chu trình và giữ định danh ổn định.
- Sức chứa trống = không giới hạn; nếu nhập phải dương. Không giảm thấp hơn số hồ sơ đang gắn ở ngăn, kể cả UNAVAILABLE.
- Không ngừng vị trí đang có hồ sơ hoặc còn con hoạt động. Không xóa cứng qua web.
- Đường dẫn hiện tại tính từ cây bằng truy vấn đệ quy. Lịch sử chuyển lưu cả chuỗi đường dẫn lúc thao tác, nên đổi tên vị trí không viết lại lịch sử.

`StorageController` nhận form; `StorageService` kiểm tra cấp/cha/trạng thái/sức chứa/version; `StorageRepository` truy vấn cây, lấy khóa transaction và ghi dữ liệu/audit. Trigger database kiểm tra quan hệ cấp và cấm thay mã/loại/cha.

### 8.3. Cách tạo kho trước khi nhập hồ sơ

1. Login Archivist → **Vị trí lưu trữ** (`/archivist/locations`).
2. Thêm mã `KHO_A`, tên `Kho A`, loại Kho, cha để trống, sức chứa để trống.
3. Thêm mã `KE_01`, tên `Kệ 01`, loại Kệ, cha Kho A.
4. Thêm mã `NGAN_01`, tên `Ngăn 01`, loại Ngăn, cha Kệ 01; ví dụ sức chứa 50.
5. Tạo thêm `NGAN_02` nếu muốn thử chuyển. Không giả định một ngăn chỉ chứa một hồ sơ.
6. Nếu đã có mã trên, dùng mã thử khác; không tạo trùng để làm bước chuẩn bị.

### 8.4. Chuyển hồ sơ: từ giao diện đến transaction

1. Mở chi tiết hồ sơ STORED đang IN_STORAGE.
2. Chọn ngăn đích khác ngăn hiện tại, nhập lý do, bấm chuyển.
3. Service khóa dòng hồ sơ, kiểm tra mã thao tác đã xử lý chưa; cùng thao tác đã thành công thì trả về mà không ghi lần nữa.
4. So version để từ chối form cũ; kiểm tra trạng thái cho phép.
5. Lấy khóa kho, kiểm tra ngăn đích/cha đang hoạt động và sức chứa còn chỗ.
6. Cập nhật current location, tăng version, thêm lịch sử nguồn/đích/đường dẫn và audit cùng transaction.
7. Thành công redirect về chi tiết; lỗi hiển thị thông báo, vị trí cũ còn nguyên.

Khóa chung kho tuần tự hóa nhập/chuyển và chỉnh sức chứa để không vượt chỗ khi hai người thao tác đồng thời. Đây là lựa chọn dễ kiểm chứng cho bài tập; nếu tải lớn cần thiết kế lại khóa theo từng vị trí với thứ tự khóa thống nhất.

### 8.5. Các trường hợp từ chối

Không chuyển nháp, UNAVAILABLE hoặc READING; không chuyển vào kho/kệ, ngăn ngừng dùng, ngăn đầy hoặc ngăn hiện tại; không chấp nhận lý do trống hay version cũ. READING được mô phỏng trong test ở tầng dữ liệu để chứng minh điều kiện chặn; chưa phải kiểm thử một quy trình giao–trả hoàn chỉnh.

## 9. Ma trận kiểm thử chi tiết

### 9.1. Cách tổ chức và bằng chứng

Môi trường kiểm tra: JDK 26.0.2.1, Maven Wrapper 3.9.16, Spring Boot 4.1.1, PostgreSQL 18.6, database mrm cổng 5433. Phiên ứng dụng kiểm tra dùng cổng ngẫu nhiên để tránh chiếm cổng bạn đang dùng.

Các test Java main chạy tường minh, có assertion và trả exit code lỗi nếu không đạt. **`mvnw package` thành công chỉ chứng minh build, không tự thực thi các bộ kiểm tra tích hợp này.**

| Bộ kiểm tra | Phạm vi | Kết quả |
|---|---|---|
| `CatalogIntegrationCheck` | Danh mục, HTTP, bốn role, CSRF, biểu mẫu và audit/rollback | 49 kiểm tra đạt |
| `CompleteCatalogCheck` | Quy tắc P2, cạnh tranh sửa, khởi động thật schema trống V1–V7 và dọn schema | 26 kiểm tra đạt |
| `FiveFeatureCheck` | Nhập/sửa/nháp, tìm kiếm, quyền dữ liệu, vị trí, cạnh tranh và chuỗi HTTP | 96 kiểm tra đạt |

Lượt cuối chạy script lúc **19:56–19:57 **: build/test-compile đạt; cả ba bộ đạt **49 + 26 + 96 = 171 assertion**, exit code 0. Bản JAR được đóng gói thành công, khởi động lại; V7 được validate thành công và không chạy lặp. Một “kiểm tra” là một assertion, không đồng nghĩa một use case hoặc một test JUnit độc lập.

Script [verify-features.ps1](../../scripts/verify-features.ps1) lưu output từng bộ vào `target/verification/`. Fixture có tiền tố riêng, rollback hoặc cleanup trong finally. Không sử dụng bệnh nhân thật, không ghi password thật vào báo cáo. Kiểm kê trước/sau xác nhận dữ liệu mẫu của người dùng không bị thay bằng dữ liệu test.

Bản bằng chứng lưu cùng tài liệu: [49 kiểm tra danh mục](evidence/five-features-2026-10-20/catalog.txt), [26 kiểm tra P2/bootstrap](evidence/five-features-2026-10-20/bootstrap-and-policy.txt), [96 kiểm tra nhóm năm](evidence/five-features-2026-10-20/five-features.txt), [kiểm kê sau kiểm thử](evidence/five-features-2026-10-20/inventory-after.txt). Sau dọn: vẫn 4 tài khoản, 5 khoa, 6 loại, 4 chính sách, 15 sự kiện danh mục; các bảng hồ sơ/vị trí/snapshot/lịch sử thử có 0 dòng. Đây là database phát triển chưa nhập hồ sơ thật, không có nghĩa ứng dụng tự xóa hồ sơ khi đóng.

### 9.2. Các tình huống và kết quả quan sát

| Mã | Thực hiện | Kết quả mong đợi/đã kiểm chứng |
|---|---|---|
| AUTH-01 | Login bốn role, GET /home | Đúng trang chủ từng role; trang render 200 |
| AUTH-02 | Sai password; tài khoản LOCKED login mới | Redirect login?error, không vào nghiệp vụ |
| AUTH-03 | Chưa login mở danh sách; Reader mở form Archivist | Redirect login; 403 trái role |
| AUTH-04 | POST logout rồi mở URL bảo vệ | Phiên không còn truy cập nghiệp vụ |
| CAT-01 | Tạo/sửa/ngừng loại; gửi version cũ | Lưu/audit đúng; từ chối ghi đè |
| CAT-02 | Archivist tạo policy; Director xác nhận/ngừng | DRAFT không dùng nhập; đúng quyền mới duyệt được |
| CAT-03 | Mã/phiên bản trùng hoặc hiệu lực chồng | Từ chối; không để dữ liệu phê duyệt mâu thuẫn |
| CAT-04 | Nhiều nhóm; ngày 29/2; năm đích nhuận | Lấy số năm dài nhất; ngày đích 28/2 theo quy tắc đã xác nhận |
| CAT-05 | Thay/ngừng danh mục sau snapshot | Snapshot cũ bất biến |
| CAT-06 | Thêm/sửa/xóa khoa cố định, POST thêm khoa | Database từ chối; web không cung cấp thêm khoa |
| BOOT-01 | Boot schema trống bằng dev,seed-users | V1–V7 đạt, 4 tài khoản, 5 khoa, 6 loại, 4 chính sách DRAFT |
| REC-01 | Nhập đủ trường | STORED/IN_STORAGE; có số, snapshot, audit, lịch sử nhập đầu |
| REC-02 | Gửi lại cùng phiếu/nội dung | Trả cùng ID, không nhân đôi hồ sơ/audit |
| REC-03 | Dùng lại phiếu với nội dung khác | Từ chối |
| REC-04 | Nháp chỉ có tên, thiếu ngày/năm/vị trí | Lưu NULL; không bịa ngày/hạn; không công bố |
| REC-05 | Bổ sung nháp rồi STORE | Chuyển chính thức, tạo snapshot/lịch sử đầu |
| REC-06 | STORED đổi ngược DRAFT | Từ chối |
| REC-07 | Số lưu trữ trùng | UNIQUE từ chối; không có bản ghi thứ hai |
| REC-08 | Cùng người bệnh, hai đợt khác | Tạo hai hồ sơ độc lập, truy vấn thấy đủ |
| REC-09 | Cùng số bệnh án/mã/ngày vào/ra | Nghi trùng, chặn STORE |
| REC-10 | Ngày đảo hoặc tương lai; policy ngừng dùng | Từ chối ở Service/bộ chọn danh mục |
| REC-11 | Sửa metadata sau khi type/policy ngừng | Giữ snapshot lịch sử; audit trước/sau và tăng version |
| REC-12 | Hai form sửa cùng version | Form cũ không ghi đè; web render lại lỗi |
| REC-13 | Nguồn đối soát dài đúng 1.000 ký tự | Lưu đầy đủ nguồn và audit trong cùng transaction, không vượt giới hạn cột lý do |
| FIND-01 | Tên có/không dấu; tên trùng, nhiều đợt | Kết quả chuẩn hóa khớp, không tự gộp |
| FIND-02 | Ba hồ sơ, 2 dòng/trang | Trang đầu 2, trang sau 1, không trùng ID |
| FIND-03 | SQL injection và ký tự wildcard | Không mở rộng truy vấn trái ý, wildcard là ký tự thật |
| FIND-04 | Reader/Director mở detail và đoán ID nháp | Detail chỉ metadata; nháp 404 |
| FIND-05 | Tìm kết hợp khoa/loại/trạng thái/ngày/năm sinh | Điều kiện AND cho kết quả tương ứng; năm khác không khớp |
| FIND-06 | Khoảng ngày đảo | HTTP 400 có thông báo lỗi |
| LOC-01 | Sai cấp cha/con; ngừng kho còn con active | Từ chối |
| LOC-02 | Sửa tên/ngừng vị trí trống; dùng version cũ | Cập nhật hợp lệ; từ chối version cũ |
| LOC-03 | Chuyển hợp lệ rồi gửi lại cùng mã thao tác | Một lần cập nhật, một dòng chuyển mới |
| LOC-04 | Chuyển vào ngăn đầy; ngừng ngăn còn hồ sơ | Từ chối, vị trí hồ sơ cũ giữ nguyên |
| LOC-05 | UNAVAILABLE/READING chuyển, READING sửa | Từ chối |
| LOC-06 | POST tạo/sửa vị trí, mã trùng | Redirect thành công hợp lệ; trùng render lại lỗi |
| RACE-01 | Hai luồng cùng số lưu trữ | Một thành công, một xung đột |
| RACE-02 | Hai luồng chuyển một hồ sơ cùng version | Một thành công; lịch sử chỉ thêm một dòng chuyển |
| RACE-03 | Hai hồ sơ tranh ngăn capacity=1 | Chỉ một hồ sơ vào ngăn |
| RACE-04 | Hai phiếu nhập cùng một đợt điều trị | Một nhập chính thức, một bị chặn nghi trùng |
| WEB-01 | POST nhập/sửa/chuyển; Reader POST trái quyền | Luồng hợp lệ đạt; trái quyền 403 |
| WEB-02 | POST thiếu CSRF | 403, không nhập hồ sơ |
| DATA-01 | Reader/Director response có marker nội bộ? | Không chứa ghi chú, đường dẫn kho, căn cứ policy hoặc UUID vị trí |
| CLEAN-01 | Kết thúc bộ kiểm tra | Fixture/schema thử được dọn; không đổi dữ liệu danh mục gốc |

Một số hàng gom nhiều assertion; không cộng số hàng để suy ra tổng kiểm tra. Bảng mô tả hành vi thực tế đã được kiểm tra; không khẳng định mọi tổ hợp nhập sai đều đã thử.

### 9.3. Kiểm thử người dùng cần thực hiện trên web

1. Dùng tài khoản/password hiện có, thử chuỗi P2 xác nhận policy → P5 tạo vị trí → P3 nhập hồ sơ giả → P4 tìm lại → P5 chuyển.
2. Đăng nhập Reader và Director để đối chiếu phạm vi màn hình.
3. Mở hai tab sửa cùng hồ sơ; lưu tab thứ nhất rồi tab thứ hai, kiểm tra thông báo dữ liệu đã thay đổi.
4. Thử thiếu trường/đảo ngày/trùng số, kiểm tra nội dung lỗi dễ hiểu và dữ liệu hợp lệ đã nhập vẫn được giữ trên form.
5. Kiểm tra bố cục trên màn hình máy bạn, zoom 100% và màn hình hẹp; phản hồi các chỗ chữ/nút chưa thuận tiện.

Kiểm thử HTTP tự động không thay thế nghiệm thu trực quan và xác nhận nghiệp vụ của bạn. Chưa đo tải p95, chưa kiểm thử khôi phục backup, chưa diễn tập nhiều người dùng thật hoặc triển khai production.

## 10. Build, chạy và kiểm thử lại

### 10.1. Build

Trong thư mục Code, dừng phiên JAR đang chạy trước khi đóng gói lại trên Windows:

```powershell
.\mvnw.cmd -B -ntp package
```

Có thể thêm `-o` khi các dependency đã tải đủ. Kiểm tra `JAVA_HOME` trỏ JDK 26; nếu `java` từ PATH chạy nhầm bản cũ, dùng `& "$env:JAVA_HOME\bin\java.exe"`.

### 10.2. Nạp password trong terminal và chạy

Nếu đang dùng file riêng đã có:

```powershell
$passwordFile = Join-Path (Get-Location) '.secrets/db-password.txt'
$env:DB_PASSWORD = [System.IO.File]::ReadAllText($passwordFile).TrimEnd([char[]]"`r`n")
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
```

Hoặc nạp bằng `Read-Host -AsSecureString` theo ghi chú hiện có. Không nhập password vào README, properties đã theo dõi Git hoặc output công khai. `.secrets/` đã được thêm vào `.gitignore`; ignore không tự xóa một file từng được commit trước đó.

Chạy JAR thay Maven:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar target/medical-record-management-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Mở `http://localhost:8080/login`. Dừng Ctrl+C. Không cần profile seed-users mỗi lần chạy khi tài khoản đã có. Cấu hình default dev là database mrm/cổng 5433; `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` có thể ghi đè. Kiểm tra đúng database trước migration.

Database/schema mới chưa có tài khoản: cung cấp đủ bốn biến `SEED_READER_PASSWORD`, `SEED_ARCHIVIST_PASSWORD`, `SEED_DIRECTOR_PASSWORD`, `SEED_ADMIN_PASSWORD` rồi chạy `dev,seed-users`; bootstrap V1–V3 tạo Archivist phục vụ V4, sau đó V4–V7 và các tài khoản còn lại. Không có password mặc định trong source.

### 10.3. Chạy bộ kiểm chứng

Giữ ứng dụng đã build mới nhất chạy, mở terminal khác tại Code:

```powershell
.\scripts\verify-features.ps1 -BaseUrl http://localhost:8080
```

Script test-compile, lấy dependency từ JAR đã build, chạy inventory và ba bộ kiểm tra tuần tự, ghi log vào `target/verification`. Không tự chạy ứng dụng; URL và các biến DB của terminal kiểm tra phải trỏ cùng database với ứng dụng. Script cần schema test tạm và fixture, vì vậy dùng database phát triển của dự án học tập.

Nếu PowerShell chặn script bởi execution policy, có thể đọc và chạy các lệnh Java trong script tại terminal theo chính sách máy; không cần thay execution policy toàn hệ thống. Test helper mặc định đọc password từ biến môi trường hoặc `.secrets/db-password.txt`.

## 11. Giải thích các kỹ thuật quan trọng để tự học/tiếp tục phát triển

1. **Constructor injection:** Spring tạo Controller/Service/Repository rồi truyền dependency qua constructor. Giúp lớp chỉ lo trách nhiệm của mình và test có thể tạo context tương ứng.
2. **Record/DTO:** Java record biểu diễn dữ liệu có cấu trúc. DTO cơ bản giúp giới hạn dữ liệu được phép trả, khác với đối tượng đầy đủ dùng để nhập/sửa.
3. **Validation hai tầng:** annotation kiểm tra chiều dài/kiểu; Service kiểm tra ngày, trạng thái và quyền nghiệp vụ. Database thêm FK/unique/check để giữ cấu trúc ngay cả khi ứng dụng có lỗi.
4. **Transaction:** một đơn vị công việc nhiều bảng cùng thành công hoặc cùng rollback. Ví dụ chuyển current location và thêm lịch sử không thể tách thành hai lần lưu độc lập.
5. **Optimistic version:** form mang version lúc mở; nếu version hiện tại khác thì không ghi đè. Người dùng tải lại để đọc thay đổi mới trước khi quyết định.
6. **Pessimistic/advisory lock:** `FOR UPDATE` giữ độc quyền dòng đang sửa; advisory transaction lock phối hợp thao tác cây kho/sức chứa/bộ chọn. Khóa tự giải phóng cuối transaction.
7. **Idempotency:** mã thao tác cho phép nhận biết một lần gửi bị lặp do mạng hoặc bấm lại; không dựa riêng vào việc ẩn nút trên HTML.
8. **Snapshot:** lưu thông tin chính sách tại thời điểm nhập bên cạnh FK. FK cho biết liên hệ; snapshot cho biết căn cứ lịch sử chính xác lúc đó.
9. **Parameterized SQL:** dữ liệu input được bind làm giá trị, không nối vào SQL. ORDER BY dùng whitelist vì tên cột không được bind như giá trị thông thường.
10. **PRG và flash attributes:** POST thành công redirect; thông báo ngắn truyền tới GET kế tiếp. Lỗi validation render lại form để sửa.
11. **Server render:** Thymeleaf nhận Model từ Java và tạo HTML; không cần frontend framework riêng để làm năm chức năng này.
12. **Migration phiên bản:** Flyway chỉ chạy bản chưa áp dụng. Thay đổi schema tiếp theo phải thêm V8 trở lên; không sửa V7 đã chạy.

## 12. Tác động đến giai đoạn sau và giới hạn còn lại

- UC-07 có thể chọn hồ sơ STORED và dùng trạng thái khả dụng làm điều kiện; quyền tạo yêu cầu vẫn theo role đã chốt.
- UC-12/13 cần thêm bảng giao–trả, kiểm tra lượt mở và tích hợp READING; hiện chỉ có điều kiện chặn trạng thái, chưa đủ chứng minh vòng đời giao–trả.
- UC-16 có thể dùng các bảng audit hiện tại, nhưng còn cần màn hình truy vấn theo quyền, chính sách bảo quản log và quyền database phù hợp. Log hiện tại không được quảng bá là hệ thống chống sửa bởi quản trị database.
- UC-18 có hạn lưu snapshot nhưng chưa có danh sách đến hạn/tác vụ cảnh báo; không tự tiêu hủy hồ sơ.
- Quản trị user/password nâng cao, thu hồi phiên đang mở sau khóa tài khoản, giới hạn đăng nhập sai, HTTPS/secret deployment và backup cần giai đoạn riêng nếu dùng ngoài bài tập.
- Hiện thực này chưa lưu PDF/bản quét; việc quản lý ảnh/logo trong static khác hoàn toàn quản lý nội dung bệnh án.
- Đợt này không sửa thư mục Note và không tạo Git commit. Người dùng đọc báo cáo, thử trên web và yêu cầu chỉnh nghiệp vụ trước khi coi là nghiệm thu cuối.

## Phụ lục A — Đọc code theo từng tính năng nhỏ

Các đường dẫn dưới đây tính từ thư mục Code, có thể mở trong IDE theo thứ tự từ trái sang phải.

| Muốn hiểu/chỉnh gì | File/method chính | Phần hiển thị hoặc dữ liệu liên quan |
|---|---|---|
| URL nào cần role nào | `src/main/java/mrm/config/SecurityConfig.java` | `requestMatchers`, login/logout, password encoder |
| Tài khoản lấy từ đâu | `authentication/DatabaseUserDetailsService.java` | `app_users JOIN roles` |
| Vì sao vào đúng trang chủ | `authentication/HomeController.java` | `/home` → URL theo role → `templates/home.html` |
| Menu và logout ở đâu | `common/WebModelAdvice.java` và `templates/fragments/layout.html` | currentRole/currentUsername/recordBase, form POST logout |
| Form nhập có những trường nào | `record/RecordForm.java` | `templates/records/form.html`, `th:field` |
| Nhập chính thức khác nháp ở đâu | `record/RecordService.java`: create/validate/update | mode STORE/DRAFT, kiểm tra đủ dữ liệu, trạng thái |
| Kiểm tra trùng và cấp số | `record/RecordRepository.java`: suspectedDuplicate/nextArchive/previousCreate | UNIQUE archive_number, archive_counters, create_token/fingerprint |
| Chọn chính sách và tính hạn | `catalog/CatalogSelectionService.java`: select | `CatalogSnapshot`, chính sách active/APPROVED/hiệu lực |
| Ghi hồ sơ và snapshot | `record/RecordRepository.java`: write | medical_records, medical_record_policies |
| Vì sao lỗi thì không lưu dở | `record/RecordService.java`: @Transactional | Repository dùng cùng datasource/transaction |
| Bấm sửa đi đâu | `record/RecordController.java`: edit/update | GET/POST `/archivist/records/{id}/edit` |
| Tìm không dấu và phân trang | `record/RecordFilter.java`, `RecordRepository.search` | mrm_search_key, query COUNT/LIMIT/OFFSET, `records/list.html` |
| Phạm vi Reader/Director | `RecordRepository.summary`, `RecordSummary.java` | `records/basic-detail.html`; SELECT cơ bản riêng |
| Vị trí/kho hoạt động thế nào | `storage/StorageService.java`, `StorageRepository.java` | recursive tree, `storage/list.html` |
| Chuyển và chống gửi lặp | `record/RecordService.java`: move | row lock/version/operation_key, record_location_history |
| Xem trước/sau và lịch sử | `RecordRepository.event/history/locationHistory` | JSONB, `records/detail.html` |
| Database có bảng/cột nào | `database/migration/V7__CreateMedicalRecordStorage.java` | [Schema hiện thực](../database/implemented-schema.md) |
| Tự chạy kiểm tra ở đâu | `src/test/java/mrm/features/FiveFeatureCheck.java` | [Script PowerShell](../../scripts/verify-features.ps1) |

Các tên file rút gọn ở cột giữa sau dòng đầu đều nằm dưới `src/main/java/mrm/`, trừ HTML và file test đã ghi rõ.

## Phụ lục B — URL và trách nhiệm xử lý

| Method/URL | Bộ phận xử lý | Quyền/mục đích |
|---|---|---|
| GET `/login` | HomeController | Form công khai |
| POST `/login` | Spring Security filter | Xác thực; không nằm trong controller POST |
| POST `/logout` | Spring Security filter | Kết thúc phiên, cần CSRF |
| GET `/home` | HomeController | Điều hướng role |
| GET `/archivist/departments` | DepartmentController | Xem năm khoa, không form thêm |
| GET/POST `/archivist/record-types` | CatalogController | Xem/thêm loại |
| POST `/archivist/record-types/{id}` | CatalogController | Sửa tên/trạng thái/version |
| GET/POST `/archivist/retention-policies` | CatalogController | Xem/tạo policy DRAFT |
| GET `/director/retention-policies` | CatalogController | Danh sách xác nhận/ngừng policy |
| GET `/archivist/records/new` | RecordController | Form tạo hồ sơ |
| POST `/archivist/records` | RecordController → RecordService | Lưu nháp hoặc nhập kho |
| GET `/archivist/records` | RecordController | Tra cứu, gồm nháp theo bộ lọc |
| GET `/reader/records`, `/director/records` | RecordController | Tra cứu hồ sơ chính thức |
| GET `/{role}/records/{id}` | RecordController | Chi tiết theo role, không phải role tùy ý |
| GET/POST `/archivist/records/{id}/edit` | RecordController → RecordService | Form/sửa/đối soát |
| POST `/archivist/records/{id}/move` | RecordController → RecordService | Chuyển ngăn |
| POST `/archivist/records/{id}/availability` | RecordController → RecordService | Xác nhận khả dụng sau kiểm tra vật lý |
| GET/POST `/archivist/locations` | StorageController | Danh sách/thêm vị trí |
| POST `/archivist/locations/{id}` | StorageController → StorageService | Sửa tên/trạng thái/sức chứa |

`{role}` ở dòng chi tiết chỉ có ba mapping cố định archivist/reader/director. Mọi URL `/admin/**` không tự cho phép mở các URL vai trò khác. Các URL POST nội bộ được form tạo cùng token; không mở GET trên thanh địa chỉ để thay cho thao tác POST.
