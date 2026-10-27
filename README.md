# Hệ thống quản lý hồ sơ bệnh án (MRM)

MRM là ứng dụng web nội bộ hỗ trợ quản lý metadata bệnh án giấy và điện tử, vị trí lưu trữ, yêu cầu đọc, xét duyệt và giao–trả hồ sơ. Ứng dụng phục vụ dự án học tập; không lưu bản quét hoặc toàn văn bệnh án và chưa tích hợp HIS hay gửi email SMTP. Thông báo được gửi trong ứng dụng.

## Tổng quan hệ thống

Phần mềm có bốn vai trò. Mỗi tài khoản được gán một vai trò; quyền được kiểm tra ở trang và thao tác nghiệp vụ.

| Vai trò | Chức năng chính |
|---|---|
| **Reader / Bác sĩ** | Tra cứu hồ sơ, khai báo hồ sơ điện tử, đăng ký mượn hồ sơ giấy, theo dõi và hủy yêu cầu khi còn được phép, quản lý thông tin cá nhân và nhận thông báo. |
| **Archivist / Nhân viên lưu trữ** | Khai báo và đối soát hồ sơ, quản lý danh mục và vị trí kho, xét duyệt yêu cầu thường, xếp lịch nhận, giao–trả, kiểm kê và xem báo cáo vận hành. |
| **Director / Giám đốc** | Duyệt yêu cầu thuộc nhánh Giám đốc, xét duyệt hồ sơ điện tử và đề nghị thay đổi vai trò/khoa; xác thực bằng mật khẩu cấp 2. Không có báo cáo hoặc kiểm kê. |
| **Admin / Quản trị viên** | Tạo và quản lý tài khoản, áp dụng thay đổi nhân sự đã được duyệt, xem nhật ký kỹ thuật/tài khoản trong phạm vi cho phép. Không truy cập dữ liệu bệnh án. |

### Các quy trình nghiệp vụ chính

- **Hồ sơ bệnh án:** hồ sơ giấy và điện tử dùng chung danh mục, được phân biệt bằng hình thức lưu trữ và trạng thái. Archivist khai báo hồ sơ giấy; Reader khai báo hồ sơ điện tử, Archivist kiểm tra rồi Director quyết định công bố.
- **Vị trí lưu trữ:** Archivist quản lý cây Kho–Kệ–Ngăn, di chuyển hồ sơ có lý do và lưu lịch sử vị trí.
- **Mượn hồ sơ giấy:** Reader gửi một lô gồm một hoặc nhiều hồ sơ; từng hồ sơ có trạng thái và kết quả xử lý riêng. Lý do bắt buộc nếu lô vượt ngưỡng số lượng hoặc có hồ sơ tử vong. Nếu lô có hồ sơ tử vong, toàn bộ lô cần Giám đốc duyệt; lô chỉ có hồ sơ thường do Archivist xử lý.
- **Xếp lịch và giao–trả:** sau khi yêu cầu được duyệt, Archivist xác nhận ngày/phòng nhận trong giới hạn nghiệp vụ, ghi nhận giao hồ sơ, tình trạng khi giao/trả và vị trí cất lại.
- **Tài khoản và nhân sự:** Admin tạo tài khoản với tên đăng nhập/mật khẩu ban đầu tự sinh, yêu cầu chọn khoa và kiểm tra căn cước không trùng. Nhân viên có thể gửi đề nghị đổi vai trò/khoa; Director xét duyệt, sau đó Admin áp dụng.
- **Thông báo và lịch sử:** chuông/inbox hiển thị việc cần xử lý và kết quả cho đúng người nhận. Có thể dọn thông báo đã xử lý; lịch sử nghiệp vụ và audit được lưu riêng.

Các thời hạn lưu hồ sơ cấu hình trong dự án là dữ liệu mô phỏng học tập, không phải xác nhận tuân thủ quy định pháp luật. Trước khi dùng dữ liệu thật cần được đơn vị có thẩm quyền rà soát quyền truy cập, quy trình, bảo mật, sao lưu và quy định lưu trữ.

## Công nghệ và cấu trúc mã nguồn

