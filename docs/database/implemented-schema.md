# Schema đang triển khai cho năm chức năng ưu tiên

**Mốc V14 —:** thêm reading_request_batches; reading_requests có batch_id, registered_on, pickup_deadline sinh tự động (+2), scheduling_mode. Yêu cầu mới không có session đến khi Archivist xếp lịch; cũ LEGACY giữ nguyên. Trigger chặn ngày nhận mới ngoài hạn. Xem [CR-011](../changes/CR-011_borrowing-batches-and-pickup-window.md); schema tiếp theo V15.


**Mốc V13 —:** bảng employees(citizen_id PK 10 chữ số, account_id UNIQUE FK, full_name, birth_date, gender, role FK). Trigger giữ tên/role khớp app_users. Tài khoản cũ chưa có thông tin vẫn được giữ nguyên, không tạo dữ liệu giả. Xem [mô hình](../verification/EMPLOYEE_MANAGEMENT.md); migration tiếp theo V14.


**Mốc V12 —:** theo làm rõ của người dùng, requester_name_snapshot lưu tên đăng nhập (reader1), thay cho display_name. Cập nhật dữ liệu cũ và SELECT username khi gửi yêu cầu mới; giữ FK requester_id. V11 bên dưới là lịch sử. Thay đổi tiếp theo dùng V13.

**Mốc V11 —:** reading_requests thêm requester_name_snapshot VARCHAR(200) NOT NULL, giữ requester_id FK. Bảng phiên/sự kiện/thông báo truy người yêu cầu qua request_id. Dữ liệu trước V11 được điền bằng tên hiện tại khi nâng cấp; xem [CR-009](../changes/CR-009_requester-name-snapshot.md). Migration tiếp theo dùng V12.

**Mốc V10 —:** notifications thêm recipient_role và thông báo công việc đang chờ; quyền xem Reader suy từ reading_requests/reading_sessions, không có bảng cấp quyền mới. Xem [CR-008](../changes/CR-008_workflow-alerts-and-reader-access.md). Thay đổi schema tiếp theo dùng V11; các mốc V7–V9 bên dưới là lịch sử.


**Mốc V9 ngày:** bảng `notifications` liên kết tài khoản, yêu cầu và sự kiện; lưu nội dung/thời điểm tạo/đọc, unique người nhận/sự kiện và chỉ mục hộp thư/chưa đọc. Xem [FR-11](../verification/FR11_INTERNAL_NOTIFICATIONS.md). V9 đã áp dụng; thay đổi schema tiếp theo dùng V10 trở lên. Các mốc dưới đây là lịch sử.

**Mốc V8 ngày:** thêm schema request/phiên đọc/tài khoản/audit/kiểm kê. Xem [báo cáo FR](../verification/FR_COMPLETION_REPORT.md). Các bảng/sơ đồ V7 bên dưới được giữ để giải thích nền tảng; V7 không còn là phiên bản mới nhất. Thay đổi mới cần V9 trở lên.

Ngày; Flyway V1–V7. Sơ đồ này mô tả code đang chạy, tách với ERD dự thảo toàn bộ hệ thống trong `erd.md`.

```mermaid
erDiagram
 roles ||-o{ app_users: role_id
 app_users ||-o{ departments: created_by
 departments ||-o{ medical_records: department_id
 record_types ||-o{ medical_records: record_type_id
 storage_locations o|-o{ storage_locations: parent_id
 storage_locations o|-o{ medical_records: current_location_id
 medical_records ||-o{ medical_record_policies: record_id
 retention_policies ||-o{ medical_record_policies: policy_id
 medical_records ||-o{ record_events: record_id
 medical_records ||-o{ record_location_history: record_id
 storage_locations ||-o{ location_events: location_id
 storage_locations ||-o{ record_location_history: to_location_id
 storage_locations o|-o{ record_location_history: from_location_id
 app_users ||-o{ medical_records: created_by_updated_by
```

Các quan hệ người tạo/duyệt danh mục và sự kiện danh mục được lược bớt để sơ đồ dễ đọc. `archive_counters` quản lý số theo tháng độc lập, không FK trực tiếp tới từng hồ sơ. Cấu trúc chính xác của V1–V7 nằm trong các Java migration; không copy sơ đồ thành SQL migration thứ hai.

## Các nhóm dữ liệu trong medical_records

