# Hoàn thành CR-012 — Bác sĩ khai báo hồ sơ điện tử

Ngày:.

## Kết quả

Đã bổ sung luồng Reader tạo mới → Archivist kiểm tra/trình → Director duyệt và công bố ngay. Hồ sơ điện tử chỉ có dữ liệu nhận diện người bệnh theo phạm vi yêu cầu, không phải phần mềm bệnh án điện tử lâm sàng đầy đủ. Không nhận giấy, không chọn vị trí/chính sách kho trong luồng mới. Luồng Archivist nhập hồ sơ giấy chưa có trên hệ thống giữ nguyên.

Mã `MRM-REC-YYYY-000001` do PostgreSQL/backend cấp khi gửi thành công. Giữ nguyên mã khi bổ sung; UNIQUE và bộ đếm nguyên tử bảo vệ gửi đồng thời; token chống nhấn gửi lại tạo trùng. Reader không được tự truyền mã, trạng thái, người duyệt hay ngày công bố để vượt bước.

Danh sách/chi tiết/form mới ở `templates/electronic/`; mã xử lý ở `mrm/electronic/`. Menu và trang chủ cả ba vai trò có Hồ sơ điện tử; Reader có Thêm bệnh án mới. Danh sách hồ sơ giấy có liên kết sang nhánh mới. Không sửa mã hay tự di chuyển dữ liệu giấy cũ.

Đọc [mô hình dữ liệu và quy tắc](../database/ELECTRONIC_RECORDS.md); hướng dẫn theo vai trò ở [Reader](../user-guides/reader.md), [Archivist](../user-guides/archivist.md), [Director](../user-guides/director.md).

## Cách tự kiểm thử

1. Khởi động lại ứng dụng với cấu hình dev như đang dùng. Flyway sẽ áp dụng Java migration V15 vào database cấu hình; không cần chạy file SQL thủ công. Lần làm việc này chỉ chạy migration trên schema kiểm thử, chưa chạy V15 trên public.
2. Reader → Thêm bệnh án mới → nhập họ tên, năm sinh, giới tính và mã người bệnh tùy chọn → Gửi kiểm tra. Kiểm tra mã tự động và trạng thái Mới gửi.
3. Archivist → chuông hoặc Hồ sơ điện tử → mở hồ sơ. Chọn Yêu cầu bổ sung, nhập nội dung; Reader sửa/gửi lại, mã phải giữ nguyên.
4. Archivist xác nhận và trình Giám đốc. Hồ sơ ở Chờ duyệt, ngày công bố vẫn trống.
5. Director mở thông báo/hồ sơ, ghi căn cứ và mật khẩu cấp 2; duyệt. Reader tải lại thấy Đã công bố và ngày công bố ngay, không có nút nhập kho hay bước giao giấy. Kiểm tra thông báo đưa về đúng hồ sơ điện tử.
6. Thử hồ sơ khác với Từ chối/Hủy; không có ngày công bố. Thử đăng nhập Reader khác đoán URL hồ sơ: không được xem. Thử duyệt trước kiểm tra hoặc dùng phiên bản cũ: bị chặn.
7. Archivist vẫn mở được `/archivist/records/new` để nhập bệnh án giấy. Hồ sơ điện tử không chiếm ngăn và không được đưa vào mượn hồ sơ giấy.

## Kiểm thử đã chạy

- `.\mvnw.cmd -o -B -ntp package`: BUILD SUCCESS.
- Chạy riêng `mrm.features.FunctionalRequirementsCheck`: **422/422 đạt**; gồm kiểm thử cũ và mới về phân quyền, mã tự động, gửi lặp, gửi đồng thời 8 hồ sơ, sửa/bổ sung giữ mã, trạng thái/phiên bản, kiểm tra/trình/duyệt, mật khẩu sai không công bố, thời điểm công bố, từ chối/hủy, dữ liệu giấy không đổi, HTML thực qua HTTP, CSRF, chống truyền mã/trạng thái giả, escape tên người bệnh, thông báo theo vai trò và audit/snapshot.
- `mrm.features.ElectronicUpgradeCheck`: **6/6 đạt**, tạo database schema ở V14 có hồ sơ giấy/yêu cầu đọc/thông báo trước rồi nâng V15, xác minh dữ liệu cũ giữ nguyên và không tự sinh bệnh án điện tử. Schema thử nghiệm đã xóa.
- Bộ kiểm tra mới đã được thêm vào `scripts/verify-features.ps1`. Maven package không tự chạy các chương trình kiểm thử main này.
- Trong các lần chạy đầu, đã sửa bộ dữ liệu kiểm thử để chọn đúng người nhận khi nhiều Archivist/Director cùng nhận thông báo, và tạo tài khoản Archivist giả lập mà migration danh mục V4 yêu cầu. Lần cuối cả hai bộ đều đạt.
- Không thay đổi dữ liệu public trong lần kiểm thử; không có bệnh án thật được tạo. Chưa kiểm tra trực quan bằng trình duyệt trong lần này; HTML các màn hình và chu trình tạo–duyệt đã được kiểm tra bằng HTTP thật.

Log: [422 kiểm tra](evidence/electronic-records-2026-10-25.txt), [nâng V14–V15](evidence/electronic-upgrade-2026-10-25.txt).
