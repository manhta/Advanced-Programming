# Hồ sơ cá nhân và thời điểm gửi kết quả nhân sự — V25

Ngày:. Quy tắc này thay thế phần lối vào/sửa email/thời điểm thông báo trong báo cáo V24. Vẫn dùng thông báo nội bộ qua chuông, không gửi SMTP.

## Giao diện cá nhân

- Menu tài khoản bỏ mục Đề nghị đổi vai trò/khoa; giữ Thông tin cá nhân, Thay đổi mật khẩu, Đăng xuất (Director vẫn có mục cấp 2).
- Trang Thông tin cá nhân chỉ giữ nút **Chỉnh sửa thông tin cá nhân**, bỏ hai nút mật khẩu và đề nghị riêng.
- Trang sửa bỏ ô Email. Có hai mục **Vai trò**, **Khoa/phòng** hiển thị giá trị hiện tại và liên kết mở `/account/change-requests`, dùng form đề nghị/lịch sử đã có. Không tạo thêm luồng cấp quyền mới.
- Nhân viên vẫn sửa họ tên, căn cước, ngày sinh, giới tính trực tiếp. Username/email/vai trò/khoa không thay đổi khi lưu form cá nhân, kể cả có gửi email/role/department bằng request tự chế. Chọn liên kết vai trò/khoa không lưu những ô cá nhân đang sửa; nếu cần giữ thay đổi, bấm Lưu trước khi mở đề nghị.

Các thay đổi dùng template chung cho các tài khoản nhân viên y tế.

## Giám đốc xét duyệt

Mặc định chọn Phê duyệt: ô lý do từ chối ẩn, disabled, không required và không gửi lên server. Chọn Từ chối: ô hiện và bắt buộc nhập. Chuyển lại Phê duyệt: ô được ẩn/vô hiệu hóa và xóa giá trị cũ để tránh gửi nhầm. Inline style bảo đảm nhãn ẩn không bị CSS label của form làm hiện lại.

Backend vẫn kiểm tra lý do khi từ chối và xác thực mật khẩu cấp 2. Nếu chọn phê duyệt nhưng cố gửi note từ chối, server bỏ qua note này. Chi tiết đơn chỉ hiển thị nội dung quyết định ở trạng thái từ chối dưới nhãn **Lý do từ chối**.

## Thời điểm thông báo

| Thao tác | Giám đốc | Admin | Nhân viên gửi đơn |
|---|---|---|---|
| Nhân viên gửi đơn | Nhận việc xét duyệt | Không nhận | Chưa có kết quả |
| Giám đốc phê duyệt | Việc xét duyệt hoàn tất | Nhận việc cần áp dụng | **Chưa gửi thông báo phê duyệt** |
| Giám đốc từ chối | Việc xét duyệt hoàn tất | Không nhận việc áp dụng | Gửi ngay từ chối và lý do |
| Admin áp dụng thành công | Không gửi thêm | Việc áp dụng hoàn tất | Gửi kết quả phê duyệt |
| Admin áp dụng lỗi hoặc bấm lặp | Không gửi thêm | Giữ đúng trạng thái thực tế | Không gửi kết quả thành công mới |

Kết quả thành công gắn với sự kiện **APPLY**, tạo trong cùng transaction với đổi quyền/khoa. Nếu có lỗi, toàn bộ cập nhật và thông báo rollback. Khi đổi role, thông báo mới lấy role đã cập nhật, nên nhân viên đăng nhập lại vẫn thấy kết quả. Không chuyển giao thông báo xét duyệt của người khác cho người đã rời vai trò Director.

## Dữ liệu V24 đang có

Migration Java `V25__NotifyStaffAfterApplication`:

