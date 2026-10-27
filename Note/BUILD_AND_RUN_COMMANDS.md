# Ghi chú: các lệnh build và chạy MRM

Ngày ghi:. Môi trường: Windows PowerShell, Maven Wrapper của dự án.

> Nội dung Note chỉ thay đổi khi người dùng yêu cầu. File này hướng dẫn để người dùng tự thực hiện; khi ghi tài liệu, chưa chạy build, ứng dụng, migration hoặc kết nối database.

## 1. Mở đúng thư mục dự án

```powershell
Set-Location -LiteralPath 'C:\Users\admin\Desktop\Project\HUST\Master\SUB\Advanced Programming\Code'
```

Tại đây phải có `pom.xml` và `mvnw.cmd`. Chạy các nhóm lệnh bên dưới trong cùng cửa sổ PowerShell nếu muốn chúng dùng chung biến môi trường.

Kiểm tra công cụ:

```powershell
java -version
javac -version
.\mvnw.cmd -version
```

`mvnw.cmd` là Maven Wrapper dành cho Windows. Lệnh kiểm tra Maven cho biết cả phiên bản Maven và Java mà Maven sử dụng. Lần đầu chạy có thể cần mạng để tải Maven/dependency.

## 2. Build và đóng gói chương trình

Lệnh thông thường:

```powershell
.\mvnw.cmd -B -ntp package
```

- `package`: thực hiện các bước cần thiết tới đóng gói, gồm biên dịch và chạy các test được cấu hình trong lifecycle.
- `-B`: chạy Maven ở batch mode.
- `-ntp`: ẩn tiến độ tải dependency, giúp log dễ đọc hơn.
- `BUILD SUCCESS`: Maven hoàn thành mục tiêu build; chưa chứng minh luồng đăng nhập hay kết nối database hoạt động ở runtime.

Với `pom.xml` hiện tại, executable JAR dự kiến là:

```text
target/medical-record-management-0.0.1-SNAPSHOT.jar
```

Nếu muốn xóa đầu ra build cũ rồi tạo lại:

```powershell
.\mvnw.cmd -B -ntp clean package
```

`clean` xóa thư mục đầu ra `target`, không xóa dữ liệu PostgreSQL. Không bắt buộc thêm `clean` cho mọi lần build.

Nếu chỉ muốn biên dịch hoặc chạy test:

```powershell
.\mvnw.cmd -B -ntp compile
.\mvnw.cmd -B -ntp test
```

Đây là hai lựa chọn riêng; không cần chạy cả hai trước `package`. `test` không thay thế kiểm thử thao tác bằng trình duyệt. Nếu chưa có test, lệnh thành công không đồng nghĩa chức năng đã được kiểm thử tự động. Khi có test tích hợp, yêu cầu database/biến môi trường tùy cấu hình test đó.

## 3. Chuẩn bị kết nối PostgreSQL trước khi chạy

Cấu hình `dev` hiện tại mặc định trỏ tới:

```text
Database: mrm
Host: 127.0.0.1
Port: 5433
User: postgres
Password: lấy từ biến DB_PASSWORD
```

Đây là thông số trong `application-dev.properties`, không phải suy ra từ tên server hiển thị trong pgAdmin. PostgreSQL server cần hoạt động; không bắt buộc mở pgAdmin.

Đặt rõ URL và username trong terminal để tránh dùng nhầm giá trị từ phiên làm việc trước:

```powershell
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:5433/mrm'
$env:DB_USERNAME = 'postgres'

$dbPasswordSecure = Read-Host 'Password PostgreSQL' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $dbPasswordSecure).Password
Remove-Variable dbPasswordSecure -ErrorAction SilentlyContinue
```

Nếu bạn đã dùng user database riêng, thay `postgres` bằng user đó. Password database khác password của các tài khoản đăng nhập website.

Các biến `$env:...` ở đây áp dụng cho tiến trình PowerShell hiện tại và các chương trình nó khởi chạy; mở terminal khác cần chuẩn bị lại. Việc chỉ đặt biến không tạo hoặc sửa dữ liệu trong PostgreSQL.

Kiểm tra đã có biến mà không in mật khẩu:

