# ERD logic và ràng buộc — chưa phải migration

Mô hình D-02: một request/một HSBA/nhiều ngày. PostgreSQL server hiện có 16.5. Sơ đồ mô tả quan hệ nghiệp vụ; chi tiết khóa, trường và tùy chọn trong [từ điển dữ liệu](data-dictionary.md).

```mermaid
erDiagram
 DEPARTMENT ||-o{ APP_USER: belongs_to
 ROLE ||-o{ APP_USER: current_role
 APP_USER ||-o{ ROLE_CHANGE_HISTORY: role_changes
 ROLE ||-o{ ROLE_CHANGE_HISTORY: new_role
 ROLE ||-o{ ROLE_PERMISSION: grants
 PERMISSION ||-o{ ROLE_PERMISSION: defines
 APP_USER ||-o{ ACCESS_GRANT: receives_scope
 STORAGE_LOCATION o|-o{ STORAGE_LOCATION: parent_of
 STORAGE_LOCATION ||-o{ MEDICAL_RECORD: current_or_return_location
 RETENTION_POLICY ||-o{ MEDICAL_RECORD: classified_by
 DEPARTMENT ||-o{ MEDICAL_RECORD: treating_department
 MEDICAL_RECORD ||-o{ LOCATION_HISTORY: moved
 APP_USER ||-o{ READING_REQUEST: owns
 MEDICAL_RECORD ||-o{ READING_REQUEST: requested_for
 READING_REQUEST ||-|{ REQUEST_REVISION: versions
 REQUEST_REVISION ||-|{ REQUEST_DAY: proposes
 REQUEST_REVISION ||-o| APPROVAL_DECISION: decided
 REQUEST_REVISION ||-o{ READING_SESSION: schedules
 APPROVAL_DECISION ||-o{ READING_SESSION: permits
 READING_SESSION ||-o| HANDOVER: fulfilled_by
 MEDICAL_RECORD ||-o{ HANDOVER: lent
 MEDICAL_RECORD ||-o{ RECORD_EXCEPTION: has
 HANDOVER o|-o{ RECORD_EXCEPTION: may_cause
 RECORD_EXCEPTION ||-o{ EXCEPTION_EVENT: progresses
 APP_USER ||-o{ AUDIT_EVENT: acts
 APP_USER ||-o{ OPERATION_RECEIPT: retries
 READING_REQUEST ||-o{ IN_APP_NOTIFICATION: notifies
```

Bản nháp được lưu bằng revision đầu DRAFT; ngày có thể chưa đủ khi nháp, chỉ bắt buộc ít nhất một ngày khi SUBMITTED. Quan hệ ERD “một hoặc nhiều” diễn tả bản gửi hợp lệ, không ép nháp đủ trường. Catalog chỉ có READER/ARCHIVIST/DIRECTOR/ADMIN. APP_USER.role_id bắt buộc tham chiếu đúng một ROLE theo D-25 đã xác nhận. ROLE_CHANGE_HISTORY chỉ là sự kiện đổi vai trò, không cấp thêm quyền; giữ lịch sử khi thay. ACCESS_GRANT chỉ thu hẹp/bổ sung quyền xem vị trí/audit/xuất trong giới hạn vai trò, không cấp quyền duyệt cho vai trò khác.

Các bảng IMPORT_BATCH/IMPORT_ROW/NOTIFICATION_OUTBOX/INVENTORY_* /ADMIN_ATTACHMENT chỉ dùng khi chọn optional; không nằm trong schema bắt buộc đầu tiên. Người nhập tay có đối soát vẫn cần provenance/initial_batch trong hồ sơ, chưa cần engine import file.

## Ràng buộc bắt buộc

