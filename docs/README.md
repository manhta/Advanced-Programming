# Medical Record Management — Bộ tài liệu phân tích và thiết kế

**V26 —:** [Xóa mềm tài khoản Admin, gom áp dụng thay đổi vai trò/khoa vào Quản lý tài khoản, và tách trang tạo thành công](verification/ACCOUNT_ARCHIVAL_AND_ADMIN_QUEUE.md).

**V25 —:** [Hồ sơ cá nhân và thời điểm thông báo nhân sự](verification/STAFF_PROFILE_AND_NOTICE_TIMING.md).

**V24 —:** [Tài khoản tự động và đề nghị thay đổi nhân sự](verification/STAFF_SELF_SERVICE_AND_APPROVAL.md), hướng dẫn theo từng vai trò và kết quả kiểm thử.

**Cập nhật V23 —:** [Mật khẩu lần đầu và cập nhật Giám đốc](verification/INITIAL_CREDENTIAL_SETUP.md): Tạ Đức Mạnh,; bắt buộc thay mật khẩu ban đầu, Giám đốc tự tạo thêm mật khẩu cấp 2.

**Cập nhật V22 —:** [Dọn dữ liệu thử và thiết lập tài khoản sử dụng](verification/OPERATIONAL_BASELINE_RESET.md): giữ 16 bệnh án, đổi mã DEMO, giữ Admin, ba tài khoản mới và xóa lịch sử thử. Mật khẩu được lưu riêng tại `.secrets/operational-accounts.txt`.

**Cập nhật V21 —:** [Thông báo thống nhất và dọn đã xử lý](verification/UNIFIED_NOTIFICATIONS_AND_CLEANUP.md): mặc định việc đang chờ, xóa thông báo đã xử lý, metadata chung cho các role và việc xác nhận lịch sau phê duyệt.

**Cập nhật:** [Thông báo Archivist và giao diện Director](verification/DIRECTOR_REPORTS_AND_NOTICE_PRESENTATION.md): thông báo theo người/hồ sơ/thời điểm, Việt hóa bảng báo cáo, bỏ quyền kiểm kê và hai khối phụ trên trang duyệt Director.

**Cập nhật:** [Form xét duyệt Archivist](verification/ARCHIVIST_REVIEW_FORM.md): gộp phê duyệt/lịch nhận; bổ sung hoặc từ chối bắt buộc lý do; nhánh tử vong chỉ trình Director, giao–trả tách sang trang riêng.

**Cập nhật:** [Menu tài khoản và danh mục Giám đốc](verification/PERSONAL_MENU_AND_DIRECTOR_DASHBOARD.md), trang cá nhân/đổi mật khẩu cho ba role và giao diện Reader đã nhận bản giấy. Căn cứ duyệt vẫn nhập text theo xác nhận mới của người dùng.

**Cập nhật — V20:** [Thông báo hủy, danh mục Archivist và bệnh án tử](verification/ARCHIVIST_NOTICES_AND_DECEASED_RECORDS.md): ẩn công việc cũ khi hủy, thông báo thay thế chứa mã, ba ô thống kê có liên kết, cập nhật ba mẫu tử vong và màu nhận diện.

**Cập nhật — V19:** [CR-013](changes/CR-013_borrowing-approval-routing.md) / [Duyệt mượn theo lượt](verification/BORROW_APPROVAL_ROUTING.md): Archivist duyệt lượt thường; nếu có tử vong, Director duyệt toàn bộ lượt. Bắt buộc lý do khi trên 5 hồ sơ hoặc có tử vong. Quy tắc này thay thế hướng dẫn trình mọi lượt lên Director ở các mốc cũ.

**Cập nhật:** [CR-011](changes/CR-011_borrowing-batches-and-pickup-window.md) / [Đăng ký nhiều hồ sơ và hạn nhận 2 ngày](verification/BORROWING_REGISTRATION.md). Reader không chọn ngày, có cửa sổ chọn hồ sơ; Archivist xếp ngày sau Director duyệt. Các mốc chọn ngày Reader trước đó là lịch sử.


