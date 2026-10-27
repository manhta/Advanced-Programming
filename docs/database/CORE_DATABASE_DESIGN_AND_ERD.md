# Báo cáo thiết kế cơ sở dữ liệu: 5 bảng cốt lõi

## 1. Phạm vi và căn cứ

Báo cáo chọn năm bảng cốt lõi theo luồng nghiệp vụ xuyên suốt của MRM: tài khoản thực hiện thao tác, hồ sơ được quản lý, một lượt đăng ký có thể gom nhiều hồ sơ, từng yêu cầu được xét duyệt riêng, và phiên giao nhận/đọc được theo dõi. Tên bảng và các ràng buộc dưới đây đối chiếu với các Java migration hiện có, nổi bật là V1, V7–V8, V11–V14, V17, V19, V23 và V26.

Năm bảng được chọn:

1. `app_users` — tài khoản và định danh người thao tác.
2. `medical_records` — nguồn dữ liệu hồ sơ bệnh án thống nhất, gồm bản giấy và bản điện tử.
3. `reading_request_batches` — thông tin chung cho một lần Reader đăng ký nhiều hồ sơ.
4. `reading_requests` — yêu cầu riêng cho từng hồ sơ trong lượt đăng ký.
5. `reading_sessions` — lịch nhận, giao hồ sơ, đọc và trả hồ sơ.

Đây là năm bảng trọng tâm để hiểu dữ liệu nghiệp vụ, không phải toàn bộ schema. Các bảng `roles`, `employees`, `request_events`, `notifications`, `storage_locations`, `record_location_history`, `medical_record_policies`, `audit_events` cùng các bảng danh mục vẫn cần thiết và được nhắc đến ở các phần liên quan.

## 2. ERD tổng quan

Sơ đồ sau tập trung vào năm bảng đã chọn. `app_users` được nối nhiều lần với `reading_requests` và `reading_sessions` vì nhiều cột FK biểu diễn các vai trò thao tác khác nhau. Cột `reading_requests.batch_id` nullable để giữ tương thích với yêu cầu lịch sử tạo trước khi hệ thống có đăng ký theo lô.

```mermaid
erDiagram
 APP_USERS ||-o{ READING_REQUEST_BATCHES: "requester_id"
 READING_REQUEST_BATCHES o|-o{ READING_REQUESTS: "batch_id; null for legacy"
 APP_USERS ||-o{ READING_REQUESTS: "requester_id"
 MEDICAL_RECORDS ||-o{ READING_REQUESTS: "record_id"
 READING_REQUESTS ||-o{ READING_SESSIONS: "request_id"
 MEDICAL_RECORDS ||-o{ READING_SESSIONS: "record_id"
 APP_USERS o|-o{ READING_SESSIONS: "handed_by / received_by / returned_by"

 APP_USERS {
 uuid id PK
 varchar username UK
 varchar display_name
 uuid role_id FK
 varchar status
 boolean must_change_password
 timestamptz deleted_at
 }
 MEDICAL_RECORDS {
 uuid id PK
 varchar record_code UK
 varchar archive_number UK "nullable for electronic-only"
 varchar patient_name
 smallint birth_year
 uuid department_id FK
 uuid record_type_id FK
 boolean deceased
 boolean has_paper_copy
 boolean has_electronic_copy
 varchar record_state
 varchar submission_state
 uuid current_location_id FK
 }
 READING_REQUEST_BATCHES {
 uuid id PK
 uuid submit_token UK
 uuid requester_id FK
 varchar purpose
 date registered_on
 boolean requires_director
 timestamptz created_at
 }
 READING_REQUESTS {
 uuid id PK
 uuid submit_token UK
 uuid batch_id FK
 uuid record_id FK
 uuid requester_id FK
 varchar requester_name_snapshot
 varchar purpose
 varchar state
 boolean requires_director
 date registered_on
 date pickup_deadline "generated as registered_on + 2"
 integer revision
 bigint version
 uuid reviewer_id FK
 uuid approver_id FK
 uuid confirmed_by FK
 timestamptz created_at
 timestamptz updated_at
 }
 READING_SESSIONS {
 uuid id PK
 uuid request_id FK
 uuid record_id FK
 date reading_on
 varchar state
 timestamptz handed_at
 uuid handed_by FK
 uuid received_by FK
 timestamptz returned_at
 uuid returned_by FK
 uuid return_location_id FK
 }
```