| Nhóm | Cột |
|---|---|
| Định danh/chống gửi lặp | id, create_token, create_fingerprint, archive_number |
| Người bệnh/đợt điều trị | medical_record_number, patient_code, patient_name, birth_year, sex, admission_date, discharge_date, deceased |
| Danh mục nguồn | department_id, record_type_id |
| Snapshot danh mục | department_name_snapshot, record_type_code_snapshot, record_type_name_snapshot |
| Lưu trữ | first_stored_on, current_location_id, retention_years_snapshot, retention_due_on |
| Trạng thái/đối soát | record_state, physical_status, quality_status, source_note, note, draft_policy_ids |
| Lịch sử chỉnh sửa | version, created_by, updated_by, created_at, updated_at |

`medical_record_policies` lưu một dòng mỗi nhóm được áp dụng, gồm FK policy và mã/tên/phiên bản/số năm/hiệu lực/căn cứ snapshot. `draft_policy_ids` chỉ là lựa chọn tạm của nháp, chưa phải chính sách áp dụng chính thức.

## Ràng buộc và transaction

- Một tài khoản một role qua `role_id`. Không tạo bảng user_roles đa vai trò.
- Số lưu trữ unique toàn hệ thống; mã người bệnh không unique. Phiếu tạo và mã chuyển cũng unique trong bảng tương ứng.
- Ngày ra không trước ngày vào; ngày đầu lưu không trước ngày ra; Service bổ sung kiểm tra ngày tương lai và dữ liệu bắt buộc.
- DRAFT không có vị trí hiện tại và có physical_status NOT_STORED. STORED cần các trường khóa/danh mục/ngày/vị trí/thời hạn; Service bổ sung kiểm tra chính sách, nguồn và trạng thái danh mục.
- `storage_locations` tự tham chiếu, mã unique trong cha kể cả cha NULL; trigger kiểm tra đúng cấp và giữ code/type/parent bất biến.
- `RecordService.create/update`: hồ sơ + chính sách snapshot + audit + lịch sử lần nhập đầu cùng transaction.
- `RecordService.move`: vị trí hiện tại + version + lịch sử + audit cùng transaction; khóa dòng và khóa kho chống cạnh tranh.
- `StorageService`: dữ liệu vị trí + audit cùng transaction; kiểm tra số đang chứa trước đổi sức chứa/ngừng.
- `CatalogService`: dữ liệu danh mục + audit cùng transaction; chặn version cũ hoặc chính sách chồng hiệu lực.

## Chỉ mục hiện tại và giới hạn

UNIQUE tạo index cho số lưu trữ, mã danh mục và mã thao tác. V7 thêm index mã người bệnh, khoa/ngày ra/UUID, vị trí hồ sơ STORED, sự kiện và lịch sử theo hồ sơ/thời gian. Tìm chứa tên có/không dấu dùng `mrm_search_key`; chưa có index trigram hoặc kết quả benchmark tải lớn.

V7 không tạo bảng lịch đọc/giao–trả. Không diễn giải READING hiện có như bằng chứng các bảng hoặc luồng này đã hoàn thiện. Thay đổi cấu trúc tiếp theo cần V8 trở lên.

Xem [báo cáo triển khai](../verification/FIVE_FEATURE_COMPLETION_REPORT.md) để đọc điều kiện nghiệp vụ và các ca kiểm thử.

## Bổ sung V8 Request đọc và kiểm kê

```mermaid
erDiagram
 app_users ||-o{ reading_requests: requester_reviewer_approver
 medical_records ||-o{ reading_requests: record_id
 reading_requests ||-|{ reading_sessions: request_id
 reading_requests ||-o{ request_events: revision_history
 medical_records ||-o{ reading_sessions: record_id
 storage_locations o|-o{ reading_sessions: return_location
 inventory_batches ||-o{ inventory_items: batch_id
 medical_records ||-o{ inventory_items: record_id
 storage_locations ||-o{ inventory_items: expected_location
```

`audit_events` lưu actor dạng username thay vì FK để còn dấu vết khi tài khoản thay đổi. Cột account version hỗ trợ thu hồi phiên cũ; approval_secret_hash không được hiển thị/xuất. Unique partial index trên record/date với trạng thái CONFIRMED/READING/RETURNED và record với trạng thái READING chặn trùng lịch/giao.

Tài liệu V7 ở trên là lịch sử; sau V8, migration tiếp theo dùng V9, không chỉnh V7/V8 đã chạy.
