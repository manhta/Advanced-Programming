# Truy vết và kế hoạch kiểm thử nghiệm thu

**Trạng thái: NOT RUN.** Đây là thiết kế kiểm chứng, không phải báo cáo test đã thực hiện. MRM-UC dùng mã mới; TC nối AC của từng use case.

## Phủ yêu cầu SRS

| Nguồn | MRM-UC/OPT | Phạm vi | Màn hình | Quy tắc MRM |
|---|---|---|---|---|
| FR-01 | 01 | Nền tảng bắt buộc | SCR-01 | BR-01/22 |
| FR-02 | 02 | Bắt buộc | SCR-03 | BR-01/19/22 |
| FR-03 | 05 | Bắt buộc | SCR-06/07 | BR-01/03 |
| FR-04 | 02/05; xuyên suốt | Bắt buộc | Mọi màn hình/báo cáo | BR-01 |
| FR-05 | 06/13 | Bắt buộc | SCR-05/16 | BR-04/16 |
| FR-06 | 06/12/13/14 | Bắt buộc | SCR-07/15/16/17 | BR-10/11/13/23 |
| FR-07 | 04 | Bắt buộc nhập tay | SCR-08 | BR-02/17 |
| FR-08 | 07/09/11 | Bắt buộc | SCR-09/10/14 | BR-05/08/09/14 |
| FR-09 | 08 | Bắt buộc | SCR-11 | BR-06 |
| FR-10 | 10; UC-15 DEFERRED | Chỉ DIRECTOR duyệt; ủy quyền nguồn hoãn theo CR-001 | SCR-12/13 | BR-06/07 |
| FR-11 | 11; OPT-01 | Lịch bắt buộc; email chưa chốt phân kỳ | SCR-14; tùy chọn hàng đợi mail | BR-08/21 |
| FR-12 | 12/13/14 | Bắt buộc | SCR-15/16/17 | BR-10/11/12/13/15 |
| FR-13 | 16; xuyên suốt | Bắt buộc | SCR-19 | BR-22 |
| FR-14 | 04/07/18 | Metadata có; workflow tử vong loại trừ | SCR-07/08/09 | BR-03 |
| FR-15 | 03/18 | Bắt buộc | SCR-04/21 | BR-17/18 |
| FR-16 | 17; OPT-05 | Báo cáo cơ sở đề xuất, thống kê bổ sung | SCR-20 | BR-20 |
| FR-17 | 04; OPT-02 | Nhập tay cơ sở; nhập tệp bổ sung; không HIS | SCR-08; tùy chọn nhập file | BR-02/17 |
| FR-18 | 07 (kiểm tra phạm vi) | Không tài khoản/workflow bên ngoài | SCR-09 chặn nhánh ngoài | BR-03/23 |
| FR-19 | OPT-04 | Bổ sung cần chọn | Màn hình kiểm kê tùy chọn | BR-16/23 |
| FR-20 | OPT-03 | Có điều kiện, chưa chọn định dạng/quyền | SCR-20 nếu được chọn | BR-01/20/22 |

## Kịch bản xuyên suốt và dữ liệu giả

Chuẩn bị U-R1/U-R2 (người đọc), U-A1/U-A2 (NVLT), U-D (Giám đốc), U-T (ADMIN); khoa K1/K2, vị trí W1/C1/S1/B1 và B2, MR1 thông thường, MR2 tử vong, MR3 blocked; hai HSBA cùng patient_code khác đợt. Dữ liệu hoàn toàn giả. Ngày T là ngày làm việc theo cấu hình, không hardcode ngày quá khứ.

1. R1 gửi MR1 cho T,T+1,T+2 → A1 trả bổ sung → R1 revision 2 → A1 trình → D xác thực/duyệt revision 2.
2. A1 xác nhận T,T+1, để T+2 PLANNED → T giao/nhận trả → T+1 giao, qua giờ chốt mở ngoại lệ, ngày sau nhận trả với lý do.
3. Hủy T+2 khi chưa giao → giữ mọi lịch sử, request APPROVED với tiến độ PARTIALLY_FULFILLED.
4. Báo cáo: một HSBA, một request, ba phiên; hai RETURNED, một CANCELLED; có lịch sử ngoại lệ, không còn handover mở; không tự coi ba phiên là ba hồ sơ.
5. Chạy biến thể bác sĩ đoán ID/vị trí, DIRECTOR bị khóa/mất vai trò, hai NVLT giao, SMTP lỗi nếu optional chọn.

