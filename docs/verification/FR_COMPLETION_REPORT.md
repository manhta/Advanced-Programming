# Báo cáo triển khai yêu cầu chức năng SRS

**Bổ sung:** FR-11 đã có chuông và thông báo nội bộ, migration V9. Đọc [báo cáo FR-11](FR11_INTERNAL_NOTIFICATIONS.md) / [CR-007](../changes/CR-007_internal-notifications.md). Kết quả kiểm chứng ngày 21/10 bên dưới giữ như mốc lịch sử; email/ủy quyền tiếp tục hoãn.

Ngày, Asia/Bangkok. Nguồn đối chiếu: **SRS_Quan_ly_luu_tru_HSBA_v0.2_da_lam_ro.docx**, phiên bản 0.2; code Java trong dự án. Không chỉnh sửa tài liệu nguồn.

Đã mở rộng nền tảng năm chức năng sang quản lý tài khoản, đăng ký đọc, kiểm tra/trình, phê duyệt có mật khẩu cấp 2, xác nhận lịch, giao–trả từng ngày, ngoại lệ, kiểm kê, báo cáo và nhật ký. Hai ngoại lệ người dùng xác nhận ngày 21/10: **không gửi email** và **chỉ Director duyệt, chưa ủy quyền**. Vì vậy không ghi “100% SRS nguyên bản”: các FR được thực hiện theo phạm vi đã điều chỉnh tại [CR-006](../changes/CR-006_full-functional-requirements.md).

## 1. Đối chiếu FR-01 đến FR-20