> Schema hiện tại của `medical_records` dùng `birth_year SMALLINT`, không có ngày/tháng sinh cho bệnh nhân.

### Mã PlantUML tương đương

Nếu cần render bằng PlantUML, có thể dùng đoạn dưới. Các quan hệ người dùng được ghi theo FK thực tế; các bảng danh mục ngoài phạm vi năm bảng được mô tả bằng ghi chú.

```plantuml
@startuml
hide circle
skinparam linetype ortho
skinparam classAttributeIconSize 0

entity "app_users" as app_users {
 * id: UUID <<PK>>
 -
 * username: VARCHAR(64) <<UK>>
 * password_hash: TEXT
 * display_name: VARCHAR(200)
 * role_id: UUID <<FK roles.id>>
 * status: VARCHAR(20)
 * version: BIGINT
 * must_change_password: BOOLEAN
 deleted_at: TIMESTAMPTZ
}

entity "medical_records" as medical_records {
 * id: UUID <<PK>>
 -
 * record_code: VARCHAR(64) <<UK>>
 archive_number: VARCHAR(64) <<UK, nullable>>
 * patient_name: VARCHAR(200)
 birth_year: SMALLINT
 * sex: VARCHAR(10)
 department_id: UUID <<FK>>
 record_type_id: UUID <<FK>>
 * deceased: BOOLEAN
 * has_paper_copy: BOOLEAN
 * has_electronic_copy: BOOLEAN
 * record_state: VARCHAR(16)
 submission_state: VARCHAR(20)
 current_location_id: UUID <<FK, nullable>>
}

entity "reading_request_batches" as batches {
 * id: UUID <<PK>>
 -
 * submit_token: UUID <<UK>>
 * requester_id: UUID <<FK app_users.id>>
 * purpose: VARCHAR()
 * registered_on: DATE
 * requires_director: BOOLEAN
 * created_at: TIMESTAMPTZ
}

entity "reading_requests" as requests {
 * id: UUID <<PK>>
 -
 * submit_token: UUID <<UK>>
 batch_id: UUID <<FK batches.id, nullable>>
 * record_id: UUID <<FK medical_records.id>>
 * requester_id: UUID <<FK app_users.id>>
 * requester_name_snapshot: VARCHAR(200)
 * purpose: VARCHAR()
 * state: VARCHAR(20)
 * requires_director: BOOLEAN
 * registered_on: DATE
 pickup_deadline: DATE <<generated>>
 * revision: INTEGER
 * version: BIGINT
 reviewer_id: UUID <<FK app_users.id>>
 approver_id: UUID <<FK app_users.id>>
 confirmed_by: UUID <<FK app_users.id>>
 * created_at: TIMESTAMPTZ
 * updated_at: TIMESTAMPTZ
}

entity "reading_sessions" as sessions {
 * id: UUID <<PK>>
 -
 * request_id: UUID <<FK requests.id>>
 * record_id: UUID <<FK medical_records.id>>
 * reading_on: DATE
 * state: VARCHAR(16)
 handed_at: TIMESTAMPTZ
 handed_by: UUID <<FK app_users.id>>
 received_by: UUID <<FK app_users.id>>
 returned_at: TIMESTAMPTZ
 returned_by: UUID <<FK app_users.id>>
 return_location_id: UUID <<FK storage_locations.id>>
}

app_users ||-o{ batches: requester_id
batches o|-o{ requests: batch_id
app_users ||-o{ requests: requester_id
medical_records ||-o{ requests: record_id
requests ||-o{ sessions: request_id
medical_records ||-o{ sessions: record_id
app_users o|-o{ sessions: "handover / return actors"

note right of app_users
 role_id tham chiếu roles.id.
 employees là bảng hồ sơ nhân viên 1:1
 với tài khoản; không nằm trong lát cắt 5 bảng.
end note

note bottom of medical_records
 department_id, record_type_id, current_location_id
 tham chiếu danh mục/vị trí ngoài sơ đồ.
 Schema bệnh nhân hiện lưu birth_year, không lưu birth_date.
end note

note bottom of requests
 request_events ghi lịch sử xử lý; notifications
 lưu thông báo chuông. Hai bảng này được lược khỏi
 sơ đồ 5 bảng, không có nghĩa là không tồn tại.
end note
@enduml
```

## 3. Phân tích 5 bảng

### 3.1 `app_users` — tài khoản thao tác

**Vai trò:** định danh người đăng nhập, xác định quyền hiện tại và liên kết các thao tác nghiệp vụ với người chịu trách nhiệm. Một tài khoản chỉ có một role hiện hành. Bốn role đang dùng là Reader, Archivist, Director và Admin.