**Cập nhật tài khoản/nhân viên:** [CR-010](changes/CR-010_unified-accounts-and-employees.md), [bảng employees và form chung](verification/EMPLOYEE_MANAGEMENT.md). Thay thế lối tạo Reader/Archivist tách riêng.


**Quy tắc hiện tại theo CR-008 ngày 22/10:** [thông báo công việc và quyền xem hồ sơ](verification/WORKFLOW_ALERTS_AND_READER_ACCESS.md). Thay thế phạm vi chuông chỉ Reader và quyền xem chi tiết không cần xin phép ở các mốc tài liệu cũ.


**Dữ liệu thực hành:** đã thêm [15 hồ sơ mẫu](verification/SAMPLE_MEDICAL_RECORDS.md) vào mrm/public, chia gần đều sáu loại, có vị trí và chính sách lưu; dùng để thử tra cứu và đăng ký đọc.

**Cập nhật:** [FR-11 — chuông và thông báo nội bộ](verification/FR11_INTERNAL_NOTIFICATIONS.md), theo [CR-007](changes/CR-007_internal-notifications.md). Kênh nội bộ được người dùng xác nhận thay cho email đang hoãn; hướng dẫn từng role đã bổ sung cách dùng.

**Phiên bản FR mở rộng:** đọc [báo cáo triển khai FR-01–20](verification/FR_COMPLETION_REPORT.md) và [CR-006](changes/CR-006_full-functional-requirements.md). Đã bổ sung tài khoản, request/lịch đọc, duyệt cấp 2, giao–trả, ngoại lệ, kiểm kê, báo cáo và audit; email/ủy quyền tiếp tục hoãn theo xác nhận người dùng. Bốn hướng dẫn dưới đây đã được cập nhật cho phiên bản này.

## Hướng dẫn sử dụng phần mềm theo vai trò

Cập nhật ****. Thư mục `user-guides/` gồm đúng bốn file, mỗi file hướng dẫn một vai trò trong phiên bản FR mở rộng. Tài liệu mô tả thao tác thực tế trên web; các tính năng chưa triển khai được ghi rõ.

| Vai trò | Hướng dẫn | Nội dung chính |
|---|---|---|
| Reader — Người đọc | [reader.md](user-guides/reader.md) | Tra cứu, đăng ký/sửa/hủy yêu cầu và theo dõi lịch/kết quả |
| Archivist — Nhân viên lưu trữ | [archivist.md](user-guides/archivist.md) | Kho/metadata, kiểm tra/trình, lịch, giao–trả, kiểm kê/báo cáo |
| Director — Giám đốc | [director.md](user-guides/director.md) | Chính sách, mật khẩu cấp 2, quyết định đọc và báo cáo |
| Admin — Quản trị kỹ thuật | [admin.md](user-guides/admin.md) | Quản trị tài khoản và nhật ký kỹ thuật; không bệnh án |

Mỗi hướng dẫn có bước thực hiện, kết quả cần kiểm tra, tình huống thường gặp và danh sách tự thực hành. Phần còn lại dưới đây là bộ tài liệu phân tích/thiết kế.

**Cập nhật triển khai:** năm chức năng ưu tiên UC-01/03/04/05/06 đã có code và kiểm chứng. Đọc [báo cáo triển khai đầy đủ](verification/FIVE_FEATURE_COMPLETION_REPORT.md) và [CR-005](changes/CR-005_five-feature-implementation.md) để biết hành vi đang chạy; phần dưới giữ bối cảnh dự thảo tổng thể ngày 18/10, không phải kết quả kiểm thử hiện tại.

Phiên bản 0.1. **Dự thảo để người dùng rà soát, chưa phải yêu cầu được duyệt hoặc chương trình đã triển khai.**

## Thứ tự đọc

