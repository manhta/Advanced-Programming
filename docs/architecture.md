# Kiến trúc thiết kế — chưa triển khai

## Thành phần

Trình duyệt → Spring Security → Controller → Service nghiệp vụ → Repository/JPA → PostgreSQL 16.5. Thymeleaf render HTML; Bootstrap/CSS/JavaScript hỗ trợ tương tác. Maven quản lý thư viện/build, không xử lý request lúc vận hành. Một ứng dụng, một cơ sở dữ liệu cho phiên bản đầu; chưa cần microservices/Redis/queue riêng.

Giữ package `mrm`, file/class tiếng Anh. Có thể đặt các lớp cùng package khi ít; tách theo nghiệp vụ khi cần, không tạo lớp/interface rỗng chỉ để đủ sơ đồ. Resources chứa template/static/cấu hình; source Java không chứa bí mật hoặc file bệnh án.

## Trách nhiệm

| Thành phần | Nhiệm vụ | Không làm |
|---|---|---|
| Controller | Nhận Form/criteria, kiểm tra đầu vào, gọi service, trả view | Không tự cập nhật trạng thái/SQL nghiệp vụ |
| Service | Quyền bản ghi, transition, transaction, version, audit | Không tin actor/time/state từ trình duyệt |
| Repository | Truy vấn đã giới hạn scope, khóa/ràng buộc theo thiết kế | Không trả toàn bộ dữ liệu để UI tự lọc quyền |
| Entity | Quan hệ/lưu trữ | Không dùng trực tiếp làm form hoặc trả mọi trường cho view |
| DTO/Form | Tập trường đúng quyền/validation | Không cho gán role/status/owner tùy ý |
| Audit/Outbox | Sự kiện cùng giao dịch hoặc lịch gửi lại | Không đảo quyết định chỉ vì email lỗi |

## Biên giao dịch

- Tạo revision: request/current revision + snapshot/ngày + audit + receipt thao tác.
- Duyệt: request version + chứng cứ xác thực + quyền hiện hành + decision + sessions PLANNED + audit + receipt. UNIQUE một quyết định cuối mỗi revision; không hai người ghi đè.
- Xác nhận: kiểm tra decision/revision + chỗ HSBA/ngày + sessions + notification/outbox + audit. Xung đột một ngày trong tập gửi thì rollback tập; có thể gửi lại tập ngày nhỏ hơn rõ ràng.
- Giao: khóa HSBA và session theo thứ tự cố định; kiểm tra bất biến + handover mở + physical/session/audit/receipt.
- Trả: khóa theo cùng thứ tự HSBA → session → handover; đóng lượt + location/history + condition/exception + audit/receipt.
- Quản lý quyền: cập nhật, thu hồi, audit; lúc ghi quyết định kiểm tra vai trò DIRECTOR/trạng thái tài khoản hiện tại. Không chỉ cache quyền từ lúc login.

Mọi transaction ngắn; không chờ người dùng nhập mật khẩu hoặc thao tác vật lý khi đang giữ khóa. Kiểm tra vật lý trước, kiểm tra dữ liệu lại lúc commit. Thất bại rollback; lỗi mạng sau commit truy bằng operation key.

## Khóa và idempotency

Version lạc hậu cho biểu mẫu báo conflict. Khóa/ràng buộc bảo vệ giao đồng thời; giới hạn một lượt mở là ràng buộc CSDL ngoài kiểm tra service. Operation receipt unique(actor, action, key), giữ payload fingerprint và result reference, liên kết cùng transaction; khác payload cùng key bị chặn, không ghi mật khẩu trong fingerprint. UUID mới không được dùng để vô tình tạo một retry mới.

## Cấu hình và triển khai sau này

Profile dev/test/prod, bí mật qua biến môi trường; `.env` chỉ là quy ước khi có cơ chế nạp. Flyway quản lý schema; không dùng tự động thay schema trên môi trường chung. Migration đã áp dụng không sửa. Truy cập CSDL bằng tài khoản ứng dụng giới hạn quyền, vận hành backup tách biệt.

Cập nhật: theo yêu cầu người dùng, các migration triển khai dùng Java kế thừa BaseJavaMigration trong `mrm.database.migration`, không dùng file `.sql`. SQL nằm trong text block Java, Flyway giữ thứ tự/lịch sử và sử dụng connection của Context. Xem [java-migrations.md](design/java-migrations.md). Chỉ kiểm tra compile trong lượt chuyển đổi; chưa chạy database migration.

## Tìm kiếm và chỉ mục

Đầu tiên hỗ trợ mã chính xác/prefix, tên có dấu không phân biệt hoa thường, filter khoa/ngày/trạng thái, phân trang có tie-break ID. Không cam kết tìm không dấu khi chưa chọn giải pháp. Chỉ mục đề xuất: khóa nghiệp vụ, mã bệnh nhân/mã bệnh án, request(owner,state), session(day,state), handover mở, audit(time,actor,target), location parent. Đánh giá query plan sau khi có dữ liệu, không xây index mọi cột.

## Thiết kế giao diện và bảo mật

Dùng session cho web cùng origin, CSRF với thao tác ghi, encode dữ liệu trong template, cookie phù hợp HTTPS. Không log mật khẩu/token; không lưu nội dung bệnh án. Lỗi hiển thị mã correlation thay stacktrace. Trạng thái hành động từ server, ẩn nút chỉ hỗ trợ trải nghiệm. Quy tắc cấp 2 và các ngưỡng chỉ có hiệu lực sau D-10/22 được xác nhận.
