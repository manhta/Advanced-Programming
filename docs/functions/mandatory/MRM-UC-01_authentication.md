# MRM-UC-01 — Đăng nhập, đăng xuất và phiên làm việc

**Bản triển khai:** xem [CR-005](../../changes/CR-005_five-feature-implementation.md) và [báo cáo năm chức năng](../../verification/FIVE_FEATURE_COMPLETION_REPORT.md) cho phạm vi đang chạy, hướng dẫn và kết quả kiểm thử. Nội dung dự thảo tổng thể dưới đây còn các nhánh mở rộng chưa triển khai.

> **Phạm vi lượt đầu đã thay đổi theo D-26:** xem [đăng nhập tối giản](../../design/authentication/basic-login-design.md). Hoãn quản lý phiên nâng cao, bắt buộc đổi mật khẩu lần đầu và quên/reset mật khẩu; session kỹ thuật cơ bản vẫn cần. Chi tiết mở rộng bên dưới không phải toàn bộ yêu cầu cho lượt đầu.

**Trạng thái tài liệu gốc (không phải tiến độ code hiện tại):** thiết kế v0.2,; chưa triển khai. Bốn role D-24 và mỗi user đúng một role D-25 đã được người dùng xác nhận; D-22 vẫn đề xuất.

## Phạm vi và tài liệu chi tiết

File này nhóm ba mục tiêu liên quan: **UC-01.A Đăng nhập**, **UC-01.B Đăng xuất**, **UC-01.C Tự đổi mật khẩu**. Tách các luồng để dễ vẽ sơ đồ, không hiểu đăng xuất là một bước phải chạy ngay sau đăng nhập. Đợt đầu ưu tiên A/B/session; C làm tiếp theo trước khi dùng tài khoản tạm buộc đổi password.

[Thiết kế theo quy trình đầy đủ](../../design/authentication/login-design.md): yêu cầu → giao diện phác thảo → dữ liệu → backend contract → UI → kiểm thử. [Nhận xét sơ đồ](../../design/authentication/diagram-review.md). [28 ca kiểm thử thiết kế](../../design/authentication/login-test-plan.md).

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | Người dùng nội bộ: READER, ARCHIVIST, DIRECTOR, ADMIN |
| Nguồn/quy tắc | FR-01; D-22/24/25; BR-01/22 |
| Dữ liệu | Username 3–64, password; CSRF hợp lệ khi POST. Không nhận role hoặc user_id từ form để cấp quyền |
| Quyền | Cùng một form login; server đọc đúng một role từ APP_USER.role_id |

## UC-01.A — Đăng nhập

**Tiền điều kiện:** account đã cấp; dịch vụ khả dụng. Người dùng chưa xác thực; session tiền xác thực chỉ phục vụ form/CSRF.

1. Mở /login; hệ thống hiển thị username/password và hướng dẫn liên hệ ADMIN, không có chọn role.
2. Gửi form, server kiểm tra CSRF/kích thước/định dạng, chuẩn hóa username; không trim password.
3. Kiểm tra status, temporary login block, role hợp lệ; xác minh password qua PasswordEncoder, không so plaintext.
4. Sai thông tin: counter cập nhật nguyên tử theo D-22, audit failure được làm sạch, trả thông báo chung không lộ tồn tại/khóa account.
5. Đúng: recheck account/version, reset counter, last_login_at và audit thành công; commit rồi tạo context, đổi session ID và lưu role/auth_version.
6. Về /home, chuyển trang role đúng DB; không phát lại POST cũ hoặc nhận redirect ra ngoài.

**Thay thế/ngoại lệ:** trống/sai định dạng báo lỗi trường; CSRF sai yêu cầu tải lại không tính sai password; username lạ không tạo account; đang bị block dù password đúng vẫn chưa được login; DB/audit lỗi trước commit không cấp session. Thay role/khóa giữa lúc kiểm tra và lưu không cấp quyền snapshot cũ. Đã có session hợp lệ mở /login thì về home.

**Hậu điều kiện:** session có một role hợp lệ hoặc không cấp phiên xác thực mới. Bộ đếm thử sai/audit có thể được lưu dù login thất bại: đây là kết quả bảo mật có chủ đích, không phải cập nhật nghiệp vụ dở dang.

## UC-01.B — Đăng xuất và vòng đời session

**Tiền điều kiện:** có session hợp lệ và CSRF cho POST logout. Người dùng bấm Đăng xuất.

1. Trình duyệt gửi POST /logout với CSRF; GET không trực tiếp thay trạng thái.
2. Server xóa security context, vô hiệu session hiện tại và cookie tương ứng.
3. Ghi audit logout, về /login với thông báo đã đăng xuất. Audit lỗi không được giữ người dùng đăng nhập; ghi fallback kỹ thuật không bí mật.
4. Tab khác cùng session request tiếp bị chặn; trang bảo vệ dùng no-store, Back không khôi phục quyền.

Session idle 30 phút theo đề xuất D-22. Cho phép nhiều session ở các trình duyệt khác nhau trong phương án cơ sở; logout chỉ session hiện tại. Role/status/password thay đổi cần tăng auth_version; request bảo vệ so với DB để loại context cũ. Khởi động lại ứng dụng dùng session memory làm người dùng phải login lại. Truy cập sai role trả 403; thiếu session về login; DB lỗi không cho qua bằng quyền cũ.

## UC-01.C — Tự đổi mật khẩu (phần làm sau login cơ sở)

**Tiền điều kiện:** đã xác thực, hoặc session giới hạn bắt buộc đổi sau reset; account đang hoạt động.

1. Nhập password hiện tại, password mới và xác nhận mới; không có trường đổi role.
2. Xác minh hiện tại, kiểm tra chính sách D-22 và hai giá trị mới khớp; sai không đổi hash.
3. Lưu hash mới, xóa must_change_password, tăng auth_version và ghi audit; thu hồi các session theo chính sách đề xuất, yêu cầu login lại.
4. Quên password không dùng luồng này: liên hệ ADMIN, reset thuộc UC-02. Khi chưa có màn hình đổi bắt buộc, không dùng account tạm để mở nghiệp vụ.

**Ngoại lệ:** nhập sai hiện tại/không đạt chính sách/CSRF sai → không đổi hash; lỗi sau ghi phải đảm bảo transaction hash/version/audit nhất quán. Không gửi password mới trong URL/log hoặc tự nhập lại vào form sau lỗi.

## Ngoài phạm vi login

Đổi secret cấp 2 chỉ DIRECTOR và vô hiệu proof cũ, thiết kế cùng UC-10; ADMIN reset thông tin xác thực thuộc UC-02. Không yêu cầu mọi user nhập secret cấp 2 ở form login. Không reset email công khai trong bản đầu.

## Tiêu chí nghiệm thu (giữ mã để truy vết)

- **AC-01-01:** Thông tin đúng → đúng trang một role từ DB; sai → không có session xác thực.
- **AC-01-02:** Đăng xuất rồi gửi lại form nhạy cảm bằng tab cũ → bị chặn; không khôi phục quyền từ cache.
- **AC-01-03:** Quá ngưỡng D-22 → tạm chặn login theo chính sách; counter nguyên tử, log không chứa bí mật.

AC/TC vẫn NOT RUN. Chi tiết kiểm chứng trong tài liệu login-test-plan. Quy tắc transaction/audit nghiệp vụ chung áp dụng khi phù hợp; login không cần operation receipt kiểu giao–trả, logout luôn ưu tiên xóa session kể cả audit DB lỗi.
