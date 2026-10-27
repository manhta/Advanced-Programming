# Sổ quyết định đề xuất

D-01–10, D-12–18 và D-20–23 vẫn **PROPOSED**, chưa có người/ngày phê duyệt nghiệp vụ. D-19 được người dùng xác nhận **chỉ cho mô phỏng học tập** ngày, không phải phê duyệt pháp lý của đơn vị y tế. D-11 **SUPERSEDED** bởi D-24. D-24 **CONFIRMED_BY_USER** ngày cho phạm vi dự án học tập. D-25 xác nhận mỗi tài khoản đúng một vai trò. P0 chốt trước code module, P1 trước nghiệm thu. Không tự chuyển các quyết định còn lại thành APPROVED.

| Mã | Mức | Phương án cơ sở, điểm cần rà soát | Người cần xác nhận |
|---|---|---|---|
| D-01 | P0 | Bắt đầu nhập kho; chưa quản lý soát/tiếp nhận từ khoa | Giám đốc/KHNV |
| D-02 | P0 | Một yêu cầu, một HSBA, một người đọc, nhiều; nhiều HSBA cần thiết kế khác | KHNV/NVLT |
| D-03 | P0 | Duyệt cả revision, không duyệt một phần; cần xác nhận có chọn từng ngày không | Giám đốc |
| D-04 | P0 | ID UUID; khóa nghiệp vụ đề xuất số lưu trữ + năm đầu lưu + kho gốc, phải đối chiếu dữ liệu thật; mã bệnh nhân không unique | NVLT |
| D-05 | P0 | Một HSBA/ngày tối đa một phiên CONFIRMED/READING/RETURNED; chưa chia ca giờ | NVLT |
| D-06 | P0 | Ngày không quá khứ, tối đa 5 ngày trong 30 ngày tới; ngày làm việc theo lịch đơn vị; giới hạn đều là đề xuất | KHNV/NVLT |
| D-07 | P0 | READER xem metadata cơ bản toàn đơn vị, chỉ yêu cầu/phiên của mình; có cần giới hạn theo khoa? | Giám đốc |
| D-08 | P0 | ADMIN kỹ thuật không mặc định xem HSBA hoặc tự cấp quyền duyệt; vai trò nhạy cảm cần căn cứ Giám đốc | Giám đốc/CNTT |
| D-09 | P0 | Cấm tự duyệt, chưa nhập thay/nhận thay; nếu tài khoản đã tạo yêu cầu trước khi đổi sang DIRECTOR thì không tự duyệt; yêu cầu chờ một DIRECTOR hợp lệ khác hoặc hủy theo quy tắc | Giám đốc |
| D-10 | P0 | Mật khẩu cấp 2 riêng, băm; chứng cứ một lần gắn user+revision+action, hiệu lực 5 phút; sai 5 lần/15 phút khóa 15 phút; không coi là MFA | Giám đốc/CNTT |
| D-11 | Hoãn | SUPERSEDED bởi D-24: không triển khai ủy quyền trong phiên bản đầu; giữ UC-15 ở optional với trạng thái DEFERRED | Người dùng xác nhận thay phạm vi |
| D-12 | P0 | Tách trạng thái request/session/physical; cờ tình trạng độc lập | KHNV/NVLT |
| D-13 | P0 | Sửa quan trọng trước mọi giao: hủy phiên tương lai, revision mới, duyệt lại; đã thực hiện một phần chỉ hủy phần chưa giao, đăng ký ngày mới qua yêu cầu khác | KHNV |
| D-14 | P0 | Chốt ngày 17:00 Asia/Bangkok, chưa trả mở ngoại lệ; không tự trả hoặc tự khóa người đọc; giờ cần xác nhận | NVLT/KHNV |
| D-15 | P0 | Tạm không khả dụng do việc ngoài hệ thống phải có căn cứ; không xây workflow ngoài, không dùng ghi chú đóng lượt đang mở | NVLT/Giám đốc |
| D-16 | P1 | Email phân kỳ có điều kiện; nếu chọn SMTP nội bộ, đề xuất đo đến SMTP chấp nhận ≤5 phút; cần duyệt thay đổi tiêu chí “thư đến” | Giám đốc/CNTT |
| D-17 | P1 | Nhập tay 100 HSBA có đối soát; nhập tệp bổ sung; dữ liệu thiếu không tạo giả | NVLT |
| D-18 | P1 | RP-01/03/04/05/06/07/10/11 tối thiểu, RP-02 chỉ hiện tại; lịch sử tại ngày phải thiết kế thêm | KHNV/Giám đốc |
| D-19 | Đã xác nhận cho học tập | CONFIRMED_BY_USER: 10/15/20/30 năm theo tài liệu Khánh Sơn; tính từ ngày đầu lưu kho; nếu ngày gốc 29/2 thì ngày đích là 28/2 kể cả năm đích nhuận; nhiều nhóm lấy dài nhất. Không tự tính lại hồ sơ cũ, không tiêu hủy, không khẳng định pháp luật hiện hành | Người dùng,; CR-004 |
| D-20 | P1 | Audit 10 năm, chỉ thêm; ghi xem chi tiết/thay đổi/xuất/thất bại quan trọng; quyền và dung lượng cần chốt | Giám đốc/CNTT |
| D-21 | P1 | p95 tìm ≤3s/100 HSBA/10 người; RPO≤24h, RTO≤4h là ngưỡng thảo luận, cần cấu hình và chấp nhận rủi ro mất giao dịch | CNTT/NVLT |
| D-22 | P1 | Session idle 30 phút; mật khẩu ≥12 ký tự, chặn phổ biến; sai 5 lần/15 phút khóa 15 phút; cần chính sách đơn vị | CNTT |
| D-23 | P1 | Nghi mất/thiếu chặn giao; hư hỏng đã biết chỉ giao khi có căn cứ và người có thẩm quyền xác nhận | KHNV/NVLT |
| D-24 | Đã xác nhận | CONFIRMED_BY_USER: chỉ READER, ARCHIVIST, DIRECTOR, ADMIN; bỏ MANAGER/DELEGATE; Director giám sát và duyệt, không ủy quyền trong bản đầu | Người dùng,; CR-001 |
| D-25 | Đã xác nhận | CONFIRMED_BY_USER: mỗi tài khoản đúng một vai trò thuộc READER/ARCHIVIST/DIRECTOR/ADMIN; chỉ READER có chức năng tạo yêu cầu theo ma trận hiện hành | Người dùng,; CR-002 |
| D-26 | Đã xác nhận | CONFIRMED_BY_USER: lượt đầu chỉ xác thực username/password rồi điều hướng theo role; hoãn quản lý phiên nâng cao, forced first-login change và quên mật khẩu. Session nền tảng vẫn là cơ chế duy trì xác thực | Người dùng,; CR-003 |
| D-27 | Đã xác nhận cho học tập | Cố định năm khoa đã khởi tạo, bỏ thêm khoa; giữ lịch sử tạo. Loại bệnh án vẫn có thể thêm/sửa/ngừng dùng; chính sách thay đổi bằng phiên bản mới | Người dùng,; CR-004 |