**Các cột cốt lõi hiện có:**

| Cột | Ý nghĩa | Ràng buộc/ghi chú |
|---|---|---|
| `id` | Định danh tài khoản nội bộ | UUID, khóa chính |
| `username` | Tên đăng nhập | Bắt buộc, duy nhất, tối đa 64 ký tự |
| `password_hash` | Mật khẩu đã băm | Không lưu mật khẩu thô, không đưa vào giao diện/log |
| `display_name` | Tên hiển thị | Bắt buộc, tối đa 200 ký tự |
| `role_id` | Role hiện tại | FK đến `roles.id`, bắt buộc; mỗi user đúng một role |
| `status` | Trạng thái tài khoản | `ACTIVE`, `LOCKED`, `DISABLED` |
| `email` | Email liên hệ | Có thể rỗng; email thật chưa được dùng làm kênh gửi trong phạm vi hiện tại |
| `department_id` | Khoa/phòng | FK đến `departments.id`, tùy tài khoản/nghiệp vụ |
| `approval_secret_hash` | Hash mật khẩu cấp 2 của Director | Có thể rỗng với role khác |
| `version` | Kiểm soát cập nhật tài khoản | Tăng theo các thay đổi tài khoản cần kiểm soát phiên bản |
| `must_change_password` | Buộc thay mật khẩu lần đầu | Mặc định true cho tài khoản tạo mới sau migration V23 |
| `deleted_at` | Đánh dấu lưu trữ tài khoản | Soft delete từ V26 để giữ FK và lịch sử |
| `created_at` | Thời điểm tạo | `TIMESTAMPTZ`, mặc định thời gian DB |

`roles` và `employees` là bảng hỗ trợ quan trọng nhưng không được tính vào năm bảng nghiệp vụ cốt lõi. `employees.account_id` là unique FK đến `app_users.id`; vì vậy thông tin nhân sự không nên bị nhét trùng vào các yêu cầu mượn.

### 3.2 `medical_records` — hồ sơ bệnh án thống nhất

**Vai trò:** nguồn chuẩn duy nhất cho metadata của hồ sơ giấy và hồ sơ điện tử. Sau hợp nhất, hệ thống không giữ một bảng hồ sơ điện tử nghiệp vụ riêng; `electronic_records` chỉ còn là view tương thích chỉ đọc.

**Các cột quan trọng:**

| Nhóm | Cột và ý nghĩa |
|---|---|
| Định danh | `id` UUID PK; `record_code` mã hiển thị duy nhất; `archive_number` số lưu trữ, có thể null cho hồ sơ chỉ điện tử; `medical_record_number` số bệnh án; `create_token` và `create_fingerprint` hỗ trợ chống gửi lặp |
| Người bệnh | `patient_code`, `patient_name`, `birth_year`, `sex`, `deceased` |
| Phân loại | `department_id`, `record_type_id`; các snapshot như `department_name_snapshot`, `record_type_name_snapshot` giữ ý nghĩa danh mục tại thời điểm ghi nhận |
| Điều trị | `admission_date`, `discharge_date` |
| Hình thức | `has_paper_copy`, `has_electronic_copy`; ít nhất một trong hai phải true |
| Kho giấy | `first_stored_on`, `current_location_id`, `physical_status`, `retention_years_snapshot`, `retention_due_on`; hồ sơ điện tử-only không có số lưu trữ/vị trí vật lý/thời hạn kho giấy |
| Trạng thái quy trình | `record_state` (`DRAFT`, `STORED`, `PUBLISHED`); `submission_state` lưu trạng thái khai báo/xét duyệt khi có |
| Chất lượng/ghi chú | `quality_status`, `source_note`, `note`, `draft_policy_ids` |
| Kiểm soát/audit | `version`, `created_by`, `updated_by`, `created_at`, `updated_at`; các actor là FK tài khoản |

**Ràng buộc nghiệp vụ nổi bật:** `record_code` duy nhất và bất biến; `archive_number` duy nhất nếu có; ngày ra viện không trước ngày vào viện; ngày lưu kho không trước ngày ra viện; hồ sơ giấy đã nhập kho phải có đủ metadata, vị trí và snapshot thời hạn; hồ sơ điện tử-only không được mang vị trí kho giấy. Không ghép bệnh nhân chỉ dựa vào tên.

