# Từ điển dữ liệu logic

**Ghi chú triển khai P2 ngày:** mô hình logic dưới đây bao gồm phần còn dự thảo. V5/V6 hiện thực `record_types`, `retention_policies`, `catalog_events`; `record_types.version` chống biểu mẫu cũ, `departments` cố định năm khoa và giữ các cột lịch sử. Chính sách có DRAFT/APPROVED, active, người/thời điểm xác nhận; bốn nhóm mô phỏng khởi tạo DRAFT theo CR-004. `CatalogSnapshot` hiện là DTO bất biến; chưa có bảng hồ sơ lưu snapshot. Chi tiết tại `docs/design/record-catalogs.md` và `docs/verification/P2_COMPLETION_REPORT.md`.

Dự thảo, các giới hạn độ dài là đề xuất cần đối chiếu dữ liệu thực tế. Đây là thiết kế, không phải SQL hoặc mã nguồn.

Quy ước: PK UUID do server cấp; FK UUID; thời điểm `timestamptz`; ngày `date`; version `bigint` tăng mỗi lần sửa. R=required, O=optional. Trường bí mật không xuất DTO/log. Đối tượng có thể sửa có created_at/by, updated_at/by, version (R); sự kiện chỉ thêm có occurred_at/actor_id (R, actor hệ thống phải chỉ rõ). Dữ liệu nhập được trim; mã chuẩn hóa theo quy tắc đã chốt, giữ giá trị nguồn nếu cần đối soát. Null là chưa có/không áp dụng, không thay bằng chuỗi rỗng hoặc ngày hiện tại.


## APP_USER / ROLE / PERMISSION / ROLE_CHANGE_HISTORY / ROLE_PERMISSION

| Trường | Kiểu logic | Bắt buộc | Kiểm tra/ý nghĩa |
|---|---|---|---|
| APP_USER.id | UUID PK | R | Bất biến |
| username | varchar(64) | R | Unique sau lowercase/trim; ký tự hợp lệ theo chính sách |
| display_name | varchar(200) | R | Tên người dùng |
| department_id | FK DEPARTMENT | O | Bắt buộc cho người đọc nội bộ thuộc khoa; ADMIN có thể không khoa |
| email | varchar(254) | O | Chỉ bắt buộc khi chọn email cho đối tượng này |
| password_hash / approval_secret_hash | text | R / O | Chỉ hash; cấp 2 tách biệt; không log/return |
| status | enum | R | ACTIVE/LOCKED/DISABLED; không xóa actor lịch sử |
| must_change_password | boolean | R | True sau reset |
| failed_login_count / failure_window_started_at / login_blocked_until | integer / timestamp / timestamp | R / O / O | Counter không âm, cửa sổ thử sai và block login tạm theo D-22; độc lập status quản trị và counter cấp 2 |
| ROLE.code / PERMISSION.code | varchar(64) unique | R | ROLE chỉ có READER/ARCHIVIST/DIRECTOR/ADMIN; PERMISSION là mã quyền ổn định |
| role_id | FK ROLE | R | Một giá trị hiện hành/user theo D-25 đã xác nhận; không UNIQUE role_id |
| auth_version | bigint | R | Mặc định 0, tăng khi khóa/đổi role/reset/đổi password, so khớp session mỗi request |
| ROLE_CHANGE_HISTORY.id/user_id/old_role_id/new_role_id | UUID/FK/FK/FK | R/R/O/R | Chỉ thêm sự kiện có actor/time/reason; không cấp quyền song song |
| ROLE_PERMISSION.role_id/permission_id | FK pair unique | R | Cấu hình quyền, thay đổi được audit |

## ACCESS_GRANT / DEPARTMENT

| Trường | Kiểu logic | Bắt buộc | Kiểm tra/ý nghĩa |
|---|---|---|---|
| ACCESS_GRANT.id/user_id/permission_id | UUID/FK | R | Quyền đặc biệt/location/audit/export theo scope |
| scope_type / department_id | enum/FK | R / O | ALL/DEPARTMENT/OWN; DEPARTMENT cần FK; không mở rộng vượt người cấp |
| valid_from/valid_to/revoked_at | timestamp | R / O / O | Hiệu lực [from,to), không xóa quyền cũ |
| basis/granted_by | varchar(1000)/FK | R | Căn cứ quyền nhạy cảm |
| DEPARTMENT.id/code/name | UUID/varchar(32)/varchar(200) | R | Mã unique, có active; không xóa đã tham chiếu |

