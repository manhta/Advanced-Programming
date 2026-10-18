# Development baseline verification

Ngày kiểm tra: 18/10/2026, múi giờ Asia/Bangkok.

## Build và khởi động

- Java: 26.0.2.1; Spring Boot: 4.1.1; Maven Wrapper: Maven 3.9.16.
- Package/class: `mrm.MedicalRecordApplication`.
- Lệnh build: `.\mvnw.cmd -o -B -ntp clean package`.
- Kết quả: BUILD SUCCESS; biên dịch lại 1 file Java và tạo `target/medical-record-management-0.0.1-SNAPSHOT.jar`.
- Build kết thúc lúc 11:23:37 +07:00. Chưa có bài kiểm thử tự động; kết quả build không đồng nghĩa nghiệp vụ đã được kiểm thử.
- Lệnh chạy: `java -jar target/medical-record-management-0.0.1-SNAPSHOT.jar --server.port=18080`.
- Kiểm tra: `Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:18080'`.
- Kết quả: HTTP 200, nội dung trang có HSBA. Đã dừng tiến trình sau khi kiểm tra.
- Dùng cổng 18080 cho lần kiểm chứng; chạy bình thường không truyền cổng sử dụng 8080.

## PostgreSQL

- Đã thử kết nối chỉ đọc: `psql -X -w -h localhost -p 5432 -U postgres -d postgres -c 'SELECT version();'`.
- Server phản hồi yêu cầu xác thực: `fe_sendauth: no password supplied`.
- Chưa xác nhận phiên bản server. Không thay đổi mật khẩu hoặc cấu hình xác thực.
- Người dùng cần chạy `SELECT version();` trong phiên đã đăng nhập và cung cấp kết quả; không gửi mật khẩu vào tài liệu/Git.

## Git và mốc nền tảng

- Trước kiểm kê: commit hiện có `4b0957e` (`first commit`), chỉ `README.md` đang được Git theo dõi.
- README có thay đổi; khung Spring Boot, Wrapper, requirement và checklist chưa được theo dõi trước bước này.
- Mốc nền tảng được ghi bằng commit có thông điệp `chore: record runnable medical record management baseline`.
- Tra cứu mã commit bằng `git log -1 --oneline` sau khi tạo mốc; không đẩy lên remote trong bước này.
- Phạm vi cần theo dõi: `.gitignore`, Maven Wrapper, `pom.xml`, `src/`, README, requirement, checklist và báo cáo kiểm chứng.
- `target/`, cấu hình IDE và file bí mật được loại trừ theo `.gitignore`.

## Giới hạn hiện tại

Chỉ có khung Spring Boot và trang xác nhận khởi động. Chưa có kết nối PostgreSQL, đăng nhập/phân quyền, metadata/kho, đăng ký, phê duyệt, lịch đọc, giao–trả, audit hoặc báo cáo nghiệp vụ.
