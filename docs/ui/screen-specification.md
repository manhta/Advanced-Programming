# Đặc tả màn hình và nguyên mẫu văn bản

Bản low-fidelity dùng để duyệt luồng/trường/thao tác, chưa phải HTML hay thiết kế đồ họa cuối. Mọi nút chỉ gợi ý quyền, quyền thật ở máy chủ. Đọc kèm [ma trận quyền](../requirements/permission-matrix.md).

## Điều hướng theo vai trò

- READER: Tổng quan của tôi → Tra cứu → Yêu cầu của tôi → Lịch đọc → Hồ sơ tài khoản.
- ARCHIVIST: Tổng quan kho → HSBA → Vị trí/danh mục → Yêu cầu mới → Chờ lịch → Giao hôm nay → Đang đọc/chưa trả → Ngoại lệ → Báo cáo; Audit chỉ khi cấp.
- DIRECTOR: Chờ duyệt → Quyết định → Ngoại lệ → Báo cáo/Audit theo quyền.
- ADMIN: Tài khoản/bốn vai trò theo căn cứ → Cấu hình kỹ thuật → Hướng dẫn vận hành; không có HSBA mặc định.

## Danh mục màn hình

| Mã | Màn hình, trường hiển thị/nhập | Hành động và validation | UC |
|---|---|---|---|
| SCR-01 | Login: username, password, thông báo chung | Đăng nhập, thông tin liên hệ cấp lại; không lộ tài khoản tồn tại | 01 |
| SCR-02 | Tổng quan: số yêu cầu/phiên đúng vai trò, cập nhật lúc nào | Đi tới danh sách đã lọc; tổng cũng phải đúng scope | 05/09/17 |
| SCR-03 | Tài khoản: username, tên, khoa, role, trạng thái | Thêm/khóa/reset; quyền nhạy cảm có căn cứ; không hiện hash | 02 |
| SCR-04 | Danh mục: mã, tên, active, policy version | Lưu/ngừng dùng; chặn trùng/đang tham chiếu | 03 |
| SCR-05 | Vị trí: cây kho/tủ/kệ/ngăn, mã, active, sức chứa nếu dùng | Tạo/sửa có kiểm soát; chặn chu trình, ngừng dùng khi có hồ sơ | 06 |
| SCR-06 | Danh sách HSBA: BASIC, filter mã/tên/khoa/ngày/trạng thái, paging | Tìm, mở chi tiết, đăng ký; filter kho chỉ người đủ quyền | 05 |
| SCR-07 | Chi tiết: BASIC + tab nghiệp vụ đúng quyền | Tạo yêu cầu, sửa metadata, chuyển vị trí, xem lịch sử theo quyền | 04/05/06/16/18 |
| SCR-08 | Nhập/sửa HSBA: trường từ dictionary, dấu bắt buộc, nguồn/thiếu | Preview trùng/ngày, lưu staging hoặc nhập kho; vị trí cũ đổi qua SCR-05 | 04/18 |
| SCR-09 | Đăng ký: một HSBA, mục đích, các ngày, quy định trả trong ngày | Lưu nháp/gửi; lỗi từng; tóm tắt trước gửi | 07 |
| SCR-10 | Yêu cầu của tôi: code, state, revision, ngày mong muốn/xác nhận, tiến độ | Xem, bổ sung, rút sửa/hủy theo state; lý do và hậu quả rõ | 09 |
| SCR-11 | NVLT kiểm tra: yêu cầu, metadata đủ quyền, khả dụng/lịch, ghi chú | Trình hoặc trả bổ sung, không sửa hộ nội dung | 08 |
| SCR-12 | Chờ duyệt: owner/khoa/mục đích/hồ sơ/ngày, revision, kiểm tra | Duyệt/từ chối cả revision, lý do; không tự duyệt; không cần vị trí | 10 |
| SCR-13 | Xác thực cấp 2: quyết định/revision tóm tắt, secret | Xác nhận một lần, không cache mật khẩu; hết hạn phải xác thực lại | 10 |
| SCR-14 | Lịch: ngày được duyệt, trạng thái từng ngày, xung đột | Xác nhận tập ngày trong duyệt; không đổi ngày ngoài phạm vi | 11 |
| SCR-15 | Giao hôm nay: session, người đọc, HSBA, vị trí, tình trạng | Đối chiếu vật lý, xác nhận giao; success có mã/giờ; conflict tải lại | 12 |
| SCR-16 | Nhận trả: lượt mở, tình trạng lúc giao, đích cất lại, chênh lệch | Nhận trả thực tế, lý do muộn/thiếu/hỏng; không sửa giờ server | 13 |
| SCR-17 | Ngoại lệ: chưa trả kể cả ngày trước, loại, người phụ trách, diễn biến | Ghi diễn biến, nhận trả, giải quyết đúng quyền; không “trả” bằng đóng ngoại lệ | 14 |
| SCR-19 | Lịch sử/audit: actor, time, action, object, outcome, correlation | Filter/detail theo quyền; không sửa/xóa | 16 |
| SCR-20 | Báo cáo: loại, filter, đơn vị đếm, thời điểm, tổng/detail | Xem; xuất chỉ khi optional được chọn và có EXPORT | 17 |
| SCR-21 | Theo dõi thời hạn: ngày gốc, nhóm/policy, mốc, chất lượng dữ liệu | Xem/điều chỉnh có căn cứ; không tiêu hủy | 18 |

