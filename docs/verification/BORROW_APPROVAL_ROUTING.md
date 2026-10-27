# Phân tuyến duyệt mượn theo cả lượt đăng ký

Ngày cập nhật:. Phạm vi: mượn hồ sơ giấy, theo yêu cầu và ảnh quy trình người dùng cung cấp. Dự án dùng Archivist cho bước kiểm tra/duyệt thông thường; không bổ sung role Trưởng phòng KHNV. Luồng công bố bệnh án mới và bảo mật dữ liệu giữ nguyên.

## Quy tắc và cách sử dụng

| Thành phần lượt đăng ký | Lý do mượn | Người quyết định |
|---|---|---|
| 1–5 hồ sơ thường | Tùy chọn | Archivist |
| Trên 5 hồ sơ thường | Bắt buộc | Archivist |
| Có ít nhất 1 hồ sơ tử vong, bất kể số lượng | Bắt buộc | Director cho mọi yêu cầu trong lượt |

Reader chọn hồ sơ trong bộ chọn, điền lý do theo quy tắc và gửi một lần. Các hồ sơ vẫn được theo dõi và xét duyệt riêng. Archivist chọn Phê duyệt/Từ chối/Trả bổ sung cho lượt thường; chọn Trình Giám đốc/Trả bổ sung cho lượt đặc biệt. Director dùng mật khẩu cấp 2 khi quyết định. Archivist xác nhận lịch, giao và nhận trả sau phê duyệt; hạn ngày nhận vẫn là ngày đăng ký + 2 ngày lịch.

Tiến trình lượt thường bỏ node Giám đốc. Thông báo trình Director chỉ phát sinh ở tuyến đặc biệt; phê duyệt/từ chối bởi Archivist cũng gửi kết quả cho Reader. Hồ sơ tử vong không còn bị loại khỏi bộ chọn nếu đủ điều kiện nhập kho/chất lượng/khả dụng.

## Dữ liệu và nguyên lý

Migration Java **V19__RouteBorrowingApproval** bổ sung `requires_director` vào `reading_request_batches` và `reading_requests`. Bản ghi cũ nhận TRUE nhằm giữ lịch sử/phê duyệt và yêu cầu đang xử lý. Yêu cầu mới tính tuyến từ các hồ sơ thật dưới khóa giao dịch; dữ liệu client không quyết định tuyến. Chỉ cần một hồ sơ tử vong thì cả batch và mọi yêu cầu con TRUE.

Khóa ngoại kép bảo đảm tuyến của yêu cầu con khớp batch. Trigger ngăn sửa tuyến sau khi gửi. Hủy một yêu cầu không hạ tuyến các yêu cầu khác. Nếu một hồ sơ được đổi thành tử vong sau đăng ký thường, kiểm tra lại trước duyệt/lên lịch/giao sẽ chặn và yêu cầu đăng ký lại. Không tự duyệt hoặc tự chuyển trạng thái dữ liệu cũ. Gửi lại cùng token/nội dung không tạo yêu cầu trùng.

Quyết định Archivist lưu trong `approver_id`, `decided_at`, `decision_note`, đồng thời giữ thông tin kiểm tra. Nhật ký có `ARCHIVIST_APPROVE`/`ARCHIVIST_REJECT`; Director tiếp tục `APPROVE`/`REJECT`. Lịch/giao trả vẫn ở `reading_sessions`, không có bảng bệnh án mới.

## Bản giấy nháp

Tab này dành riêng Archivist, chứa hồ sơ mới Lưu nháp, chưa xác nhận nhập kho. Không đồng nghĩa giấy đang mượn, không phải bản điện tử chờ công bố. Hoàn thiện dữ liệu rồi xác nhận nhập kho mới xuất hiện trong Hồ sơ chính thức và đủ điều kiện xét cho mượn. Đã thêm giải thích trực tiếp vào tab.

## Kiểm thử

Kết quả: **BUILD SUCCESS; 570/570 kiểm tra chức năng/HTTP, 12/12 kiểm tra nâng cấp dữ liệu đạt; JavaScript `node -check` đạt.** Kiểm thử dùng dữ liệu giả trên schema riêng và đã xác nhận tự xóa; không chạy migration hoặc sửa dữ liệu trong schema ứng dụng thật.

Các ca trọng tâm:

1. Lượt thường không lý do được gửi; Archivist phê duyệt, lưu đúng người quyết định, Reader nhận thông báo; không thông báo trình Director.
2. Không thể gửi action trình Director cho lượt thường, không thể tự duyệt/từ chối thay Director cho lượt có tử vong. Các endpoint vẫn kiểm tra role, CSRF, owner, phiên bản và trạng thái.
3. Một bệnh án tử vong hoặc lượt trộn có tử vong nhưng thiếu lý do: từ chối ở backend, không lưu batch hoặc yêu cầu dở dang.
4. Lượt trộn có lý do: mọi yêu cầu con TRUE; gửi lại cùng token giữ nguyên ID; sửa tuyến bằng SQL bị trigger chặn.
5. Director duyệt từng yêu cầu con, sau đó Archivist xác nhận lịch, giao/nhận trả được cả hồ sơ tử vong. Vẫn kiểm tra hạn 2 ngày và mật khẩu cấp 2.
6. Hủy yêu cầu tử vong không hạ tuyến của yêu cầu thường cùng lượt. Cập nhật tử vong sau đăng ký thường bị chặn khi Archivist cố duyệt, yêu cầu vẫn chưa được duyệt.
7. HTML preselect bệnh án tử vong có `required` ngay cả không có JavaScript. Trình duyệt: chọn 1 bệnh án tử vong hiện cảnh báo bắt buộc lý do; bỏ chọn gỡ `required`. Archivist thấy Phê duyệt/Từ chối/Trả bổ sung ở lượt thường.
8. Hồi quy: đăng nhập, tài khoản/nhân viên, danh mục/chính sách, kho, khai báo/công bố bệnh án, tra cứu, lịch sử/phân trang, thông báo, giao–trả, kiểm kê, báo cáo và bảo vệ thông tin nội bộ đều qua bộ kiểm tra. Nâng cấp giữ nguyên ID, dữ liệu và tuyến Director của yêu cầu cũ.

Bằng chứng: [functional.txt](evidence/approval-routing-2026-10-26/functional.txt), [upgrade.txt](evidence/approval-routing-2026-10-26/upgrade.txt), [build.txt](evidence/approval-routing-2026-10-26/build.txt), [giao diện lý do bắt buộc](evidence/approval-routing-2026-10-26/deceased-reason.png). Lệnh build: `.\mvnw.cmd -o -B -ntp package`. Các Java check được chạy riêng vì Maven package không tự chạy những lớp main kiểm thử này.

## Áp dụng trên máy phát triển

Dừng ứng dụng cũ và build/chạy lại với cấu hình `dev` cùng các biến DB đã dùng. Flyway tự áp dụng V19 vào database cấu hình. Không xóa dữ liệu hoặc sửa checksum migration cũ. Đăng ký một lượt mới để thử tuyến duyệt mới; các yêu cầu đã tồn tại trước V19 giữ tuyến Director. Kiểm tra ba trường hợp trong bảng quy tắc, sau đó theo dõi chuông và tiến trình bằng Reader/Archivist/Director.