- Java theo phiên bản khai báo trong `pom.xml`; Maven Wrapper giúp build mà không cần cài Maven riêng.
- Spring Boot, Spring MVC, Thymeleaf, Spring Security, Bean Validation, JDBC/JdbcTemplate và Flyway Java migrations.
- PostgreSQL lưu dữ liệu nghiệp vụ. Cấu hình phát triển mặc định dùng `jdbc:postgresql://127.0.0.1:5433/mrm`, username `postgres`; có thể ghi đè bằng `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
- `src/main/java/mrm/`: controller, service, bảo mật, truy cập dữ liệu, migration và nghiệp vụ.
- `src/main/resources/templates/`: giao diện Thymeleaf; `static/css`, `static/js`, `static/images`: tài nguyên trình duyệt.
- `src/test/` và `scripts/`: kiểm tra và công cụ xác minh.
- `docs/user-guides/`: hướng dẫn theo vai trò; `docs/`: yêu cầu, thiết kế, schema và kiểm chứng.

## Yêu cầu trước khi chạy

1. Cài JDK tương thích với `java.version` trong `pom.xml` và PostgreSQL.
2. Tạo database `mrm` trên PostgreSQL; cấu hình username/password có quyền tạo bảng và chạy migration.
3. Mở PowerShell tại thư mục gốc dự án, nơi có `pom.xml` và `mvnw.cmd`.

Kiểm tra Java và Maven Wrapper:

```powershell
java -version
.\mvnw.cmd -version
```

### Cấu hình kết nối database

Nhập password database an toàn trong cửa sổ PowerShell hiện tại. Không ghi password vào mã nguồn hoặc commit vào Git.

```powershell
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:5433/mrm'
$env:DB_USERNAME = 'postgres'
$dbPasswordSecure = Read-Host 'Password PostgreSQL' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $dbPasswordSecure).Password
Remove-Variable dbPasswordSecure
```

Nếu PostgreSQL của bạn dùng địa chỉ, cổng, database hoặc username khác, thay `DB_URL` và `DB_USERNAME` cho phù hợp. Password PostgreSQL là password kết nối database, không phải password tài khoản MRM.

### Khởi động ứng dụng

Build và chạy test đã cấu hình trong Maven:

```powershell
.\mvnw.cmd -B -ntp package
```

Khởi động ứng dụng với profile phát triển:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
```

Flyway sẽ áp dụng các migration còn thiếu khi ứng dụng kết nối database. Database phải tồn tại trước khi khởi động. Giữ cửa sổ PowerShell mở trong lúc server chạy; nhấn `Ctrl+C` để dừng.

Mở trang đăng nhập tại <http://localhost:8080/login>. Nếu dùng cổng hoặc host khác, mở địa chỉ tương ứng. Không chạy seed tài khoản demo trên database đang có dữ liệu nghiệp vụ.

### Khởi động bằng JAR

Sau khi build thành công và đã nạp các biến kết nối database trong cùng phiên PowerShell:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar target/medical-record-management-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Nếu chưa có `JAVA_HOME`, có thể chạy `java -jar target/medical-record-management-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev` sau khi bảo đảm `java` trỏ đúng JDK.

### Khởi tạo tài khoản cho database phát triển hoàn toàn mới

Database trống không có tài khoản Admin mặc định. Chỉ với database phát triển mới, có thể bật profile `seed-users` và nhập bốn mật khẩu seed trong PowerShell trước khi chạy:

```powershell
$seedVariables = @(
    'SEED_READER_PASSWORD',
    'SEED_ARCHIVIST_PASSWORD',
    'SEED_DIRECTOR_PASSWORD',
    'SEED_ADMIN_PASSWORD'
)
foreach ($variableName in $seedVariables) {
    $secureValue = Read-Host "Password seed cho $variableName" -AsSecureString
    $plainValue = [System.Net.NetworkCredential]::new('', $secureValue).Password
    [Environment]::SetEnvironmentVariable($variableName, $plainValue, 'Process')
}
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev,seed-users'
foreach ($variableName in $seedVariables) {
    [Environment]::SetEnvironmentVariable($variableName, $null, 'Process')
}
Remove-Variable secureValue, plainValue -ErrorAction SilentlyContinue
```

Các biến seed không có password mặc định; chỉ dùng giá trị cục bộ, không ghi vào README/Git. Lệnh chạy server sẽ chiếm cửa sổ terminal, vì vậy mở cửa sổ PowerShell khác để làm việc và xóa các biến môi trường sau khi dừng server. Seed tạo tài khoản kiểm thử legacy để phát triển. Với database sử dụng hiện tại, hãy dùng tài khoản được Admin bàn giao và không bật seed. Nếu ứng dụng chưa có tài khoản để đăng nhập, cần khởi tạo database theo quy trình quản trị được dự án cung cấp trước khi sử dụng.

