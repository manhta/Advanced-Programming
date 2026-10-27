# Form tài khoản chung và bảng nhân viên —

Phạm vi tại [CR-010](../changes/CR-010_unified-accounts-and-employees.md). Form chung tạo đồng thời tài khoản và thông tin nhân viên; không còn nút Tạo Reader/Tạo Archivist riêng.

## Bảng employees

| Cột | Kiểu/điều kiện | Ý nghĩa |
|---|---|---|
| citizen_id | VARCHAR(10), PK, đúng 10 chữ số, không trùng | ID căn cước theo yêu cầu dự án; lưu chuỗi để giữ số 0 đầu |
| account_id | UUID, UNIQUE, NOT NULL, FK app_users.id | Tài khoản liên kết |
| full_name | VARCHAR(200), bắt buộc | Tên nhân viên |
| birth_date | DATE, bắt buộc | Ngày sinh; backend không nhận ngày tương lai hoặc trước 1800 |
| gender | MALE / FEMALE / OTHER | Nam / Nữ / Khác |
| role | FK roles.code | READER / ARCHIVIST / DIRECTOR / ADMIN |

Quy tắc căn cước 10 chữ số được thực hiện đúng theo yêu cầu cho dự án này. Không coi dữ liệu thỏa quy tắc độ dài là căn cước đã được xác minh. Không lưu căn cước dưới kiểu số.

Một nhân viên gắn với một tài khoản; ID căn cước không thay UUID của app_users. Tên và role của employees phải khớp tài khoản: trigger kiểm tra hồ sơ khi INSERT/UPDATE, trigger trên app_users đồng bộ tên/role khi sửa tài khoản, kể cả sửa qua câu SQL. Vai trò vẫn do tài khoản quyết định khi đăng nhập, không có hai nguồn phân quyền độc lập.

## Cách dùng

1. Admin vào **Tạo tài khoản** hoặc **Tài khoản**. Điền username, tên nhân viên, căn cước 10 số, ngày sinh, giới tính, role và mật khẩu 12–128 ký tự; khoa/email tùy chọn.
2. Chọn **Bác sĩ** hoặc **Nhân viên lưu trữ** theo nhiệm vụ; Giám đốc/Quản trị viên vẫn thuộc bốn role hiện hữu.
3. Bấm Tạo tài khoản. Backend tạo app_users và employees trong một transaction; lỗi trùng username/căn cước hoặc lỗi lưu hồ sơ sẽ rollback cả tài khoản mới và audit trong giao dịch đó. Mật khẩu được băm.
4. Tại danh sách tài khoản, Admin xem/sửa căn cước, ngày sinh và giới tính bên cạnh thông tin tài khoản. Thay tên hoặc role sẽ đồng bộ sang employees. Không xóa hồ sơ nhân viên khi khóa tài khoản.
5. Tài khoản cũ hiển thị **Chưa có hồ sơ nhân viên**. Điền đủ ba trường căn cước/ngày sinh/giới tính rồi lưu để bổ sung. Nếu chỉ quản trị trạng thái/mật khẩu, để trống cả ba. Không yêu cầu nhập bù dữ liệu giả để tiếp tục đăng nhập.

Khi sửa, nếu cung cấp một trường nhân viên thì phải cung cấp đầy đủ cả ba; hồ sơ có sẵn không bị xóa khi gửi trống cả ba trường. Chỉ Admin truy cập form/API này. Đường dẫn nghiệp vụ và Reader vẫn không có quyền xem danh sách nhân viên/căn cước. Audit không ghi mật khẩu, số căn cước hoặc ngày sinh.

## Quan hệ với các bảng khác

app_users tiếp tục lưu dữ liệu đăng nhập và vai trò. reading_requests.requester_id vẫn trỏ tới app_users; từ tài khoản đó có thể liên kết employees.account_id. requester_name_snapshot tiếp tục lưu username theo quyết định V12; không chuyển lại sang họ tên nhân viên. reading_sessions/request_events/notifications không nhân bản thông tin căn cước/ngày sinh.

Ví dụ truy vấn cho người quản trị database:

```sql
SELECT e.citizen_id, e.full_name, e.birth_date, e.gender, e.role, u.username
FROM employees e
JOIN app_users u ON u.id = e.account_id
ORDER BY u.username;
```

V13 không sửa V1–V12, không tự tạo hồ sơ nhân viên cho tài khoản cũ. Thay đổi schema tiếp theo dùng V14 trở lên. Khởi động lại ứng dụng dev để dùng form và xử lý Java mới. Các kiểm tra dùng dữ liệu nhân viên giả trong schema riêng và không tạo căn cước thử vào database thật. Kết quả kiểm chứng hoàn tất được bổ sung ở cuối báo cáo.

## Kết quả kiểm chứng

Build package thành công. Bộ FR **227/227** và bootstrap **26/26** đạt, tổng **253 kiểm tra** trong lần sửa này. Đây là các chương trình Java chạy trực tiếp, không phải số test JUnit trong lệnh package. Các schema tạm đã dọn.

Các kiểm tra mới gồm: mã căn cước sai không tạo tài khoản; ngày sinh tương lai bị chặn; Reader không tạo nhân viên; giữ số 0 đầu ID; trùng ID rollback tài khoản; sửa tên/role tài khoản đồng bộ hồ sơ; SQL sửa role hồ sơ trái tài khoản bị chặn; Admin cập nhật ngày sinh/giới tính; audit không chép riêng căn cước vào details; form HTTP chung lưu cả tài khoản và nhân viên; nhãn tiếng Việt render đúng. Các ca đăng nhập, quyền, CSRF, workflow, notification, báo cáo trong bộ FR cũng đạt.

V13 đã áp dụng vào `mrm/public` lúc **08:46:56 **. Kiểm kê xác nhận 4 tài khoản, **employees = 0** (chưa nhập nhân viên thật), 15 hồ sơ, 2 yêu cầu đọc vẫn được giữ. Không bịa thông tin nhân viên cho tài khoản demo. Phiên ứng dụng phụ dùng xác minh đã dừng; khởi động lại phiên dev của người dùng để form mới hoạt động.

Log: [FR](evidence/employees-2026-10-22/employees-fr.txt), [bootstrap](evidence/employees-2026-10-22/employees-bootstrap.txt), [database sau nâng cấp](evidence/employees-2026-10-22/employees-inventory.txt). Giao diện được kiểm tra qua HTTP/template; lần này không tạo ảnh chụp trình duyệt.