## TC theo từng AC

Mỗi dòng: thiết lập dữ liệu theo tiền điều kiện UC → thực hiện tình huống mô tả → kiểm tra kết quả qua UI/phản hồi, DB và audit phù hợp quyền trong môi trường test. Cột kết quả chỉ cập nhật khi thực chạy.

| TC | AC tham chiếu | Tình huống và kết quả cần quan sát | Trạng thái |
|---|---|---|---|
| TC-01-01 | [AC-01-01](../functions/mandatory/MRM-UC-01_authentication.md) | Thông tin đúng → vào đúng trang vai trò; sai → không có session xác thực. | NOT RUN |
| TC-01-02 | [AC-01-02](../functions/mandatory/MRM-UC-01_authentication.md) | Đăng xuất rồi gửi lại form nhạy cảm bằng tab cũ → bị chặn. | NOT RUN |
| TC-01-03 | [AC-01-03](../functions/mandatory/MRM-UC-01_authentication.md) | Quá ngưỡng D-22 → tạm khóa theo chính sách, không ghi mật khẩu trong log. | NOT RUN |
| TC-02-01 | [AC-02-01](../functions/mandatory/MRM-UC-02_account-and-role-management.md) | Khóa tài khoản đang mở trang duyệt → lần lưu sau bị chặn. | NOT RUN |
| TC-02-02 | [AC-02-02](../functions/mandatory/MRM-UC-02_account-and-role-management.md) | ADMIN không có quyền đọc HSBA gọi URL chi tiết → bị từ chối. | NOT RUN |
| TC-02-03 | [AC-02-03](../functions/mandatory/MRM-UC-02_account-and-role-management.md) | Đặt lại mật khẩu cấp 2 → chứng cứ xác thực cũ mất hiệu lực, quyết định cũ vẫn còn. | NOT RUN |
| TC-03-01 | [AC-03-01](../functions/mandatory/MRM-UC-03_reference-data.md) | Danh mục đã dùng bị vô hiệu hóa → lịch sử vẫn hiển thị tên/mã. | NOT RUN |
| TC-03-02 | [AC-03-02](../functions/mandatory/MRM-UC-03_reference-data.md) | Đổi chính sách → hồ sơ cũ giữ policy snapshot trừ khi có điều chỉnh được duyệt. | NOT RUN |
| TC-03-03 | [AC-03-03](../functions/mandatory/MRM-UC-03_reference-data.md) | Ngày lưu 29/2 → mốc thời hạn tuân thủ quy tắc D-19 sau xác nhận. | NOT RUN |
| TC-04-01 | [AC-04-01](../functions/mandatory/MRM-UC-04_record-entry-and-initial-data.md) | Hai HSBA cùng mã bệnh nhân nhưng khác đợt/khóa hợp lệ → lưu hai bản ghi. | NOT RUN |
| TC-04-02 | [AC-04-02](../functions/mandatory/MRM-UC-04_record-entry-and-initial-data.md) | Hai thao tác cùng tạo khóa nghiệp vụ → chỉ một bản ghi chính thức. | NOT RUN |
| TC-04-03 | [AC-04-03](../functions/mandatory/MRM-UC-04_record-entry-and-initial-data.md) | Dữ liệu cũ thiếu ngày đầu lưu → không tự tính thời hạn hoặc coi đã đối soát. | NOT RUN |
| TC-05-01 | [AC-05-01](../functions/mandatory/MRM-UC-05_record-search.md) | Bác sĩ tìm/mở chi tiết → không có kho/tủ/kệ/ngăn trong dữ liệu trả. | NOT RUN |
| TC-05-02 | [AC-05-02](../functions/mandatory/MRM-UC-05_record-search.md) | Khoảng ngày đảo ngược → báo lỗi trước khi truy vấn. | NOT RUN |
| TC-05-03 | [AC-05-03](../functions/mandatory/MRM-UC-05_record-search.md) | Kết quả 100 bản ghi → phân trang không lặp/mất khi dữ liệu ổn định. | NOT RUN |
| TC-06-01 | [AC-06-01](../functions/mandatory/MRM-UC-06_storage-and-location-history.md) | Chuyển thành công → một lịch sử cũ/mới khớp current location. | NOT RUN |
| TC-06-02 | [AC-06-02](../functions/mandatory/MRM-UC-06_storage-and-location-history.md) | Hai người đổi cùng version → chỉ một thành công, người sau tải lại. | NOT RUN |
| TC-06-03 | [AC-06-03](../functions/mandatory/MRM-UC-06_storage-and-location-history.md) | Ngừng ngăn còn chứa hồ sơ → bị chặn, lịch sử cũ không mất. | NOT RUN |
| TC-07-01 | [AC-07-01](../functions/mandatory/MRM-UC-07_submit-reading-request.md) | Một HSBA/3 ngày → một request, ba ngày mong muốn, chưa có lượt giao. | NOT RUN |
| TC-07-02 | [AC-07-02](../functions/mandatory/MRM-UC-07_submit-reading-request.md) | Đổi user_id trên form → server vẫn gắn chủ tài khoản hợp lệ, không tạo hộ. | NOT RUN |
| TC-07-03 | [AC-07-03](../functions/mandatory/MRM-UC-07_submit-reading-request.md) | Submit hai lần cùng khóa → một request duy nhất. | NOT RUN |
| TC-08-01 | [AC-08-01](../functions/mandatory/MRM-UC-08_review-and-submit-for-approval.md) | Thiếu mục đích rõ → trả bổ sung, chủ yêu cầu thấy lý do. | NOT RUN |
| TC-08-02 | [AC-08-02](../functions/mandatory/MRM-UC-08_review-and-submit-for-approval.md) | Revision đã đổi khi NVLT nhấn trình → từ chối ghi version cũ. | NOT RUN |
| TC-08-03 | [AC-08-03](../functions/mandatory/MRM-UC-08_review-and-submit-for-approval.md) | NVLT gọi trực tiếp hành động duyệt → bị từ chối. | NOT RUN |
| TC-09-01 | [AC-09-01](../functions/mandatory/MRM-UC-09_amend-cancel-and-track.md) | Thay mục đích trước mọi giao sau duyệt → phải duyệt lại, quyết định cũ không áp cho revision mới. | NOT RUN |
| TC-09-02 | [AC-09-02](../functions/mandatory/MRM-UC-09_amend-cancel-and-track.md) | Hủy sau một ngày đã trả → ngày cũ còn nguyên, ngày tương lai hủy. | NOT RUN |
| TC-09-03 | [AC-09-03](../functions/mandatory/MRM-UC-09_amend-cancel-and-track.md) | Hủy và duyệt đồng thời → chỉ luồng hợp lệ theo version được ghi. | NOT RUN |
| TC-10-01 | [AC-10-01](../functions/mandatory/MRM-UC-10_approval-and-step-up.md) | Chưa xác thực mà gửi action trực tiếp → bị chặn. | NOT RUN |
| TC-10-02 | [AC-10-02](../functions/mandatory/MRM-UC-10_approval-and-step-up.md) | Tài khoản bị khóa hoặc mất vai trò DIRECTOR đúng lúc lưu → không tạo decision. | NOT RUN |
| TC-10-03 | [AC-10-03](../functions/mandatory/MRM-UC-10_approval-and-step-up.md) | Cùng operation key retry → cùng decision; tự duyệt → bị chặn. | NOT RUN |
| TC-11-01 | [AC-11-01](../functions/mandatory/MRM-UC-11_confirm-reading-schedule.md) | Duyệt 3 ngày, xác nhận 2 → hai CONFIRMED và một PLANNED, không đổi phạm vi quyết định. | NOT RUN |
| TC-11-02 | [AC-11-02](../functions/mandatory/MRM-UC-11_confirm-reading-schedule.md) | Hai NVLT giữ chỗ cùng HSBA/ngày cho hai người → chỉ một thành công. | NOT RUN |
| TC-11-03 | [AC-11-03](../functions/mandatory/MRM-UC-11_confirm-reading-schedule.md) | READER xem lịch người khác → từ chối; thông báo không lộ vị trí. | NOT RUN |
| TC-12-01 | [AC-12-01](../functions/mandatory/MRM-UC-12_handover-at-reading-room.md) | Hai lệnh đồng thời → một handover chưa trả duy nhất. | NOT RUN |
| TC-12-02 | [AC-12-02](../functions/mandatory/MRM-UC-12_handover-at-reading-room.md) | Lỗi ghi audit trong transaction → toàn bộ giao rollback. | NOT RUN |
| TC-12-03 | [AC-12-03](../functions/mandatory/MRM-UC-12_handover-at-reading-room.md) | Retry cùng khóa → trả lại cùng mã/giờ giao, không cập nhật lại giờ. | NOT RUN |
| TC-13-01 | [AC-13-01](../functions/mandatory/MRM-UC-13_return-to-storage.md) | Trả ngày hôm sau → vẫn đóng lượt với timestamp thực, lưu lý do và ngoại lệ quá. | NOT RUN |
| TC-13-02 | [AC-13-02](../functions/mandatory/MRM-UC-13_return-to-storage.md) | Trả có thiếu → IN_STORAGE nhưng block, không được giao lượt mới. | NOT RUN |
| TC-13-03 | [AC-13-03](../functions/mandatory/MRM-UC-13_return-to-storage.md) | Tài khoản người duyệt bị khóa sau giao → vẫn cho NVLT nhận trả. | NOT RUN |
| TC-14-01 | [AC-14-01](../functions/mandatory/MRM-UC-14_exceptions-and-reconciliation.md) | Chạy đối soát hai lần → một OVERDUE mở mỗi handover. | NOT RUN |
| TC-14-02 | [AC-14-02](../functions/mandatory/MRM-UC-14_exceptions-and-reconciliation.md) | Đánh dấu không đến cho READING → bị chặn. | NOT RUN |
| TC-14-03 | [AC-14-03](../functions/mandatory/MRM-UC-14_exceptions-and-reconciliation.md) | Nhận trả nhưng còn hư hỏng → đóng phần quá hạn, ngoại lệ condition còn mở. | NOT RUN |
| TC-16-01 | [AC-16-01](../functions/mandatory/MRM-UC-16_history-and-audit.md) | Lọc actor/time/action → đúng bộ dữ liệu biết trước. | NOT RUN |
| TC-16-02 | [AC-16-02](../functions/mandatory/MRM-UC-16_history-and-audit.md) | Thử sửa/xóa qua đường dẫn trực tiếp → không có quyền chức năng. | NOT RUN |
| TC-16-03 | [AC-16-03](../functions/mandatory/MRM-UC-16_history-and-audit.md) | Quyết định/giao–trả → có correlation liên kết các sự kiện, không có password/token. | NOT RUN |
| TC-17-01 | [AC-17-01](../functions/mandatory/MRM-UC-17_operational-reports.md) | Một HSBA, một request, ba phiên → ba phiên, một request, một HSBA distinct. | NOT RUN |
| TC-17-02 | [AC-17-02](../functions/mandatory/MRM-UC-17_operational-reports.md) | Có lượt giao hôm qua chưa trả → RP-11 hôm nay vẫn có. | NOT RUN |
| TC-17-03 | [AC-17-03](../functions/mandatory/MRM-UC-17_operational-reports.md) | Bộ lọc theo khoa → cả tổng và dòng chi tiết cùng scope. | NOT RUN |
| TC-18-01 | [AC-18-01](../functions/mandatory/MRM-UC-18_retention-and-record-classification.md) | Ngày đầu lưu khác ngày nhập → tính từ ngày đầu lưu. | NOT RUN |
| TC-18-02 | [AC-18-02](../functions/mandatory/MRM-UC-18_retention-and-record-classification.md) | Nhiều nhóm → kết quả theo quy tắc đã duyệt, có thể truy vết phiên bản. | NOT RUN |
| TC-18-03 | [AC-18-03](../functions/mandatory/MRM-UC-18_retention-and-record-classification.md) | Đến mốc lưu → không xóa hồ sơ hoặc tạo lệnh tiêu hủy. | NOT RUN |
| TC-19-01 | [AC-19-01](../functions/mandatory/MRM-UC-19_backup-and-recovery.md) | Restore tách biệt, đối chiếu schema/số liệu/lượt mở/audit và đo thời gian | NOT RUN |
| TC-19-02 | [AC-19-02](../functions/mandatory/MRM-UC-19_backup-and-recovery.md) | Dữ liệu sau backup mất: ghi khoảng mất, đối soát vật lý/sổ tạm, không trả giả | NOT RUN |
| TC-19-03 | [AC-19-03](../functions/mandatory/MRM-UC-19_backup-and-recovery.md) | Hồ sơ chưa xác minh sau restore vẫn block giao mới | NOT RUN |