| FR | Kết quả hiện thực | Cách sử dụng / bằng chứng |
|---|---|---|
| FR-01 Đăng nhập/đăng xuất | Giữ xác thực PBKDF2/role/CSRF; bổ sung audit thành công/thất bại/logout và kiểm tra lại trạng thái tài khoản mỗi request | `/login`, nút Đăng xuất, log AUTH |
| FR-02 Tài khoản/vai trò | Admin tạo, sửa tên/khoa/email tùy chọn, đổi một role, khóa/vô hiệu hóa/kích hoạt, đặt lại password; version chống ghi đè; bảo vệ Admin cuối | `/admin/accounts`; không hiển thị hash/password cũ |
| FR-03 Tra cứu | Giữ tìm mã/tên có-không dấu và các bộ lọc, phân trang; đã thử trên hơn 100 hồ sơ giả | `/reader/records`, `/archivist/records`, `/director/records` |
| FR-04 Phân quyền dữ liệu | Reader chỉ metadata và yêu cầu của mình; Archivist vận hành; Director giám sát/duyệt; Admin không dữ liệu bệnh án | HTTP 403/404, DTO và SELECT riêng; export kiểm tra cùng quyền |
| FR-05 Vị trí và lịch sử | Giữ Kho–Kệ–Ngăn, sức chứa/version; chuyển/cất lại sau đọc có lịch sử nguồn/đích/người/thời điểm/lý do | Chi tiết hồ sơ, `/archivist/locations`; ba cấp theo CR-005 |
| FR-06 Trạng thái | Nhập/khả dụng/READING/nhận trả nhất quán; không giao trùng, không chuyển/sửa hồ sơ đang đọc | Session RETURNED khác physical IN_STORAGE; damaged/incomplete trả vào UNAVAILABLE |
| FR-07 Metadata/đối soát đầu | Giữ nhập tay, nháp, dữ liệu thiếu không bịa ngày, sửa có lý do/snapshot/unique | Form hồ sơ; bộ thử 100+ hồ sơ giả trong schema riêng |
| FR-08 Đăng ký nhiều ngày | Reader chọn một hồ sơ, mục đích, 1–5 ngày trong 30 ngày tới; mỗi ngày một session | Chi tiết hồ sơ → Đăng ký đọc; yêu cầu tử vong bị chặn backend |
| FR-09 Tổng hợp/trình | Danh sách yêu cầu chung, báo cáo lọc trạng thái; Archivist trả bổ sung hoặc trình Director theo từng yêu cầu | `/archivist/requests`, thao tác review |
| FR-10 Phê duyệt | Director thiết lập mật khẩu cấp 2 riêng, nhập lại khi duyệt/từ chối; lưu người, lúc, căn cứ và revision; cấm tự duyệt | `/director/approval-secret`, `/director/requests`; **ủy quyền hoãn theo xác nhận** |
| FR-11 Kết quả/lịch và email | Kết quả kiểm tra/quyết định/lịch hiển thị cho người yêu cầu trong ứng dụng | **Phần email/5 phút hoãn theo người dùng; không tuyên bố hoàn thành gửi email** |
| FR-12 Giao–trả | Giao đúng ngày xác nhận, ghi người giao/nhận và giờ/tình trạng; trả ghi giờ/chất lượng/ngăn; quá ngày cần ngoại lệ | Thao tác trên từng session tại chi tiết yêu cầu Archivist |
| FR-13 Audit | AUTH, tài khoản, quyết định, revision, lịch, giao–trả, ngoại lệ, kiểm kê, xem/xuất/in báo cáo; hợp nhất audit cũ về hồ sơ/vị trí/danh mục | Báo cáo Nhật ký lọc actor/action/entity/; không log mật khẩu; không có API xóa log |
| FR-14 Tử vong | Giữ metadata/vị trí/DECEASED; đăng ký đọc tử vong bị chặn | Xử lý khai thác trực tiếp ngoài app theo SRS |
| FR-15 Thời hạn lưu | Giữ ngày đầu lưu, chính sách/phiên bản/snapshot, nhiều nhóm lấy dài nhất; xem trong báo cáo danh mục/thời hạn | 10/15/20/30 là mô phỏng đã xác nhận; không tiêu hủy |
| FR-16 Báo cáo/thống kê | Danh mục/thời hạn, tổng hợp kho, yêu cầu/quyết định, phiên/giao–trả/chưa trả, lịch sử vị trí, audit, kiểm kê, thống kê xử lý yêu cầu | `/archivist/reports`, `/director/reports`; Admin chỉ technical audit |
| FR-17 Khởi tạo không HIS | Chọn phương án nhập tay; không API HIS hoặc kết nối database HIS | Nhập tệp chưa được chọn, nên AC-06 có điều kiện không áp dụng |
| FR-18 Cơ quan ngoài | Không tạo role hoặc luồng tài khoản cơ quan ngoài | Giữ ngoài phạm vi theo chính SRS |
| FR-19 Kiểm kê | Đợt theo vị trí hoặc toàn kho, snapshot danh sách, ghi đủ/thiếu/sai vị trí/hư hỏng; chốt sau đối soát đủ | `/archivist/inventories`; Director chỉ danh sách/kết quả tổng hợp |
| FR-20 Xuất/in | Xuất CSV UTF-8 BOM mở Excel, theo đúng bộ lọc và quyền; chống công thức bảng tính; in/lưu PDF bằng trình duyệt, ghi audit yêu cầu in | Nút Xuất CSV, In/lưu PDF tại báo cáo; không tạo XLSX hoặc PDF bằng server |

Các nội dung trong bảng là chức năng đã viết, cần đọc cùng kết quả chạy ở mục 8; không coi mã FR trùng mã MRM-UC. Ví dụ FR-03 tương ứng MRM-UC-05, không phải MRM-UC-03.

## 2. Database và thư mục được bổ sung

Java migration **V8__CreateAccessWorkflow** đã áp dụng trên `mrm/public`, giữ nguyên V1–V7 và dữ liệu người dùng. Thay đổi tiếp theo phải dùng V9 trở lên.

| Thành phần | Nội dung |
|---|---|
| app_users | Thêm department_id, email tùy chọn, approval_secret_hash, version; giữ password_hash và một role_id |
| reading_requests | Hồ sơ, người yêu cầu, mục đích, trạng thái, revision/version, kiểm tra/quyết định/xác nhận, người và thời điểm |
| reading_sessions | Một ngày một dòng; người/giờ/tình trạng giao–trả, ngăn cất lại, ngoại lệ |
| request_events | Nhật ký từng revision, trước khi sửa nội dung, trình, quyết định, lịch và giao–trả |
| audit_events | Nhật ký xác thực, tài khoản và các chức năng mới; không chứa mật khẩu |
| inventory_batches/items | Đợt, danh sách kỳ vọng và kết quả kiểm tra thực tế; snapshot đường dẫn lúc tạo đợt |

