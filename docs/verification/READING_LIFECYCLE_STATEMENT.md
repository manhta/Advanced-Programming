# Bảng yêu cầu theo tiến trình giao–trả —

## Hành vi

- Ngày đăng ký hiện ở mọi trạng thái; ngày nhận chỉ hiện sau xác nhận lịch, khi đang mượn và khi hoàn thành. Hủy/từ chối/đang xét duyệt để trống ngày nhận. Không còn dòng Chờ nhân viên lưu trữ xếp lịch trong ô ngày.
- Tiêu đề cột căn giữa; các giá trị vẫn giữ căn lề phù hợp để dễ đọc.
- Reader lọc theo trạng thái hiển thị cùng tên/số lưu trữ và khoảng ngày. Phân trang giữ toàn bộ bộ lọc.
- Mặc định ngày đăng ký giảm dần, cùng ngày dùng thời điểm gửi giảm dần. Sắp xếp trạng thái dùng thứ tự tiến trình và ngày đăng ký tăng dần trong nhóm; cùng ngày dùng thời điểm gửi tăng dần, UUID làm khóa ổn định cuối cùng.
- Ghi chú khi đang xử lý chỉ có mục đích mượn đã gửi. Đang mượn hiển thị Đang mượn, Hoàn thành hiển thị Ngày trả DD/MM/YYYY, Hủy hiển thị Đã hủy; không hiện lý do trong ba trường hợp này. Từ chối để trống ghi chú.

## Nguồn dữ liệu

Không thêm mã trạng thái hoặc migration. `reading_requests.state` giữ nguyên quy trình duyệt. `display_state` được truy vấn dựa trên `reading_sessions`: CONFIRMED có phiên READING thành BORROWED; CONFIRMED có ít nhất một phiên đã trả với thời gian trả thật, không còn phiên phải thực hiện/chưa trả, thành COMPLETED. Phiên hủy không cản hoàn tất các phiên đã trả. Yêu cầu đã hủy luôn hiển thị CANCELLED. Lịch đã xác nhận nhưng chưa giao không được tính là đang mượn; trả một phần không được tính hoàn thành. Ngày trả lấy lần trả cuối, định dạng theo giờ Việt Nam.

Bộ lọc nhận SUBMITTED, NEEDS_INFO, PENDING_APPROVAL, APPROVED, CONFIRMED, BORROWED, COMPLETED, REJECTED, CANCELLED; sort chỉ nhận registered hoặc status. Giá trị không hợp lệ báo lỗi nghiệp vụ; sort SQL chọn từ hai nhánh cố định. Reader chỉ truy vấn yêu cầu của mình. Danh sách staff dùng chung cách tính trạng thái/ngày nhưng bộ lọc Reader không mở thêm dữ liệu cho role khác.

## Xác minh

- Maven offline package: BUILD SUCCESS.
- `FunctionalRequirementsCheck -preview`: **504/504 đạt** trong schema tạm, đã dừng app thử và xóa schema.
- Ca mới: ngày đăng ký khi hủy, ẩn ngày nhận lúc xét duyệt, không hiện thông báo chờ xếp lịch; lọc trạng thái kết hợp từ khóa/; sắp xếp trong nhóm từ cũ tới mới, mặc định mới trước; phân biệt lịch xác nhận/đang giao/đã trả; hoàn thành có ngày trả; trả một phần chưa hoàn thành; ghi chú không lộ lại lý do sau giao/trả/hủy; phân trang giữ trạng thái/sort; từ chối giá trị lọc và sort không hợp lệ.
- Trình duyệt: danh sách dữ liệu kiểm thử LIFE_ hiển thị Mới gửi, Đã xác nhận lịch, Hoàn thành/Ngày trả, Đã hủy với ngày đăng ký; kiểm tra filter Hoàn thành chỉ còn một dòng và giữ sort trạng thái. Ảnh ở viewport mặc định; chưa xác nhận mọi thiết bị.
- Không sửa dữ liệu thật hoặc Note.

Minh chứng: [ảnh](evidence/reading-lifecycle-2026-10-25/statement.png), [log](evidence/reading-lifecycle-2026-10-25/functional.txt).

## File liên quan

`ReadingService.java` (truy vấn, trạng thái hiển thị, lọc/sort), `ReadingController.java` (tham số bộ lọc), `ReadingLabels.java` (nhãn), `templates/reading/list.html`, `static/css/portal.css`, `docs/user-guides/reader.md`.

Khởi động lại ứng dụng và Ctrl+F5 để sử dụng bản mới.