### 3.3 `reading_request_batches` — lượt đăng ký mượn

**Vai trò:** giữ phần thông tin chung khi Reader gửi nhiều hồ sơ trong một thao tác. Bảng này giúp lý do, ngày đăng ký và tuyến phê duyệt thống nhất cho cả lượt đăng ký, trong khi từng hồ sơ vẫn được xử lý và theo dõi riêng.

| Cột | Ý nghĩa | Ràng buộc/ghi chú |
|---|---|---|
| `id` | UUID của lượt đăng ký | PK |
| `submit_token` | Khóa idempotency của thao tác gửi | Unique để chống gửi lặp |
| `requester_id` | Người gửi | FK đến `app_users.id` |
| `purpose` | Lý do dùng chung | Bắt buộc, không rỗng sau trim, tối đa ký tự; quy tắc yêu cầu lý do phụ thuộc số lượng/loại hồ sơ |
| `registered_on` | Ngày gửi | Bắt buộc, kiểu DATE |
| `requires_director` | Tuyến duyệt của cả batch | Bất biến sau khi gửi; true nếu lượt có hồ sơ tử vong theo quy tắc hiện tại |
| `created_at` | Thời điểm tạo | `TIMESTAMPTZ`, mặc định DB |

Một batch có thể có nhiều `reading_requests`. Tuyến phê duyệt được sao xuống từng request; FK ghép `(batch_id, requires_director)` bảo đảm cờ tuyến ở request khớp batch, không thể tùy ý sửa sau gửi. Các batch lịch sử vẫn giữ tuyến phê duyệt cũ.

### 3.4 `reading_requests` — yêu cầu trên từng hồ sơ

**Vai trò:** là bản ghi trung tâm của workflow mượn. Mỗi dòng xác định một người yêu cầu một hồ sơ cụ thể; Archivist/Director đưa quyết định ở cấp dòng, tránh việc một hồ sơ trong batch bị lẫn trạng thái với hồ sơ khác.

| Cột | Ý nghĩa | Ràng buộc/ghi chú |
|---|---|---|
| `id` | UUID yêu cầu | PK |
| `submit_token` | Token thao tác gửi | Unique |
| `batch_id` | Batch cha | FK nullable vì yêu cầu cũ trước V14 không có batch |
| `record_id` | Hồ sơ được yêu cầu | FK `medical_records.id` |
| `requester_id` | Tài khoản yêu cầu | FK `app_users.id` |
| `requester_name_snapshot` | Tên đăng nhập snapshot tại lúc gửi | NOT NULL; V12 chốt snapshot là username, giữ danh tính hiển thị lịch sử dù tài khoản đổi tên |
| `purpose` | Lý do mượn | Bắt buộc theo cấu trúc hiện tại; nghiệp vụ có thể lưu thông báo mặc định khi lý do không bắt buộc |
| `state` | Trạng thái workflow | Check constraint giới hạn tập trạng thái được phép trong migration ban đầu; code migration mới có thể mở rộng, phải kiểm tra schema đang chạy khi thêm trạng thái |
| `requires_director` | Yêu cầu Director duyệt hay không | Bất biến sau khi gửi; phải đồng bộ với batch nếu `batch_id` có giá trị |
| `registered_on` | Ngày đăng ký | DATE, được backfill cho dữ liệu cũ; ngày nhận không lưu ở đây |
| `pickup_deadline` | Hạn nhận | Cột generated = `registered_on + 2` ngày lịch; trigger chặn `reading_sessions.reading_on` ngoài khoảng hợp lệ với request mới |
| `revision`, `version` | Revision nội dung và optimistic locking | Giúp phát hiện cập nhật cũ/xung đột |
| `reviewer_id`, `reviewed_at`, `review_note` | Kiểm tra bởi Archivist | FK/timestamp/note; có thể rỗng trước khi xử lý |
| `approver_id`, `decided_at`, `decision_note` | Quyết định Director khi cần | FK/timestamp/note |
| `confirmed_by`, `confirmed_at`, `confirmation_note` | Xác nhận lịch bởi Archivist | Lịch cụ thể được phản ánh ở session tương ứng |
| `created_at`, `updated_at` | Mốc tạo/sửa | Timestamp có timezone |

**Ràng buộc quan trọng:** unique `(batch_id, record_id)` ngăn cùng một hồ sơ lặp trong một lượt đăng ký; `submit_token` chống gửi trùng; request phải tham chiếu tài khoản và hồ sơ còn tồn tại. Hủy/ra quyết định nên giữ lịch sử thay vì xóa cứng.