Các package mới dưới `src/main/java/mrm/`: `account`, `audit`, `workflow`, `inventory`, `report`; thêm `config/AccountStateFilter`. HTML tương ứng dưới `templates/account`, `reading`, `inventory`, `report`; menu chung bổ sung các liên kết theo role. Không thêm dependency bên ngoài và không tạo file SQL.

`AccountStateFilter` so role/trạng thái/version trong database với phiên đang dùng. Khi Admin đổi/khóa tài khoản hoặc đặt lại mật khẩu, phiên cũ bị thu hồi ở request kế tiếp. Không tự đổi mật khẩu các tài khoản hiện có.

## 3. Nguyên lý luồng đọc hồ sơ

```mermaid
flowchart TD
 A[Reader gửi SUBMITTED] -> B[Archivist kiểm tra]
 B -> C[NEEDS_INFO cần bổ sung]
 C -> D[Reader sửa tăng revision]
 D -> A
 B -> E[PENDING_APPROVAL]
 E -> F[Director xác thực cấp 2]
 F -> G[REJECTED]
 F -> H[APPROVED]
 H -> I[Archivist xác nhận CONFIRMED]
 I -> J[Phiên từng ngày CONFIRMED]
 J -> K[Giao trong ngày READING]
 K -> L[Nhận trả RETURNED]
 L -> M[Hồ sơ IN_STORAGE hoặc UNAVAILABLE]
```

### 3.1. Tạo yêu cầu và sửa nội dung

`ReadingController` nhận POST từ Thymeleaf, `ReadingService.submit` kiểm tra tài khoản READER, hồ sơ STORED không tử vong/đủ điều kiện, mục đích/ngày và mã phiếu. Cùng token/nội dung trả lại cùng ID; token đã dùng nội dung khác bị từ chối.

Một request một hồ sơ. Ngày lặp được loại trùng và sắp thứ tự; ngày trước hôm nay hoặc quá 30 ngày bị chặn. Chưa áp dụng lịch nghỉ riêng của đơn vị. Các giới hạn này là mặc định dự án tại CR-006.

Reader chỉ được xem/sửa yêu cầu của mình. Sửa trước lần giao đầu tăng revision, xóa các session chưa từng được giao của lịch cũ rồi tạo lịch mới; lịch sử request_events giữ nội dung/ngày/quyết định trước đó. Tất cả quyết định/lịch hiện tại reset, phải trình duyệt lại. Sau khi đã giao một ngày, không sửa nội dung; hủy phần chưa thực hiện và tạo request mới.

### 3.2. Kiểm tra, phê duyệt, xác nhận

Archivist chọn trả bổ sung hoặc trình. Director nhập mật khẩu cấp 2 ở đúng POST ghi quyết định cho version hiện tại; không có bằng chứng xác thực dùng lại từ lần duyệt trước. Secret băm PBKDF2, khác password đăng nhập; thiết lập secret phải nhập đúng password đăng nhập hiện tại.

Sai cấp 2 được ghi bằng transaction riêng để không mất log khi quyết định rollback. Khóa theo tài khoản duyệt và kiểm tra số lần sai trong 15 phút; từ 5 lần sai không cho quyết định trong cửa sổ đó. Cấm requester_id bằng approver_id, kể cả tài khoản đổi role sau khi gửi.

Archivist chỉ xác nhận request APPROVED, không có ngày đã qua, hồ sơ đủ điều kiện. Unique index chặn một hồ sơ/ngày có hai session CONFIRMED/READING/RETURNED; hai người xác nhận đồng thời chỉ một thành công. Xác nhận mọi ngày trong một transaction, không để nửa lịch thành công.