## Kiểm thử tích hợp bắt buộc

| TC | Thiết lập / thao tác | Kết quả |
|---|---|---|
| TC-X01 | Hai NVLT nhận cùng version, đồng thời giao MR1 | Một handover mở; READING/AT_READING_ROOM khớp; một conflict |
| TC-X02 | Mất phản hồi sau commit, retry cùng key | Cùng ID/giờ kết quả; không thêm lượt |
| TC-X03 | Tiêm lỗi sau insert handover trước state/audit | Rollback toàn bộ; không lượt/state dở dang |
| TC-X04 | DIRECTOR mở trang, tài khoản bị khóa/đổi vai trò trước lưu | Không decision; request còn chờ; log DENIED |
| TC-X05 | R1 sửa revision lúc D duyệt | Một luồng hợp lệ theo version, không quyết định revision sai |
| TC-X06 | Trả MR1 nhưng DAMAGED, mở block | RETURNED + IN_STORAGE + block; lượt mới bị chặn |
| TC-X07 | READING qua ngày, scheduler đối soát lặp | Vẫn READING, một OVERDUE; không trả giả |
| TC-X08 | MR1 RETURNED trong ngày, R2 xin xác nhận cùng ngày | Chặn theo D-05; nếu đổi sang ca cần thay đặc tả |
| TC-X09 | ADMIN/R1 gọi trực tiếp URL metadata vị trí/audit | Không lộ dữ liệu, tổng số hoặc nội dung lỗi |
| TC-X10 | Restore bản sao và so handover với sổ tạm | Phát hiện chênh, block giao mới đến đối soát; đo RPO/RTO |
| TC-X11 | Audit lưu giữ dài hạn, khôi phục một mẫu | Đọc được actor/object/outcome; không chứng minh đã tồn tại 10 năm chỉ bằng test ngắn |
| TC-X12 | Ngày chốt/00:00, hạn proof cấp 2, 29/2 | Đúng timezone, hạn proof cấp 2, quy tắc policy đã duyệt |

