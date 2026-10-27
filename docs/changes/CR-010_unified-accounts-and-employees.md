# CR-010 — Form tài khoản chung và hồ sơ nhân viên

Ngày. Người dùng yêu cầu bỏ các nút tạo Reader/Archivist riêng, dùng một form chọn role; nhãn READER là **Bác sĩ**, ARCHIVIST là **Nhân viên lưu trữ**. Đồng thời yêu cầu bảng nhân viên gồm ID căn cước 10 chữ số, tên, ngày sinh, giới tính và role.

Đã giữ nguyên bốn mã quyền READER/ARCHIVIST/DIRECTOR/ADMIN; chỉ đổi nhãn HTML, không thêm role mới hay đổi quyền nghiệp vụ. Các nhãn tương ứng là Bác sĩ/Nhân viên lưu trữ/Giám đốc/Quản trị viên. Một lối Tạo tài khoản chung từ trang chủ Admin và menu Tài khoản.

Trước thay đổi, app_users chứa username/password băm/display_name/role/khoa/email, chưa có bộ hồ sơ nhân viên gồm căn cước/ngày sinh/giới tính. Java migration V13 bổ sung employees, liên kết một-một tùy chọn từ tài khoản: tài khoản cũ có thể chưa có hồ sơ; một hồ sơ nhân viên luôn liên kết đúng một tài khoản. Không bịa căn cước/ngày sinh cho tài khoản có sẵn.

Đọc [thiết kế và kiểm chứng](../verification/EMPLOYEE_MANAGEMENT.md), [hướng dẫn Admin](../user-guides/admin.md). Những hướng dẫn cũ về nút tạo tách biệt đã được thay thế.