### 3.3. Giao và nhận trả

Giao chỉ từ session CONFIRMED trong đúng ngày Asia/Bangkok, requester còn ACTIVE/READER và hồ sơ đang IN_STORAGE/chất lượng NORMAL. Transaction ghi handed_by, received_by, handed_at, tình trạng, chuyển hồ sơ thành READING và audit. Unique partial index chặn hai lượt đang mở cùng hồ sơ.

Nhận trả chỉ từ READING, ghi thời điểm thực tế/nhân viên/chất lượng và ngăn đích hợp lệ. Nếu trả sang ngăn khác, kiểm tra sức chứa và thêm lịch sử vị trí. NORMAL trở về IN_STORAGE, DAMAGED/INCOMPLETE thành UNAVAILABLE. Các trường hợp chất lượng bất thường cần kiểm tra và khắc phục trước khi mở khả dụng bằng nghiệp vụ hồ sơ.

Nếu ngày trả khác ngày đọc, phải có ngoại lệ trước; hệ thống không tự điền một giờ trả cuối ngày giả. Bộ lọc sessions trạng thái READING giúp rà soát chưa trả. NO_SHOW chỉ áp dụng trước giao; không được dùng để che việc đang giữ hồ sơ. Hủy không được thực hiện khi đang READING và không xóa lượt RETURNED.

## 4. Cách dùng từng vai trò

### Reader

1. Login tài khoản Reader, tra cứu hồ sơ chính thức không tử vong.
2. Mở chi tiết → **Đăng ký đọc**. Nhập mục đích, ngày theo `yyyy-MM-dd`, nhiều ngày cách nhau dấu phẩy; bấm Gửi yêu cầu.
3. Mở **Yêu cầu / lịch đọc**, theo dõi kết quả kiểm tra/quyết định/xác nhận ngay trong app. Chưa có email.
4. Nếu NEEDS_INFO, mở yêu cầu và bổ sung; nếu chủ động sửa trước giao, nhập đủ lại tập ngày muốn đọc. Việc sửa phải duyệt lại.
5. Sau CONFIRMED, đến phòng đọc theo ghi chú. Nhận/trả giấy đúng quy trình, Archivist ghi nhận trên hệ thống.
6. Nếu hủy phần chưa thực hiện, nhập lý do. Lượt đã giao phải nhận trả trước khi hủy phần còn lại.

### Archivist

1. **Yêu cầu / lịch đọc**: mở request SUBMITTED, kiểm tra hồ sơ/mục đích/; ghi nội dung rồi chọn **Đủ điều kiện — trình Director** hoặc **Trả bổ sung**.
2. Khi APPROVED, ghi phòng đọc/hướng dẫn, bấm xác nhận lịch. Nếu xung đột phải kiểm tra lịch khác, không sửa trực tiếp database.
3. Đúng ngày đọc, đối chiếu người nhận và tình trạng giấy, bấm giao tại session tương ứng. Trang chi tiết cập nhật READING.
4. Khi nhận giấy về, chọn ngăn cất lại, chất lượng và tình trạng thực tế; bấm nhận trả. Nếu trả khác ngày, ghi ngoại lệ trước.
5. Dùng báo cáo sessions/READING để rà chưa trả; ghi ngoại lệ hoặc không đến phù hợp. Không dùng đổi khả dụng để giả lập nhận trả.
6. **Kiểm kê**: tạo đợt, chọn kho/kệ/ngăn hoặc toàn kho; đối chiếu từng dòng rồi chốt. Phát hiện sai vị trí không tự sửa bản ghi; dùng chức năng chuyển/đối soát hồ sơ riêng với lý do.

### Director

