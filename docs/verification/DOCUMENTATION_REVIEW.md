# Kiểm tra chất lượng bộ tài liệu dự thảo

Kiểm tra ban đầu; cập nhật theo CR-001. Đây là kiểm tra tài liệu, không phải build/test chương trình hoặc xác nhận nghiệp vụ.

- Đã đọc/đối chiếu FR-01–20 từ SRS và các khác biệt chính trong hai tài liệu nhận xét.
- 18 use case bắt buộc (17 nghiệp vụ, 1 vận hành); 6 use case bổ sung.
- 75 mã AC được bảo toàn: 72 đang áp dụng/đề xuất cho chức năng hiện hành, 3 AC-15 DEFERRED. Mandatory liên kết TC; optional có điều kiện chọn riêng.
- Liên kết Markdown nội bộ: không phát hiện đường dẫn đứt.
- UTF-8 tiếng Việt: không phát hiện ký tự thay thế hoặc lỗi mất dấu dạng ký tự hỏi giữa từ.
- Code fence và số cột bảng Markdown: kiểm tra đạt.
- Mermaid: đã rà soát văn bản/syntax và quan hệ, chưa render bằng công cụ đồ họa trong đợt này. Cần xem preview Mermaid khi rà soát; mô tả văn bản đi kèm vẫn là căn cứ đọc.
- Đã thống nhất D-05 giữ chỗ cho CONFIRMED/READING/RETURNED, PLANNED chưa giữ chỗ; ca TC-X08 kiểm tra giới hạn này.
- Đã kiểm tra sửa sau duyệt, revision/decision, phiên theo ngày, quá hạn không trả giả, quyền hết hạn và một lượt giao mở.
- Không sửa source Java, pom.xml, schema, cấu hình runtime hoặc dữ liệu PostgreSQL trong đợt tài liệu.
- Người dùng đã xác nhận D-24 về bốn vai trò; D-11 SUPERSEDED. D-25 mỗi tài khoản một vai trò cũng đã được người dùng xác nhận. Các D-01–10 và D-12–23 vẫn PROPOSED. Không có phê duyệt của đơn vị y tế hoặc UAT/load test/restore mới. Tiêu chí áp dụng NOT RUN; AC/TC-15 DEFERRED.

## Phần cần người dùng xác nhận

D-02/03/04/05/07/10/11/13 trước code lõi; ngày/giờ và giới hạn lịch; scope báo cáo; phân kỳ email; căn cứ thời hạn và quy tắc thiếu dữ liệu; RPO/RTO/giờ phục vụ. Xem docs/REVIEW_CHECKLIST.md từ thư mục gốc.

## Kiểm tra cập nhật CR-001

- Không còn MANAGER/DELEGATE là tác nhân của mandatory; chỉ xuất hiện để giải thích loại bỏ hoặc trong UC-15 hoãn.
- Chỉ DIRECTOR duyệt; kiểm tra role/tài khoản tại commit, không còn delegation trong ERD/decision.
- 18 UC mandatory, 6 optional có điều kiện và 1 UC-15 hoãn; 20 màn hình cơ sở, SCR-18 không dùng.
- Các link tới UC-15 đã chuyển optional; mã không bị đổi số. Kiểm thử role TC-R01–05 đã mô tả, chưa chạy.
- Đã kiểm tra link nội bộ, bảng Markdown và AC duy nhất sau cập nhật. Chỉ sửa tài liệu.

## Thiết kế đăng nhập và D-25 (CR-002, )

Đã soạn login-design, nhận xét ảnh, ký pháp PlantUML và 28 ca LG-TC NOT RUN. D-25 đã xác nhận; schema thiết kế dùng APP_USER.role_id đơn và ROLE_CHANGE_HISTORY. UC-01 phân luồng A/B/C, secret cấp 2 chỉ DIRECTOR và thuộc UC-10; logout ưu tiên xóa session dù audit lỗi. D-22 ngưỡng còn đề xuất. Không code/migration/runtime change, không chạy test ứng dụng cho đợt tài liệu. PlantUML chưa render; chỉ kiểm tra nguồn và tham chiếu.
