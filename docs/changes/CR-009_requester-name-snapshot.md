# CR-009 — Lưu họ tên người yêu cầu đọc

**Điều chỉnh theo làm rõ của người dùng ngày — V12:** người dùng muốn giá trị lưu trong bảng là username như `reader1`, không phải display_name như `Reader Demo`. Cột `requester_name_snapshot` được giữ tên để tương thích truy vấn nhưng từ V12 có nghĩa là **tên đăng nhập người yêu cầu tại thời điểm gửi**. V12 cập nhật các yêu cầu cũ từ `app_users.username`; code gửi mới cũng SELECT username. Không thay display_name của tài khoản, requester_id hoặc quyết định/lịch đọc. Các mô tả lấy display_name và kết quả V11 bên dưới là lịch sử, đã được thay thế ở phần này.

V12 đã áp dụng vào mrm/public. Kiểm tra 2/2 yêu cầu có requester_name_snapshot khớp username tài khoản; giữ 15 hồ sơ. Build thành công, FR 206/206 và bootstrap 26/26 đạt. Log lần điều chỉnh tại [bằng chứng username](../verification/evidence/requester-username-2026-10-22/). Người dùng cần khởi động lại phiên dev để yêu cầu gửi tiếp theo dùng code mới.

Ngày. Theo yêu cầu bổ sung thông tin ai là người xin đọc trong database.

`reading_requests.requester_id` đã lưu tài khoản gửi, là FK tới app_users. Bổ sung `requester_name_snapshot VARCHAR(200) NOT NULL`: họ tên hiển thị tại thời điểm gửi. Tên này lấy ở backend từ chính tài khoản đã xác thực, không lấy từ form, không đổi khi sửa mục đích/ngày đọc hoặc khi Admin đổi tên tài khoản. Yêu cầu mới sau đổi tên sẽ lấy tên mới.

Theo điều chỉnh tiếp theo của người dùng, danh sách/chi tiết yêu cầu chỉ hiển thị username (ví dụ reader1), không hiển thị Reader Demo. Snapshot họ tên vẫn lưu trong database để giữ lịch sử. Báo cáo yêu cầu và phiên đọc thêm cột requester_name_snapshot. Tên hiển thị không thay thế UUID trong phân quyền; tên có thể trùng, requester_id vẫn là định danh chính xác.

Không thêm tên trùng lặp vào reading_sessions/request_events/notifications: những bảng này đã liên kết yêu cầu qua request_id. request_events.actor là người thực hiện từng thao tác (có thể là Archivist hoặc Director), không được ghi đè bằng người yêu cầu. Tương tự handed_by/returned_by vẫn là người giao/nhận thực tế.

Java migration V11 thêm cột và điền tên cho yêu cầu đã có từ display_name hiện tại lúc nâng cấp. **Đối với yêu cầu trước V11, không thể khẳng định đó là tên tại ngày gửi nếu tài khoản từng đổi tên trước nâng cấp.** V1–V10 được giữ nguyên; schema tiếp theo dùng V12 trở lên.

Truy vấn kiểm tra:

```sql
SELECT q.id, q.requester_id, q.requester_name_snapshot,
 u.username, u.display_name AS current_account_name,
 q.state, q.created_at
FROM reading_requests q
JOIN app_users u ON u.id = q.requester_id
ORDER BY q.created_at DESC;
```

Để truy người xin đọc từ lịch đọc:

```sql
SELECT s.id, s.reading_on, q.requester_id,
 q.requester_name_snapshot, u.username
FROM reading_sessions s
JOIN reading_requests q ON q.id = s.request_id
JOIN app_users u ON u.id = q.requester_id;
```

Kết quả kiểm chứng được bổ sung ở cuối tài liệu; log tại [bằng chứng](../verification/evidence/requester-snapshot-2026-10-22/). Sau cập nhật cần khởi động lại ứng dụng dev để dùng Java/template mới. Không sửa Note.

## Kết quả thực hiện

Build package thành công. Bộ FR đạt **206/206**, bootstrap/chính sách đạt **26/26** trong các schema tạm đã dọn; tổng **232 kiểm tra** ở lần sửa này, không cộng các bộ kiểm thử của lần trước. Kiểm tra mới xác nhận UUID người gửi, tên tại thời điểm gửi, tên không đổi sau Admin đổi tên hoặc Reader sửa yêu cầu, yêu cầu mới lấy tên mới và lịch đọc truy người gửi qua FK.

V11 áp dụng vào `mrm/public` . Kiểm tra sau migration: 15 hồ sơ, 2 yêu cầu, cả 2 có snapshot không rỗng và khớp tên tài khoản hiện tại khi nâng cấp. Không thay trạng thái yêu cầu hay tài khoản. Phiên ứng dụng phụ phục vụ xác minh đã được dừng; cần khởi động lại phiên dev của người dùng.

