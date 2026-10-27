# Kế hoạch kiểm thử đăng nhập — chưa chạy

> **Phạm vi lượt đầu đã thay đổi theo D-26:** xem [đăng nhập tối giản](basic-login-design.md). Hoãn quản lý phiên nâng cao, bắt buộc đổi mật khẩu lần đầu và quên/reset mật khẩu; session kỹ thuật cơ bản vẫn cần. Chi tiết mở rộng bên dưới không phải toàn bộ yêu cầu cho lượt đầu.

Tất cả NOT RUN. Đây là tài liệu thiết kế, không tạo account, sửa password hoặc thực thi request. D-22 còn đề xuất; nếu thay ngưỡng phải đổi dữ liệu/expected result cùng lúc.

## Dữ liệu chuẩn bị sau khi được phép code

T-R1 READER, T-A1 ARCHIVIST, T-D1 DIRECTOR, T-T1 ADMIN, một account LOCKED quản trị, một DISABLED, một ACTIVE bị login_blocked_until tương lai, một account must_change_password=true. Mỗi account có đúng một role FK và hash thật được tạo qua encoder, không ghi secret vào Git. Thiết lập đồng hồ test/các timestamp có thể điều khiển, không phải chờ 15/30 phút thật. Môi trường test PostgreSQL tách biệt, không dùng hồ sơ bệnh nhân.

## Ca kiểm thử

| TC | Thiết lập/thao tác | Kết quả cần kiểm chứng | Trạng thái |
|---|---|---|---|
| LG-TC-01 | Lần lượt login đúng bốn account | Một role từ DB, đúng home, không có role selector | NOT RUN |
| LG-TC-02 | Username trống/sai định dạng, password trống | Lỗi trường, không tạo context, không tăng password-failure counter | NOT RUN |
| LG-TC-03 | Sai password, username không tồn tại, LOCKED, DISABLED | Cùng nội dung lỗi chung; không tiết lộ account/role; không có session xác thực | NOT RUN |
| LG-TC-04 | Thêm role=DIRECTOR vào form READER | Server bỏ/từ chối trường không được phép, không nâng quyền | NOT RUN |
| LG-TC-05 | Bốn lần sai trong cửa sổ rồi đúng | Thành công và reset counter/window theo D-22 | NOT RUN |
| LG-TC-06 | Sai lần thứ 5 trong 15 phút | login_blocked_until=giờ hiện tại+15 phút, status quản trị không bị đổi | NOT RUN |
| LG-TC-07 | Password đúng khi đang tạm block | Không login; không kéo dài khóa do mỗi lần thử; sau hết hạn có thể xác thực lại | NOT RUN |
| LG-TC-08 | Lần sai sau cửa sổ chưa chạm ngưỡng | Bắt đầu window mới, count=1 | NOT RUN |
| LG-TC-09 | Hai lần sai song song khi count=3 | Counter nguyên tử đạt 5, không thất thoát một lần đếm | NOT RUN |
| LG-TC-10 | Login thiếu/sai CSRF | Request bị chặn, không tăng counter password, không cấp auth | NOT RUN |
| LG-TC-11 | So session ID trước/sau login | ID được thay, cookie/context đúng; không log raw session ID | NOT RUN |
| LG-TC-12 | Chưa login mở /admin/home | Về login; sau login READER vẫn chỉ reader home, không trở thành ADMIN | NOT RUN |
| LG-TC-13 | Đã READER mở /director/home hoặc /admin/home | HTTP 403, không dữ liệu trang khác role | NOT RUN |
| LG-TC-14 | Hơn idle timeout theo D-22 rồi gửi request | Session không hợp lệ, yêu cầu login lại, không phát lại POST cũ | NOT RUN |
| LG-TC-15 | POST logout CSRF hợp lệ, dùng tab khác/back | Context/session hiện tại mất hiệu lực, request kế bị chặn; cache không cấp quyền | NOT RUN |
| LG-TC-16 | GET /logout hoặc POST thiếu token | Không tự logout bằng GET; POST sai token không thay trạng thái | NOT RUN |
| LG-TC-17 | Login cùng user ở hai trình duyệt, logout một | Chỉ session hiện tại bị logout theo phương án cho nhiều session | NOT RUN |
| LG-TC-18 | ADMIN khóa hoặc đổi role khi user đang có session | auth_version tăng; request kế loại context cũ và yêu cầu login lại | NOT RUN |
| LG-TC-19 | Role/status/auth_version đổi giữa password check và commit | Không tạo session quyền cũ; trả lỗi thích hợp, không vượt quyền | NOT RUN |
| LG-TC-20 | DB lỗi trong login hoặc kiểm tra account hiện hành | Lỗi dịch vụ, không cấp/tiếp tục quyền bằng dữ liệu cũ | NOT RUN |
| LG-TC-21 | Audit/DB lỗi lúc logout | Session vẫn bị hủy; có kênh ghi lỗi kỹ thuật không bí mật | NOT RUN |
| LG-TC-22 | Đăng nhập sai/đúng, kiểm tra audit | Actor nullable khi không nhận diện, outcome/reason/correlation có, không password/hash/token | NOT RUN |
| LG-TC-23 | Gửi external returnUrl và saved POST | Không redirect ra ngoài, không tự gửi lại POST; home theo role | NOT RUN |
| LG-TC-24 | Account có must_change_password=true | Chỉ tới đổi bắt buộc/logout; chưa có luồng đổi thì không mở nghiệp vụ | NOT RUN |
| LG-TC-25 | Tạo user không role hoặc hai thay đổi role đồng thời | FK bắt buộc; một role_id cuối theo version, giữ lịch sử; không quan hệ nhiều role hiện hành | NOT RUN |
| LG-TC-26 | Khởi động lại app dùng session memory | User phải login lại; không suy ra account bị xóa | NOT RUN |
| LG-TC-27 | User/password Unicode/khoảng trắng đã cấp hợp lệ | Password không trim/đổi chữ; username chuẩn hóa nhất quán, hash match | NOT RUN |
| LG-TC-28 | Đăng nhập khi đã có session, mở /login | Về home đúng role, không lồng session account khác | NOT RUN |

## Liên kết với AC hiện có

- AC-01-01 → LG-TC-01/02/03/04/11/12/13.
- AC-01-02 → LG-TC-14/15/16/18/21.
- AC-01-03 → LG-TC-05–10/22.
- D-25 → LG-TC-04/18/19/25; TC-R05 được điều chỉnh theo FK đơn.
- Tự đổi password UC-01.C, reset UC-02 và cấp 2 UC-10 có bộ ca riêng sau khi bắt đầu các phần đó; không đánh đạt dựa trên việc login thành công.

## Bằng chứng cần thu khi thực chạy

Ngày/phiên bản/command/cấu hình; seed account không chứa password; kết quả HTTP và màn hình theo role; DB counter/role/version; audit mẫu đã làm sạch; kết quả NOT RUN/PASS/FAIL và người kiểm. Không chỉ chụp trang chào rồi kết luận mọi chức năng auth đã đúng.
