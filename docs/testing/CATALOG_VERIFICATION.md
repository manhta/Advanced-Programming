# Kiểm chứng danh mục loại bệnh án/chính sách bảo quản

Ngày:. Múi giờ Asia/Bangkok (+07:00).

## Cập nhật hoàn thiện P2

V6 đã áp dụng; lượt kiểm tra cuối có **49 kiểm tra CatalogIntegrationCheck và 26 kiểm tra CompleteCatalogCheck đạt**. Báo cáo có đối chiếu phạm vi/bằng chứng tại `docs/verification/P2_COMPLETION_REPORT.md`. Các kết quả 45 kiểm tra/V5 bên dưới được giữ như lịch sử lượt trước.

Sau khi chuẩn bị runtime libraries theo hướng dẫn bên dưới và chạy ứng dụng ở cổng phù hợp:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -cp 'target/classes;target/test-classes;target/catalog-verification-libs/*' mrm.catalog.CatalogIntegrationCheck http://localhost:18081
& "$env:JAVA_HOME\bin\java.exe" -cp 'target/classes;target/test-classes;target/catalog-verification-libs/*' mrm.catalog.CompleteCatalogCheck
```

Bộ mở rộng cần quyền CREATE/DROP schema trong database phát triển để kiểm tra khởi động thật từ trống. Nó chỉ xóa schema thử có tiền tố `p2_bootstrap_` và UUID do nó tự tạo, sau khi đóng ứng dụng thử. Không chạy kiểm tra này trên database vận hành. Kiểm tra đồng thời dùng hai luồng thực sự; kiểm tra ngày nhuận bao gồm năm đích thường và nhuận, đều về 28/2 theo quy tắc học tập đã xác nhận.

## Kết quả thực hiện

- Build `.\mvnw.cmd -o -B -ntp package`: BUILD SUCCESS, kết thúc 15:50:27 +07:00.
- Khởi động JAR với profile dev và cổng ngẫu nhiên 55858: thành công; Flyway áp dụng V5 trên database `mrm` tại `127.0.0.1:5433`, PostgreSQL 18.6, +07:00.
- Chạy bộ kiểm tra PostgreSQL/HTTP: **45 kiểm tra đạt**, mã thoát 0, lượt cuối bắt đầu 15:50:59 +07:00.
- Danh mục còn lại sau kiểm tra: sáu loại bệnh án khởi tạo; không để lại policy/tài khoản/danh mục/sự kiện thử. Chính sách nghiệp vụ thực tế chưa được tạo vì chưa chốt số năm/căn cứ.
- Đã dừng phiên ứng dụng dùng để kiểm chứng. Không yêu cầu dừng hoặc sửa phiên chạy khác của người dùng.

## Phạm vi

`src/test/java/mrm/catalog/CatalogIntegrationCheck.java` là chương trình kiểm thử tích hợp chạy tường minh, không phải bài JUnit được Surefire tự chạy. `package` biên dịch nó nhưng không tự chạy các kiểm tra database/HTTP.

Các kiểm tra repository/service dùng transaction rollback. Kiểm tra HTTP tạo bốn tài khoản thử với mật khẩu ngẫu nhiên trong bộ nhớ và các danh mục có mã ngẫu nhiên; khối finally xóa đúng các bản ghi thử và sự kiện liên quan. Không in mật khẩu hoặc sửa tài khoản demo. Nếu tiến trình bị dừng cưỡng bức giữa chừng cần kiểm tra dữ liệu thử còn lại; không chạy trên database vận hành.

## Các nhóm kiểm tra

- Bean Validation: mã/tên rỗng hoặc sai, số năm/phiên bản không dương, thiếu ngày/căn cứ.
- Chuẩn hóa mã, lưu tên tiếng Việt, sửa tên và ngừng dùng loại, giữ bản ghi lịch sử.
- Mã loại trùng; mã+phiên bản chính sách trùng; ngày kết thúc trước ngày hiệu lực.
- Chính sách mới là DRAFT và inactive; xác nhận ghi Director; ngừng dùng vẫn giữ APPROVED/lịch sử.
- Chặn khoảng hiệu lực chồng nhau của cùng mã, chấp nhận hai khoảng không giao nhau.
- Audit tạo/sửa danh mục và rollback nếu ghi audit thất bại.
- HTTP login mới có CSRF; người chưa đăng nhập bị chuyển về login.
- Quyền READER/ARCHIVIST/DIRECTOR/ADMIN; Archivist không gọi được thao tác xác nhận của Director.
- Render trang loại và chính sách cả danh sách rỗng và có dữ liệu, form nhập lỗi hiển thị lại validation.
- Form POST hợp lệ, bind ngày/số, redirect sau thành công; POST thiếu CSRF bị chặn.
- Dữ liệu thử được rollback/xóa sau kiểm tra.

## Chuẩn bị và chạy lại

1. Build tại thư mục gốc:

 ```powershell