```powershell
[pscustomobject]@{
 DB_URL_SET = -not [string]::IsNullOrWhiteSpace($env:DB_URL)
 DB_USERNAME_SET = -not [string]::IsNullOrWhiteSpace($env:DB_USERNAME)
 DB_PASSWORD_SET = -not [string]::IsNullOrWhiteSpace($env:DB_PASSWORD)
}
```

## 4. Chạy chương trình bình thường khi phát triển

Sau bước 3, chạy:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
```

Giữ nguyên dấu nháy đơn bao quanh tham số `-D...` khi sao chép lệnh PowerShell. Tên goal chính xác là `spring-boot:run`.

Nếu không ghi đè cổng HTTP, mở:

```text
http://localhost:8080/login
```

Cổng `8080` là cổng website; `5433` là cổng PostgreSQL, hai cổng có vai trò khác nhau.

Khi khởi động, Flyway đang bật trong profile `dev` sẽ kiểm tra lịch sử và chạy migration còn thiếu. Migration đã áp dụng thành công không tự chạy lại mỗi lần khởi động. Không sửa migration cũ để yêu cầu chạy lại; thay đổi schema tiếp theo cần migration mới.

Lệnh chạy server tiếp tục chiếm terminal và chờ yêu cầu; đây là trạng thái bình thường. Xem log lỗi nếu server dừng, thay vì chỉ nhìn thông báo Maven cuối cùng.

## 5. Chạy kèm kiểm tra kết nối database

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev,db-check'
```

Hai profile cùng được bật làm cho `DatabaseConnectionCheck` có `@Profile("dev & db-check")` được tạo thành bean và chạy qua `ApplicationRunner`. `db-check` là tên profile do dự án quy ước, không phải tên file hay đường dẫn Java.

Khi kiểm tra thành công, log của lớp này có:

```text
=== PostgreSQL connection verified ===
Database: mrm
User:...
Server version:...
Server port:...
```

Giá trị thực tế lấy từ server. Không coi phiên bản ví dụ trong tài liệu cũ là kết quả hiện tại. Đây vẫn là lần khởi động toàn ứng dụng: Flyway có thể chạy migration còn thiếu và web server tiếp tục hoạt động, không phải lệnh kiểm tra rồi tự thoát.

## 6. Tạo tài khoản mẫu khi cần

Bạn đã tạo tài khoản mẫu thành công trước đó, nên thường chỉ cần chạy `dev` như bước 4. Chỉ bật `seed-users` khi cần khởi tạo các tài khoản mẫu còn thiếu.

Sau khi chuẩn bị `DB_PASSWORD`, nhập password website cho bốn tài khoản:

```powershell
$seedVariables = @(
 'SEED_READER_PASSWORD',
 'SEED_ARCHIVIST_PASSWORD',
 'SEED_DIRECTOR_PASSWORD',
 'SEED_ADMIN_PASSWORD'
)

foreach ($variableName in $seedVariables) {
 $seedPasswordSecure = Read-Host "Password cho $variableName" -AsSecureString
 $seedPasswordPlain = [System.Net.NetworkCredential]::new('', $seedPasswordSecure).Password
 [Environment]::SetEnvironmentVariable($variableName, $seedPasswordPlain, 'Process')
}

Remove-Variable seedPasswordSecure, seedPasswordPlain -ErrorAction SilentlyContinue

.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev,seed-users'
```

| Username | Role | Nguồn mật khẩu khi tạo mới |
|---|---|---|
| `reader1` | READER | `SEED_READER_PASSWORD` |
| `archivist1` | ARCHIVIST | `SEED_ARCHIVIST_PASSWORD` |
| `director1` | DIRECTOR | `SEED_DIRECTOR_PASSWORD` |
| `admin1` | ADMIN | `SEED_ADMIN_PASSWORD` |

Không có password mặc định được tài liệu này cung cấp. `DemoUserInitializer` giữ nguyên tài khoản đã tồn tại; nhập giá trị seed khác không đổi mật khẩu cũ. Chỉ nhập biến môi trường mà chưa chạy ứng dụng thì dữ liệu chưa được ghi vào database.

Log hoàn thành:

```text
Demo accounts initialized; existing accounts were preserved.
```