1. Khóa nghiệp vụ HSBA theo D-04 còn chờ xác nhận: namespace kho gốc + năm đầu lưu + số lưu trữ chuẩn hóa; giữ kho gốc ổn định khi chuyển vị trí. Không UNIQUE mã bệnh nhân.
2. Vị trí không chu trình; mã unique trong cùng cha, kho gốc unique riêng. Không vô hiệu hóa vị trí đang có current reference.
3. UNIQUE(request_id, revision_no); request.current_revision phải thuộc đúng request. Snapshot HSBA/owner/revision không bị thay sau gửi.
4. UNIQUE(revision_id, requested_day), ngày hợp lệ; ít nhất một ngày khi gửi; validation theo lịch và giới hạn.
5. UNIQUE(revision_id) trên quyết định cuối. Chứng cứ cấp 2 một lần gắn user/revision/action, hết hạn không dùng lại.
6. UNIQUE(revision_id, reading_day) trên session; mọi session liên kết decision cùng revision. Record/reader của session phải khớp snapshot, không cho client gán.
7. Ràng buộc unique có điều kiện trên (record_id, reading_day) cho session CONFIRMED/READING/RETURNED theo D-05. PLANNED chưa giữ chỗ; RETURNED vẫn giữ giới hạn một lượt/. CANCELLED/NO_SHOW giải phóng chỗ; tái xác nhận chỉ khi còn giờ phục vụ. Nếu cần nhiều ca phải đổi mô hình trước code.
8. UNIQUE(session_id) trên handover. UNIQUE(record_id) có điều kiện returned_at trống: tối đa một lượt mở toàn bộ các ngày, kể cả phiên quá hạn hôm trước.
9. returned_at≥handed_at; nhóm trường nhận trả đi cùng nhau. READING/phần physical phải cập nhật cùng transaction; không thể chỉ dùng CHECK liên bảng, cần service/khóa và ca kiểm chứng bất biến.
10. Chỉ actor đang hoạt động với vai trò DIRECTOR được ghi quyết định; giữ authority_snapshot tại lúc ghi, không có delegation_id hoặc bảng ủy quyền trong schema bản đầu. APP_USER.role_id NOT NULL là một FK đơn, không UNIQUE role_id vì nhiều tài khoản có thể cùng vai trò. Đổi role tăng auth_version, ghi ROLE_CHANGE_HISTORY và audit; thu hồi session cũ ở request tiếp theo.
11. UNIQUE(actor_id, action, operation_key) trên receipt, payload fingerprint và result reference. Receipt chỉ được ghi cùng transaction kết quả, không coi receipt dang dở là thành công.
12. Ngoại lệ OVERDUE mở tối đa một/ handover, chạy đối soát lặp không nhân đôi; không áp constraint này cho loại khác khi nhiều sự cố thật.
13. Audit không được sửa/xóa qua ứng dụng. FK lịch sử không cascade xóa. Timestamp event không thay bằng giờ trình duyệt.

## D-05 cần rà soát trước lập trình

Đề xuất thống nhất cuối cho tài liệu: **một HSBA tối đa một phiên CONFIRMED/READING/RETURNED trong cùng ngày**; CANCELLED/NO_SHOW giải phóng chỗ nhưng chỉ tái xác nhận khi vẫn còn thời gian phục vụ. Cách này mạnh hơn “không giao trùng cùng lúc”; nếu muốn nhiều ca/ngày phải thêm slot và sửa BR-09/UC-11. Mọi tài liệu sử dụng cùng tập giữ chỗ này.

## Xóa, điều chỉnh và chuyển đổi

Không xóa cứng hồ sơ/yêu cầu/decision/handover đã dùng. Nháp có thể hủy, giữ dấu vết tối thiểu. Sai dữ liệu qua adjustment có actor/reason/căn cứ, before/after, tham chiếu event gốc; điều chỉnh không tự đánh dấu thực trả khi thất lạc. Di chuyển cây vị trí có kiểm soát, không sửa địa chỉ lịch sử.

Thiết kế chi tiết lát cắt đăng nhập: [login-design.md](../design/authentication/login-design.md). Không dùng bảng nhiều-nhiều USER_ROLE cho vai trò hiện hành trong bản đầu; không có bảng session PostgreSQL trong phương án một máy chủ dùng session memory.