.\mvnw.cmd -o -B -ntp package
 ```

 Bỏ `-o` nếu máy chưa tải đủ dependency. Cần PostgreSQL và cấu hình dev; V1–V5 phải áp dụng thành công. V4 hiện phụ thuộc tài khoản `archivist1` có sẵn, xem checklist.

2. Chuẩn bị thư viện runtime từ JAR, không cài thư viện mới:

 ```powershell
 Add-Type -AssemblyName System.IO.Compression.FileSystem
 $catalogLibPath = Join-Path (Get-Location) 'target/catalog-verification-libs'
 New-Item -ItemType Directory -Path $catalogLibPath -Force | Out-Null
 $catalogArchive = [System.IO.Compression.ZipFile]::OpenRead(
 (Join-Path (Get-Location) 'target/medical-record-management-0.0.1-SNAPSHOT.jar'))
 try {
 foreach ($catalogEntry in $catalogArchive.Entries) {
 if ($catalogEntry.FullName.StartsWith('BOOT-INF/lib/') -and $catalogEntry.Name.EndsWith('.jar')) {
 [System.IO.Compression.ZipFileExtensions]::ExtractToFile(
 $catalogEntry, (Join-Path $catalogLibPath $catalogEntry.Name), $true)
 }
 }
 } finally { $catalogArchive.Dispose() }
 ```

3. Chạy ứng dụng với profile dev trong terminal thứ nhất, chuẩn bị DB_PASSWORD như hướng dẫn bình thường. Dùng cổng còn trống, ví dụ 18081:

 ```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev' '-Dspring-boot.run.arguments=-server.port=18081'
 ```

4. Trong terminal thứ hai, đặt biến database cùng thông số. Bộ kiểm tra ưu tiên DB_PASSWORD; nếu thiếu, đọc `.secrets/db-password.txt` và chỉ bỏ ký tự xuống dòng ở cuối. Không in nội dung file.

 ```powershell
 & "$env:JAVA_HOME\bin\java.exe" -cp 'target/classes;target/test-classes;target/catalog-verification-libs/*' mrm.catalog.CatalogIntegrationCheck http://localhost:18081
 ```

 Nếu không truyền URL, chỉ chạy nhóm database/service/validation. Bộ kiểm tra dùng `archivist1` và `director1` cho các giao dịch rollback nên cần hai tài khoản này tồn tại. HTTP sử dụng tài khoản thử riêng, không cần biết password demo.

5. Kiểm tra dòng PASSED và mã thoát 0; dừng ứng dụng kiểm tra bằng Ctrl+C sau khi xong.

## Giới hạn

Chưa nghiệm thu trực quan trên trình duyệt hoặc lưu snapshot xuyên suốt với hồ sơ UC-04 chưa triển khai. Bộ mở rộng đã kiểm tra snapshot bất biến, tính hạn 29/2 và hai luồng sửa loại bệnh án cùng version; kiểm tra overlap phê duyệt chính sách hiện chạy tuần tự.
