# Form xét duyệt yêu cầu của nhân viên lưu trữ

Ngày cập nhật:. Không thay cấu trúc database hoặc sửa migration cũ.

## Giao diện và cách sử dụng

Mở **Yêu cầu → Mở và xử lý** khi yêu cầu mới gửi. Trang chỉ giữ thông tin yêu cầu và phần xét duyệt; bỏ lịch sử xử lý, khối Lịch nhận và giao–trả và khối Hủy phần chưa thực hiện khỏi giao diện xét duyệt Archivist.

| Kết quả | Nội dung nhập | Lịch/phòng nhận | Kết quả lưu |
|---|---|---|---|
| Phê duyệt cho mượn | Tích xác nhận phê duyệt; ghi chú tùy chọn | Bắt buộc ngày nhận và phòng nhận | Phê duyệt và xác nhận lịch trong cùng lần gửi, trạng thái CONFIRMED |
| Trả bổ sung | Ô “Cần bổ sung”, bắt buộc | Ẩn và không gửi | NEEDS_INFO, Reader được thông báo |
| Từ chối | Ô “Lý do hủy / từ chối”, bắt buộc | Ẩn và không gửi | REJECTED, lưu căn cứ từ chối |
| Đủ điều kiện — trình Giám đốc | Nhận xét tùy chọn | Không có ở bước kiểm tra | PENDING_APPROVAL, thông báo cho Director |

Lượt có bất kỳ hồ sơ tử vong nào và yêu cầu lịch sử giữ tuyến Director chỉ có Trình Giám đốc/Trả bổ sung; nhân viên lưu trữ không tự phê duyệt/từ chối thay Director. Sau khi Director phê duyệt, Archivist vẫn xác nhận ngày/phòng nhận ở bước tiếp theo. Các trường hợp thường được gộp duyệt và lịch ngay trong form mới.

Ngày nhận vẫn từ hôm nay đến hạn ngày đăng ký + 2 ngày lịch. Ô phòng nhận lưu vào confirmation_note hiện có và hiện trong node Xác nhận lịch nhận của Reader. “Từ chối” là quyết định REJECTED; nhãn lý do hủy/từ chối không chuyển thành thao tác hủy sau phê duyệt.

## Nguyên lý và vị trí file

- `src/main/resources/templates/reading/detail.html`: form lựa chọn, ghi chú, xác nhận, ngày/phòng; không có lịch sử/giao trả/hủy trên trang Archivist.
- `src/main/resources/static/js/request-review.js`: đổi nhãn, required theo kết quả; ẩn và disabled ngày/phòng/checkbox khi không chọn phê duyệt.
- `src/main/java/mrm/workflow/ReadingController.java`: nhận các trường tùy chọn, gọi service tổng hợp.
- `ReadingService.reviewAndSchedule`: transaction ngoài bao bọc review và confirm. Kiểm tra checkbox, ngày/phòng, thẩm quyền tuyến duyệt, phiên bản và trạng thái. Nếu xếp lịch thất bại, rollback cả quyết định, lịch sử và thông báo; không có yêu cầu đã duyệt dở dang.
- `ReadingService.review`: ghi chú có thể trống cho phê duyệt/trình; bổ sung/từ chối luôn yêu cầu lý do ở backend, kể cả khi bỏ qua JavaScript.

## Trang giao–trả riêng

Để tiếp tục giao và nhận trả hồ sơ sau khi bỏ khối này khỏi màn xét duyệt, mở **Quản lý giao–trả hồ sơ** trên yêu cầu đã xác nhận lịch, hoặc nhấp **Chờ giao** trên trang chủ. Trang riêng tại `/archivist/requests/{id}/circulation`, HTML `reading/circulation.html`.

Trang này giữ thao tác giao đúng ngày, nhận trả/cất lại, ghi chất lượng và ngoại lệ. Các POST trở lại trang giao–trả. Chỉ Archivist truy cập được; Reader vẫn theo dõi tiến trình ở trang riêng như trước. Director giữ phần xem lịch/lịch sử hiện có. Lịch sử trong database không bị xóa khi bỏ khối hiển thị của Archivist.

## Kiểm thử

**BUILD SUCCESS; 668/668 kiểm tra chức năng/HTTP đạt; JavaScript `node -check` đạt.** Kiểm thử bằng dữ liệu giả trên schema tạm, không sửa yêu cầu hoặc tài khoản của database thật. Schema tạm đã xóa và bản chạy thử đã dừng.

Đã kiểm tra trên trình duyệt cả ba lựa chọn thường: phê duyệt hiện checkbox/ngày/phòng và ghi chú không required; bổ sung/từ chối đổi nhãn, bắt buộc lý do, ẩn và disabled lịch nhận. Bộ kiểm thử xác nhận lượt tử vong không có trường lịch ở bước trình duyệt; chỉ hiện khi Director đã phê duyệt.

Bằng chứng: [functional.txt](evidence/archivist-review-2026-10-26/functional.txt), [build.txt](evidence/archivist-review-2026-10-26/build.txt), [phê duyệt](evidence/archivist-review-2026-10-26/approval-and-schedule.png), [bổ sung](evidence/archivist-review-2026-10-26/needs-info.png), [từ chối](evidence/archivist-review-2026-10-26/rejected.png).

Các ca chính: ghi chú phê duyệt trống vẫn thành công; thiếu checkbox/ngày/phòng hoặc ngày quá hạn bị chặn; lỗi xếp lịch rollback toàn bộ; lý do bổ sung/từ chối bắt buộc; lượt tử vong chỉ trình duyệt và không có lịch trước Director; gửi lặp không ghi trùng; phân quyền/CSRF; lịch và phòng hiện đúng trên tiến trình Reader; trang giao–trả riêng vẫn hoạt động.

Sau cập nhật, khởi động lại ứng dụng bằng cấu hình hiện tại, không cần chạy migration. Thử chuyển từng kết quả trên form để kiểm tra các ô tương ứng hiện/ẩn và tính bắt buộc.