1. **Mật khẩu cấp 2**: nhập password đăng nhập hiện tại, đặt secret mới 12–128 ký tự khác password đăng nhập. Không dùng giá trị ví dụ trong mã test cho tài khoản thật.
2. **Yêu cầu / lịch đọc**: mở PENDING_APPROVAL, xem mục đích/ngày/revision và kết quả kiểm tra.
3. Chọn phê duyệt/từ chối, ghi căn cứ, nhập lại secret, bấm ghi quyết định. Không tự duyệt yêu cầu của mình nếu từng tạo khi còn là Reader.
4. Xem báo cáo/kiểm kê tổng hợp và audit được phép. Không có vị trí ngăn chi tiết trong báo cáo Director.
5. Nghiệp vụ xác nhận chính sách lưu vẫn ở màn hình riêng, không phải quyết định cho một request đọc.

### Admin

1. **Tài khoản**: tạo username 3–64 ký tự Latin thường/số/`._-`, họ tên, một role, khoa tùy chọn và password 12–128 ký tự.
2. Trong dòng tài khoản, sửa tên/khoa/role/status; password mới để trống để giữ password cũ. LOCKED/DISABLED chặn sử dụng; thay đổi version thu hồi phiên cũ.
3. Không được tự khóa/đổi role tài khoản Admin đang thao tác hoặc làm mất Admin hoạt động cuối cùng.
4. **Báo cáo / nhật ký** chỉ trả AUTH/ACCOUNT. Tài khoản Admin không được truy cập yêu cầu/hồ sơ hoặc xuất report lâm sàng.

## 5. Báo cáo, tìm log và xuất dữ liệu

Tám nhóm báo cáo trong ReportService:

| Loại | Nội dung / bộ lọc thường dùng |
|---|---|
| records | Danh mục hồ sơ, ngày đầu lưu, nhóm năm và hạn; q tìm trong dữ liệu được phép, state lọc IN_STORAGE/UNAVAILABLE/READING |
| status | Số lượng theo khoa/loại/trạng thái/nhóm năm, số liệu hiện tại |
| requests | Yêu cầu, mục đích, người gửi/kiểm tra/duyệt, quyết định và lịch; state như PENDING_APPROVAL |
| sessions | Ngày đọc, người/giờ/tình trạng giao–trả, ngoại lệ; READING để xem chưa trả |
| locations | Nguồn/đích, người, lúc, lý do; chỉ Archivist |
| audit | AUTH/ACCOUNT/REQUEST và hợp nhất sự kiện RECORD/LOCATION/CATALOG theo quyền; lọc actor/action/entity/ngày |
| inventory | Tổng hồ sơ kỳ vọng và số đúng/thiếu/sai vị trí/hư hỏng của từng đợt |
| request-statistics | Đếm request theo ngày tạo và state; trung bình giờ từ tạo đến quyết định đối với request đã có decided_at |

Ngày lọc records là updated_at, requests là created_at, sessions là reading_on, location/audit là occurred_at, inventory là ngày tạo đợt; bảng tổng hợp kho không tái dựng tồn kho một ngày quá khứ. Thống kê xử lý request dùng nội dung/quyết định hiện tại; lịch sử revision xem tại chi tiết, không coi là chỉ số lịch sử mọi lần duyệt.

Xuất CSV dùng đúng truy vấn/role/bộ lọc như web, UTF-8 BOM và escape dấu ngoặc/dấu phẩy; giá trị bắt đầu công thức được bảo vệ trước khi mở bằng Excel. Nút in gọi hộp in của trình duyệt, có thể chọn Save as PDF; audit ghi yêu cầu mở chế độ in, không thể chứng minh người dùng đã in giấy/lưu file thành công hoặc ngăn Ctrl+P từ bên ngoài nút.

## 6. Nhật ký và bảo quản

- Audit không chứa mật khẩu đăng nhập/cấp 2, không expose hash trong UI hoặc CSV.
- Thay đổi hồ sơ cũ vẫn giữ JSON before/after và lý do trong record_events; request_events giữ revision và thao tác nối tiếp. Báo cáo audit tổng hợp thông tin; chi tiết before/after xem từ hồ sơ.
- App không có thao tác xóa/sửa audit. Chính sách dự án là giữ tối thiểu 10 năm, không có job tự dọn log; giữ lâu hơn không làm giảm thời hạn tối thiểu.
- Chưa thử nghiệm đủ 10 năm thực tế, chưa thiết kế kho lưu trữ log ngoại tuyến/WORM, chưa hạn chế superuser PostgreSQL sửa trực tiếp. Đây là giới hạn vận hành cần thực hiện trước sử dụng thật.