### 3.5 `reading_sessions` — lịch và giao nhận/đọc/trả

**Vai trò:** ghi nhận từng lịch nhận/đọc gắn với yêu cầu và hồ sơ, bao gồm giao bản giấy, trả bản giấy và vị trí trả lại. Bảng này là nơi cần xem khi cần xác định hồ sơ đang thực sự được mượn/đang ở phòng đọc; chỉ trạng thái `APPROVED` của yêu cầu chưa có nghĩa hồ sơ đã được giao.

| Cột | Ý nghĩa | Ràng buộc/ghi chú |
|---|---|---|
| `id` | UUID session | PK |
| `request_id` | Yêu cầu nguồn | FK `reading_requests.id` |
| `record_id` | Hồ sơ | FK `medical_records.id` |
| `reading_on` | Ngày nhận/đọc được Archivist xếp | DATE NOT NULL; trigger kiểm tra cửa sổ đăng ký + 2 ngày lịch cho yêu cầu theo lịch mới |
| `state` | Trạng thái phiên | Check trong migration V8; xem enum Java/service hiện hành trước khi dùng để suy ra trạng thái UI |
| `handed_at`, `handed_by` | Thời điểm/người giao hồ sơ | Nullable trước khi giao; actor FK `app_users` |
| `received_by` | Người Reader nhận | FK tài khoản, nullable trước giao |
| `returned_at`, `returned_by` | Thời điểm/người nhận trả | Null nghĩa là chưa ghi nhận trả; không dùng trạng thái yêu cầu để thay thế timestamp trả thực tế |
| `hand_condition`, `return_condition` | Tình trạng lúc giao/nhận lại | Text giới hạn 1000 ký tự |
| `return_location_id` | Vị trí hồ sơ sau khi trả | FK `storage_locations.id` |
| `exception_note`, `exception_at` | Ghi nhận ngoại lệ | Các trường ghi chú lịch sử theo schema V8; UI hiện có thể không cung cấp mọi thao tác ngoại lệ |

**Chống giao trùng:** unique partial index `ux_daily_reading` trên `(record_id, reading_on)` cho các phiên giữ chỗ (`CONFIRMED`, `READING`, `RETURNED` trong V8 ban đầu); partial index `ux_one_open_handover` giới hạn một phiên `READING` cho mỗi hồ sơ. Trigger V14 kiểm tra hạn nhận. Các ràng buộc cụ thể nên được đối chiếu với migration mới nhất nếu workflow được mở rộng.

## 4. Luồng dữ liệu chính

1. Reader đã đăng nhập (`app_users`) gửi một batch. Hệ thống ghi một `reading_request_batches` và tạo một `reading_requests` cho từng hồ sơ.
2. Mỗi request trỏ đến một `medical_records`; cờ `requires_director` chốt tuyến xử lý theo nội dung của batch.
3. Archivist kiểm tra yêu cầu; nếu tuyến yêu cầu Director, Director ra quyết định. Trạng thái và actor xử lý được ghi trên request; lịch sử chi tiết được lưu thêm ở `request_events`.
4. Khi được duyệt và Archivist xếp ngày/phòng nhận, hệ thống tạo hoặc cập nhật `reading_sessions`.
5. Khi giao hồ sơ, session được đánh dấu đang đọc và ghi người/thời điểm giao. Khi nhận lại, ghi `returned_at`, người nhận trả, tình trạng và vị trí trả.
6. `medical_records.physical_status`/vị trí hiện tại phải được cập nhật đồng bộ với sự kiện giao–trả trong cùng transaction nghiệp vụ. `reading_sessions` là lịch sử giao dịch; `medical_records` là ảnh chụp trạng thái hiện tại.

## 5. Nguyên tắc thiết kế và chuẩn hóa