## STORAGE_LOCATION / RETENTION_POLICY

| Trường | Kiểu logic | Bắt buộc | Kiểm tra/ý nghĩa |
|---|---|---|---|
| STORAGE_LOCATION.id/parent_id | UUID/FK self | R/O | Root không parent; không chu trình |
| code/name/type | varchar(32)/varchar(200)/enum | R | WAREHOUSE/CABINET/SHELF/COMPARTMENT; đúng thứ tự cha con |
| active/capacity | boolean/integer | R/O | Capacity nếu dùng phải dương; không mặc định 1 HSBA/ngăn |
| RETENTION_POLICY.id/code/version_no | UUID/varchar(32)/integer | R | Unique code+version, version dương |
| years/effective_from/effective_to | smallint/date/date | R/R/O | Nhóm theo nguồn 10/15/20/30, khoảng hiệu lực không đảo |
| basis | varchar() | R | Tham chiếu căn cứ do đơn vị cung cấp, trạng thái DRAFT/APPROVED |
| leap_day_rule | enum | R | Đề xuất LAST_DAY_OF_FEBRUARY, chờ D-19 |

## MEDICAL_RECORD

| Trường | Kiểu logic | Bắt buộc | Kiểm tra/ý nghĩa |
|---|---|---|---|
| id | UUID PK | R | ID nội bộ bất biến |
| archive_number/business_year/origin_warehouse_id | varchar(64)/smallint/FK | R | Khóa đề xuất D-04; warehouse gốc không đổi khi di chuyển |
| medical_record_number | varchar(64) | O | Giữ mã nguồn, không tự coi duy nhất |
| patient_code/patient_name | varchar(64)/varchar(200) | R | Patient_code không unique, tên không dùng ghép |
| birth_year/sex | smallint/enum | R | Năm≤năm hiện tại; sex UNKNOWN được phép nếu nguồn thiếu |
| department_id | FK DEPARTMENT | R | Khoa điều trị cuối |
| admission_date/discharge_date | date/date | R | Ra≥vào; lịch sử thiếu phải staging, chưa công bố |
| first_stored_on | date | R cho công bố | Không dùng created_at thay thế; ≤ngày hiện tại |
| record_type/deceased | varchar(32)/boolean | R | Deceased chặn workflow, không chặn quản lý metadata |
| retention_policy_id/retention_years_snapshot/retention_due_on | FK/smallint/date | R/R/R cho xác minh | Tính từ first_stored_on; lưu phiên bản; staging có thể chưa biết |
| current_location_id | FK STORAGE_LOCATION | R khi công bố | Nếu đang đọc là vị trí về dự kiến; không khẳng định vị trí thực tế |
| physical_state | enum | R | IN_STORAGE/AT_READING_ROOM/TEMP_UNAVAILABLE |
| condition/blocked_for_use | enum/boolean | R | NORMAL/DAMAGED/MISSING/NEEDS_RECONCILIATION; mở block phải có căn cứ |
| data_quality/publication_status | enum/enum | R | VERIFIED/NEEDS_REVIEW; STAGING/PUBLISHED; staging không tìm công khai/giao |
| source_reference/initial_batch_id | varchar(500)/UUID | O | Nguồn dữ liệu/lô nhập tay để đối soát |
| notes | varchar() | O | Không chép nội dung bệnh án vào metadata |

## LOCATION_HISTORY / RECORD_ADJUSTMENT

| Trường | Kiểu logic | Bắt buộc | Kiểm tra/ý nghĩa |
|---|---|---|---|
| LOCATION_HISTORY.id/record_id | UUID/FK | R | Sự kiện chỉ thêm |
| from_location_id/to_location_id | FK/FK | O/R | From null chỉ nhập kho đầu tiên |
| from_path_snapshot/to_path_snapshot | varchar(500) | O/R | Giữ địa chỉ lịch sử khi cây thay đổi |
| reason/event_type | varchar(1000)/enum | R | INITIAL_STORAGE/MOVE/RETURN/ADJUSTMENT |
| handover_id/actor_id/occurred_at | FK/FK/timestamp | O/R/R | Nếu chuyển khi trả, tham chiếu handover |
| RECORD_ADJUSTMENT.target_type/target_id/original_event_id | enum/UUID/UUID | R/R/O | Điều chỉnh metadata hoặc lịch sử có thẩm quyền; bản gốc giữ nguyên |
| before_values/after_values/reason/basis | jsonb/jsonb/text/text | R | Chỉ trường hợp lệ, tránh bí mật, audit người xác nhận |

