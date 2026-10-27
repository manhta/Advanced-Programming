# MRM-UC-03 — Quản lý danh mục khoa, loại hồ sơ và chính sách lưu

**Bản triển khai:** xem [CR-005](../../changes/CR-005_five-feature-implementation.md) và [báo cáo năm chức năng](../../verification/FIVE_FEATURE_COMPLETION_REPORT.md) cho phạm vi đang chạy, hướng dẫn và kết quả kiểm thử. Nội dung dự thảo tổng thể dưới đây còn các nhánh mở rộng chưa triển khai.

**Trạng thái tài liệu gốc (không phải tiến độ code hiện tại):** dự thảo v0.1; ưu tiên bắt buộc.

**Tiến độ:** đã triển khai P2 cho dự án học tập theo CR-004/D-19/D-27. Năm khoa cố định; loại bệnh án có optimistic version; chính sách có dự thảo/xác nhận/ngừng dùng; bộ chọn kiểm tra hiệu lực và tạo snapshot. Báo cáo: [P2_COMPLETION_REPORT](../../verification/P2_COMPLETION_REPORT.md). Tích hợp lưu snapshot/FK vào bệnh án thuộc UC-04 và nghiệm thu trực quan bởi người dùng còn theo dõi riêng.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | NVLT; Giám đốc xác nhận danh mục nghiệp vụ |
| Nguồn và quy tắc | FR-07/15; D-19; BR-18/19 |
| Tiền điều kiện | Có quyền quản lý danh mục, danh sách mã/ý nghĩa đã rà soát. |
| Dữ liệu đầu vào | Mã ≤32, tên ≤200, trạng thái hoạt động; retention policy có số năm, phiên bản, ngày hiệu lực và căn cứ. |

## Kích hoạt và luồng chính

1. Mở loại danh mục, xem danh sách và các mục ngừng hoạt động.
2. Thêm/sửa tên hoặc tạo phiên bản chính sách lưu; kiểm tra mã duy nhất và thời gian hiệu lực.
3. Xem tác động đến hồ sơ hiện hữu trước khi lưu thay đổi quy tắc.
4. Lưu danh mục và audit; mục ngừng dùng không xuất hiện ở lựa chọn mới nhưng vẫn đọc được trong lịch sử.

## Luồng thay thế và ngoại lệ

E1: Số năm không dương/mã trùng → không lưu.

E2: Mục đã tham chiếu → không xóa cứng; có thể ngừng dùng theo quyền.

E3: Thay số năm lưu → tạo phiên bản, không âm thầm tính lại lịch sử; muốn áp dụng lại cần đợt điều chỉnh có căn cứ.

## Hậu điều kiện, giao dịch và audit

Danh mục dùng được cho nhập liệu; không tạo chức năng tiêu hủy. Audit tên/mã/trạng thái/phiên bản quy tắc.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-03-01:** Danh mục đã dùng bị vô hiệu hóa → lịch sử vẫn hiển thị tên/mã.
- **AC-03-02:** Đổi chính sách → hồ sơ cũ giữ policy snapshot trừ khi có điều chỉnh được duyệt.
- **AC-03-03:** Ngày lưu 29/2 → mốc thời hạn tuân thủ quy tắc D-19 sau xác nhận.

## Điểm cần người dùng rà soát

D-19/D-27 đã được người dùng xác nhận cho học tập, không phải phê duyệt pháp lý của trung tâm. AC-03-03 đã kiểm tra quy tắc 29/2 về 28/2; AC-03-01/02 đã kiểm tra ở mức danh mục/snapshot, phần lưu/đọc cùng hồ sơ cần kiểm thử bổ sung khi triển khai UC-04.
