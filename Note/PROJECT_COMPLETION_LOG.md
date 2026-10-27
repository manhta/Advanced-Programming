# Nhật ký hoàn thành công việc — MRM

 Múi giờ: Asia/Bangkok (UTC+07:00).

## 1. Mục đích và cách ghi nhận

Tập trung các mốc quan trọng đã hoàn thành để theo dõi tiến độ và tra cứu bằng chứng. Nhật ký được tổng hợp từ tài liệu dự án, log người dùng cung cấp và xác nhận trong cuộc trao đổi; không phải kết quả kiểm thử lại khi tạo file.

- Chỉ thêm, sửa hoặc xóa nội dung Note khi người dùng yêu cầu. Nhật ký này không tự cập nhật khi sửa code, build hoặc chạy chương trình.
- Ghi ngày/giờ hoàn thành khi có bằng chứng. Không dùng thời gian sửa file làm thời gian hoàn thành nghiệp vụ.
- Nếu chỉ biết ngày, ghi rõ chưa có giờ; nếu không xác định được ngày, ghi “Chưa xác định”. Ngày ghi nhận không thay thế ngày hoàn thành.
- “Hoàn thành tài liệu”, “hoàn thành giao diện” và “đã kiểm thử chức năng” là các kết quả khác nhau. Không suy ra toàn bộ use case hoặc giai đoạn đã hoàn thành từ một phần đã chạy được.
- Chỉ ghi người thực hiện/xác nhận khi có thông tin; hiện dùng “người dùng” và “Codex”, không tự gán danh tính khác.

## 2. Các nơi đã lưu một phần lịch sử trước khi lập nhật ký

| File | Nội dung đã có |
|---|---|
| [PROJECT_CHECKLIST.md](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/PROJECT_CHECKLIST.md>) | Checklist giai đoạn, một số ngày xác nhận và cập nhật |
| [BASELINE_VERIFICATION.md](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/docs/verification/BASELINE_VERIFICATION.md>) | Build và khởi động nền tảng; có giờ kết thúc build |
| [Thư mục changes](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/docs/changes>) | Các thay đổi đã xác nhận về vai trò và phạm vi đăng nhập |

Các tài liệu trên không phải nhật ký hoàn thành tập trung. Một số mục “chưa triển khai” trong checklist là trạng thái tại thời điểm ghi cũ; file này bổ sung các kết quả được xác nhận về sau, không tự đánh dấu lại checklist.

## 3. Các mốc hoàn thành đã ghi nhận

| Mã mốc | Ngày hoàn thành/xác nhận | Giờ (+07:00) | Nội dung và phạm vi hoàn thành | Bằng chứng/kết quả |
|---|---|---|---|---|
| MRM-DONE-001 | | Chưa ghi nhận | Thống nhất package gốc `mrm`, tên khởi động `MedicalRecordApplication`, artifact `medical-record-management`; đồng bộ tên JAR trong README ở mốc nền tảng | Checklist mục 0 và báo cáo baseline |
| MRM-DONE-002 | | 11:23:37 — giờ kết thúc build | Build lại nền tảng với `.\mvnw.cmd -o -B -ntp clean package` | `BUILD SUCCESS`, tạo JAR; baseline ghi rõ lúc này chưa có test tự động |
| MRM-DONE-003 | | Chưa ghi nhận | Chạy JAR nền tảng ở cổng 18080 và kiểm tra HTTP | HTTP 200; đã dừng tiến trình sau kiểm tra, theo báo cáo baseline |
| MRM-DONE-004 | | Chưa ghi nhận | Soạn bản dự thảo phân tích/thiết kế giai đoạn 1–5, phân chia chức năng mandatory và optional | Checklist và tài liệu trong `docs`; hoàn thành soạn bản dự thảo, chưa đồng nghĩa mọi quyết định nghiệp vụ đã được duyệt |
| MRM-DONE-005 | | Chưa ghi nhận | Xác nhận bốn role: READER, ARCHIVIST, DIRECTOR, ADMIN; mỗi tài khoản đúng một role | Checklist và các thay đổi CR-001/CR-002 |
| MRM-DONE-006 | | Chưa ghi nhận | Ghi nhận phạm vi đăng nhập tối giản: xác thực và điều hướng theo role; hoãn quản lý phiên nâng cao, đổi mật khẩu bắt buộc lần đầu, quên/reset mật khẩu | CR-003 và phần D-26 trong checklist; đây là hoàn thành xác nhận phạm vi |
| MRM-DONE-007 | | Chưa ghi nhận | Chuyển migration xác thực sang Java, cấu hình Flyway và checksum; biên dịch thành công | Checklist ghi `.\mvnw.cmd -o -B -ntp clean compile` đạt `BUILD SUCCESS`; lúc ghi mốc này chưa chạy migration trên database |
| MRM-DONE-008 | | Chưa ghi nhận | Tạo ghi chú giải thích Spring MVC/Thymeleaf, classpath, điều hướng đăng nhập/đăng xuất và truyền dữ liệu Java–HTML | [SPRING_MVC_THYMELEAF_GUIDE.md](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/Note/SPRING_MVC_THYMELEAF_GUIDE.md>); hoàn thành tài liệu |
| MRM-DONE-009 | | Chưa ghi nhận | Tạo hướng dẫn lệnh build, chạy, profile, biến môi trường database, seed tài khoản và chạy JAR | [BUILD_AND_RUN_COMMANDS.md](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/Note/BUILD_AND_RUN_COMMANDS.md>); hoàn thành tài liệu, không phải lần chạy các lệnh |
| MRM-DONE-010 | | Chưa ghi nhận | Sửa lỗi trang login render dở do tạo session CSRF sau khi response đã được gửi | Thêm `spring.thymeleaf.servlet.produce-partial-output-while-processing=false`; người dùng xác nhận “Đã chạy lại thành công”; đã bổ sung nguyên nhân/cách sửa vào ghi chú Thymeleaf |
| MRM-DONE-011 | | Chưa ghi nhận | Hoàn thiện phần nhận diện bên trái trang login theo phản hồi: bỏ logo nhỏ và chữ MRM, dùng tiêu đề hệ thống ở trên, một logo lớn tròn, nền xanh, điểm nhấn vàng nhạt và tên trung tâm; thanh ngang đồng màu `#e3d795` | Codex cập nhật `login.html`; người dùng xác nhận “Như vậy phần logo đã xong”. Mốc này chỉ xác nhận phần logo/nhận diện |
| MRM-DONE-012 | | Chưa ghi nhận | Tạo nhật ký hoàn thành tập trung và tổng hợp các mốc có căn cứ | File `PROJECT_COMPLETION_LOG.md` này |