Tài khoản mới do Admin tạo sẽ phải đổi mật khẩu ở lần đăng nhập đầu tiên. Giám đốc còn phải tự thiết lập mật khẩu cấp 2. Không chia sẻ mật khẩu trong tài liệu hoặc commit vào repository.

### Kiểm thử chức năng

Sau khi build và khởi động ứng dụng, mở PowerShell thứ hai tại thư mục dự án:

```powershell
.\scripts\verify-features.ps1 -BaseUrl http://localhost:8080
```

Các bộ kiểm tra có thể dùng dữ liệu và schema thử. Chỉ chạy trên môi trường phát triển phù hợp; đọc `docs/testing/` và hướng dẫn script trước khi chạy trên database khác.

## Hướng dẫn sử dụng theo vai trò

### Reader / Bác sĩ

1. Đăng nhập bằng tài khoản được cấp. Ở lần đầu, đổi mật khẩu ban đầu theo form bắt buộc.
2. Từ trang chủ, mở **Tra cứu hồ sơ** để tìm theo mã, tên hoặc bộ lọc. Hồ sơ khai báo của bạn nằm ở tab riêng; hồ sơ chưa được duyệt không xuất hiện như hồ sơ chính thức.
3. Để khai báo hồ sơ điện tử, mở **Thêm bệnh án mới**, nhập metadata, kiểm tra thông tin và gửi xét duyệt. Theo dõi kết quả ở danh sách khai báo hoặc chuông thông báo; bổ sung nếu được yêu cầu.
4. Để mượn hồ sơ giấy, tìm hồ sơ có bản giấy và còn đủ điều kiện, chọn một hoặc nhiều hồ sơ, ghi lý do khi biểu mẫu yêu cầu rồi gửi. Archivist sẽ kiểm tra và xếp lịch; nếu lô có hồ sơ tử vong, toàn bộ lô chờ Giám đốc duyệt.
5. Mở **Lịch sử yêu cầu** để theo dõi tiến trình, ngày/phòng nhận, kết quả, bổ sung hoặc hủy khi trạng thái còn cho phép. Sau khi nhận hồ sơ giấy, mở chi tiết hồ sơ để xem thông tin được cấp quyền.
6. Mở menu tên tài khoản để xem/chỉnh sửa thông tin cá nhân, đổi mật khẩu hoặc đăng xuất. Đề nghị đổi vai trò/khoa nằm trong form chỉnh sửa cá nhân; thay đổi chỉ có hiệu lực sau khi Director duyệt và Admin áp dụng.
7. Dùng **Thông báo** để xem việc đang chờ và kết quả. Có thể xóa thông báo đã xử lý; việc đang chờ không thể xóa.

Hướng dẫn chi tiết: [Reader](docs/user-guides/reader.md).

### Archivist / Nhân viên lưu trữ

1. Đăng nhập và mở **Tra cứu hồ sơ** hoặc **Hồ sơ lưu trữ** để xem hồ sơ theo quyền.
2. Tạo danh mục loại hồ sơ/chính sách khi được giao. Tạo vị trí theo thứ tự **Kho → Kệ → Ngăn** tại **Vị trí lưu trữ**.
3. Nhập hồ sơ giấy tại **Thêm hồ sơ**, khai báo metadata và vị trí; lưu nháp khi cần đối soát hoặc xác nhận hồ sơ đã được nhập kho.
4. Mở **Yêu cầu đọc** hoặc thông báo công việc, đối chiếu người yêu cầu, hồ sơ và lý do. Chọn phê duyệt, yêu cầu bổ sung hoặc từ chối. Nội dung bổ sung/lý do từ chối phải nhập theo yêu cầu của biểu mẫu.
5. Với hồ sơ thường, khi phê duyệt hãy xác nhận lịch nhận và phòng nhận. Với lô có hồ sơ tử vong, chờ Giám đốc quyết định rồi mới xác nhận lịch.
6. Tại **Giao–trả**, đối chiếu hồ sơ và người nhận, ghi tình trạng khi giao; lúc trả, kiểm tra tình trạng, chọn ngăn cất lại và lưu nhận trả.
7. Dùng **Kiểm kê** để tạo đợt, ghi tình trạng từng hồ sơ và chốt sau khi đối chiếu. Mở **Báo cáo / nhật ký** để xem hoặc xuất dữ liệu trong phạm vi Archivist.

