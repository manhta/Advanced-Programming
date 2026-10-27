# MRM-UC-08 — NVLT kiểm tra, trả bổ sung và trình duyệt

**Quy tắc hiện tại V19 —:** [CR-013](../../changes/CR-013_borrowing-approval-routing.md) thay thế phần duyệt mượn trong bản dự thảo dưới đây. Không có tử vong: Archivist phê duyệt/từ chối. Có bất kỳ tử vong: Director duyệt mọi hồ sơ trong lượt. Lý do bắt buộc nếu trên 5 hồ sơ hoặc có tử vong; Reader không chọn. Yêu cầu cũ giữ tuyến duyệt trước nâng cấp.

**Trạng thái:** dự thảo v0.1; ưu tiên bắt buộc.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | NVLT |
| Nguồn và quy tắc | FR-09; D-03; BR-05/06/22 |
| Tiền điều kiện | Yêu cầu SUBMITTED, revision hiện hành; NVLT có quyền kiểm tra. |
| Dữ liệu đầu vào | Request/revision/version; ghi chú kiểm tra; lý do bổ sung khi trả lại. |

## Kích hoạt và luồng chính

1. NVLT mở hàng đợi SUBMITTED, xem hồ sơ/người/mục đích/.
2. Kiểm tra đủ thông tin, tình trạng vật lý và khả năng phục vụ; không tự sửa nội dung thay người yêu cầu.
3. Chọn trình duyệt hoặc trả bổ sung với lý do rõ.
4. Máy chủ kiểm tra lại revision/state/version; trình chuyển PENDING_APPROVAL, trả chuyển NEEDS_INFORMATION.
5. Lưu kết quả kiểm tra/người/giờ; hiển thị tiến độ cho người yêu cầu và hàng đợi người duyệt.

## Luồng thay thế và ngoại lệ

A1: Không khả dụng một ngày → trả bổ sung/giữ để xử lý, không tự bỏ ngày vì D-03 duyệt cả revision.

E1: Yêu cầu đã hủy hoặc revision thay → không trình, yêu cầu tải lại.

E2: Hai NVLT xử lý → một chuyển trạng thái; không có hai lần trình độc lập.

A2: Hồ sơ nghi sai metadata → tạm dừng và mở đối soát, không chỉnh request để lách.

## Hậu điều kiện, giao dịch và audit

PENDING_APPROVAL hoặc NEEDS_INFORMATION; physical không đổi. Audit kiểm tra, trình, trả bổ sung kèm lý do.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-08-01:** Thiếu mục đích rõ → trả bổ sung, chủ yêu cầu thấy lý do.
- **AC-08-02:** Revision đã đổi khi NVLT nhấn trình → từ chối ghi version cũ.
- **AC-08-03:** NVLT gọi trực tiếp hành động duyệt → bị từ chối.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.