## 7. NFR được giữ và phần chưa nghiệm thu

Giữ các hàng rào cần thiết để FR đúng: xác thực/quyền backend/CSRF, hash secret, transaction, FK/unique/check/row lock/version, lọc dữ liệu export và chống công thức CSV. Không bỏ chúng chỉ vì người dùng cho phép cân nhắc NFR.

Chưa nghiệm thu tải/p95, triển khai nhiều máy/LAN, HTTPS/secret production, backup/restore/RPO/RTO, sẵn sàng, quy định pháp luật áp dụng hoặc nghiệm thu trực quan bởi cơ sở y tế. Đây không phải tuyên bố sản phẩm production-ready. Không cần xây quy trình kiểm tra hồ sơ tại khoa/SLA 24 giờ/lịch thứ Năm trong đợt này; SRS nêu đó là bối cảnh và còn hỏi có đưa vào phần mềm hay không.

## 8. Kiểm thử và cách chạy lại

### Phương pháp

- Maven package/test-compile kiểm tra biên dịch/đóng gói. Các bộ tích hợp là Java main có assertion, chạy tường minh, không tự chạy bởi Surefire khi package.
- Ba bộ hồi quy cũ: CatalogIntegrationCheck (49), CompleteCatalogCheck (26, bootstrap đã cập nhật tới V8), FiveFeatureCheck (96).
- FunctionalRequirementsCheck boot app đầy đủ với schema `fr_check_<UUID>` riêng trong PostgreSQL, 100+ hồ sơ giả, secret/password tổng hợp riêng, cổng HTTP ngẫu nhiên. Finally đóng app và DROP đúng schema thử; không sửa tài khoản/password trong schema public.
- Kiểm tra Service/SQL và HTTP với cookie/CSRF thật; thử vai trò khác, đoán ID, sai/version cũ, cạnh tranh xác nhận, trả muộn và chênh lệch kiểm kê. Không sử dụng dữ liệu bệnh nhân thật.

### Kết quả

Ngày: ba bộ hồi quy đạt 49 + 26 + 96 = 171 assertion –09:11. Sau sửa CSV nhiều dòng và kết quả kiểm kê mâu thuẫn, bộ FR cuối đạt 127 assertion –09:30, exit code 0; tổng bằng chứng **298 assertion đạt**. Bộ FR boot V1–V8, dùng hơn 100 hồ sơ giả, chạy Service/SQL và HTTP thật; schema thử được dọn. JAR cuối build thành công; V8 đã áp dụng schema public. Chưa có nghiệm thu trực quan bởi người dùng hoặc kết luận đạt NFR production.

### Các ca quan trọng

Bằng chứng lưu cùng báo cáo: [49 danh mục](evidence/fr-2026-10-21/catalog.txt), [26 bootstrap/P2](evidence/fr-2026-10-21/bootstrap-and-policy.txt), [96 năm chức năng](evidence/fr-2026-10-21/five-features.txt), [127 FR mở rộng](evidence/fr-2026-10-21/functional-requirements.txt), [kiểm kê trước](evidence/fr-2026-10-21/inventory.txt). Đây là assertion, không phải 298 test JUnit độc lập.

[Kiểm kê sau](evidence/fr-2026-10-21/inventory-after.txt) xác nhận vẫn 4 tài khoản, 5 khoa, 6 loại và 4 chính sách; các bảng hồ sơ/request/phiên/kiểm kê thử trong public có 0 dòng. Giữ 11 audit xác thực phát sinh từ hồi quy, không xóa nhật ký để làm số liệu về 0. Không thay password các tài khoản hiện có; dữ liệu 100+ hồ sơ giả chỉ tồn tại trong schema thử và đã được dọn.