1. [Phạm vi và nguồn](requirements/scope.md).
2. [Sổ quyết định](requirements/decision-log.md): đọc trước khi đánh giá use case.
3. [Quy tắc](requirements/business-rules.md), [ma trận quyền](requirements/permission-matrix.md).
4. [Chức năng bắt buộc](functions/mandatory/README.md): ưu tiên triển khai.
5. [Chức năng bổ sung](functions/optional/README.md): chỉ làm khi được chọn.
6. [Luồng và trạng thái](design/workflow-and-states.md), [màn hình](ui/screen-specification.md).
7. [ERD](database/erd.md), [từ điển dữ liệu](database/data-dictionary.md), [kiến trúc](architecture.md).
8. [Truy vết nghiệm thu](testing/traceability.md), [phi chức năng](requirements/non-functional.md).
9. [Kế hoạch vận hành](deployment/operations-plan.md), [checklist rà soát](REVIEW_CHECKLIST.md).

## Quy ước trạng thái

- `mandatory`: chức năng thiết yếu cho phiên bản cơ sở đề xuất; gồm yêu cầu nguồn và biện pháp cần thiết để quy trình nhất quán. Không có nghĩa mọi chi tiết đã được khách hàng ký duyệt.
- `optional`: bổ sung hoặc phụ thuộc điều kiện. Email FR-11 vẫn là yêu cầu ưu tiên cao có điều kiện trong SRS. Đề xuất phân kỳ phải được xác nhận, kênh trong ứng dụng không tự thay thế email.
- Mã `MRM-UC-xx` là mã mới, không ghi đè mã UC bị khác nhau trong tài liệu nguồn.
- Mã `D-xx`: phương án đang chờ rà soát. Mã `BR-xx` trong bộ này là mã nội bộ mới; FR/RP chỉ mã của SRS nguồn.
- Tất cả use case áp dụng các quy tắc chung về quyền, dữ liệu, transaction, audit và retry.
- Sơ đồ Mermaid dùng cho luồng, trạng thái và quan hệ dữ liệu; không tạo sơ đồ lặp lại từng use case.

Đây là bản phân tích/thiết kế cho giai đoạn 1–5. Phê duyệt, khảo sát thực tế và thử nghiệm chưa xảy ra. Giai đoạn 6–15 mới có kế hoạch chuẩn bị, không đánh dấu đã triển khai.

## Quy mô bản dự thảo

18 use case bắt buộc (17 nghiệp vụ + 1 vận hành), 6 use case bổ sung, 25 mục quyết định với trạng thái riêng (D-24/D-25 đã xác nhận, D-11 bị thay thế), 20 màn hình cơ sở và ma trận phủ FR-01–20. Tiêu chí đang áp dụng vẫn NOT RUN; AC-15/TC-15 DEFERRED.

## Cập nhật

[CR-001 — Thu hẹp bốn vai trò](changes/CR-001_four-role-scope.md) đã được người dùng xác nhận cho dự án học tập. Ma trận mới chỉ READER/ARCHIVIST/DIRECTOR/ADMIN; UC-15 chuyển optional với trạng thái hoãn, SCR-18 không dùng. Mô hình mỗi tài khoản một vai trò D-25 đã được người dùng xác nhận.

## Thiết kế chức năng đầu tiên: đăng nhập

[Login design](design/authentication/login-design.md) → [Kế hoạch kiểm thử](design/authentication/login-test-plan.md). D-25 mỗi tài khoản một vai trò đã xác nhận theo [CR-002](changes/CR-002_single-role-and-login-design.md). Đây là bối cảnh thiết kế trước triển khai; tiến độ code hiện tại xem báo cáo năm chức năng ở đầu file. D-22 còn đề xuất.

## Phạm vi đăng nhập ưu tiên mới nhất

Theo [CR-003](changes/CR-003_basic-login-first.md) / D-26, đọc [basic-login-design.md](design/authentication/basic-login-design.md) trước: xác thực và điều hướng role, hoãn các chức năng nâng cao. Thiết kế rộng và 28 ca test cũ là tài liệu cho đợt sau; bộ BASIC-01–09 mới là kiểm thử lát cắt đầu.
