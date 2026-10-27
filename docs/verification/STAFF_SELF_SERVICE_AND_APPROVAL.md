# Tạo tài khoản tự động và đề nghị thay đổi nhân sự

**Cập nhật thay thế V25:** Lối vào đề nghị được gộp trong trang sửa thông tin cá nhân, bỏ sửa email; thông báo phê duyệt cho nhân viên chỉ gửi sau khi Admin áp dụng. Ô lý do chỉ hiện khi từ chối. Quy tắc mới và kiểm thử xem [báo cáo V25](STAFF_PROFILE_AND_NOTICE_TIMING.md). Nội dung bên dưới ghi nhận triển khai V24 trước điều chỉnh.

Ngày:. Migration Java **V24__CreateStaffChangeRequests**. Theo xác nhận của người dùng: thông báo nội bộ qua chuông; vai trò được tự đề nghị là Bác sĩ/Nhân viên lưu trữ; Giám đốc quyết định, Admin áp dụng. Không dùng SMTP hoặc email thật.

## Tạo và quản lý tài khoản

Admin không còn nhập username/mật khẩu trên form tạo. Nhập họ tên, căn cước đúng 10 chữ số theo quy ước dự án, ngày sinh, giới tính, vai trò và **khoa hợp lệ bắt buộc**; email liên hệ tùy chọn. Backend kiểm tra khoa kể cả khi bỏ qua HTML required.

Username: `[tên].[chữ cái đầu họ và tên đệm][ddMMyyyy]@mrm.com`, bỏ dấu, chữ thường. Ví dụ Nguyễn Văn An, → `an.nv12041990@mrm.com`. Nếu người khác trùng tên và ngày sinh, thêm `-2`, `-3` trước `@mrm.com`; khóa theo gốc tên và unique DB tránh phát sinh trùng. Mật khẩu riêng ngẫu nhiên dài 25 ký tự, chỉ lưu bản băm. Tài khoản vẫn phải tự thay mật khẩu lần đầu; Director phải tạo thêm cấp 2 theo V23.

Kết quả tạo hiển thị username/mật khẩu ban đầu trong phần bàn giao, có `Cache-Control: no-store`; không ghi vào audit/file tài khoản hay flash session. GET tạo mới không hiển thị lại mật khẩu. Admin cần lưu để bàn giao ngay; nếu mất, có thể cấp lại tại trang chỉnh sửa tài khoản theo cơ chế hiện có. Mật khẩu trong `.secrets/operational-accounts.txt` chỉ dành các tài khoản ban đầu đã cấp trước đây, không phải danh sách mật khẩu của tài khoản tạo mới qua web.

Căn cước kiểm tra trước lưu, đồng thời `employees.citizen_id` là khóa chính. Khi hai phiên tạo cùng căn cước, chỉ một phiên thành công; tài khoản của phiên lỗi bị rollback, không để lại app_users mồ côi. Lỗi hiển thị ngay trên form Admin, giữ các trường đã nhập. Danh sách quản lý có cột Họ tên liên kết tới trang sửa, cột Tên tài khoản là văn bản riêng.

## Tự sửa thông tin cá nhân

Nhân viên mở menu → Thông tin cá nhân → Chỉnh sửa thông tin cá nhân. Được sửa họ tên, căn cước, ngày sinh, giới tính và email. Chỉ sửa tài khoản đang xác thực, không nhận account ID đích từ form. Username giữ nguyên khi sửa tên/ngày sinh, tránh thay đường dẫn và danh tính đăng nhập đang dùng.

Phiên bản dòng tài khoản được kiểm tra để không ghi đè thay đổi ở tab khác. Căn cước trùng được báo lỗi và rollback cả thay đổi tên. Backend bỏ qua role/khoa bị gửi kèm vào form cá nhân. Vai trò/khoa cần dùng form đề nghị riêng. Phiên đang sửa được cập nhật version; phiên khác kiểm tra lại ở yêu cầu kế tiếp.

## Quy trình đề nghị