## 4. Công việc người dùng đã xác nhận thành công nhưng chưa rõ ngày/giờ

Các dòng này được tổng hợp vào nhật ký. Không gán ngày tổng hợp thành ngày hoàn thành thực tế.

| Mã mốc | Ngày/giờ hoàn thành | Công việc | Kết quả/xác nhận đã có |
|---|---|---|---|
| MRM-DONE-013 | Chưa xác định | Kết nối Java/Spring Boot với PostgreSQL database riêng `mrm` | Người dùng xác nhận kết nối thành công; log tiếp tục cho thấy kết nối hoạt động |
| MRM-DONE-014 | Chưa xác định | Khởi tạo tài khoản mẫu cho bốn role | Người dùng báo đã nhận `Demo accounts initialized; existing accounts were preserved.`; tài khoản mẫu `reader1`, `archivist1`, `director1`, `admin1` |
| MRM-DONE-015 | Chưa xác định | Kiểm tra chức năng đăng nhập cơ bản | Người dùng xác nhận đã kiểm tra chức năng thành công. Chưa có báo cáo chi tiết kết quả từng test case, nên không tự đánh dấu toàn bộ kế hoạch test đạt |
| MRM-DONE-016 | Chưa xác định | Bổ sung và chạy thử chức năng khoa/phòng | Đã có module department; người dùng xác nhận chạy thử thành công và dừng phiên chạy. Chưa có kết quả chi tiết từng ca kiểm thử |

## 5. Mốc quan sát kỹ thuật có timestamp từ log

Đây là thời điểm sự kiện xảy ra trong một phiên chạy, không nhất thiết là lần đầu chức năng được hoàn thành.

| Thời gian (+07:00) | Sự kiện quan sát được | Ý nghĩa và giới hạn |
|---|---|---|
| 14:51:58.963 | Flyway báo schema `public` ở version 3, không còn migration cần chạy | Log trước đó xác nhận 3 migration hợp lệ; không suy ra đây là thời điểm chúng được áp dụng lần đầu |
| 14:51:59.949 | `Started MedicalRecordApplication` trên cổng 8080, profile `dev` | Ứng dụng khởi động được; chưa chứng minh trang login render thành công |
| 14:52:03.358 | Render login lỗi tại `SpringActionTagProcessor`, dòng 357 | Ghi lại sự cố dẫn tới mốc sửa lỗi MRM-DONE-010; không phải mốc hoàn thành |

Trong phiên log trên, Flyway ghi URL `jdbc:postgresql://127.0.0.1:5433/mrm` và server PostgreSQL 18.6. Báo cáo ngày 18/10 ghi server 16.5 của phiên kiểm tra khi đó; giữ nguyên hai quan sát theo thời điểm/điểm kết nối, không tự kết luận đã nâng cấp server.

## 6. Mẫu bổ sung cho các công việc tiếp theo

Khi người dùng yêu cầu cập nhật nhật ký, sử dụng mẫu:

```text
Mã mốc: MRM-DONE-...
Ngày hoàn thành: DD/MM/YYYY hoặc Chưa xác định
Giờ hoàn thành: HH:mm:ss +07:00 hoặc Chưa ghi nhận
Ngày ghi nhận: DD/MM/YYYY
Nội dung:
Phạm vi hoàn thành: tài liệu / giao diện / triển khai / kiểm thử / xác nhận
Người thực hiện hoặc xác nhận:
File hoặc chức năng liên quan:
Lệnh kiểm tra nếu có:
Kết quả và bằng chứng:
Giới hạn hoặc công việc còn lại:
```

Không ghi mật khẩu, token hoặc dữ liệu bệnh nhân thật vào nhật ký. Nhật ký này phục vụ quản lý tiến độ dự án; audit log nghiệp vụ của ứng dụng là chức năng riêng.