Nếu cần vừa seed vừa kiểm tra kết nối trong một lần khởi động:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev,seed-users,db-check'
```

## 7. Dừng chương trình

Nhấn `Ctrl + C` trong terminal đang chạy. Nếu PowerShell hỏi xác nhận kết thúc batch job, xác nhận để dừng tiến trình. Chờ terminal trở về dấu nhắc trước khi chạy lệnh tiếp theo.

Đóng tab trình duyệt không dừng server Java. Dừng ứng dụng không xóa database hay các tài khoản đã tạo.

Không khởi động hai bản ứng dụng cùng cổng. Nếu gặp lỗi cổng đang được sử dụng, kiểm tra phiên chạy trước đã dừng chưa.

## 8. Chạy bản JAR đã đóng gói

Đây là cách chạy thay cho Maven khi muốn kiểm tra sản phẩm build. Sau bước 3 và một lần `package` thành công:

```powershell
java -jar '.\target\medical-record-management-0.0.1-SNAPSHOT.jar' --spring.profiles.active=dev
```

Để kiểm tra database bằng bản JAR:

```powershell
java -jar '.\target\medical-record-management-0.0.1-SNAPSHOT.jar' '--spring.profiles.active=dev,db-check'
```

Chạy JAR không tự biên dịch mã nguồn hay đóng gói HTML vừa sửa. Sau thay đổi, chạy lại `package` rồi mới chạy JAR mới. Nếu đổi `artifactId`, `version` hoặc `finalName` trong `pom.xml`, kiểm tra lại tên JAR trước khi dùng lệnh.

## 9. Khi có lỗi khởi động

Đọc phần log phía trên `BUILD FAILURE`, nhất là `APPLICATION FAILED TO START`, `Caused by` và thông báo exception. Dòng `Process terminated with exit code: 1` chỉ cho biết tiến trình thất bại, chưa nói nguyên nhân.

Nếu cần thêm thông tin từ Maven, chạy lại lệnh đang lỗi với `-e`, ví dụ:

```powershell
.\mvnw.cmd -e spring-boot:run '-Dspring-boot.run.profiles=dev'
```

Lần chạy lại vẫn khởi động ứng dụng và có thể chạy migration còn thiếu; đây không phải thao tác chỉ đọc log cũ. Trước khi chia sẻ log, kiểm tra để không gửi thông tin bí mật.

| Biểu hiện | Kiểm tra trước |
|---|---|
| Thiếu `DB_PASSWORD` | Đặt biến trong cùng terminal chạy Maven/Java |
| `Connection refused` | Dịch vụ PostgreSQL, host, port `5433` |
| Password authentication failed | Username và password PostgreSQL |
| Database does not exist | Database `mrm` trên đúng server/port |
| Thiếu `SEED_...` | Cấp biến cho tài khoản còn thiếu hoặc chạy `dev` nếu không cần seed |
| Port already in use | Phiên ứng dụng trước còn chạy |
| Migration validation failed | Đọc lỗi Flyway; không xóa lịch sử hoặc sửa migration cũ tùy tiện |
| Đăng nhập thất bại | Dùng username/password website, không dùng password PostgreSQL |

## 10. Xóa biến khỏi terminal sau khi hoàn tất

Dừng ứng dụng trước, rồi nếu không cần dùng tiếp:

```powershell
$variablesToClear = @(
 'DB_PASSWORD',
 'DB_URL',
 'DB_USERNAME',
 'SEED_READER_PASSWORD',
 'SEED_ARCHIVIST_PASSWORD',
 'SEED_DIRECTOR_PASSWORD',
 'SEED_ADMIN_PASSWORD'
)

foreach ($variableName in $variablesToClear) {
 [Environment]::SetEnvironmentVariable($variableName, $null, 'Process')
}
```

Lệnh này chỉ xóa biến của tiến trình PowerShell hiện tại; không đổi password PostgreSQL hoặc password tài khoản website.

## 11. Quy trình thường dùng ngắn gọn

1. Mở PowerShell tại thư mục Code.
2. Chuẩn bị `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` theo bước 3.
3. Build: `.\mvnw.cmd -B -ntp package`.
4. Chạy: `.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'`.
5. Mở `/login`, thử chức năng cần kiểm tra.
6. Nhấn `Ctrl + C` khi kết thúc.

Không cần chạy `seed-users` mỗi lần; dùng `db-check` khi cần xác nhận thông tin kết nối. Nội dung file này không tự được đồng bộ khi cấu hình dự án thay đổi.