1. Nhân viên mở `/account/change-requests`, chọn vai trò đích READER/ARCHIVIST, một khoa hoạt động và nhập lý do.
2. Backend chụp vai trò/khoa hiện tại, tạo đơn `SUBMITTED`. Nếu không thay đổi gì, đã có đơn chờ, token bị dùng cho nội dung khác hoặc không có Giám đốc khác hoạt động thì từ chối gửi. Gửi lại cùng token/nội dung trả về cùng đơn.
3. Các Giám đốc hoạt động khác người gửi nhận việc qua chuông và trang `/director/staff-change-requests`. Người gửi chưa nhận thông báo kết quả ở bước này. Quyền/khoa chưa đổi.
4. Giám đốc mở đơn, chọn phê duyệt hoặc từ chối, xác thực mật khẩu cấp 2. Từ chối phải có lý do; kiểm tra cả HTML/JavaScript và backend. Không tự xét duyệt đơn của mình.
5. Nếu từ chối, đơn `REJECTED`, chỉ gửi kết quả từ chối và lý do cho nhân viên, không gửi Admin làm việc. Nếu phê duyệt, đơn `APPROVED`, nhân viên nhận đúng kết quả phê duyệt, còn Admin nhận việc cần cập nhật. Ghi chú phê duyệt nội bộ không được đưa vào nội dung thông báo kết quả cho nhân viên.
6. Admin mở `/admin/staff-change-requests` hoặc chuông, xem đúng vai trò/khoa được duyệt và bấm áp dụng. Không sửa mục đích đích ở nút áp dụng. Backend khóa đơn/tài khoản, kiểm tra phiên bản, trạng thái, tài khoản/khoa còn hoạt động và vai trò/khoa hiện tại còn khớp ảnh chụp lúc gửi.
7. Áp dụng thành công: đơn `APPLIED`; cập nhật app_users role/department, employees.role theo trigger, tăng version để phiên cũ hết hiệu lực. Nhân viên đăng nhập lại; thông báo kết quả riêng của họ còn xem được theo role mới, các thông báo xét duyệt của người khác thuộc quyền cũ tiếp tục bị ẩn. Không phát thêm kết quả thứ ba cho nhân viên ở bước áp dụng.

Mỗi nhân viên tối đa một đơn `SUBMITTED`/`APPROVED`; có thể gửi đơn mới sau từ chối/hoàn tất. Yêu cầu đã xử lý hoặc version cũ không áp dụng lại được. Trường hợp vai trò/khoa bị đổi ngoài quy trình trong lúc chờ, Admin bị chặn ghi đè và phải đối chiếu lại dữ liệu; không tự sửa baseline hoặc âm thầm đổi đơn. Trang sửa tài khoản Admin có sẵn vẫn là công cụ quản trị được phân quyền từ trước; luồng tự phục vụ của nhân viên không dùng công cụ này để tự cấp quyền.

Director muốn tự đề nghị chỉ được chọn vai trò đích trong phạm vi trên, phải có một Director hoạt động khác xét duyệt. Hệ thống chặn áp dụng làm mất Director hoạt động cuối cùng, có khóa tuần tự cho bước kiểm tra này.

## Thông báo nội bộ

Thông báo dùng chung bảng/chuông/UI hiện có. Director có việc khi đơn còn SUBMITTED; Admin có việc khi đơn còn APPROVED. Đọc thông báo không đồng nghĩa đã xử lý. Nhân viên chỉ nhận APPROVE/REJECT; kết quả chưa đọc được tính vào chuông và tab đang chờ, đọc xong chuyển sang đã xử lý. Từ chối có nội dung lý do bên dưới tiêu đề; mọi văn bản được escape HTML.

Admin chỉ nhận thông báo nhân sự, không nhận thông tin bệnh án. Thông báo nhân sự không dựng khối metadata hồ sơ/bệnh nhân. Xóa thông báo đã xử lý không xóa đơn/sự kiện; việc còn chờ không xóa được. Sau khi vai trò đổi, chỉ chuyển quyền xem thông báo về **đơn của chính nhân viên**, không chuyển thông báo xét duyệt của người khác.

## Database và mã nguồn