## Kiểm thử bốn vai trò sau CR-001

| TC | Thiết lập/thao tác | Kết quả | Trạng thái |
|---|---|---|---|
| TC-R01 | READER/ARCHIVIST/ADMIN gửi thẳng hành động duyệt | DENIED, không decision, audit thất bại | NOT RUN |
| TC-R02 | DIRECTOR không sẵn sàng, mở trang bằng ADMIN | Request vẫn chờ, không nút hoặc đường duyệt thay | NOT RUN |
| TC-R03 | ADMIN cố cấp MANAGER/DELEGATE qua form/API | Từ chối vai trò ngoài catalog bốn giá trị | NOT RUN |
| TC-R04 | DIRECTOR xem báo cáo/ngoại lệ trước đây MANAGER giám sát | Được xem trong scope, không tự sửa metadata/giao–trả | NOT RUN |
| TC-R05 | Gửi đồng thời hai thay đổi role cho một user | Một role_id hiện hành, một thay đổi thắng theo version; giữ lịch sử; không có role active thứ hai | NOT RUN |

## Optional và phi chức năng

Các AC-OPT trong từng file optional là TC-OPT tương ứng khi được chọn. Nếu không chọn thì ghi DEFERRED có căn cứ, không đánh PASS. Mỗi NFR-M01–12 trong non-functional.md là một gói kiểm thử với cấu hình/dữ liệu/kết quả riêng. FR-11 không đánh hoàn thành email chỉ vì có màn hình xem lịch.