| Nhóm | Đã đưa vào bộ kiểm tra |
|---|---|
| Tài khoản | Tạo/sửa/khóa; role sai bị chặn; version cũ; không tự mất Admin; phiên bị thu hồi sau khóa |
| Cấp 2 | Secret khác password login; xác thực login khi đặt; sai không thay quyết định; log sai sống qua rollback; cấm tự duyệt; khóa sau 5 lần sai |
| Request | Chống gửi lặp/token đổi nội dung; Reader không thấy request người khác; bổ sung/tăng revision; duyệt/từ chối; sửa mất hiệu lực quyết định |
| Lịch | Mỗi ngày một session; chưa confirm không giao; không giao trước; hai luồng confirm cùng hồ sơ/ngày chỉ một thành công |
| Giao–trả | READING khóa chuyển; không hủy khi đang đọc; trả đổi ngăn ghi lịch sử; trả hỏng UNAVAILABLE; trả muộn cần ngoại lệ; không đến không giả giao |
| Kiểm kê | 100+ dòng snapshot; chốt khi chưa đủ bị chặn; thiếu/sai cần lý do; chốt rồi không sửa; không tự thay vị trí thật |
| Báo cáo | Mọi query/template; bộ lọc tham số; CSV có nội dung được phép; Reader/Admin không xuất hồ sơ; Director không đường dẫn kho; audit lọc người/hành động |
| HTTP | Login bốn role; GET/POST form thật, menu, CSRF, yêu cầu→duyệt→xác nhận→giao→trả, account và inventory |
| Dọn dữ liệu | Schema thử được xóa; secret không xuất hiện trong audit |

### Chạy trên máy phát triển

```powershell
# Tại Code, sau khi nạp DB_PASSWORD theo README
.\mvnw.cmd -B -ntp package
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
# Terminal khác cùng thư mục, ứng dụng đang chạy bản mới nhất
.\scripts\verify-features.ps1 -BaseUrl http://localhost:8080
```

Log ở `target/verification/`, bộ FR riêng có file `functional-requirements.txt`. Quyền database của người chạy cần tạo/dọn schema kiểm tra; không dùng script kiểm thử trên database production. Kiểm thử không tự đặt secret cho Director thật; bạn cần tự thiết lập trên web.

## 9. Trình tự bạn tự nghiệm thu

1. Archivist chuẩn bị một hồ sơ giả và ngăn đủ chỗ; Director xác nhận policy nếu cần.
2. Director thiết lập mật khẩu cấp 2 của chính mình.
3. Reader đăng ký hôm nay và ngày mai, mục đích rõ ràng.
4. Archivist thử trả bổ sung; Reader sửa; Archivist trình; Director duyệt đúng revision.
5. Archivist xác nhận lịch, giao phiên hôm nay, nhận trả vào ngăn đã đối chiếu. Kiểm tra đồng thời trang hồ sơ và lịch sử request.
6. Reader kiểm tra trạng thái/ngày còn lại; thử hủy ngày chưa thực hiện khi không còn hồ sơ đang giữ.
7. Archivist tạo đợt kiểm kê, ghi kết quả và chốt; nếu sai vị trí thì xử lý bằng chức năng chuyển riêng.
8. Director/Archivist thử báo cáo, filter, CSV và in; Admin thử tài khoản và audit kỹ thuật, kiểm tra bị chặn bệnh án.
9. Ghi phản hồi bố cục/thuật ngữ/giới hạn nghiệp vụ trước khi nghiệm thu cuối. Không dùng tài khoản người khác để thử quyền; có thể dùng các tài khoản demo mình đã được cấp password.

## 10. Tài liệu liên quan

- [Bốn hướng dẫn sử dụng theo role](../README.md).
- [Báo cáo năm chức năng nền tảng](FIVE_FEATURE_COMPLETION_REPORT.md) là lịch sử mốc V7, giữ làm tài liệu giải thích nhập kho/tra cứu.
- [Checklist dự án](../../PROJECT_CHECKLIST.md), [CR-006](../changes/CR-006_full-functional-requirements.md).
- Note được giữ nguyên theo quy tắc chỉ sửa khi người dùng yêu cầu.
