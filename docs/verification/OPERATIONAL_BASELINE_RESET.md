# Dọn dữ liệu thử và thiết lập tài khoản sử dụng

**Cập nhật sau báo cáo này:** Giám đốc đã đổi từ Lê Văn Minh sang Tạ Đức Mạnh, username `manh.td24102003@mrm.com`, và ba nhân viên phải tự thiết lập mật khẩu lần đầu. Bảng bên dưới ghi nhận đúng trạng thái tại thời điểm reset ban đầu. Thông tin đang sử dụng xem [báo cáo V23](INITIAL_CREDENTIAL_SETUP.md).

Ngày thực hiện:. Database: `mrm`, schema `public`, PostgreSQL 18.6 tại `127.0.0.1:5433`.

Phạm vi theo xác nhận của người dùng: giữ bệnh án, danh mục, chính sách và cấu trúc kho; đổi mã DEMO; xóa giao dịch thử; giữ nguyên tài khoản Admin; tạo một tài khoản mới cho mỗi vai trò còn lại. Tên đăng nhập theo tên không dấu, chữ cái đầu họ/tên đệm và ngày sinh; mật khẩu riêng, không dùng công thức ngày sinh.

## Tài khoản

| Vai trò | Họ tên giả lập | Ngày sinh | CCCD theo quy ước dự án | Tên đăng nhập |
|---|---|---|---|---|
| Bác sĩ | Nguyễn Văn An | | 0000001001 | an.nv12041990@mrm.com |
| Nhân viên lưu trữ | Trần Thị Linh | | 0000001002 | linh.tt18091993@mrm.com |
| Giám đốc | Lê Văn Minh | | 0000001003 | minh.lv05031980@mrm.com |
| Quản trị viên | Giữ nguyên dữ liệu hiện tại | Giữ nguyên | Giữ nguyên | admin1 |

Mật khẩu ngẫu nhiên riêng dài 25 ký tự; Giám đốc có thêm mật khẩu cấp 2 riêng. File `.secrets/operational-accounts.txt` chứa thông tin đăng nhập mới, không đưa mật khẩu vào tài liệu Git. Cả bản ghi tài khoản Admin (gồm mật khẩu, tên, vai trò, trạng thái) đã được kiểm tra giữ nguyên bằng fingerprint trước/sau giao dịch. Ba nhân viên mới có bản ghi `employees` liên kết tài khoản; thông tin giả lập dùng để vận hành dự án học tập.

## Dữ liệu giữ lại và xóa

Giữ đủ **16 bệnh án**, gồm 15 hồ sơ mẫu và một hồ sơ đã khai báo, UUID và các liên kết danh mục/vị trí không đổi. Giữ 5 khoa, 6 loại bệnh án, 4 chính sách, 8 vị trí và 11 snapshot chính sách bệnh án. Giữ cờ bản giấy/điện tử, tử vong và thông tin người bệnh. Các hồ sơ giả lập vẫn có tên người bệnh giả lập; việc giữ các mẫu này đã được người dùng xác nhận.

Đổi 15 mã `DEMO-MRM-001`…`015` thành **MRM-REC-000002**…**MRM-REC-000016**. Mã `MRM-REC-000001` đã có được giữ nguyên. Số lưu trữ giấy dùng bộ đếm riêng `YYYY-MM-NNNNNN`; các số DEMO được chuyển thành số chuẩn mới, không đặt lại bộ đếm. Mã bệnh án/mã người bệnh còn mang tiền tố DEMO cũng được chuyển sang mã nội bộ không trùng. Mã người bệnh thay thế chỉ là định danh của mẫu, không suy luận hay gộp danh tính bệnh nhân.

Bảng đối chiếu từng UUID/mã cũ/mã mới: `target/verification/operational-record-code-mapping.csv`.

| Bảng | Trước | Sau |
|---|---|---|
| app_users | 5 | 4 |
| employees | 1 | 3 |
| reading_request_batches | 9 | 0 |
| reading_requests | 10 | 0 |
| reading_sessions | 4 | 0 |
| request_events | 30 | 0 |
| notifications | 36 | 0 |
| electronic_record_events | 5 | 0 |
| record_events | 36 | 0 |
| record_location_history | 17 | 0 |
| location_events | 8 | 0 |
| catalog_events | 16 | 0 |
| audit_events | 291 | 1 |
| inventory_batches / inventory_items | 0 / 0 | 0 / 0 |

Một audit mới ghi nhận thao tác reset, không chứa mật khẩu. Tài khoản cũ `reader1`, `reader2`, `archivist1`, `director1` đã xóa; hồ sơ nhân viên cũ được xóa theo liên kết. Tham chiếu tác giả/người kiểm tra/người duyệt của bệnh án và danh mục được chuyển sang tài khoản mới cùng vai trò để tránh khóa ngoại mồ côi. Đây là thiết lập lại bộ dữ liệu học tập, không khẳng định nhân viên mới thực hiện các thao tác lịch sử. Ghi chú xét duyệt cũ trên bệnh án được xóa; nội dung bệnh án và trạng thái đã công bố được giữ.