## READING_REQUEST / REQUEST_REVISION / REQUEST_DAY

| Trường | Kiểu logic | Bắt buộc | Kiểm tra/ý nghĩa |
|---|---|---|---|
| READING_REQUEST.id/code/owner_id/record_id | UUID/varchar(64)/FK/FK | R | Code unique; một owner/record trong phương án cơ sở |
| state/current_revision_id | enum/FK | R | State theo thiết kế; current revision phải thuộc request |
| REQUEST_REVISION.id/request_id/revision_no | UUID/FK/integer | R | Unique request+no, tăng dần |
| owner_snapshot/record_snapshot/department_snapshot | FK+snapshot | R khi gửi | Giữ căn cứ nội dung được duyệt, snapshot trường tối thiểu |
| purpose | varchar(1000) | R khi gửi | Đề xuất tối thiểu 10 ký tự; không chứa toàn văn HSBA |
| submitted_at/submitted_by | timestamp/FK | O | Có khi gửi; revision sau gửi bất biến |
| REQUEST_DAY.revision_id/requested_day | FK/date | R | Unique pair; ít nhất một khi submit |
| review_result/review_note/reviewed_by/reviewed_at | enum/text/FK/timestamp | O | Kiểm tra/trả bổ sung/trình; lưu event nếu có nhiều lần |

## STEP_UP_PROOF / APPROVAL_DECISION

| Trường | Kiểu logic | Bắt buộc | Kiểm tra/ý nghĩa |
|---|---|---|---|
| STEP_UP_PROOF.id/user_id/revision_id/action | UUID/FK/FK/enum | R | Gắn một lần quyết định, không lưu secret |
| issued_at/expires_at/consumed_at | timestamp | R/R/O | Chỉ consume nguyên tử cùng quyết định; vô hiệu khi quyền/secret thay đổi |
| DECISION.id/revision_id/actor_id | UUID/FK/FK | R | Unique revision cuối, không sửa/xóa |
| outcome/reason | enum/varchar(1000) | R/O | APPROVE/REJECT; REJECT phải có reason |
| decided_at/authority_snapshot/step_up_proof_id | timestamp/jsonb/FK | R/R/R | Snapshot actor/role DIRECTOR không chứa password; proof gắn đúng revision |

## READING_SESSION / HANDOVER

| Trường | Kiểu logic | Bắt buộc | Kiểm tra/ý nghĩa |
|---|---|---|---|
| SESSION.id/revision_id/decision_id/record_id/reader_id | UUID/FK | R | Cùng revision và snapshot, không gán từ client |
| reading_day/state | date/enum | R | PLANNED/CONFIRMED/READING/RETURNED/CANCELLED/NO_SHOW/EXPIRED |
| confirmed_at/confirmed_by/cancel_reason | timestamp/FK/varchar(1000) | O | Trường đi cùng sự kiện tương ứng |
| HANDOVER.id/session_id/record_id | UUID/FK/FK | R | Unique session; record khớp session |
| handed_at/handed_by/received_by/condition_out | timestamp/FK/FK/text | R | Người nhận đúng reader, không nhận thay bản đầu |
| returned_at/return_received_by/condition_in/return_location_id | timestamp/FK/text/FK | O cùng nhóm | Returned_at≥handed_at, có khi đã thực trả |
| late_reason/discrepancy_note | varchar(1000) | O | Bắt buộc tương ứng trả muộn/chênh lệch |

## RECORD_EXCEPTION / EXCEPTION_EVENT