## Nguyên mẫu luồng chính

```text
[Tra cứu HSBA] Mã/tên [........] Khoa [....] Ngày [.. đến..] [Tìm]
Mã hồ sơ | Người bệnh | Khoa | Khoảng điều trị | Trạng thái | [Chi tiết]
Bác sĩ không thấy cột vị trí. NVLT có vị trí theo quyền.
```

```text
[Yêu cầu MR-REQ-...] Revision 2 Chờ duyệt
Người đọc [...] Khoa [...] HSBA [...]
Mục đích [...]
Ngày đề nghị: 20/10, 21/10, 22/10
NVLT kiểm tra [...]
[Lý do nếu từ chối...] [Từ chối] [Duyệt] → xác thực cấp 2 → kết quả
Không có nút thay nội dung, thay HSBA hoặc tự thêm ngày khi duyệt.
```

```text
[Giao phiên...] Người đọc [...] HSBA [...] Hôm nay [...]
Vị trí kho dự kiến [...] Tình trạng hiện tại [...] Khả dụng [...]
Tình trạng kiểm tra [....] Ghi chú [....]
[Đã đối chiếu người nhận và mã hồ sơ] [Xác nhận giao]
Kết quả: mã giao [...], giờ [...], trạng thái Đang đọc / hoặc Xung đột tải lại
```

```text
[Nhận trả lượt...] Giao lúc [...] Tình trạng lúc giao [...]
Tình trạng trả [....] Vị trí cất [....] Chênh lệch/lý do muộn [....]
[Xác nhận thực nhận trả]
Nếu hỏng/thiếu: đã nhận về nhưng chặn giao tiếp cho đến đối soát.
```

## Trạng thái dùng chung và khả năng tiếp cận

Có loading, rỗng, validation theo trường, success có mã kết quả, conflict, session expired, forbidden và lỗi có correlation. Khi lỗi không xóa input không bí mật; password luôn xóa. Sau timeout không khẳng định thao tác chưa lưu; cung cấp tra cứu hoặc retry cùng operation key. Bảng dài phân trang, header rõ; label cho input, dùng bàn phím, không chỉ màu để biểu đạt state. Form ngày hiển thị timezone/quy định cần biết.

## Màn hình bổ sung khi được chọn

Email: hàng đợi/kết quả gửi; nhập file: mẫu → upload → preview lỗi → xác nhận → kết quả lô; export: lựa chọn format/bộ lọc/trạng thái tải; kiểm kê: tạo đợt → ghi từng dòng → đối soát → đóng; thống kê: kỳ/chỉ số/công thức; minh chứng: upload loại hành chính → trạng thái kiểm tra → tải có quyền.

## Kịch bản duyệt nguyên mẫu

READER tự tìm và gửi ba; NVLT trả bổ sung rồi trình; người duyệt thử từ chối/duyệt và bị khóa/mất quyền DIRECTOR; NVLT xác nhận lịch/giao/trả; thêm ca chưa trả/hư hỏng; ADMIN thử truy cập trái quyền. Ghi chỗ khó hiểu, trường thiếu, người xác nhận và. Chưa có buổi duyệt thực tế; không đánh dấu hoàn tất nghiệm thu giao diện.

SCR-18 (ủy quyền) được giữ số làm lịch sử nhưng không có màn hình này trong phiên bản đầu. Có 20 màn hình cơ sở còn áp dụng; không đánh lại số SCR-19–21. Chỉ DIRECTOR có nút quyết định, chỉ READER có nút tạo yêu cầu theo D-25 đã xác nhận.

SCR-01 được thiết kế chi tiết tại [login-design.md](../design/authentication/login-design.md): một form không chọn role, trạng thái lỗi, home theo bốn role và hợp đồng logout. Đây là wireframe văn bản, chưa có template được triển khai.