Sau reset, không còn đăng ký, lịch giao–nhận hay thông báo thử. Hồ sơ có trạng thái đang đọc sẽ được đưa về trong kho khi xóa lượt mượn; tại thời điểm thực hiện không có hồ sơ ở trạng thái đang đọc. Dữ liệu phát sinh sau reset được lưu bình thường, không có tác vụ tự động xóa khi khởi động.

## Thực hiện và bảo vệ dữ liệu

1. Kiểm kê bảng và tất cả khóa ngoại tới tài khoản.
2. Tạo bản sao lưu đầy đủ bằng `pg_dump -format=custom`: `target/verification/mrm-before-operational-reset.dump` (134.587 byte), kiểm tra danh mục bằng `pg_restore -list`.
3. Phát hiện checksum V21 có sẵn là `822223776`, khác mã nguồn hiện tại `-896787899`. Đã xác minh cột `deleted_at`, ràng buộc `notification_deleted_time` và chỉ mục `ix_notifications_visible` đúng cấu trúc V21, chạy lại backfill idempotent (0 dòng mới), rồi đồng bộ **riêng V21**. Không repair các migration khác. Bản sao lưu chứa cả lịch sử trước đồng bộ.
4. Áp dụng migration Java **V22__AllowEmailUsernames** để đồng bộ tên đăng nhập dạng email ở DB với Java/UI, vẫn chấp nhận tài khoản cũ.
5. Diễn tập toàn bộ reset trong giao dịch và rollback, không lưu tài khoản thử hay mật khẩu của lần diễn tập.
6. Áp dụng trong một giao dịch với khóa độc quyền các bảng liên quan, có thời hạn chờ khóa. Tạm mở đúng hai trigger bảo vệ danh mục khoa và mã hồ sơ để chuyển tham chiếu/mã theo yêu cầu, sau đó bật lại trước commit. Không vô hiệu hóa khóa ngoại.
7. Kiểm tra số bệnh án, số vai trò, mật khẩu băm, mã không trùng, không còn DEMO/giao dịch cũ và ràng buộc mã bất biến trước commit. Lưu mật khẩu cục bộ trước commit, đánh dấu đã commit sau thành công.
8. Đọc lại dữ liệu sau commit: xác thực mật khẩu qua encoder thật, nạp quyền qua `DatabaseUserDetailsService`, so khớp tên tiếng Việt/ngày sinh/CCCD nhân viên và kiểm tra các bảng lịch sử đã dọn.

Công cụ Java chỉ chạy khi gọi tường minh: `src/test/java/mrm/features/OperationalBaselineMaintenance.java`; mặc định chỉ kiểm kê, `-rehearse` hoàn tác, `-apply <backup.dump>` yêu cầu bản sao lưu. Bảng `data_reset_history` đánh dấu lần reset để ngăn chạy lại xóa công việc mới và ngăn cả hai điểm seed demo. File backup và mật khẩu nằm trong thư mục được `.gitignore` loại trừ. Không xóa `target/verification` nếu còn cần bản sao lưu.

## Kiểm thử

- Build offline thành công: `target/verification/operational-reset-build.log`.
- **726/726** kiểm tra chức năng/HTTP trên schema tạm thành công, gồm tạo tài khoản theo công thức email, từ chối tên sai và ngăn bootstrap/initializer tái tạo demo khi có dấu reset. Schema tạm đã xóa sau kiểm tra: `target/verification/operational-reset-functional.log`.
- **16/16** kiểm tra nâng cấp dữ liệu từ V14 đến V22 đạt; schema kiểm thử đã xóa: `target/verification/operational-reset-upgrade.log`.
- Diễn tập rollback: `target/verification/operational-reset-rehearsal.log`.
- Áp dụng và kiểm tra sau commit: `target/verification/operational-reset-applied.log`, `target/verification/operational-reset-verified.log`.
- Kiểm kê sau cùng xác nhận 16 bệnh án, 4 tài khoản, 3 nhân viên, không có yêu cầu/lịch mượn/thông báo: `target/verification/operational-reset-inventory.log`.
- Đối chiếu V21 và migration V22: `target/verification/operational-reset-v21-reconciliation.log`, `target/verification/operational-reset-migration.log`.

## Sử dụng tiếp

Khởi động lại ứng dụng bằng profile `dev` để dùng JAR/classes mới. Đăng nhập bằng tài khoản mới trong file mật khẩu, Admin tiếp tục mật khẩu cũ. Phiên của tài khoản đã xóa sẽ hết hiệu lực ở yêu cầu tiếp theo. Bác sĩ tra cứu 16 hồ sơ bằng mã mới, tạo yêu cầu mới; nhân viên lưu trữ và Giám đốc sẽ nhận thông báo theo quy trình hiện tại. Có thể đổi mật khẩu từ menu cá nhân. Thông tin tài khoản mới thay thế các tên demo được nhắc trong tài liệu kiểm thử cũ; không sửa lịch sử báo cáo kiểm thử trước đây.