Hướng dẫn chi tiết: [Archivist](docs/user-guides/archivist.md).

### Director / Giám đốc

1. Đăng nhập, đổi mật khẩu ban đầu nếu được yêu cầu và thiết lập mật khẩu cấp 2 riêng.
2. Mở **Yêu cầu** hoặc chuông thông báo để xử lý lô mượn được trình lên. Chỉ lô có hồ sơ tử vong cần Director duyệt; quyết định áp dụng cho toàn bộ lô. Nhập lý do khi từ chối và xác thực bằng mật khẩu cấp 2.
3. Với hồ sơ điện tử mới, mở hồ sơ Archivist đã kiểm tra, đối chiếu metadata rồi duyệt công bố hoặc từ chối; nhập căn cứ và mật khẩu cấp 2.
4. Mở **Đề nghị thay đổi nhân sự** để duyệt/từ chối yêu cầu đổi vai trò/khoa. Lý do từ chối bắt buộc. Khi duyệt, Admin cần áp dụng thay đổi trước khi nhân viên nhận thông báo kết quả.
5. Quản lý chính sách lưu trữ và danh mục được cấp quyền. Menu cá nhân dùng để cập nhật thông tin, đổi password cấp 2, đổi mật khẩu hoặc đăng xuất.

Director không có mục báo cáo hoặc kiểm kê. Hướng dẫn chi tiết: [Director](docs/user-guides/director.md).

### Admin / Quản trị viên

1. Đăng nhập rồi mở **Tài khoản → Tạo tài khoản**. Nhập họ tên, CCCD, ngày sinh, giới tính, vai trò và khoa; khoa là bắt buộc. Username và mật khẩu ban đầu được hệ thống sinh. Nếu CCCD trùng, sửa thông tin theo cảnh báo trên form.
2. Ghi lại thông tin bàn giao mật khẩu ban đầu tại thời điểm tạo và chuyển cho đúng nhân viên qua kênh an toàn. Mật khẩu không hiển thị lại sau khi rời trang kết quả.
3. Mở **Quản lý tài khoản** để tìm, sửa, khóa hoặc xóa mềm tài khoản. Xóa mềm giữ lịch sử và tham chiếu nghiệp vụ. Đổi vai trò/khoa theo quy trình nhân sự phải xuất phát từ đề nghị được Director duyệt; Admin áp dụng ở khu vực quản lý tài khoản.
4. Mở **Báo cáo / nhật ký** để xem sự kiện xác thực và quản trị tài khoản được phép. Admin không xem dữ liệu bệnh án hoặc báo cáo lâm sàng.
5. Dùng menu cá nhân để xem thông tin cá nhân, đổi mật khẩu hoặc đăng xuất.

Hướng dẫn chi tiết: [Admin](docs/user-guides/admin.md).

## Tài liệu tham khảo

- [Bộ tài liệu phân tích và thiết kế](docs/README.md)
- [Kiến trúc hệ thống](docs/architecture.md)
- [Phạm vi yêu cầu](docs/requirements/scope.md), [quy tắc nghiệp vụ](docs/requirements/business-rules.md), [ma trận quyền](docs/requirements/permission-matrix.md)
- [Yêu cầu chức năng và phi chức năng hiện thực](docs/verification/AS_BUILT_FUNCTIONAL_AND_NFR_REPORT.md)
- [Thiết kế cơ sở dữ liệu và ERD](docs/database/CORE_DATABASE_DESIGN_AND_ERD.md), [schema đã triển khai](docs/database/implemented-schema.md)
- [Quy trình và trạng thái](docs/design/workflow-and-states.md), [kiểm thử và truy vết](docs/testing/traceability.md)

## Lưu ý bảo mật và dữ liệu

- Không commit password, secret cấp 2, file `.secrets/` hoặc dữ liệu hồ sơ thật.
- Chỉ dùng dữ liệu giả lập trong phát triển. Database `mrm` phải là database đúng trước khi chạy migration.
- Không sửa migration Flyway đã áp dụng. Thay đổi schema mới cần migration Java tiếp theo.
- Sao lưu/khôi phục, cấu hình production, HTTPS, giám sát và nghiệm thu bảo mật cần được thiết lập riêng; tài liệu/code hiện có không chứng minh các yêu cầu vận hành này đã đạt.