| Trường | Kiểu logic | Bắt buộc | Kiểm tra/ý nghĩa |
|---|---|---|---|
| EXCEPTION.id/record_id/session_id/handover_id | UUID/FK/FK/FK | R/R/O/O | Phải cùng đối tượng nếu cùng có |
| type/state/description/assignee_id | enum/enum/text/FK | R/R/R/O | OVERDUE/MISSING/DAMAGE/DATA_MISMATCH; OPEN/IN_REVIEW/RESOLVED |
| opened_at/opened_by/resolved_at/resolved_by | timestamp/FK/timestamp/FK | R/R/O/O | Không đóng OVERDUE khi chưa có kết quả hợp lệ |
| EXCEPTION_EVENT.id/exception_id/action/note/basis | UUID/FK/enum/text/text | R/R/R/R/O | Diễn biến chỉ thêm, actor và timestamp; block vẫn giữ nếu chưa giải quyết |

## AUDIT_EVENT / OPERATION_RECEIPT / IN_APP_NOTIFICATION

| Trường | Kiểu logic | Bắt buộc | Kiểm tra/ý nghĩa |
|---|---|---|---|
| AUDIT.id/actor_id/occurred_at/action | UUID/FK/timestamp/varchar(64) | R | System actor riêng khi tự động; auth chưa nhận diện có thể dùng actor_user_id nullable và attempted_username đã làm sạch, không giả user có thật |
| target_type/target_id/outcome/correlation_id | varchar(64)/UUID/enum/UUID | R/O/R/R | SUCCESS/FAILURE/DENIED; target có thể chưa có khi login thất bại |
| before_after/reason | jsonb/text | O | Chỉ trường được phép; không token/password/toàn văn |
| RECEIPT.id/actor_id/action/operation_key | UUID/FK/varchar(64)/UUID | R | Unique actor+action+key |
| payload_fingerprint/result_type/result_id/completed_at | text/enum/UUID/timestamp | R | Cùng transaction; giữ trong vòng đời giao dịch |
| NOTIFICATION.id/recipient_id/request_id/event_type | UUID/FK/FK/enum | R | Thông tin tóm tắt, không vị trí nhạy cảm |
| content/read_at | varchar(1000)/timestamp | R/O | Đọc thông báo không thay state nghiệp vụ |

## Bảng tùy chọn khi được chọn

- IMPORT_BATCH: id, file_hash, filename đã làm sạch, uploader, status, totals, checked_at, approved_by; IMPORT_ROW: batch_id+row_no unique, dữ liệu chuẩn hóa, lỗi, record_id nếu commit. Không lưu toàn văn HSBA.
- NOTIFICATION_OUTBOX: id/event_key unique, recipient, payload tóm tắt, PENDING/ACCEPTED/FAILED, attempt_count, next_attempt_at, accepted_at, lỗi được làm sạch. Không coi ACCEPTED là đã đọc.
- INVENTORY_RUN/ITEM: id, phạm vi, thời điểm snapshot; unique(run,record), vị trí kỳ vọng/thực tế, kết quả, người kiểm; điều chỉnh qua UC-06, không tự ghi đè kho.
- ADMIN_ATTACHMENT: id, loại hành chính được phép, tên hiển thị, storage_key ngoài webroot, mime thực, size, checksum, uploader, access scope, status/quarantine. Không bản quét HSBA.

## Điểm phải xác nhận trước migration

Khóa nghiệp vụ D-04, giới hạn trường theo mẫu thực tế; danh mục sex/type; giá trị thiếu của dữ liệu cũ; ngày gốc/thời hạn; scope quyền; tham số lịch; chính sách chứng cứ; ngôn ngữ tìm kiếm. FK/audit/receipt phải giữ lịch sử, không cascade xóa người dùng/HSBA đã tham chiếu.

## Thu hẹp bốn vai trò (CR-001)

Không có bảng APPROVAL_DELEGATION, không có delegation_id trong decision hoặc vai trò MANAGER/DELEGATE ở schema phiên bản đầu. APP_USER.role_id là FK đơn bắt buộc theo D-25 đã xác nhận; ROLE_CHANGE_HISTORY chỉ giữ sự kiện thay role, không cấp nhiều role hiện hành. Không cần migration xóa bảng vì schema ứng dụng chưa được triển khai.

Chi tiết bộ đếm đăng nhập, session, lỗi và audit: [thiết kế đăng nhập](../design/authentication/login-design.md). Đây vẫn là thiết kế, chưa tạo bảng hoặc migration.