## Bổ sung triển khai nhóm năm ngày

[CR-005](../changes/CR-005_five-feature-implementation.md) ghi nhận các mặc định đã triển khai theo quyền tự thực hiện người dùng giao: bắt đầu từ nhập kho, số lưu trữ unique toàn ứng dụng/tự cấp theo tháng, cây Kho–Kệ–Ngăn, nháp và snapshot, phạm vi metadata theo role, tìm tiếng Việt và kiểm soát cạnh tranh. Đây là IMPLEMENTED_DEFAULT để người dùng đọc/điều chỉnh, không biến các quyết định tổng thể P0/P1 ngoài phạm vi thành đã được duyệt.

Với bản code năm chức năng, D-01/D-04/D-07/D-12/D-15/D-17/D-23 áp dụng phần lựa chọn liên quan trong CR-005; nội dung luồng đọc/giao–trả chưa triển khai vẫn chờ giai đoạn sau. D-19/D-24/D-25/D-26/D-27 giữ trạng thái xác nhận như trên.

Phiếu phản hồi: `D-xx | Đồng ý/Sửa | Nội dung thay thế | Lý do | Người xác nhận | Ngày`. Khi duyệt ghi trạng thái APPROVED và cập nhật tài liệu bị ảnh hưởng.

## Mốc FR mở rộng ngày

Theo [CR-006](../changes/CR-006_full-functional-requirements.md), phạm vi mở rộng từ năm chức năng nền tảng sang các FR SRS. Người dùng xác nhận hoãn email và tiếp tục chỉ Director duyệt, không ủy quyền; giữ bốn role/một role mỗi tài khoản. Mật khẩu cấp 2 cho quyết định đọc được triển khai theo FR-10; điều này không buộc đổi password lần đầu hay mở lại quên mật khẩu tự phục vụ ở D-26.

Các giới hạn về số ngày, xử lý revision, nhận trả/ngoại lệ, kiểm kê và CSV/in là mặc định triển khai đã ghi trong CR-006, để người dùng rà soát. Không tự xem các NFR còn TBD hoặc văn bản pháp lý là đã được xác nhận.