- `staff_change_requests`: requester_id, original_role/original_department_id, requested_role/requested_department_id, reason, state, version, người/thời điểm xét duyệt và áp dụng, token chống gửi lặp.
- `staff_change_events`: lịch sử SUBMIT/APPROVE/REJECT/APPLY, người thao tác và thời điểm.
- `notifications`: thêm staff_request_id/staff_event_id; ràng buộc chỉ nhận đúng một trong ba loại nguồn (mượn hồ sơ, bệnh án điện tử, nhân sự), khóa ngoại ghép đảm bảo sự kiện thuộc đúng đơn. Recipient ADMIN chỉ hợp lệ cho thông báo nhân sự.
- `app_users`/`employees` không thêm cột: tận dụng version, department_id và trigger đồng bộ họ tên/vai trò hiện có. Không sửa dữ liệu bệnh án/danh mục/kho khi áp dụng migration này.

Java chính: `account/AccountService.java`, `AccountController.java`, `PersonalAccountController.java`, `StaffChangeService.java`, `StaffChangeController.java`, `notification/NotificationService.java`, `config/SecurityConfig.java`.

HTML chỉnh giao diện: `templates/account/new.html`, `list.html`, `profile.html`, **`profile-edit.html`**, **`change-list.html`**, **`change-detail.html`**; navbar/menu chung `templates/fragments/layout.html`; chuông `templates/notification/inbox.html`.

## Kiểm thử

Build: `target/verification/staff-management-build.log`.

**86/86 kiểm tra mới** bằng `StaffManagementCheck`, server HTTP thật trên schema tạm:

- Sinh username đúng tên tiếng Việt/ngày sinh, mật khẩu thực tế đăng nhập được và bị yêu cầu đổi lần đầu, xử lý trùng username bằng suffix.
- Form không có hai ô thủ công, khoa bắt buộc ở browser/server, lỗi căn cước trùng ngay trên form/giữ dữ liệu, thiếu CSRF nhận 403.
- Gửi đồng thời cùng căn cước: đúng một tài khoản được tạo, không có tài khoản mồ côi.
- Link ở họ tên, username cột riêng; tự sửa hồ sơ giữ username và quyền/khoa, duplicate CCCD rollback toàn bộ, tab cũ bị chặn ghi đè.
- Đơn không tự đổi quyền, token idempotent, giới hạn đơn đang chờ, cấm xin Admin/Director, người khác không xem đơn, nhân viên không gọi endpoint xét duyệt.
- Lý do từ chối bắt buộc, sai cấp 2 không quyết định; Director duyệt chưa áp dụng quyền; Admin áp dụng đúng, employees đồng bộ, phiên cũ bị thu hồi.
- Chuông Director/Admin/nhân viên, link mở đúng đơn, kết quả nhân viên không lộ ghi chú phê duyệt nội bộ, từ chối có lý do và escape HTML, đọc kết quả xóa badge, việc hoàn tất mới xóa được.
- Chặn tự duyệt, áp dụng lặp, áp dụng đơn từ chối hoặc trước duyệt, bảo vệ dữ liệu khoa đã đổi ngoài đơn và Director cuối cùng; người rời role Director không giữ quyền xem thông báo duyệt của nhân viên khác.

Log: `target/verification/staff-management-functional.log`.

**726/726 hồi quy**: `target/verification/staff-management-regression.log`; **99/99 thiết lập mật khẩu lần đầu**: `target/verification/staff-management-initial-setup.log`; **16/16 nâng cấp**: `target/verification/staff-management-upgrade.log`. Các schema kiểm thử đều được xóa sau chạy, không thêm dữ liệu thử vào public.

## Áp dụng và sử dụng

Đã sao lưu đầy đủ database hiện tại tại `target/verification/mrm-before-staff-management.dump`, kiểm tra danh mục bằng pg_restore. Đã áp dụng V24 thành công trên `mrm/public` tại `127.0.0.1:5433`: `target/verification/staff-management-migration.log`. Kiểm kê trước/sau ở `staff-management-inventory-before.log`/`staff-management-inventory-after.log` xác nhận số dòng các bảng hiện có không đổi; hai bảng nhân sự mới trống, không thêm tài khoản/đơn thử vào dữ liệu sử dụng. Mật khẩu và thông tin tài khoản hiện tại giữ nguyên.

Khởi động lại bằng profile `dev` để dùng mã mới. Không bật seed demo. Nhân viên dùng menu tài khoản để sửa thông tin/gửi đề nghị; Giám đốc dùng mục **Duyệt thay đổi nhân sự**; Admin dùng **Thay đổi nhân sự đã duyệt**. Đây là thông báo nội bộ theo xác nhận của người dùng, không cần cấu hình email thật.
