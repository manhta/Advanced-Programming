# Mật khẩu lần đầu và cập nhật thông tin Giám đốc

Ngày:. Migration Java: **V23__RequireInitialCredentialSetup**.

## Kết quả

Giám đốc đã đổi thành **Tạ Đức Mạnh**, ngày sinh ****, tên đăng nhập **`manh.td24102003@mrm.com`**. Cập nhật cả `app_users.display_name`, `username`, `email` và `employees.full_name`, `birth_date`; giữ UUID, vai trò, CCCD giả lập `0000001003` và các liên kết nghiệp vụ. Phiên của tên cũ hết hiệu lực. Không xóa thêm bệnh án hay giao dịch hiện có.

Ba tài khoản nhân viên vừa tạo đã được bật yêu cầu thiết lập ban đầu: Bác sĩ Nguyễn Văn An, Nhân viên lưu trữ Trần Thị Linh và Giám đốc Tạ Đức Mạnh. Admin hiện có được giữ nguyên vì đã sử dụng trước đó. Tài khoản được tạo mới, bao gồm Admin, mặc định phải thiết lập lần đầu. Migration giữ trạng thái tài khoản cũ; công cụ bảo trì chỉ bật cờ cho đúng ba nhân viên theo yêu cầu lần này.

Mật khẩu đăng nhập hiện có tiếp tục dùng để vào lần đầu, không tự đổi sang mật khẩu mới do hệ thống chọn. File `.secrets/operational-accounts.txt` đã cập nhật tên/ngày sinh/username Giám đốc và đánh dấu mật khẩu ban đầu phải thay. Mật khẩu cấp 2 cấp sẵn của Giám đốc đã xóa khỏi database; phải tự tạo mới trên giao diện. Mật khẩu do người dùng chọn sau này không ghi vào file hay nhật ký.

## Cách sử dụng

1. Khởi động lại ứng dụng bằng profile `dev`, mở `/login`.
2. Đăng nhập bằng tên tài khoản hiện tại và mật khẩu ban đầu trong file tài khoản.
3. Hệ thống chuyển đến `/account/initial-setup`. Nhập **Mật khẩu mới** và **Xác nhận mật khẩu mới**, không có ô mật khẩu cũ.
4. Riêng Giám đốc, nhập thêm **Mật khẩu cấp 2** và **Xác nhận mật khẩu cấp 2**.
5. Mỗi mật khẩu dài 12–128 ký tự. Mật khẩu đăng nhập mới phải khác mật khẩu ban đầu. Mật khẩu cấp 2 khác cả mật khẩu đăng nhập mới và mật khẩu ban đầu.
6. Chọn **Lưu và tiếp tục**. Chỉ khi mọi trường hợp lệ mới lưu, chuyển đến trang chủ theo vai trò. Nếu sai, thông báo lỗi hiện trên trang; chưa có thay đổi mật khẩu dở dang.
7. Những lần đăng nhập sau dùng mật khẩu đã tự chọn. Trang lần đầu không còn được dùng để đổi mật khẩu. Đổi mật khẩu bình thường tại menu cá nhân tiếp tục yêu cầu mật khẩu hiện tại.

Có nút Đăng xuất trên màn hình thiết lập. Người dùng không thể bỏ qua màn hình bằng URL nghiệp vụ trực tiếp hoặc gửi form tới endpoint khác. Nếu Director đã hoàn tất mật khẩu đăng nhập nhưng chưa có mật khẩu cấp 2 (ví dụ vừa được đổi vai trò), hệ thống chỉ yêu cầu thiết lập mật khẩu cấp 2 trước khi tiếp tục.

## Nguyên lý và file liên quan

- `database/migration/V23__RequireInitialCredentialSetup.java`: thêm `app_users.must_change_password`; tài khoản cũ ban đầu FALSE, mặc định các INSERT mới TRUE.
- `config/SecurityConfig.java`: sau đăng nhập kiểm tra cờ hoặc Director thiếu mật khẩu cấp 2, điều hướng tới màn hình thiết lập; endpoint thiết lập yêu cầu xác thực, vẫn giữ CSRF.
- `config/AccountStateFilter.java`: kiểm tra trạng thái/phiên/role, sau đó chặn tất cả đường dẫn nghiệp vụ khi chưa hoàn thành. Chỉ cho phép màn hình thiết lập, tài nguyên CSS/JS/ảnh và đăng xuất.
- `account/PersonalAccountController.java`: GET/POST `/account/initial-setup`, luôn lấy người dùng từ Authentication, không nhận ID tài khoản đích từ form. Lỗi chỉ giữ thông báo, không đưa giá trị mật khẩu trở lại HTML/flash.
- **`resources/templates/account/initial-setup.html`**: giao diện thiết lập riêng cho mọi vai trò, trường cấp 2 chỉ hiện với Giám đốc cần thiết lập.
- `account/AccountService.java`: khóa dòng tài khoản trong transaction, kiểm tra cờ/role, độ dài/xác nhận/khác mật khẩu, băm bằng encoder hiện có, cập nhật hai mật khẩu và cờ một lần. Tăng `version`, giữ phiên hiện tại với version mới, thu hồi các phiên cũ ở yêu cầu kế tiếp. Từ chối gọi lại phương thức thiết lập nếu đã hoàn tất. Admin cấp lại mật khẩu bật cờ thay mật khẩu và xóa mật khẩu cấp 2 theo cơ chế cũ.
- `config/DevelopmentMigrationConfig.java`: tài khoản Archivist vừa bootstrap trước V4 được đánh dấu lần đầu sau khi hoàn tất migration. Database đã reset vẫn giữ chốt không seed demo trở lại.
- `features/DirectorIdentityMaintenance.java` trong test: công cụ tường minh cập nhật dữ liệu hiện tại, có chế độ diễn tập rollback, không chạy lúc khởi động ứng dụng. Dấu `data_reset_history.DIRECTOR_IDENTITY_20261026` chặn chạy lại để không xóa thiết lập do người dùng đã chọn.