1. Soft-delete các thông báo APPROVE gửi cho **chính người yêu cầu** ở phiên bản cũ; giữ nguyên thông báo công việc của Admin.
2. Với đơn đã APPLIED, tạo kết quả dựa trên sự kiện APPLY và thời điểm Admin áp dụng; không tạo trùng nhờ khóa duy nhất recipient/event.
3. Giữ thông báo từ chối/lý do, đơn, các sự kiện lịch sử và dữ liệu nhân viên. Runtime cũng ẩn nguồn APPROVE của chính người gửi để không hiển thị kết quả sớm.

Không sửa các migration cũ, không đổi mật khẩu hay thông tin bệnh án.

## File chỉnh sửa

- `src/main/resources/templates/account/profile.html`: nút trang thông tin.
- `src/main/resources/templates/account/profile-edit.html`: bỏ email, hai liên kết vai trò/khoa.
- `src/main/resources/templates/account/change-detail.html`: lý do từ chối và JavaScript ẩn/hiện.
- `src/main/resources/templates/fragments/layout.html`: menu dùng chung.
- `AccountService.updateOwnProfile`, `PersonalAccountController`: không nhận/cập nhật email.
- `StaffChangeService.decide/apply`: từ chối gửi ngay, thành công gửi sau áp dụng.
- `NotificationService`: kết quả nhân viên theo APPLY/REJECT, công việc Admin vẫn theo APPROVE.

## Kiểm thử

- Build thành công: `target/verification/staff-profile-flow-build.log`.
- **95/95** kiểm tra nhân sự/HTTP: `staff-profile-flow-functional.log`. Bao gồm menu/lối vào mới, form không có email, request chèn email không đổi dữ liệu, role/khoa vẫn được bảo vệ, Giám đốc phê duyệt chưa tạo thông báo cho nhân viên, Admin áp dụng thành công mới gửi, áp dụng lỗi/bấm lặp không gửi trùng, từ chối vẫn gửi ngay lý do.
- **12/12** nâng cấp V24 → V25: `staff-profile-flow-upgrade.log`. Có dữ liệu cũ APPROVED/APPLIED/REJECTED, xác nhận ẩn kết quả sớm, giữ Admin work, bù đúng một APPLY result, giữ lý do từ chối và mọi đơn/sự kiện, khởi động lại không gửi trùng.
- **726/726** hồi quy: `staff-profile-flow-regression.log`.
- **16/16** nâng cấp bệnh án: `staff-profile-flow-record-upgrade.log`.
- Thực thi chính inline JavaScript từ template bằng Node VM: `staff-profile-flow-javascript.log`, kiểm tra Phê duyệt → Từ chối → Phê duyệt, hidden/display/disabled/required và xóa giá trị cũ.

Tổng 849 kiểm tra Java đạt, cộng kiểm tra JavaScript. Các schema thử đã xóa; không đưa người dùng/đơn mẫu của kiểm thử vào public.

## Sử dụng

Đã sao lưu database trước cập nhật tại `target/verification/mrm-before-staff-profile-flow.dump` và kiểm tra danh mục dump. V25 đã áp dụng trên `mrm/public`: `staff-profile-flow-migration.log`. Kiểm tra chỉ đọc sau migration ở `staff-profile-flow-public-verified.log` xác nhận không còn thông báo phê duyệt sớm đang hiện cho nhân viên, và mọi đơn đã áp dụng có kết quả dựa trên APPLY. Một đơn APPROVED đang chờ Admin vẫn được giữ nguyên. Kiểm kê trước/sau cho thấy số dòng các bảng hiện có không đổi; không đổi tài khoản, mật khẩu hay dữ liệu bệnh án.

Khởi động lại ứng dụng bằng profile `dev`. Nhân viên mở menu tên → Thông tin cá nhân → Chỉnh sửa thông tin cá nhân → nhấp Vai trò hoặc Khoa/phòng để mở form đề nghị. Giám đốc xét duyệt trên trang hiện có; Admin áp dụng trên trang hiện có. Thông báo kết quả sẽ theo thời điểm mới như bảng trên.