## Điều kiện kết thúc nghiệm thu

Tất cả P0 được xác nhận; AC bắt buộc đạt; không còn lỗi nghiêm trọng làm sai quyền, quyết định, trạng thái kho hoặc mất dữ liệu; UAT theo vai trò; optional có trạng thái rõ; restore thử có bằng chứng; người có trách nhiệm ghi người/ngày/kết luận. Chưa thực hiện các hoạt động này trong đợt tài liệu.

## Các mã hoãn theo CR-001

UC-15 / AC-15-01–03 / TC-15-01–03: DEFERRED, không phải điều kiện hoàn thành mandatory bản đầu. Giữ mã không cấp lại cho chức năng khác. Kiểm thử bắt buộc thêm: READER/ARCHIVIST/ADMIN gọi quyết định trực tiếp đều bị chặn; DIRECTOR vắng mặt không có luồng duyệt thay; catalog vai trò không có MANAGER/DELEGATE. D-25 đã xác nhận; phải kiểm thử mỗi user luôn có đúng một role.

## Lát cắt đăng nhập chi tiết

[LG-TC-01–28](../design/authentication/login-test-plan.md) cụ thể hóa AC-01 và D-25; tất cả NOT RUN. D-25 đã xác nhận, một role hiện hành theo FK đơn; không chờ duyệt lại danh sách vai trò. Ngưỡng D-22 vẫn PROPOSED.
