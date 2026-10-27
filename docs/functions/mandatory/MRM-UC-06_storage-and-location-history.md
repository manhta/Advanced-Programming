# MRM-UC-06 — Quản lý vị trí và chuyển hồ sơ

**Bản triển khai:** xem [CR-005](../../changes/CR-005_five-feature-implementation.md) và [báo cáo năm chức năng](../../verification/FIVE_FEATURE_COMPLETION_REPORT.md) cho phạm vi đang chạy, hướng dẫn và kết quả kiểm thử. Nội dung dự thảo tổng thể dưới đây còn các nhánh mở rộng chưa triển khai.

**Trạng thái tài liệu gốc (không phải tiến độ code hiện tại):** dự thảo v0.1; ưu tiên bắt buộc.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | NVLT |
| Nguồn và quy tắc | FR-05/06; D-15; BR-04/16/19/23 |
| Tiền điều kiện | NVLT có quyền; chuyển thường chỉ HSBA trong kho, không lượt giao mở. |
| Dữ liệu đầu vào | Vị trí mã/tên/cha/loại/active/capacity nếu dùng; HSBA, đích, lý do chuyển, version. |

## Kích hoạt và luồng chính

1. NVLT tạo/sửa cây kho, kiểm tra loại cha/con và không tạo chu trình.
2. Chọn HSBA, xem vị trí hiện tại, chọn đích đang hoạt động và lý do.
3. Máy chủ kiểm tra trạng thái hiện thời, sức chứa nếu được cấu hình, version và quyền.
4. Cập nhật vị trí và thêm lịch sử cũ/mới, người, giờ trong một transaction.
5. Có thể đánh dấu tạm không khả dụng do việc ngoài hệ thống với căn cứ và người chịu trách nhiệm; mở lại cần xác nhận vật lý.

## Luồng thay thế và ngoại lệ

E1: HSBA đang đọc → không chuyển qua luồng này, thực hiện nhận trả UC-13.

E2: Ngăn có hồ sơ → không ngừng hoạt động/xóa; chuyển hồ sơ trước. Đổi cấu trúc cha làm thay địa chỉ phải có đợt có kiểm soát và lịch sử.

E3: Không tìm thấy vật lý → mở ngoại lệ và chặn giao; không chuyển sang một vị trí giả.

E4: Đích không hợp lệ/đầy/version cũ → rollback, giữ vị trí cũ.

## Hậu điều kiện, giao dịch và audit

Vị trí nhất quán với lịch sử; tối đa một current location, nhiều HSBA có thể cùng ngăn. Biến động ngoài hệ thống không trở thành workflow duyệt ngoài.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-06-01:** Chuyển thành công → một lịch sử cũ/mới khớp current location.
- **AC-06-02:** Hai người đổi cùng version → chỉ một thành công, người sau tải lại.
- **AC-06-03:** Ngừng ngăn còn chứa hồ sơ → bị chặn, lịch sử cũ không mất.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.
