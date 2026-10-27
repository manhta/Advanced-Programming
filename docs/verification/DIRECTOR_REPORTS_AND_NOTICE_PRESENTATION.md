# Thông báo Archivist và tinh gọn giao diện Giám đốc

Ngày thực hiện:. Không thêm migration hoặc sửa dữ liệu database thực tế.

## Thông báo yêu cầu cần kiểm tra

Archivist thấy **Người yêu cầu** (họ tên và tên đăng nhập), **Hồ sơ yêu cầu** (mã và người bệnh), **Ngày giờ yêu cầu** (thời điểm tạo yêu cầu theo giờ Việt Nam). Không hiển thị UUID yêu cầu hoặc mục đích trong phần nội dung thông báo công việc SUBMIT/AMEND. Thời điểm thông báo phát sinh vẫn có ở dòng Lúc; ngày giờ yêu cầu lấy từ reading_requests.created_at.

Thông báo cũ được trình bày bằng dữ liệu có cấu trúc truy vấn từ yêu cầu/người dùng/nhân viên/hồ sơ; không cần xóa hoặc viết lại message lịch sử. Thông báo công việc mới lưu nội dung không chứa mục đích. Các kết quả duyệt, lịch nhận và hủy vẫn dùng logic thông báo đã có. Nội dung mục đích trong trang xét duyệt vẫn giữ để người duyệt đánh giá yêu cầu.

File: `NotificationService.java` (WorkInfo và phép chiếu dữ liệu), `templates/notification/inbox.html` (nhóm thông tin), `static/css/portal.css` (bố cục).

## Báo cáo Giám đốc

Đã bỏ hai đoạn thống kê/diễn giải dài được người dùng chỉ ra, cùng phần giới thiệu kỹ thuật về Admin/vị trí khỏi trang Director. Bộ lọc, xuất CSV và in vẫn hoạt động.

| Mã cột | Nhãn trên giao diện |
|---|---|
| actor | Người thao tác |
| action | Hành động |
| entity_type | Loại đối tượng |
| entity_id | Mã đối tượng |
| outcome | Kết quả |
| details | Nội dung |
| at | Thời điểm |

Các cột của loại báo cáo khác cũng được ánh xạ nhãn tiếng Việt. Thay đổi áp dụng cho tiêu đề bảng HTML/bản in; khóa truy vấn và tiêu đề kỹ thuật trong CSV giữ nguyên để không làm thay đổi cấu trúc xuất dữ liệu.

Khi kiểm tra trực quan đã phát hiện các ô dữ liệu đang trống dù có dòng. Một phép thử render riêng xác nhận `row[c]` không trả giá trị trong cấu hình Thymeleaf hiện tại; đã dùng `row.get(c)` và bổ sung kiểm tra dữ liệu thật hiện trong ô.

File: `templates/report/list.html`, `report/ReportLabels.java`.

## Bỏ kiểm kê đối với Director

Kiểm kê chỉ dành Archivist. Director không còn liên kết trên navigation hoặc lựa chọn Kết quả kiểm kê trong báo cáo. Các URL `/director/inventories`, chi tiết và truy vấn/xuất báo cáo `type=inventory` bị chặn. Service cũng kiểm tra quyền để không chỉ ẩn giao diện. Dữ liệu kiểm kê và nghiệp vụ của Archivist vẫn giữ nguyên.

File: `fragments/layout.html`, `SecurityConfig.java`, `InventoryController.java`, `InventoryService.java`, `ReportService.java` và bộ lọc trong HTML báo cáo.

## Trang Director xét duyệt yêu cầu đọc

Bỏ toàn bộ hai khối **Lịch nhận và giao–trả** và **Lịch sử xử lý** khỏi `templates/reading/detail.html`. Thông tin yêu cầu, lý do và form quyết định với mật khẩu cấp 2 vẫn có. Không xóa lịch sử trong database; Reader vẫn có trang theo dõi tiến trình, Archivist vẫn quản lý giao–trả ở trang riêng.

## Kiểm thử

**BUILD SUCCESS; 688/688 kiểm tra chức năng/HTTP đạt.** Đã kiểm tra trực quan thông báo Archivist, tiêu đề/nội dung bảng báo cáo và trang duyệt Director. Bản chạy thử đã dừng, schema tạm đã xóa. Các ca kiểm tra gồm: metadata thông báo mới/cũ không chứa mục đích; đủ bảy nhãn cột tiếng Việt; ô báo cáo có nội dung thực; các đoạn diễn giải đã bị bỏ; kiểm kê Director bị chặn ở menu, URL, service và xuất báo cáo; form Director vẫn ghi quyết định nhưng không hiện hai khối được yêu cầu bỏ.

Kiểm thử chạy bằng tài khoản giả trên schema riêng, không đổi dữ liệu thật. Sau cập nhật chỉ cần khởi động lại ứng dụng để nhận giao diện/quyền mới, không cần migration.


Bằng chứng: [functional.txt](evidence/director-cleanup-2026-10-26/functional.txt), [build.txt](evidence/director-cleanup-2026-10-26/build.txt), [thông báo](evidence/director-cleanup-2026-10-26/archivist-notice.png), [báo cáo](evidence/director-cleanup-2026-10-26/director-report.png), [trang duyệt](evidence/director-cleanup-2026-10-26/director-review.png).