- **Tách master data khỏi giao dịch:** hồ sơ bệnh án nằm trong `medical_records`; yêu cầu/lượt đăng ký/phiên mượn nằm ở các bảng workflow riêng.
- **Không nhân bản hồ sơ theo hình thức:** cờ `has_paper_copy` và `has_electronic_copy` mô tả hai hình thức của cùng một hồ sơ. Một hồ sơ điện tử-only không cần một dòng thứ hai trong bảng khác.
- **Batch và request có mục đích khác nhau:** batch giữ dữ liệu chung một lần gửi; request giữ quyết định riêng từng hồ sơ.
- **Session không đồng nghĩa với request:** request mô tả quyền/yêu cầu; session mô tả lịch và sự kiện thực tế nhận/đọc/trả.
- **Giữ danh tính lịch sử:** các FK actor không nên bị cascade xóa. Tài khoản dùng `deleted_at` để lưu trữ mềm; snapshot username giữ tên đăng nhập lúc yêu cầu được tạo.
- **Dữ liệu nhạy cảm:** `password_hash` và `approval_secret_hash` không bao giờ lưu dạng rõ, hiển thị, hoặc ghi vào log. Metadata hồ sơ không nên chứa toàn văn nội dung bệnh án.
- **Ngày và thời điểm:** ngày nghiệp vụ dùng `DATE`; mốc sự kiện dùng `TIMESTAMPTZ`; giao diện hiển thị theo múi giờ Asia/Bangkok/Vietnam đã cấu hình.
- **Tính nhất quán giao dịch:** tạo hồ sơ + chính sách snapshot + audit/lịch sử; duyệt + event + notification; giao/trả + cập nhật trạng thái vật lý cần thực hiện nguyên tử trong transaction.

## 6. Chỉ mục và hiệu năng

Các migration đã tạo unique index/constraint cho `app_users.username`, `medical_records.record_code`/`archive_number`, token gửi, batch-record; index hỗ trợ tìm request theo người gửi và ngày tạo, batch theo người gửi, session theo ngày/trạng thái. Các partial unique index bảo vệ lịch trùng và phiên đang mở.

Tìm kiếm hồ sơ theo tên không dấu đã có hàm chuẩn hóa trong schema; chưa nên khẳng định hiệu năng trên dữ liệu lớn nếu chưa có benchmark. Khi dữ liệu thực tăng, hãy xem `EXPLAIN (ANALYZE, BUFFERS)` trên truy vấn thực trước khi thêm index, và cân nhắc trigram/full-text index cho tìm tên thay vì thêm index theo phỏng đoán.

## 7. Bảng hỗ trợ ngoài lát cắt 5 bảng

| Bảng | Vì sao quan trọng nhưng không nằm trong 5 bảng cốt lõi đã chọn |
|---|---|
| `roles` | Danh mục bốn role, FK đích của `app_users.role_id` |
| `employees` | Hồ sơ nhân viên 1:1 với tài khoản; lưu CCCD/ngày sinh/giới tính/role nghiệp vụ |
| `request_events` | Nhật ký bất biến/timeline các bước xử lý yêu cầu |
| `notifications` | Hộp thư chuông nội bộ gắn tài khoản và sự kiện |
| `storage_locations` | Cây kho/kệ/ngăn và vị trí hiện tại/lúc trả |
| `record_location_history`, `record_events` | Lịch sử di chuyển và thay đổi hồ sơ |
| `departments`, `record_types`, `retention_policies`, `medical_record_policies` | Danh mục phân loại/khoa và snapshot chính sách lưu trữ |
| `audit_events` | Nhật ký thao tác bảo mật/nghiệp vụ tổng quát |

Vì vậy, ERD 5 bảng này là sơ đồ tập trung để trình bày mô hình lõi, không thay cho sơ đồ toàn schema. Xem [schema đang triển khai](implemented-schema.md), [ERD logic tổng thể](erd.md) và [từ điển dữ liệu](data-dictionary.md) để biết các bảng còn lại.

## 8. Các điểm cần hiểu đúng khi trình bày

1. **Không gọi `reading_requests` là bảng lưu toàn bộ lịch sử.** Bảng này giữ trạng thái hiện tại và một số actor/mốc xử lý; timeline chi tiết nằm ở `request_events`.
2. **Không suy ra “đang mượn” chỉ từ yêu cầu đã duyệt.** Cần xem `reading_sessions.state`, `handed_at`, `returned_at`, đồng thời đảm bảo trạng thái vật lý trong `medical_records` được đồng bộ.
3. **Không xem ngày đăng ký là ngày nhận.** `registered_on` ở request/batch; ngày nhận do Archivist xếp nằm ở `reading_sessions.reading_on`.
4. **Không dùng xóa cứng với tài khoản/hồ sơ đã tham gia giao dịch.** Dùng lưu trữ mềm hoặc trạng thái nghiệp vụ để giữ FK, audit và khả năng giải trình.
5. **ERD logic không phải danh sách migration.** Nếu đổi lược đồ, migration Java mới là căn cứ schema thực tế; không sửa migration cũ đã được áp dụng.