Sau khi tên đăng nhập đổi, lịch sử cũ có actor dạng chuỗi vẫn ghi tên ở thời điểm phát sinh; không sửa ngược nhật ký. Các liên kết dùng UUID tiếp tục hoạt động. Màn hình thông tin hiện tại lấy tên nhân viên mới.

## Sao lưu và áp dụng

- Sao lưu database trước V23: `target/verification/mrm-before-initial-setup.dump`.
- Áp dụng V23: `target/verification/initial-setup-migration.log`.
- Diễn tập rollback, kiểm tra tên/ngày sinh/nhân viên và fingerprint Admin: `target/verification/director-identity-rehearsal.log`.
- Giao dịch cập nhật dữ liệu thật và file tài khoản: `target/verification/director-identity-applied.log`.
- Kiểm tra sau commit: `target/verification/director-identity-verified.log` xác nhận tên tiếng Việt/ngày sinh, ba mật khẩu ban đầu khớp bản băm, cờ thiết lập bật, cấp 2 chưa được tạo và tên đăng nhập Director cũ không còn. Kiểm kê `target/verification/initial-setup-inventory.log` xác nhận vẫn 16 bệnh án, 4 tài khoản, 3 nhân viên; audit tăng từ 1 lên 2 do ghi nhận lần cập nhật này, không phát sinh yêu cầu mượn/thông báo.
- Bản file tài khoản trước đổi tên lưu riêng tại `.secrets/operational-accounts-before-director-update.txt`, chỉ dùng đối chiếu lịch sử. Mật khẩu cấp 2 cũ trong bản này đã mất hiệu lực.

Không đưa file mật khẩu hoặc bản sao lưu vào Git. `Note` không được chỉnh sửa trong lần cập nhật này.

## Kiểm thử

**99/99 kiểm tra thiết lập ban đầu đạt**, dùng server HTTP và PostgreSQL schema tạm qua `InitialCredentialSetupCheck`:

- Cả bốn vai trò bị chặn mở trang nghiệp vụ trước thiết lập, kể cả truy cập URL trực tiếp hoặc gửi sang endpoint đổi mật khẩu thông thường.
- Người chưa đăng nhập không vào trang thiết lập; gửi sai CSRF nhận 403.
- Form không có trường mật khẩu cũ; chỉ Director có trường cấp 2.
- Mật khẩu ngắn, trùng mật khẩu ban đầu hoặc xác nhận sai không làm hoàn tất cờ.
- Director thiếu cấp 2, cấp 2 trùng mật khẩu đăng nhập/ban đầu hoặc xác nhận sai không đổi dở dang mật khẩu đăng nhập.
- Hoàn tất hợp lệ: mở được trang chủ, mật khẩu ban đầu không đăng nhập được, đăng nhập tiếp bằng mật khẩu mới thành công, phiên đồng thời cũ bị thu hồi.
- Form lần đầu gửi lại không thể đổi mật khẩu của tài khoản đã hoàn tất; đổi mật khẩu thông thường vẫn kiểm tra mật khẩu cũ.
- Admin tạo tài khoản và cấp lại mật khẩu đều bật cờ cần đổi. Director chỉ thiếu cấp 2 được yêu cầu riêng cấp 2, không đổi lại mật khẩu đăng nhập.
- Mật khẩu không xuất hiện trong chi tiết audit; schema tạm được xóa sau kiểm thử.

Log: `target/verification/initial-setup-http.log`.

**726/726 kiểm tra hồi quy** đạt: `target/verification/initial-setup-regression.log`. Các kịch bản hồi quy cũ đặt trạng thái tài khoản fixture thành đã thiết lập để tiếp tục kiểm tra nghiệp vụ; chốt bắt buộc lần đầu được kiểm tra riêng bằng HTTP thật trong bộ 99 kiểm tra phía trên, không sửa trạng thái để bỏ qua.

**16/16 kiểm tra nâng cấp V14 → V23** đạt: `target/verification/initial-setup-upgrade.log`. Build thành công: `target/verification/initial-setup-build.log`.
