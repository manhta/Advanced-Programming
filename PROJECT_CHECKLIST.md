# Medical Record Management — Lộ trình xây dựng chương trình

## Hồ sơ cá nhân và thông báo nhân sự — V25,

- [x] Trang thông tin cá nhân chỉ giữ nút chỉnh sửa; menu bỏ đề nghị riêng, trang sửa bỏ email và thêm hai liên kết Vai trò/Khoa vào form đề nghị đã có.
- [x] Giám đốc chỉ thấy lý do khi chọn từ chối; backend bắt buộc lý do từ chối và bỏ note chèn vào phê duyệt.
- [x] Thông báo thành công của nhân viên chỉ gửi sau Admin áp dụng; từ chối vẫn gửi ngay với lý do.
- [x] V25 xử lý thông báo sớm cũ và bù kết quả APPLY, giữ công việc Admin và đơn/sự kiện.
- [x] Build, 849 kiểm tra Java và kiểm tra JavaScript đạt. [Báo cáo](docs/verification/STAFF_PROFILE_AND_NOTICE_TIMING.md).
- [x] Sao lưu và áp dụng V25 trên mrm/public, xác minh không còn kết quả phê duyệt sớm cho nhân viên và giữ đơn đang chờ Admin.
- [ ] Người dùng khởi động lại profile `dev`, thử đường dẫn từ thông tin cá nhân và kiểm tra chuông qua từng bước.

## Tự phục vụ nhân viên và duyệt thay đổi nhân sự — V24,

- [x] Admin tự sinh username/mật khẩu; bắt buộc khoa; báo trùng căn cước ngay trên form, kiểm tra cả trường hợp gửi đồng thời.
- [x] Liên kết chỉnh sửa nằm ở họ tên, tên tài khoản là cột riêng.
- [x] Nhân viên tự sửa thông tin cá nhân; không cho gửi kèm role/khoa để tự cấp quyền.
- [x] Form đề nghị đổi vai trò/khoa → Giám đốc duyệt bằng mật khẩu cấp 2 → Admin bấm áp dụng.
- [x] Lý do từ chối bắt buộc; thông báo nội bộ theo đúng vai trò, kết quả cho nhân viên chỉ phê duyệt/từ chối và lý do từ chối.
- [x] Chặn tự duyệt, đơn đang chờ trùng, áp dụng lặp, ghi đè dữ liệu mới; bảo vệ Giám đốc cuối cùng và thu hồi phiên cũ sau đổi quyền.
- [x] Build và 86 kiểm tra mới, 726 hồi quy, 99 mật khẩu lần đầu, 16 nâng cấp đều đạt. [Báo cáo](docs/verification/STAFF_SELF_SERVICE_AND_APPROVAL.md).
- [x] Sao lưu và áp dụng V24 vào mrm/public; kiểm kê xác nhận các bảng dữ liệu cũ giữ nguyên số dòng, bảng đề nghị mới trống.
- [ ] Người dùng khởi động lại bằng profile `dev`, tạo tài khoản và kiểm thử quy trình trên web.

## Thiết lập tài khoản lần đầu — V23,

- [x] Giám đốc Tạ Đức Mạnh,; username `manh.td24102003@mrm.com`, giữ nguyên UUID và liên kết dữ liệu.
- [x] Trang `/account/initial-setup` yêu cầu mật khẩu mới và xác nhận, không nhập mật khẩu cũ; áp dụng mọi vai trò của tài khoản mới.
- [x] Giám đốc bắt buộc tự tạo mật khẩu cấp 2 riêng; lưu hai mật khẩu nguyên tử, không có thay đổi dở dang khi xác nhận sai.
- [x] Chặn truy cập nghiệp vụ trước thiết lập; giữ CSRF, thu hồi phiên khác, chặn gửi lại form lần đầu sau hoàn tất.
- [x] Bật cờ cho ba nhân viên mới trong database hiện tại; giữ Admin đang sử dụng. Sao lưu và diễn tập trước cập nhật.
- [x] Build đạt; 99/99 kiểm tra thiết lập lần đầu, 726/726 hồi quy và 16/16 nâng cấp. [Báo cáo](docs/verification/INITIAL_CREDENTIAL_SETUP.md).
- [ ] Người dùng khởi động lại bằng profile `dev`, đăng nhập và tự đặt mật khẩu mới; Giám đốc tự tạo mật khẩu cấp 2.

## Thiết lập dữ liệu sử dụng — V22,

- [x] Sao lưu đầy đủ database trước reset, diễn tập rollback và kiểm tra sau commit.
- [x] Giữ Admin nguyên trạng; tạo một Bác sĩ, một Nhân viên lưu trữ, một Giám đốc có hồ sơ nhân viên; mật khẩu riêng trong `.secrets/operational-accounts.txt`.
- [x] Giữ 16 bệnh án, danh mục và cấu trúc kho; đổi mã DEMO theo bộ đếm chuẩn, không đổi UUID.
- [x] Xóa yêu cầu/lịch mượn, thông báo, kiểm kê và nhật ký thử; giữ một audit reset mới.
- [x] Chặn seed demo tái tạo tài khoản và chặn công cụ reset chạy lại xóa công việc mới.
- [x] 726/726 kiểm tra chức năng/HTTP đạt trên schema tạm. [Báo cáo](docs/verification/OPERATIONAL_BASELINE_RESET.md).
- [ ] Người dùng khởi động lại ứng dụng bằng profile `dev`, đăng nhập bằng tài khoản mới và bắt đầu dữ liệu nghiệp vụ mới.

## Thông báo chung và dọn việc đã xử lý — V21,

- [x] Bố cục metadata chung cho mọi vai trò và bệnh án/đơn mượn; giữ nội dung bổ sung/từ chối, ngày/phòng xác nhận.
- [x] Mặc định chỉ thông báo đang chờ; có tab Đã xử lý/Tất cả, chuông chỉ đếm việc đang chờ chưa đọc.
- [x] Xóa riêng/hàng loạt bằng deleted_at, kiểm tra chính chủ/CSRF và chặn xóa pending; giữ dữ liệu nghiệp vụ/audit.
- [x] Sau phê duyệt có việc xác nhận lịch cho Archivist; hỗ trợ luồng hai bước cũ, kiểm tra revision/version để bỏ việc lỗi thời.
- [x] Build đạt, 722/722 chức năng/HTTP, 16/16 nâng cấp; kiểm tra trình duyệt, schema tạm đã xóa. [Báo cáo](docs/verification/UNIFIED_NOTIFICATIONS_AND_CLEANUP.md).
- [x] Database public đã có V21; dữ liệu thông báo thử đã dọn khi thiết lập dữ liệu sử dụng V22.

## Thông báo và giao diện Director — [x] Thông báo công việc Archivist hiển thị người yêu cầu, hồ sơ, ngày giờ đăng ký; không hiển thị mục đích, áp dụng cả thông báo cũ.
- [x] Báo cáo Director bỏ đoạn diễn giải dài, tiêu đề cột tiếng Việt; sửa cách đọc từng ô để dữ liệu hiển thị đúng.
- [x] Bỏ kiểm kê khỏi menu, báo cáo và quyền truy cập URL/service của Director; giữ cho Archivist.
- [x] Trang duyệt Director bỏ khối lịch giao–trả và lịch sử xử lý, giữ form quyết định.
- [x] Build thành công, 688/688 chức năng/HTTP; kiểm tra trực quan và xóa schema tạm. [Báo cáo](docs/verification/DIRECTOR_REPORTS_AND_NOTICE_PRESENTATION.md).
- [ ] Người dùng khởi động lại ứng dụng để nhận thay đổi; không cần migration.

## Form xét duyệt Archivist — [x] Bỏ lịch sử, lịch giao–trả và hủy khỏi trang xét duyệt; chuyển giao–trả sang trang riêng.
- [x] Phê duyệt ghi chú tùy chọn, checkbox xác nhận và ngày/phòng nhận bắt buộc; lưu quyết định/lịch nguyên tử.
- [x] Bổ sung/từ chối bắt buộc lý do, không gửi các trường lịch đang ẩn.
- [x] Lượt cần Director chỉ trình kiểm tra, chưa xếp lịch; xếp lịch sau Director phê duyệt.
- [x] Build/JS đạt; 668/668 chức năng/HTTP, đã kiểm tra trình duyệt và xóa schema tạm. [Báo cáo](docs/verification/ARCHIVIST_REVIEW_FORM.md).
- [ ] Người dùng khởi động lại ứng dụng và nghiệm thu form mới; không cần migration.

## Menu cá nhân và danh mục Giám đốc — [x] Ba ô thống kê Director mở danh sách riêng, cùng giao diện và số liệu tương ứng.
- [x] Menu dưới tên nhân viên cho Reader/Archivist/Director: thông tin cá nhân, đổi mật khẩu, đăng xuất; Director thêm mật khẩu cấp 2 thay mục navigation/thẻ tác vụ cũ.
- [x] Trang cá nhân chỉ đọc chính chủ; đổi mật khẩu có kiểm tra mật khẩu cũ, xác nhận mới, băm/audit/CSRF và thu hồi phiên khác.
- [x] Reader đã nhận giấy chỉ thấy thông tin hồ sơ, ẩn ba nhắc mượn cuối trang theo đúng người đang giữ giấy.
- [x] Giữ phần căn cứ/lý do duyệt dạng text theo xác nhận người dùng, hoãn bộ chọn công văn.
- [x] Build/JS đạt, 643/643 chức năng/HTTP; kiểm tra trình duyệt, schema tạm đã xóa. [Báo cáo](docs/verification/PERSONAL_MENU_AND_DIRECTOR_DASHBOARD.md).
- [ ] Người dùng khởi động lại ứng dụng để dùng giao diện mới; không cần migration.

## Thông báo hủy và danh mục Archivist — V20,

- [x] Bỏ nút mở cho yêu cầu đã hủy, gửi Archivist thông báo có mã hồ sơ và ẩn công việc cũ khỏi danh sách/badge.
- [x] Bổ sung liên kết ba ô thống kê và bảng Chờ kiểm tra/Chờ giao, đồng bộ số đếm và phân quyền.
- [x] Kiểm tra dữ liệu thật: chưa có tử vong; chuyển ngẫu nhiên DEMO-MRM-006/008/009, thêm ghi chú, đồng bộ chính sách 30 năm cho bản giấy 009 và lưu bản sao trước sửa.
- [x] Màu riêng bệnh án tử trong bộ chọn, nhãn ghi chú tra cứu; giữ quy tắc lý do và Director duyệt cả lượt.
- [x] Build/JS đạt; 591/591 chức năng/HTTP, 14/14 nâng cấp; kiểm tra trình duyệt, schema tạm đã xóa. [Báo cáo](docs/verification/ARCHIVIST_NOTICES_AND_DECEASED_RECORDS.md).
- [ ] Người dùng khởi động lại ứng dụng đang chạy để nhận giao diện và xử lý mới; V20/dữ liệu mẫu đã áp dụng vào mrm/public.

## Phân tuyến duyệt mượn — V19,

- [x] Lượt thường do Archivist duyệt/từ chối; lượt có tử vong trình Director cho mọi yêu cầu con.
- [x] Bắt buộc lý do khi trên 5 hồ sơ hoặc có tử vong, kiểm tra cả server và bộ chọn.
- [x] Lưu tuyến batch/yêu cầu bất biến; giữ lịch sử cũ, chặn bỏ qua bước duyệt.
- [x] Cập nhật tiến trình, thông báo, hướng dẫn ba role và giải thích Bản giấy nháp.
- [x] Build/JS đạt; 570/570 chức năng/HTTP và 12/12 nâng cấp; đã kiểm tra trên trình duyệt, schema tạm đã xóa.
- [ ] Khởi động lại ứng dụng để áp dụng V19 và nghiệm thu trên database đang dùng theo [báo cáo](docs/verification/BORROW_APPROVAL_ROUTING.md).

## Gộp hồ sơ giấy/điện tử — V17,

- [x] Một bảng medical_records, hai cờ hình thức; view tương thích chỉ đọc, không lưu trùng; giữ ID/mã/lịch sử/thông báo.
- [x] Gộp tra cứu và bộ lọc, bỏ menu điện tử riêng; mã mở metadata công bố trực tiếp, giữ quyền dữ liệu nội bộ.
- [x] Cột mượn riêng cho bản giấy đủ điều kiện; backend chặn chỉ điện tử và giữ quy tắc giữ chỗ/phê duyệt/giao–trả.
- [x] Bổ sung giấy trên cùng bản ghi đã có; giữ mã và lịch sử, không tăng số bệnh án; ngăn dùng mã tự động để tạo phiếu giấy thứ hai.
- [x] Mã chung duy nhất/bất biến; bộ cấp mã bỏ qua mã giấy lịch sử; migration rollback nếu xung đột.
- [x] Build thành công, FR 483/483, migration 10/10; đã kiểm tra trình duyệt bằng dữ liệu giả, các schema thử nghiệm đã xóa.
- [x] Cập nhật user-guides/mô hình dữ liệu và [báo cáo](docs/verification/UNIFIED_RECORDS_COMPLETION.md).
- [ ] Khởi động lại để áp dụng V17 vào database cấu hình và nghiệm thu trên máy người dùng.


## Form điện tử chi tiết và giao diện chung — [x] Reader khai báo khoa, loại, ngày vào/ra, tử vong, nguồn và ghi chú tùy chọn; bỏ các mục lưu trữ giấy, giữ mã tự động.
- [x] Lưu/sửa/xem đầy đủ các trường mới, bảo toàn snapshot, Java migration V16 giữ dữ liệu cũ.
- [x] Header, banner, thẻ tác vụ và dashboard theo mẫu cho cả bốn role; số liệu thật theo quyền; menu, chuông và đăng xuất thật.
- [x] Hướng dẫn bốn role và vị trí file chỉnh giao diện; build/JS đạt, FR 455/455 và upgrade 7/7 đạt; đã xác minh trực quan trên trình duyệt với dữ liệu giả.
- [ ] Khởi động lại để áp dụng V16 và nghiệm thu theo [báo cáo](docs/verification/PORTAL_AND_EXTENDED_RECORD_FORM.md).


## CR-012 — Reader khai báo hồ sơ điện tử,

- [x] Reader chỉ nhập thông tin bệnh nhân, sinh mã MRM-REC-YYYY-000001 ở backend, chống trùng/gửi lặp.
- [x] Archivist kiểm tra/trả bổ sung/trình; vẫn giữ quyền nhập hồ sơ giấy.
- [x] Director duyệt kèm mật khẩu cấp 2, công bố ngay trong transaction; không nhận giấy hoặc nhập kho cho nhánh điện tử.
- [x] Danh sách/tìm kiếm/chi tiết, sửa/gửi lại/hủy theo trạng thái, phân quyền chủ khai báo, thông báo chuông, audit và snapshot.
- [x] Java migration V15, kiểm thử khởi tạo và nâng cấp có dữ liệu V14; không đổi mã hồ sơ giấy cũ.
- [x] Hướng dẫn ba vai trò và báo cáo; build thành công, FR 422/422 và upgrade 6/6 đạt trên schema riêng tự xóa.
- [ ] Khởi động lại ứng dụng để Flyway áp dụng V15 vào database đang dùng, nghiệm thu trực quan theo [hướng dẫn kiểm thử](docs/verification/ELECTRONIC_RECORD_INTAKE.md).


## Thông báo đăng nhập nhanh — [x] Toast JavaScript tự đóng sau 5 giây, có nút đóng, dùng chung cả bốn vai trò.
- [x] Chỉ hiện sau đăng nhập thành công; không lặp khi tải lại/quay về trang chủ.
- [x] Cập nhật hướng dẫn bốn vai trò và vị trí file chỉnh giao diện/thời gian.
- [x] Build/cú pháp JS đạt; FR 375/375, schema thử nghiệm tự xóa. Xem [báo cáo](docs/verification/LOGIN_SUCCESS_TOAST.md).


## Reader — Hủy tại trang tiến trình,

- [x] Thêm nút Hủy yêu cầu phía trên các node trong reading/history.html cho Mới gửi/Cần bổ sung/Chờ duyệt; dùng cùng backend kiểm tra sở hữu, CSRF và phiên bản.
- [x] Cập nhật user-guides/reader.md với đường dẫn HTML, CSS, cách dùng và điều kiện hủy mới.
- [x] Build thành công; FR 359/359, kiểm tra form trên cả ba trạng thái đang chờ, hủy bằng token của trang tiến trình, ẩn nút sau hủy/duyệt. Schema thử nghiệm đã tự xóa; không thay database ứng dụng.


## Reader — Rút gọn hiển thị hủy,

- [x] Trạng thái Đã hủy; để trống hai cột ngày khi hủy.
- [x] Thông báo hủy cũ/mới chỉ có nội dung Yêu cầu đã hủy; giữ lịch sử dữ liệu.
- [x] Build thành công; kiểm tra FR 352/352 trên schema thử nghiệm tự xóa.

## Reader — Tiến trình xử lý dạng node,

- [x] Bỏ tổng kết kết quả tìm kiếm và liên kết theo dõi cuối form đăng ký.
- [x] Mọi trạng thái trong danh sách đều có liên kết Theo dõi tiến trình.
- [x] Node dọc ghi bước/người/thời gian; phân biệt bước đã xử lý, đang chờ, sắp tới, hủy/từ chối.
- [x] Nút thông tin hồ sơ sau hoàn tất duyệt/xác nhận lịch và chỉ khi quyền xem còn hiệu lực.
- [x] Build thành công; FR 343/343 trên schema thử nghiệm tự xóa; không thay đổi database ứng dụng.
- [ ] Khởi động lại ứng dụng và nghiệm thu trực quan theo [báo cáo](docs/verification/READER_PROGRESS_TIMELINE.md).

## Reader — Sao kê yêu cầu và giao diện đăng ký,

- [x] Bỏ tên người bệnh dưới số lưu trữ; tìm tên/số lưu trữ kết hợp khoảng ngày đăng ký.
- [x] Mặc định 7 ngày lịch gần nhất; 10 dòng/trang, giữ bộ lọc khi chuyển trang.
- [x] Cột ngày đăng ký riêng, để trống khi chưa được duyệt; giữ dữ liệu thực tế.
- [x] Tra cứu có một trạng thái tiếng Việt, các ô ngày/select căn cùng hàng.
- [x] Hộp chọn hồ sơ cùng kiểu ô lý do; ẩn số tối đa; chỉ bắt buộc lý do khi >5 hồ sơ (backend và frontend).
- [x] Sao lưu và xóa đúng hai đăng ký LEGACY 007/012 của reader1; giữ bệnh án gốc.
- [x] Build và cú pháp JavaScript đạt; FR 333/333 trên schema thử nghiệm tự xóa.
- [ ] Khởi động lại, kiểm tra trực quan và nghiệm thu theo [báo cáo](docs/verification/READER_REQUEST_STATEMENT.md).

## Reader — Hủy và theo dõi yêu cầu,

- [x] Trạng thái tiếng Việt; bỏ chú giải mã cuối bảng.
- [x] Nút hủy dưới trạng thái và liên kết theo dõi chỉ hiện cho yêu cầu chưa có quyết định của Giám đốc.
- [x] Trang theo dõi Reader chỉ xem lịch sử, không còn form sửa/hủy.
- [x] Kiểm tra sở hữu, CSRF, phiên bản và trạng thái thật khi hủy; giữ luồng xử lý của nhân viên.
- [x] Build thành công; chạy riêng bộ FR đạt 316/316 trên schema thử nghiệm tự xóa.
- [ ] Khởi động lại ứng dụng và nghiệm thu theo [hướng dẫn kiểm thử](docs/verification/READER_REQUEST_HISTORY.md).

## Reader — Menu, mở rộng và hồ sơ còn trống,

- [x] Menu Trang chủ → Tra cứu hồ sơ → Đăng ký mượn hồ sơ.
- [x] Dấu mở rộng thông tin nhận diện từng hồ sơ đã chọn, giữ quyền Director duyệt.
- [x] Lọc đăng ký đang hoạt động của mọi Reader và hồ sơ đang mượn; chặn đồng thời tại backend, mở lại khi hết giữ chỗ và đủ điều kiện.
- [x] Build/JS hợp lệ; FR 289/289 đạt, đã xác nhận giao diện trên trình duyệt với dữ liệu giả; schema giữ V14.
- [ ] Người dùng khởi động lại và nghiệm thu theo [báo cáo](docs/verification/BORROW_PICKER_AVAILABILITY.md).

## CR-011 — Đăng ký nhiều hồ sơ, không chọn ngày — [x] Reader có mục Đăng ký mượn hồ sơ, bộ chọn giống đính kèm: tìm kiếm, phân trang, chọn nhiều/bỏ chọn; không gửi ngày nhận.
- [x] Batch 1–20 hồ sơ, từng hồ sơ duyệt riêng; toàn bộ hợp lệ mới lưu, token chống gửi trùng.
- [x] Archivist chỉ định một ngày nhận sau Director duyệt; ngày không quá ngày đăng ký +2 ngày lịch, có cuối tuần; không cho Reader chèn ngày, không gia hạn khi sửa mục đích.
- [x] V14 áp dụng mrm/public, giữ 2 yêu cầu/4 lịch cũ LEGACY và 15 hồ sơ; metadata/phân quyền/giao–trả được đồng bộ.
- [x] Build/JS hợp lệ; 444/444 kiểm tra đạt; đã xem trực tiếp bộ chọn trên trình duyệt với dữ liệu giả, lưu ảnh và cập nhật hướng dẫn.
- [ ] Người dùng khởi động lại, thực hành Reader đăng ký → Archivist trình → Director duyệt → Archivist xếp ngày nhận. Xem [báo cáo](docs/verification/BORROWING_REGISTRATION.md).

## Reader — Chọn ngày đọc,

- [x] Thay chuỗi ngày bằng input date và thêm/xóa tối đa 5 ô; đổi nhãn theo yêu cầu, xóa hướng dẫn cũ trên form.
- [x] Áp dụng đăng ký/sửa, nhận nhiều input dates, giữ kiểm tra ngày ở backend và không đổi schema V13.
- [x] Build và kiểm tra cú pháp JS thành công, FR 250/250 đạt trong schema tạm. Xem [báo cáo](docs/verification/READER_DATE_PICKER.md).
- [ ] Người dùng tải lại bản ứng dụng mới và xác nhận thao tác lịch/Thêm ngày/Xóa ngày trên trình duyệt.

## Tìm tài khoản bằng căn cước — [x] Thêm bộ lọc căn cước chính xác 10 chữ số, giữ số 0 đầu; dùng riêng hoặc kết hợp tên/role; để trống không loại tài khoản thiếu hồ sơ nhân viên.
- [x] Build thành công, FR 246/246 đạt; không đổi schema V13. Xem [báo cáo](docs/verification/ACCOUNT_PAGES_AND_SEARCH.md).

## Tách trang tài khoản và tìm kiếm — [x] Trang tạo riêng không kèm danh sách; trang quản lý chỉ bảng ba cột và bộ lọc; tên dẫn tới trang sửa riêng.
- [x] Tìm theo username/họ tên, role, kết hợp và để trống; xử lý không có kết quả, ký tự đặc biệt và ID không tồn tại.
- [x] Giữ CSRF/phân quyền/Admin cuối/version, hồ sơ nhân viên và mật khẩu băm; không đổi schema V13.
- [x] Build thành công, bộ FR 240/240 đạt trong schema tạm đã dọn.
- [ ] Người dùng khởi động lại và nghiệm thu theo [hướng dẫn](docs/verification/ACCOUNT_PAGES_AND_SEARCH.md).

## CR-010 — Tài khoản chung và hồ sơ nhân viên

- [x] Một form Tạo tài khoản; nhãn HTML READER = Bác sĩ, ARCHIVIST = Nhân viên lưu trữ.
- [x] Bảng employees liên kết 1:1 tài khoản, căn cước 10 số dạng chuỗi, tên/ngày sinh/giới tính/role; kiểm tra trùng và đồng bộ tên/role.
- [x] Tạo/cập nhật hồ sơ nhân viên trên trang Admin; giao dịch không để lại tài khoản tạo dở khi lỗi.
- [x] Build thành công, FR 227/227 và bootstrap 26/26 đạt; V13 đã áp dụng mrm/public.
- [ ] Người dùng khởi động lại, bổ sung thông tin nhân viên thật cho tài khoản cũ và nghiệm thu. Xem [hướng dẫn](docs/verification/EMPLOYEE_MANAGEMENT.md).


## Admin tạo Reader/Archivist — [x] Thêm nút tạo Reader và Archivist trên trang chủ Admin; form chọn sẵn đúng role theo nút.
- [x] Kiểm chứng tài khoản mới đăng nhập đúng quyền, chặn trùng username và chặn Archivist quản trị tài khoản.
- [x] Build thành công; bộ FR 215/215 đạt trong schema tạm; không thay schema V12 hoặc thêm tài khoản thật.
- [ ] Người dùng khởi động lại và thực hành tạo tài khoản theo [hướng dẫn Admin](docs/user-guides/admin.md).

## Làm rõ CR-009 — Username trong database

- [x] Chuyển requester_name_snapshot sang lưu username như reader1, thay vì Reader Demo; code tạo mới lấy username từ tài khoản xác thực.
- [ ] Người dùng refresh bảng reading_requests trong pgAdmin để kiểm tra giá trị username sau nâng cấp V12.

## CR-009 — Người yêu cầu đọc

- [x] Bổ sung requester_name_snapshot bên cạnh requester_id, lấy tự động từ tài khoản gửi; giữ tên khi tài khoản đổi tên.
- [x] Hiển thị họ tên/username trong yêu cầu và báo cáo; bảng liên quan truy qua request_id, không lặp dữ liệu người gửi.
- [x] V11 áp dụng mrm/public, điền tên cho 2 yêu cầu có sẵn; 15 hồ sơ còn nguyên; build và 232 kiểm tra đạt.
- [ ] Người dùng khởi động lại ứng dụng, kiểm tra cột mới trong pgAdmin và giao diện. Xem [CR-009](docs/changes/CR-009_requester-name-snapshot.md).

## CR-008 — Thông báo công việc và quyền xem chi tiết

- [x] Reader gửi/gửi lại báo chuông Archivist ACTIVE; Archivist trình báo Director ACTIVE.
- [x] Mở chuông đúng trang của role, trạng thái đọc riêng từng người nhận, ẩn thông báo khi đổi role.
- [x] Số lưu trữ mở hồ sơ; có cột Theo dõi yêu cầu riêng.
- [x] Reader chưa duyệt vào trang xin phép tối thiểu; backend kiểm tra sở hữu/trạng thái/thời hạn, sửa/hủy làm mất quyền của yêu cầu đó.
- [x] Migration V10 và tài liệu [CR-008](docs/changes/CR-008_workflow-alerts-and-reader-access.md).
- [x] V10 áp dụng mrm/public; build thành công, 370/370 kiểm tra đạt, giữ 15 hồ sơ mẫu và yêu cầu thực hành.
- [ ] Người dùng khởi động lại ứng dụng và nghiệm thu ba thay đổi theo [hướng dẫn](docs/verification/WORKFLOW_ALERTS_AND_READER_ACCESS.md).


## Sửa đăng nhập ngày

- [x] Giữ quyền tài khoản; xử lý token CSRF cũ tại POST /login bằng form mới và thông báo hết hiệu lực.
- [x] Build thành công; bộ FR 178/178 đạt trong schema tạm, gồm đăng nhập Archivist/Admin và giữ CSRF nghiệp vụ.
- [ ] Người dùng khởi động lại ứng dụng và xác nhận đăng nhập trên form mới. Xem [báo cáo](docs/verification/LOGIN_CSRF_RECOVERY.md).

## Dữ liệu thực hành ngày

- [x] Kiểm tra mrm/public trước nhập: 0 hồ sơ; người dùng xác nhận kích hoạt lại GENERAL đã duyệt.
- [x] Nhập kho 15 mẫu giả lập, phân bổ sáu loại 3/3/3/2/2/2; kho mẫu riêng, snapshot chính sách và lịch sử đầy đủ.
- [x] Xác nhận sau commit: 15 hồ sơ, 15 liên kết chính sách, 15 sự kiện nhập và 15 lịch sử vị trí; tìm theo tên giả lập trả đủ 15.
- [ ] Người dùng thực hành tra cứu và đăng ký đọc trên mẫu. Xem [dữ liệu mẫu](docs/verification/SAMPLE_MEDICAL_RECORDS.md).

## Mốc FR-11 ngày

Theo [CR-007](docs/changes/CR-007_internal-notifications.md); [báo cáo chi tiết](docs/verification/FR11_INTERNAL_NOTIFICATIONS.md).

- [x] Migration Java V9 đã áp dụng vào mrm/public.
- [x] Thông báo cùng transaction cho bổ sung, duyệt/từ chối, xác nhận lịch và hủy; chống lặp sự kiện.
- [x] Chuông chung, badge, hộp thư phân trang/lọc, đánh dấu một/tất cả và mở yêu cầu.
- [x] Quyền sở hữu Reader, CSRF, chống đọc/mở thông báo người khác và bảo vệ khi đổi role.
- [x] Kiểm tra trình duyệt với dữ liệu giả; 341 kiểm tra tổng thể đạt (49 + 26 + 96 + 170), gồm rollback và cập nhật đồng thời.
- [x] Cập nhật hướng dẫn bốn role; email vẫn hoãn, tải trang để cập nhật chuông.
- [ ] Người dùng nghiệm thu trên web theo kịch bản báo cáo.

## Mốc mở rộng FR ngày

Phạm vi mới theo [CR-006](docs/changes/CR-006_full-functional-requirements.md): hoàn thiện FR ngoài năm chức năng nền tảng. Quyết định hiện tại: bốn role, chỉ Director duyệt, hoãn email và ủy quyền. Các mục "chưa triển khai" của mốc năm chức năng phía dưới là lịch sử trước mốc này; tiến độ mới xem bảng FR trong [báo cáo FR](docs/verification/FR_COMPLETION_REPORT.md).

- [x] FR-01/02: login/logout audit; Admin tạo/sửa/khóa/vô hiệu hóa/đổi một role/reset password, version và thu hồi phiên cũ.
- [x] FR-03/04/05/07/14/15/17: giữ các chức năng metadata/kho/phân quyền/đối soát đã kiểm thử, khởi tạo bằng nhập tay, không HIS/bản quét/tiêu hủy.
- [x] FR-08/09: Reader yêu cầu một hồ sơ/nhiều; Archivist kiểm tra/trả bổ sung/trình; sửa tăng revision và vô hiệu quyết định/lịch trước khi giao.
- [x] FR-10: Director thiết lập secret cấp 2 riêng, duyệt/từ chối tại đúng thao tác/version, cấm tự duyệt, chặn sai nhiều lần. Ủy quyền không thuộc phạm vi được chọn.
- [x] Phần kết quả/lịch FR-11: hiển thị trong app. Email được người dùng xác nhận hoãn, không đánh dấu gửi thư là đạt.
- [x] FR-06/12: session theo ngày, lịch không trùng, giao đúng ngày, trả kèm chất lượng/ngăn, cập nhật hồ sơ/audit cùng transaction.
- [x] Ngoại lệ chưa trả/trả muộn/không đến; không tự trả khi hết; hủy giữ lượt đã trả và chặn khi đang giữ giấy.
- [x] FR-13: audit xác thực/tài khoản/request/revision/quyết định/lịch/giao–trả/kiểm kê/xuất/in; truy vấn theo người/ngày/hành động/đối tượng; giữ audit cũ, không có API xóa/sửa log.
- [x] FR-16/20: tám nhóm báo cáo; CSV UTF-8 theo quyền/bộ lọc và chống công thức; chế độ in/lưu PDF bằng trình duyệt có log yêu cầu in.
- [x] FR-19: mở đợt kiểm kê theo vị trí, snapshot danh sách, ghi chênh lệch từng hồ sơ, chốt khi đã kiểm đủ; không tự thay metadata/vị trí.
- [x] Java migration V8 đã áp dụng vào mrm/public; giữ V1–V7, không đổi password các tài khoản có sẵn.
- [x] Bộ kiểm chứng đạt 49 + 26 + 96 + 127 = **298 assertion**, gồm schema trống V1–V8, hơn 100 hồ sơ giả và HTTP bốn role. Schema thử được dọn.
- [x] Báo cáo FR, README và bốn hướng dẫn sử dụng đã cập nhật; Note giữ nguyên.
- [ ] Người dùng chạy chuỗi Reader → Archivist → Director → Archivist giao/nhận và nghiệm thu trực quan/nghiệp vụ.
- [ ] NFR vận hành: tải/p95, LAN/HTTPS, backup/restore, lưu trữ log ngoài DB và tuân thủ thực tế; chưa kết luận đạt, không tự coi là yêu cầu bắt buộc viết thêm màn hình.

 Mục đích: quản lý công việc từ phân tích đến bàn giao website quản lý lưu trữ và khai thác hồ sơ bệnh án (HSBA).

Cập nhật tiến độ và ưu tiên: ****.

## Cách sử dụng

- Đánh dấu `[x]` khi có kết quả kiểm chứng được; giữ `[ ]` khi chưa hoàn thành.
- Ưu tiên nhóm năm chức năng ở phần dưới; các giai đoạn 0–15 là lộ trình tổng thể. Trong từng chức năng, chốt thiết kế liên quan trước khi triển khai và kiểm thử.
- Mỗi công việc có thể tách thành GitHub Issue; ghi người phụ trách, liên kết tài liệu và kết quả kiểm tra.
- Các phương án trong SRS dự thảo và hai báo cáo nhận xét chưa mặc nhiên là yêu cầu đã được duyệt.
- Tên file, folder, class và biến dùng tiếng Anh. Nội dung hướng dẫn và giao diện có thể dùng tiếng Việt.
- File này là kế hoạch; không khởi tạo chức năng hoặc thay đổi cơ sở dữ liệu.
- Theo yêu cầu ngày, Codex được chủ động bổ sung vào **PROJECT_CHECKLIST.md** các đầu việc triển khai quan trọng, phụ thuộc, ràng buộc dữ liệu, phân quyền, xử lý lỗi và kiểm thử phát sinh khi xây dựng phần mềm; không cần chờ yêu cầu cập nhật checklist riêng mỗi lần.
- Việc cập nhật kế hoạch không tự cho phép đổi phạm vi nghiệp vụ hoặc thực thi code/database ngoài công việc người dùng giao. Quyết định nghiệp vụ chưa được xác nhận phải ghi là đề xuất/chờ chốt.
- Chỉ đánh dấu hoàn thành với phạm vi và bằng chứng cụ thể; không suy ra toàn bộ use case hoàn thành từ một màn hình hay một lần build.
- Quy tắc thư mục `Note` vẫn giữ nguyên: chỉ cập nhật khi người dùng yêu cầu. Quyền chủ động bổ sung checklist không áp dụng cho Note.

## Ưu tiên hiện tại — hoàn thiện trước năm chức năng chính

Nhóm ưu tiên phục vụ quản lý hồ sơ trước khi triển khai luồng yêu cầu đọc, phê duyệt, xác nhận lịch và giao–trả. Số thứ tự dưới đây là thứ tự nhóm công việc, không thay đổi mã use case đã có.

| Ưu tiên | Mã use case | Tên đầy đủ theo danh mục use case | Phạm vi thực hiện trước | Trạng thái ngày |
|---|---|---|---|---|
| 1 | **MRM-UC-01** | **Đăng nhập, đăng xuất và phiên làm việc** | Đăng nhập/đăng xuất cơ bản, một role/tài khoản, điều hướng và bảo vệ URL theo role | Người dùng đã xác nhận hoàn thành phần đăng nhập cơ bản; không bao gồm quản lý phiên nâng cao/đổi/reset mật khẩu |
| 2 | **MRM-UC-03** | **Quản lý danh mục khoa, loại hồ sơ và chính sách lưu** | Danh mục khoa, loại bệnh án và chính sách bảo quản tối thiểu để nhập kho | Đã triển khai; người dùng xác nhận quy tắc mô phỏng học tập. Lưu snapshot vào hồ sơ thuộc UC-04; nghiệm thu trực quan cuối chờ người dùng |
| 3 | **MRM-UC-04** | **Nhập kho, cập nhật metadata và đối soát dữ liệu ban đầu** | Nhập/sửa thông tin hồ sơ, kiểm tra trùng và dữ liệu, chọn vị trí, ghi nhận nhập kho và nguồn dữ liệu | Đã triển khai và kiểm chứng theo CR-005; chờ người dùng nghiệm thu |
| 4 | **MRM-UC-05** | **Tra cứu và xem chi tiết HSBA** | Danh sách, lọc, phân trang và chi tiết theo quyền | Đã triển khai và kiểm chứng trong nhóm năm; chờ người dùng nghiệm thu |
| 5 | **MRM-UC-06** | **Quản lý vị trí và chuyển hồ sơ** | Vị trí lưu trữ tối thiểu phục vụ UC-04; sau đó quản lý vị trí, chuyển hồ sơ và lịch sử | Đã triển khai cây vị trí/chuyển/lịch sử; tích hợp giao–trả thuộc giai đoạn sau |

Tài liệu: [UC-01](docs/functions/mandatory/MRM-UC-01_authentication.md), [UC-03](docs/functions/mandatory/MRM-UC-03_reference-data.md), [UC-04](docs/functions/mandatory/MRM-UC-04_record-entry-and-initial-data.md), [UC-05](docs/functions/mandatory/MRM-UC-05_record-search.md), [UC-06](docs/functions/mandatory/MRM-UC-06_storage-and-location-history.md).

**Thứ tự làm thực tế:** UC-01 cơ bản đã xác nhận → thiết kế UC-04 → bổ sung UC-03 và vị trí tối thiểu của UC-06 → triển khai/kiểm thử UC-04 → UC-05 → hoàn thiện UC-06. Sau khi năm chức năng đạt phạm vi đã chốt mới chuyển trọng tâm sang luồng đọc hồ sơ. UC-02 quản trị tài khoản đầy đủ và UC-18 theo dõi thời hạn đầy đủ chưa nằm trong nhóm năm này; phần dữ liệu phụ thuộc cần thiết vẫn phải được chuẩn bị.

### P1. MRM-UC-01 — Đăng nhập, đăng xuất và phiên làm việc

- [x] Có form đăng nhập chung, xác thực tài khoản từ PostgreSQL và kiểm tra mật khẩu băm.
- [x] Mỗi tài khoản một trong bốn role READER/ARCHIVIST/DIRECTOR/ADMIN; điều hướng đến URL theo role.
- [x] Có cấu hình bảo vệ URL theo role, form logout POST và bảo vệ CSRF.
- [x] Người dùng đã kiểm tra và xác nhận hoàn thành chức năng đăng nhập cơ bản.
- [x] Hoàn thiện phần logo/nhận diện trang login theo xác nhận của người dùng.
- [x] Người dùng đã xác nhận sửa thành công lỗi render login bằng `spring.thymeleaf.servlet.produce-partial-output-while-processing=false`.
- [x] Đã kiểm tra đúng/sai mật khẩu, tài khoản khóa khi login mới, bốn role, URL trái quyền và logout/truy cập lại; xem báo cáo nhóm năm. Không bao gồm các ca phiên/đổi mật khẩu nâng cao đã hoãn.

**Hoãn theo D-26:** quản lý phiên nâng cao, bắt buộc đổi mật khẩu lần đầu, quên/reset mật khẩu. Không đưa các mục này trở lại điều kiện hoàn thành đăng nhập cơ bản nếu chưa đổi phạm vi.

### P2. MRM-UC-03 — Quản lý danh mục khoa, loại hồ sơ và chính sách lưu

- [x] Có bảng `departments`, module Java và giao diện khoa/phòng; người dùng xác nhận chạy thử thành công.
- [x] Tạo và chạy Java migration `V4__InsertMainDepartments`; build thành công và truy vấn xác nhận đủ năm khoa hoạt động, tên tiếng Việt chính xác.
- [x] Danh mục đã có: Nội tổng hợp - Y học cổ truyền - Phục hồi chức năng; Ngoại tổng hợp - Liên chuyên khoa; Nhi - Chăm sóc sức khỏe sinh sản; Khám bệnh - Cấp cứu - Hồi sức tích cực - Chống độc; Truyền nhiễm.
- [x] Người dùng xác nhận cố định năm khoa; bỏ form thêm, chặn POST và thêm trigger database chặn thêm/sửa/xóa. Giữ `created_by`/`created_at` để bảo toàn lịch sử.
- [x] Triển khai danh mục loại bệnh án, sáu loại mẫu, thêm/sửa tên/ngừng dùng; tách khỏi khoa, dấu hiệu tử vong và nhóm thời hạn bảo quản. Danh sách mẫu còn có thể rà soát theo phạm vi hồ sơ.
- [x] Triển khai chính sách lưu có mã/phiên bản, số năm, ngày hiệu lực và căn cứ; Archivist tạo DRAFT, Director xác nhận/ngừng dùng. Không tự sinh chính sách số năm đã duyệt.
- [x] Có Java migration V5 và nhật ký danh mục cùng transaction; không sửa nội dung phiên bản chính sách đã lưu, chặn phiên bản active cùng mã chồng khoảng hiệu lực.
- [x] Ngày đã áp dụng V5 trên `mrm`; build đạt, 45 kiểm tra tích hợp PostgreSQL/HTTP đạt (phân quyền, CSRF, validation, tạo/sửa/ngừng dùng, xác nhận, trùng/overlap, audit và rollback). Xem [kết quả kiểm chứng](docs/testing/CATALOG_VERIFICATION.md) và [thiết kế/hướng dẫn](docs/design/record-catalogs.md). Bộ kiểm tra chạy tường minh, không tự chạy khi Maven package.
- [x] Người dùng xác nhận mô phỏng 10/15/20/30 năm theo tài liệu Khánh Sơn; tính từ ngày đầu lưu kho, 29/2 về 28/2 ở năm đích, nhiều nhóm lấy dài nhất. Không xác nhận pháp luật hiện hành. V6 tạo bốn chính sách DRAFT, hiệu lực dự kiến; Director xác nhận trong ứng dụng trước khi dùng.
- [x] `CatalogSelectionService` chặn ID không tồn tại/ngừng dùng, chính sách chưa APPROVED hoặc ngoài hiệu lực; không có API xóa cứng. Đã kiểm tra bộ chọn và bản chụp bất biến.
- [x] Cung cấp `CatalogSnapshot` gồm ID/mã/tên/phiên bản/số năm/căn cứ, ngày gốc và hạn dự kiến; không tự cập nhật snapshot khi danh mục đổi. **P3 hiện đã nhận bàn giao: lưu snapshot/FK cùng hồ sơ bằng V7 và kiểm thử hồ sơ cũ giữ bản chụp.**
- [x] V6 thêm optimistic version cho loại bệnh án, kiểm tra biểu mẫu cũ và ghi audit cùng transaction; đã thử hai luồng sửa đồng thời.
- [x] Đã kiểm thử giao diện qua HTTP, validation/CSRF và bốn role; người dùng xác nhận quy tắc học tập. **Nghiệm thu trực quan bởi người dùng chưa thực hiện**, theo dõi tại giai đoạn 13 và báo cáo P2, không tự coi đã nghiệm thu.
- [x] `DevelopmentMigrationConfig` xử lý bootstrap profile `dev,seed-users`: V1–V3 → tạo Archivist nếu thiếu bằng password được cấp → V4–V6 → các tài khoản còn lại. Khởi động thật trên schema trống tách biệt thành công; không sửa V1–V5 hoặc đổi password tài khoản có sẵn.

**Kết quả triển khai P2:** xem [báo cáo hoàn thiện](docs/verification/P2_COMPLETION_REPORT.md). Quy tắc học tập đã được người dùng xác nhận; thao tác Director xác nhận chính sách trong ứng dụng và nghiệm thu cuối không được tự ghi nhận là đã thực hiện bởi người dùng.

- [x] Lượt kiểm chứng cuối cùng: build thành công, schema `mrm` version 6; **49 kiểm tra danh mục/HTTP + 26 kiểm tra P2 mở rộng đạt**. Đã dọn dữ liệu/schema thử và dừng phiên ứng dụng kiểm tra.

### P3. MRM-UC-04 — Nhập kho, cập nhật metadata và đối soát dữ liệu ban đầu

Các lựa chọn tối giản dưới đây được triển khai theo phạm vi người dùng giao; xem [CR-005](docs/changes/CR-005_five-feature-implementation.md). Đây là mặc định triển khai cho bài tập để người dùng đọc lại, không ghi là từng quy tắc đã được cơ sở y tế phê duyệt.

- [x] Quản lý từ nhập kho; kiểm tra hồ sơ giấy trước nhập do Archivist thực hiện ngoài ứng dụng.
- [x] Form có số lưu trữ/số bệnh án/mã người bệnh, họ tên/năm sinh/giới tính, khoa/loại, ngày vào/ra/đầu lưu, tử vong, chính sách, ngăn, chất lượng, nguồn và ghi chú.
- [x] UUID nội bộ; số lưu trữ unique toàn ứng dụng, tự cấp YYYY-MM-NNNNNN theo tháng ngày đầu lưu hoặc giữ số nguồn hợp lệ. Mã người bệnh không unique, một người có nhiều đợt.
- [x] Cây Kho → Kệ → Ngăn; FK/unique/check, migration Java V7 và transaction. V7 đã áp dụng; không sửa migration đã chạy.
- [x] Repository → Service → Controller/Form → Thymeleaf, sử dụng JDBC hiện có.
- [x] Validation ngày/trường/danh mục/vị trí, nghi trùng đợt và unique số lưu trữ; không lấy ngày tạo thay ngày đầu lưu.
- [x] Chỉ Archivist nhập/sửa; ghi người, thời gian, lý do, trước/sau; hồ sơ, snapshot, audit, lịch sử vị trí cùng transaction.
- [x] Tích hợp CatalogSelectionService và lưu CatalogSnapshot/FK thật; hồ sơ cũ giữ snapshot khi danh mục ngừng dùng, chỉ chọn lại khi chủ động đổi phân loại.
- [x] Lưu nháp DRAFT khi thiếu dữ liệu, không bịa ngày, chưa công bố hoặc tính hạn; đối soát đủ mới STORE. Không chuyển STORED ngược DRAFT.
- [x] Không sửa số lưu trữ/ngày đầu lưu của hồ sơ STORED; chuyển vị trí qua chức năng riêng; version cũ không ghi đè.
- [x] Mã phiếu/fingerprint chống gửi lại tạo trùng; kiểm tra trùng và sức chứa trong khóa transaction chống hai phiếu đồng thời.
- [x] Kiểm tra dữ liệu hợp lệ, nháp, trùng, ngày đảo/tương lai, cùng người nhiều đợt, danh mục ngừng, snapshot, rollback, HTTP/CSRF/quyền và cạnh tranh. Xem báo cáo nhóm năm.

### P4. MRM-UC-05 — Tra cứu và xem chi tiết HSBA

- [x] Reader/Director chỉ xem STORED và metadata cơ bản; không nhận vị trí/ghi chú/nguồn nội bộ/policy snapshot/audit. Admin không có quyền bệnh án.
- [x] Danh sách/chi tiết theo role; kiểm tra backend khi gọi trực tiếp URL/UUID, nháp không được phép xem trả 404.
- [x] Tìm chính xác số lưu trữ/số bệnh án/mã người bệnh; tìm tên có/không dấu; lọc năm sinh, khoa, loại, khoảng ngày ra viện và trạng thái.
- [x] Hàm chuẩn hóa tiếng Việt, truy vấn tham số, escape wildcard; không ghép input thành SQL/ORDER BY.
- [x] Phân trang 1–100 dòng, sắp xếp whitelist và khóa phụ UUID ổn định; giữ bộ lọc khi chuyển trang; xử lý không kết quả/lỗi ngày/ID không có.
- [x] DTO và SELECT cơ bản riêng cho Reader/Director, không chỉ giấu cột HTML.
- [x] Kiểm tra người trùng tên/một người nhiều đợt, lọc kết hợp gồm năm sinh, phân trang, injection, đoán ID nháp và rò rỉ vị trí trong response.
- [ ] Đo hiệu năng theo ngưỡng phi chức năng sau khi người dùng xác nhận tải/dữ liệu; chưa kết luận đạt p95 của hệ thống thật.

### P5. MRM-UC-06 — Quản lý vị trí và chuyển hồ sơ

- [x] Cây ba cấp Kho → Kệ → Ngăn; nhiều hồ sơ/ngăn, sức chứa ngăn tùy chọn.
- [x] Danh sách/thêm/sửa theo quyền Archivist; mã unique trong cha; đúng cấp; mã/cha/loại bất biến, không chu trình; không API xóa cứng.
- [x] Không ngừng vị trí có hồ sơ hoặc con hoạt động; không giảm sức chứa dưới số đang chứa; audit vị trí.
- [x] Chuyển hồ sơ IN_STORAGE sang ngăn hoạt động còn chỗ; lý do bắt buộc; ghi cũ/mới, đường dẫn snapshot, người và thời gian.
- [x] Current location/lịch sử/audit cùng transaction; row lock, version và mã thao tác xử lý cạnh tranh/gửi lặp.
- [x] Chặn nháp, UNAVAILABLE và READING; READING đã được mô phỏng trong kiểm thử để xác nhận hàng rào.
- [x] Kiểm tra chuyển hợp lệ, sai cấp, đầy/ngừng, ngăn có hồ sơ, sửa vị trí version cũ và hai luồng chuyển/tranh ngăn.
- [ ] Kiểm thử điều kiện lượt giao chưa trả xuyên suốt với UC-12/13 khi luồng giao–trả được xây dựng; không coi kiểm tra trạng thái READING hiện tại là đã hoàn thiện giao–trả.

### Mốc hoàn thành nhóm năm chức năng

- [x] Archivist đăng nhập → nhập hồ sơ → tìm/xem → sửa → chuyển vị trí → đối chiếu lịch sử; đã thử qua HTTP và PostgreSQL.
- [x] Reader/Director tra cứu đúng metadata; không được vị trí/nháp hoặc thao tác Archivist; Admin không có quyền dữ liệu bệnh án.
- [x] Bộ dữ liệu giả có tên trùng/một người nhiều đợt; truy vấn và phân trang khớp, fixture đã dọn sau kiểm tra.
- [x] Build JAR thành công; script kiểm chứng đạt **49 + 26 + 96 = 171 assertion**, bootstrap V1–V7 trên schema trống đạt. Các test main chạy tường minh, không tự chạy bởi Maven package.
- [x] Có [báo cáo đầy đủ](docs/verification/FIVE_FEATURE_COMPLETION_REPORT.md): kiểm kê, nguyên lý, hướng dẫn sử dụng, database, kiểm thử và giới hạn. Log chạy tại target/verification; bản bằng chứng lưu tại docs/verification/evidence/five-features-/.
- [ ] Người dùng kiểm tra giao diện và xác nhận nghiệm thu nhóm ưu tiên. Không coi nhóm này là hoàn thành yêu cầu đọc, duyệt, lịch, giao–trả, báo cáo hoặc nghiệm thu toàn hệ thống.

## Danh mục chức năng còn lại — ngoài nhóm năm ưu tiên

Tên và mã được giữ theo danh mục tài liệu hiện có, không đánh lại số. Đây là phần đối chiếu cho lộ trình sau, không yêu cầu triển khai đồng thời với nhóm ưu tiên.

| Mã use case | Tên chức năng bắt buộc còn lại |
|---|---|
| MRM-UC-02 | Quản lý tài khoản, vai trò và cấp lại thông tin xác thực |
| MRM-UC-07 | Tạo và gửi yêu cầu đọc |
| MRM-UC-08 | NVLT kiểm tra, trả bổ sung và trình duyệt |
| MRM-UC-09 | Theo dõi, bổ sung, sửa và hủy yêu cầu/phiên |
| MRM-UC-10 | Phê duyệt/từ chối với xác thực cấp 2 |
| MRM-UC-11 | Xác nhận lịch và xem kết quả trong ứng dụng |
| MRM-UC-12 | Giao HSBA tại phòng đọc |
| MRM-UC-13 | Nhận trả, cất lại và ghi chênh lệch |
| MRM-UC-14 | Đối soát chưa trả, không đến và bất thường |
| MRM-UC-16 | Tra cứu lịch sử và nhật ký kiểm toán |
| MRM-UC-17 | Báo cáo vận hành tối thiểu |
| MRM-UC-18 | Theo dõi nhóm và thời hạn lưu |
| MRM-UC-19 | Sao lưu, khôi phục và đối soát |

Audit các thao tác quan trọng vẫn được tích hợp khi làm chức năng tương ứng; chưa có màn hình UC-16 không có nghĩa hoãn toàn bộ việc ghi lịch sử. Sao lưu/khôi phục là yêu cầu vận hành, không bắt buộc tạo màn hình riêng.

| Mã chức năng bổ sung/hoãn | Tên chức năng | Phân kỳ |
|---|---|---|
| MRM-OPT-01 | Email kết quả và lịch đọc | Bổ sung/có điều kiện; chờ chọn phạm vi |
| MRM-OPT-02 | Nhập metadata từ tệp có kiểm soát | Bổ sung, không đồng nhất với form nhập từng hồ sơ UC-04 |
| MRM-OPT-03 | Xuất Excel/PDF theo quyền | Bổ sung |
| MRM-OPT-04 | Kiểm kê kho theo đợt | Bổ sung |
| MRM-OPT-05 | Thống kê tổng hợp và phân tích xử lý | Bổ sung |
| MRM-OPT-06 | Tệp minh chứng hành chính giới hạn | Bổ sung, chờ chốt |
| MRM-UC-15 | Ủy quyền duyệt | Hoãn theo phạm vi bốn role; không thuộc phiên bản đầu |

Tài liệu đầy đủ tại [mandatory](docs/functions/mandatory/README.md) và [optional](docs/functions/optional/README.md). HIS, lưu toàn văn/bản quét bệnh án, chữ ký số, tiêu hủy và workflow cơ quan ngoài vẫn ngoài phạm vi hiện tại.

Luồng mục tiêu toàn hệ thống: **Tra cứu → đăng ký đọc → NVLT kiểm tra/trình → duyệt → xác nhận lịch → giao tại phòng đọc → nhận trả → tra cứu lịch sử/báo cáo.**

## 0. Kiểm kê nền tảng hiện có

- [x] Có dự án Maven với `pom.xml` và Maven Wrapper.
- [x] Có cấu hình Spring Boot 4.1.1 và Java 26 trong `pom.xml`.
- [x] Đã đổi tên artifact thành `medical-record-management`.
- [x] Xác nhận package gốc `mrm`; file `src/main/java/mrm/MedicalRecordApplication.java` khai báo class `MedicalRecordApplication` và `package mrm;`.
- [x] Đồng bộ README với `pom.xml`: `target/medical-record-management-0.0.1-SNAPSHOT.jar`; bổ sung quy ước package và file khởi động.
- [x] Chạy lại build và ứng dụng ngày: BUILD SUCCESS, HTTP 200; xem `docs/verification/BASELINE_VERIFICATION.md`.
- [x] Kiểm tra phiên bản PostgreSQL server: **16.5**, theo kết quả `SELECT version();` người dùng cung cấp; client `psql` là **18.6**.
- [x] Ghi nhận danh sách file được quản lý và tạo commit mốc nền tảng chạy được; xem `docs/verification/BASELINE_VERIFICATION.md` và `git log`.

**Hoàn thành khi:** chạy được khung ứng dụng bằng hướng dẫn và xác định rõ các chức năng nghiệp vụ chưa được xây dựng.

## Trạng thái tài liệu ngày

Đã soạn **bản dự thảo phân tích/thiết kế giai đoạn 1–5** tại [docs/README.md](docs/README.md), chia chức năng thành [mandatory](docs/functions/mandatory/README.md) và [optional](docs/functions/optional/README.md). Chưa viết mã ứng dụng, SQL migration hoặc cấu hình triển khai trong đợt này.

| Giai đoạn | Tài liệu đã chuẩn bị | Phần còn chờ |
|---|---|---|
| 1 | Phạm vi, nguồn, phân loại chức năng, trách nhiệm | Người dùng xác nhận scope và phân kỳ |
| 2 | Quy tắc và 23 quyết định đề xuất | Phê duyệt P0/P1, đối chiếu dữ liệu/quy trình thực tế |
| 3 | 18 UC bắt buộc, 6 UC bổ sung và 1 UC hoãn, luồng/trạng thái, AC và truy vết | Xác nhận nghiệp vụ, tải/RPO/RTO |
| 4 | Ma trận quyền, 20 màn hình, nguyên mẫu văn bản | Duyệt với từng vai trò, chỉnh theo phản hồi |
| 5 | ERD, dictionary, biên giao dịch và kiến trúc | Khóa nghiệp vụ/mô hình/quyền được duyệt trước migration |

Trong các giai đoạn 1–5 bên dưới, ô [x] về soạn thảo chỉ đánh dấu công việc **soạn tài liệu** đã thực hiện. Những dòng “chốt”, “xác nhận”, “rà soát với người nghiệp vụ” giữ [ ] đến khi có phản hồi. Phần ưu tiên đầu file phản ánh tiến độ triển khai mới; các điều kiện chuyển giai đoạn chưa mặc nhiên đạt, không đánh dấu toàn bộ giai đoạn triển khai 6–15 đã hoàn thành.

## 1. Chốt phạm vi phiên bản đầu

- [x] Đọc và đối chiếu SRS v0.2 với hai báo cáo nhận xét; lập danh sách điểm thống nhất/mâu thuẫn. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Xác định ai xác nhận nghiệp vụ: Giám đốc, KHNV, nhân viên lưu trữ (NVLT), CNTT. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Xác định người dùng: READER, ARCHIVIST, DIRECTOR, ADMIN; bỏ MANAGER/DELEGATE theo D-24. **(Đã được người dùng xác nhận về danh sách vai trò.)**
- [ ] Chốt có đưa nhân viên soát HSBA và bước tiếp nhận từ khoa vào phần mềm hay không.
- [ ] Chốt điểm bắt đầu quản lý: từ nhập kho hay từ tiếp nhận/soát hồ sơ.
- [x] Liệt kê chức năng bắt buộc của bản đầu và chức năng để sau. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Ghi rõ chỉ quản lý metadata, vị trí, trạng thái, lịch đọc và lịch sử HSBA giấy. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Ghi rõ ngoài phạm vi hiện tại: bản quét/toàn văn, HIS, chữ ký số, yêu cầu bên ngoài, workflow khai thác HSBA tử vong, tiêu hủy. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Phân biệt quản lý metadata HSBA tử vong với workflow xin khai thác nằm ngoài hệ thống. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [ ] Quyết định kiểm kê kho, tệp minh chứng, nhập Excel và email có thuộc bản đầu không.
- [ ] Chốt danh sách báo cáo cần dùng; không mặc định triển khai toàn bộ danh mục đề xuất.

**Đầu ra dự kiến:** `docs/requirements/scope.md`, `docs/requirements/decision-log.md`.

**Hoàn thành khi:** từng chức năng có phạm vi, mức ưu tiên, người dùng và người xác nhận; mọi điểm chưa chốt được đánh dấu rõ.

## 2. Chốt quy tắc nghiệp vụ và ngoại lệ

### 2.1. Hồ sơ và kho

- [ ] Chốt khóa nghiệp vụ duy nhất; phân biệt ID kỹ thuật, số bệnh án, số lưu trữ và mã bệnh nhân.
- [ ] Xác định một bệnh nhân có nhiều đợt điều trị/nhiều hồ sơ như thế nào.
- [ ] Chốt trường metadata bắt buộc, nguồn dữ liệu và quyền sửa.
- [ ] Chốt cấu trúc kho/tủ/kệ/ngăn và sức chứa nếu cần; không mặc định mỗi ngăn chỉ chứa một hồ sơ.
- [ ] Chốt điều kiện nhập kho, chuyển vị trí, tạm không khả dụng và đối soát.
- [ ] Chốt ngày đầu lưu kho, nhóm thời hạn và xử lý nhiều nhóm cùng áp dụng.
- [ ] Đơn vị xác nhận căn cứ áp dụng thời hạn lưu; không tự coi mốc hết hạn là quyền tiêu hủy.
- [ ] Chốt cách phản ánh biến động vật lý do luồng xử lý ngoài phần mềm.

### 2.2. Yêu cầu, phê duyệt và lịch đọc

- [ ] Chọn một mô hình: một yêu cầu/một HSBA/nhiều ngày hoặc một yêu cầu/nhiều HSBA/nhiều ngày.
- [ ] Chốt duyệt toàn yêu cầu hay từng chi tiết/; có duyệt một phần hay không.
- [ ] Chốt bước NVLT kiểm tra, yêu cầu bổ sung và trình duyệt.
- [ ] Chốt quy tắc sửa, gửi lại và hủy ở từng trạng thái.
- [ ] Chốt phiên bản yêu cầu được duyệt; thay đổi quan trọng phải xử lý lại quyết định như thế nào.
- [ ] Chốt lịch hoạt động, ngày nghỉ, thời gian đăng ký trước và giới hạn số ngày.
- [ ] Phân biệt ngày mong muốn với ngày NVLT xác nhận; chốt có được đổi ngày và khi nào cần duyệt lại.
- [ ] Chốt đặt trùng lịch; phân biệt giới hạn đặt lịch trong ngày với chống giao đồng thời.
- [ ] Chốt có cho tự duyệt, đăng ký thay, giao cho người nhận thay hay không.
- [ ] Chốt chặn đăng ký HSBA tử vong trong workflow nội bộ và thông báo phù hợp.
- [ ] Chốt không đến, hủy phiên, hồ sơ không tìm thấy và thiếu/hư hỏng trước khi giao.

### 2.3. Giao–trả, quyền và thông báo

- [ ] Chốt điều kiện giao: quyền NVLT, quyết định hợp lệ, lịch đúng ngày, HSBA khả dụng.
- [ ] Chốt thông tin và tình trạng cần ghi khi giao, nhận trả, cất lại kho.
- [ ] Chốt giờ kết thúc ngày, cách phát hiện chưa trả, người xử lý và quy trình trả muộn.
- [ ] Giữ hồ sơ đang được giữ cho đến khi thực nhận trả; ghi ngoại lệ không thay thế nhận trả.
- [ ] Chốt xác thực cấp 2: loại thông tin, thời hạn, số lần thử, khóa/cấp lại; không mặc định là MFA.
- [x] Xác nhận hoãn ủy quyền khỏi phiên bản đầu; chỉ DIRECTOR duyệt theo D-24 ().
- [ ] Chốt phạm vi tra cứu và trường được xem theo từng vai trò, kể cả báo cáo/xuất tệp.
- [ ] Chốt SMTP nếu dùng email, người nhận, sự kiện, nội dung, thử lại và mốc đo 5 phút.
- [ ] Chốt sự kiện kiểm toán, quyền xem, bảo toàn và chính sách lưu 10 năm theo tài liệu nghiệp vụ.

**Đầu ra dự kiến:** `docs/requirements/business-rules.md`, sổ quyết định có người/ngày xác nhận.

**Hoàn thành khi:** các quyết định ảnh hưởng cấu trúc dữ liệu, quyền và luồng chính đã chốt trước khi lập trình phần tương ứng.

## 3. Đặc tả quy trình, trạng thái và nghiệm thu

- [x] Vẽ luồng xuyên suốt, chỉ rõ người thao tác tại từng bước. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Tách trạng thái yêu cầu, phiên đọc và HSBA vật lý; thông báo có trạng thái riêng. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Với mỗi chuyển trạng thái, ghi tác nhân, điều kiện, dữ liệu bắt buộc, kết quả và nhật ký. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Mô tả nhánh bổ sung, từ chối, hủy, không đến, không khả dụng và chưa trả cuối ngày. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Chuẩn hóa danh mục mã use case; giải quyết khác biệt UC-07 giữa các tài liệu. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Viết use case cho tài khoản/quyền, HSBA/kho, yêu cầu, duyệt DIRECTOR, lịch, giao–trả, báo cáo, nhập dữ liệu. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Viết tiêu chí chấp nhận có đầu vào, thao tác và kết quả quan sát được cho từng chức năng. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Lập ma trận yêu cầu → quy tắc → use case → màn hình/chức năng → ca kiểm thử. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [ ] Chốt tải đồng thời, quy mô dữ liệu, ngưỡng đáp ứng và cấu hình đo.
- [ ] Chốt RPO (mất dữ liệu tối đa), RTO (thời gian khôi phục), thời gian phục vụ và cách làm khi gián đoạn.

**Tài liệu dự thảo:** `docs/functions/mandatory/`, `docs/functions/optional/`, `docs/requirements/acceptance-criteria.md`, `docs/testing/traceability.md`.

**Hoàn thành khi:** có thể viết ca kiểm thử mà không phải tự đoán quy tắc. Khoảng 100 HSBA chỉ là quy mô thử, không phải tải đồng thời.

## 4. Thiết kế quyền và giao diện

- [x] Lập ma trận hành động × vai trò × phạm vi bản ghi × trường dữ liệu. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Tách quản trị kỹ thuật khỏi quyền đọc dữ liệu nhạy cảm và quyền duyệt. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Thiết kế đăng nhập, đăng xuất, đổi mật khẩu, khóa/đặt lại tài khoản và hết phiên. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Phác thảo menu và trang tổng quan cho từng vai trò. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Phác thảo danh sách/chi tiết/thêm/sửa HSBA, bộ lọc và phân trang. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Phác thảo vị trí kho, chuyển vị trí và lịch sử. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Phác thảo đăng ký đọc, yêu cầu của tôi, bổ sung/hủy. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Phác thảo hàng đợi NVLT, hàng đợi duyệt, xác thực; màn hình ủy quyền được hoãn. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Phác thảo xác nhận lịch, giao–trả và đối soát cuối ngày. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Phác thảo báo cáo, nhật ký, nhập dữ liệu và quản trị danh mục. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Thiết kế thông báo lỗi, trạng thái rỗng, không có quyền, mất kết nối và dữ liệu đã bị người khác thay đổi. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [ ] Rà soát nguyên mẫu theo từng vai trò với người nghiệp vụ.

**Đầu ra dự kiến:** `docs/requirements/permission-matrix.md`, `docs/ui/screen-specification.md`.

**Hoàn thành khi:** người dùng xác nhận được thao tác cần làm, trường được thấy và phản hồi của hệ thống.

## 5. Thiết kế PostgreSQL và kiến trúc ứng dụng

- [ ] Vẽ ERD theo mô hình yêu cầu đã chốt; xác định quan hệ một/nhiều.
- [x] Thiết kế tài khoản, vai trò/quyền, khoa/phòng và vị trí kho. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Thiết kế HSBA, chính sách thời hạn và lịch sử vị trí. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Thiết kế yêu cầu/phiên bản, chi tiết nếu cần, quyết định của DIRECTOR. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Thiết kế phiên theo ngày, lượt giao–trả và ngoại lệ. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Thiết kế audit; lô nhập/lỗi và outbox thông báo nếu các chức năng này được chọn. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Viết từ điển dữ liệu: ý nghĩa, kiểu, độ dài, bắt buộc, mặc định và giá trị hợp lệ. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Xác định khóa chính/ngoại, unique, check constraint và chính sách xóa/vô hiệu hóa. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Thiết kế ràng buộc một HSBA tối đa một lượt giao chưa trả. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [ ] Chốt transaction/khóa hoặc phiên bản dữ liệu khi duyệt, xác nhận lịch, giao và trả.
- [ ] Chốt chống gửi lặp: cùng thao tác gửi lại không sinh giao dịch trùng.
- [ ] Chọn chỉ mục theo tra cứu thực tế; quyết định tìm tiếng Việt/có dấu/không dấu nếu cần.
- [x] Phân biệt ngày nghiệp vụ với timestamp sự kiện; thống nhất múi giờ và thời gian máy chủ. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Chọn DTO/Form để giới hạn dữ liệu đầu vào/đầu ra theo quyền. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Giữ package `mrm` đơn giản ban đầu; chỉ tách package con khi số lớp/chức năng cần tổ chức thêm. **(Đã soạn bản dự thảo, chờ rà soát.)**
- [x] Quy định Controller nhận/trả web, Service xử lý nghiệp vụ/transaction, Repository truy cập dữ liệu. **(Đã soạn bản dự thảo, chờ rà soát.)**

**Đầu ra dự kiến:** `docs/database/erd.md`, `docs/database/data-dictionary.md`, `docs/architecture.md`.

**Hoàn thành khi:** mô hình đáp ứng cả luồng chính, ngoại lệ và truy vết; không chỉ đủ cho CRUD.

## 6. Chuẩn bị nền tảng triển khai chức năng

Thực hiện sau khi hoàn tất các quyết định thiết kế liên quan.

- [ ] Chốt phiên bản dependency trong Maven; dùng BOM Spring Boot cho thư viện được quản lý.
- [x] Có Spring MVC, Thymeleaf, Validation, JDBC/JdbcTemplate, PostgreSQL, Flyway và Security theo `pom.xml`; tiếp tục dùng JDBC, không cần bổ sung JPA chỉ để hoàn thiện checklist.
- [ ] Tạo database phát triển và tài khoản ứng dụng; giới hạn quyền phù hợp.
- [ ] Tách cấu hình phát triển/kiểm thử/vận hành, đưa bí mật ra ngoài Git.
- [ ] Tạo file mẫu biến môi trường; ghi cách nạp trong PowerShell/IDE (không giả định Boot tự đọc `.env`).
- [ ] Tạo migration nền tảng và chạy thử từ database trống; không sửa migration đã áp dụng ở môi trường dùng chung.
- [ ] Tạo dữ liệu giả và dữ liệu danh mục tối thiểu; không commit dữ liệu bệnh nhân thật.
- [ ] Thiết lập layout Thymeleaf, Bootstrap, menu và trang lỗi dùng chung.
- [ ] Thiết lập xử lý lỗi, validation, log kỹ thuật và cơ chế audit.
- [ ] Thiết lập kiểm thử có PostgreSQL cho các ràng buộc/giao dịch quan trọng.
- [ ] Thiết lập build kiểm tra tự động trên Git nếu nhóm dùng CI.

**Hoàn thành khi:** build được, kết nối CSDL được, migration tái tạo được môi trường và không có bí mật trong repository.

## 7. Xây dựng tài khoản và phân quyền

- [x] Triển khai đăng nhập/đăng xuất cơ bản bằng session; mật khẩu băm; người dùng xác nhận hoàn thành lát cắt đăng nhập. Xem P1 để phân biệt với kiểm thử chi tiết và phần nâng cao hoãn.
- [ ] Quản lý cấp/khóa tài khoản, đổi/đặt lại mật khẩu theo chính sách đã chốt.
- [x] Có bốn role cố định, một role/tài khoản, tài khoản mẫu và điều hướng cơ bản theo role.
- [ ] Hoàn thiện quản trị tài khoản/quyền và nhật ký thay đổi theo UC-02 khi tới phạm vi đó; chưa có màn hình quản trị đầy đủ.
- [ ] Kiểm soát truy cập tại máy chủ cho trang, thao tác và dữ liệu.
- [ ] Cấu hình CSRF, hết phiên và cookie phù hợp môi trường triển khai.
- [ ] Kiểm thử truy cập trực tiếp URL và sửa ID để xem dữ liệu không thuộc quyền.
- [ ] Xác nhận từng vai trò chỉ thấy menu, trường và thao tác hợp lệ.

**Hoàn thành khi:** không thể vượt quyền bằng cách bỏ qua giao diện; có luồng đăng nhập phục vụ các bước sau.

## 8. Xây dựng kho và metadata HSBA

- [ ] Quản lý khoa/phòng, loại hồ sơ và nhóm thời hạn đã được duyệt.
- [ ] Quản lý cấu trúc vị trí, trạng thái hoạt động và sức chứa nếu có.
- [ ] Thêm/sửa metadata với validation, phát hiện trùng và lịch sử sửa quan trọng.
- [ ] Ghi nhận nhập kho, ngày đầu lưu và vị trí theo phạm vi đã chốt.
- [ ] Tra cứu, lọc, phân trang và xem chi tiết bằng dữ liệu đúng quyền.
- [ ] Chuyển vị trí có lý do, vị trí cũ/mới, người và thời điểm.
- [ ] Theo dõi khả dụng/tình trạng; xử lý đối soát theo quy tắc.
- [ ] Hiển thị mốc thời hạn lưu mà không tạo chức năng tiêu hủy.
- [ ] Kiểm tra bác sĩ không nhận vị trí chi tiết trong phản hồi máy chủ hoặc xuất tệp.

**Hoàn thành khi:** NVLT quản lý và tìm đúng hồ sơ; người dùng khác chỉ xem dữ liệu được phép.

## 9. Xây dựng đăng ký, tổng hợp và phê duyệt

- [ ] Tạo/gửi yêu cầu theo mô hình đã chốt, kiểm tra HSBA, ngày và mục đích.
- [ ] Theo dõi yêu cầu của tôi; bổ sung/sửa/hủy đúng trạng thái.
- [ ] Chặn nhánh khai thác nằm ngoài workflow nội bộ.
- [ ] NVLT kiểm tra, trả bổ sung hoặc trình duyệt và lưu lịch sử.
- [ ] Người duyệt xem hàng đợi đúng phạm vi được giao.
- [ ] Xác thực cấp 2, xử lý sai/hết hạn/khóa theo chính sách.
- [ ] Duyệt/từ chối, bắt buộc lý do theo quy tắc; lưu quyết định gắn phiên bản nội dung.
- [ ] Kiểm tra vai trò DIRECTOR và trạng thái tài khoản tại lúc lưu quyết định; không có duyệt thay trong bản đầu.
- [ ] Xử lý hai người duyệt đồng thời, sửa sau duyệt và tự duyệt theo quyết định nghiệp vụ.
- [ ] Nhật ký có người, thời gian và kết quả; không ghi mật khẩu/token.

**Hoàn thành khi:** một yêu cầu đi từ đăng ký đến quyết định đúng quyền, đúng phiên bản, truy vết được.

## 10. Xây dựng xác nhận lịch và phiên đọc

- [ ] NVLT kiểm tra khả dụng và lịch đã được duyệt.
- [ ] Xác nhận lịch trong phạm vi quyết định; tạo phiên riêng theo ngày.
- [ ] Kiểm soát lịch trùng và giới hạn phòng đọc theo quy tắc đã chốt.
- [ ] Người đọc xem được ngày mong muốn, ngày xác nhận và kết quả.
- [ ] Hủy/đổi lịch/không đến đúng điều kiện, không ảnh hưởng sai quyết định cũ.
- [ ] Nếu có email: lưu outbox cùng nghiệp vụ, thử lại và theo dõi trạng thái gửi.
- [ ] Kiểm tra lỗi email không xóa quyết định hoặc lịch đã lưu.

**Hoàn thành khi:** danh sách phiên theo ngày chính xác, sẵn sàng cho giao–trả.

## 11. Xây dựng giao–trả và đối soát

- [ ] Hiển thị phiên được phép giao của ngày nghiệp vụ và vị trí cho NVLT.
- [ ] Xác nhận giao với người nhận, người giao, thời điểm và tình trạng.
- [ ] Kiểm tra điều kiện và cập nhật giao nhận/phiên/HSBA trong transaction.
- [ ] Bảo đảm hai NVLT cùng giao một HSBA chỉ một thao tác thành công.
- [ ] Bảo đảm nhấn lại/gửi lại sau lỗi mạng không sinh lượt giao trùng.
- [ ] Nhận trả với tình trạng, người nhận trả, thời điểm và vị trí cất lại.
- [ ] Ghi chênh lệch/hư hỏng/thiếu hồ sơ và luồng đối soát.
- [ ] Liệt kê chưa trả cuối; ghi nguyên nhân, người xử lý và kết quả.
- [ ] Cho nhận trả muộn theo quy tắc; không tự điền trả hoặc chuyển về trong kho khi chưa trả thực tế.
- [ ] Xử lý màn hình cũ, phiên bị hủy và trạng thái đã thay đổi bởi người khác.

**Hoàn thành khi:** chạy được toàn bộ luồng từ tạo HSBA đến nhận trả và kiểm tra lịch sử. Đây là mốc demo nghiệp vụ xuyên suốt đầu tiên.

## 12. Hoàn thiện báo cáo, audit và nhập dữ liệu

- [ ] Xây dựng báo cáo ưu tiên đã chốt: danh mục/vị trí, yêu cầu chờ xử lý, giao–trả và phiên chưa trả.
- [ ] Bổ sung lịch sử khai thác, lịch sử vị trí, audit và báo cáo khác theo phạm vi.
- [ ] Chốt ngày chốt, đơn vị đếm HSBA/yêu cầu/phiên và công thức thời gian xử lý.
- [ ] Phân quyền xem/xuất và ghi lịch sử xuất theo chính sách.
- [ ] Đối chiếu số báo cáo với bộ dữ liệu biết trước; không đếm yêu cầu ba ngày thành ba HSBA.
- [ ] Nếu có nhập tệp: cung cấp mẫu, kiểm tra trước nhập, lỗi từng dòng và phát hiện trùng.
- [ ] Chốt nhập lại/lỗi một phần/đối soát; không tạo lịch sử hoặc ngày giả cho dữ liệu thiếu.
- [ ] Lưu tổng số dòng, số thành công/lỗi, người nhập và người đối soát.
- [ ] Kiểm tra audit bảo toàn, truy vấn đúng quyền và có phương án lưu dài hạn.

**Hoàn thành khi:** kết quả báo cáo/nhập dữ liệu đối soát được, không làm sai trạng thái kho hoặc rò rỉ trường bị hạn chế.

## 13. Kiểm thử tổng thể và dùng thử

- [ ] Chạy kiểm thử nghiệp vụ trọng yếu trong từng giai đoạn, không chờ đến đây mới kiểm thử.
- [ ] Hoàn thiện ca kiểm thử trong ma trận truy vết và lưu kết quả.
- [ ] Kiểm tra từng vai trò, từng trạng thái và từng nhánh ngoại lệ.
- [ ] Kiểm tra tài khoản DIRECTOR bị khóa hoặc đổi vai trò ngay khi lưu quyết định.
- [ ] Kiểm tra đồng thời/gửi lặp cho giao, trả và phê duyệt.
- [ ] Kiểm tra rollback khi một bước ghi dữ liệu trong transaction thất bại.
- [ ] Kiểm tra dữ liệu đầu vào không hợp lệ, truy cập trái quyền và CSRF.
- [ ] Kiểm tra tìm kiếm/báo cáo với dữ liệu và tải đã thống nhất.
- [ ] Thử mất kết nối, thông báo lỗi, thao tác chưa xác nhận và cách đối soát.
- [ ] Dùng dữ liệu giả khi phát triển; chỉ đưa dữ liệu thật vào môi trường được phép theo quy trình.
- [ ] Cho NVLT, bác sĩ, người duyệt và quản trị chạy kịch bản dùng thử.
- [ ] Ghi lỗi, mức ảnh hưởng, người xử lý và kết quả kiểm tra lại.

**Hoàn thành khi:** các tiêu chí bắt buộc đạt, lỗi chặn vận hành đã xử lý, người dùng xác nhận kết quả thử.

## 14. Triển khai và nghiệm thu vận hành

- [ ] Chuẩn bị máy chủ nội bộ, mạng, DNS/địa chỉ truy cập và HTTPS.
- [ ] Cấu hình ứng dụng thành dịch vụ, tài khoản chạy và quyền CSDL phù hợp.
- [ ] Thiết lập bí mật/cấu hình vận hành ngoài Git và tài khoản quản trị ban đầu.
- [ ] Triển khai JAR và migration theo hướng dẫn có thể lặp lại.
- [ ] Thiết lập log, giám sát, cảnh báo và theo dõi dung lượng.
- [ ] Sao lưu tự động, kiểm tra lỗi sao lưu và lưu bản sao tách khỏi máy chủ chính.
- [ ] Thực hiện khôi phục sang môi trường tách biệt; đối chiếu dữ liệu/lịch sử và đo RPO/RTO.
- [ ] Chuẩn bị rollback ứng dụng và xử lý tương thích migration; không mặc định chạy JAR cũ sẽ đảo được schema.
- [ ] Chốt quy trình ghi nhận tạm/đối soát khi hệ thống ngừng hoạt động.
- [ ] Nhập khoảng 100 HSBA chạy thử sau khi dữ liệu được đối soát và người có trách nhiệm xác nhận.
- [ ] Đào tạo người dùng, bàn giao tài khoản/quy trình hỗ trợ và theo dõi giai đoạn thử.
- [ ] Nghiệm thu theo SRS đã thống nhất và lập danh sách việc còn lại.

**Hoàn thành khi:** hệ thống chạy ở môi trường mục tiêu, khôi phục được dữ liệu và có người chịu trách nhiệm vận hành.

## 15. Hồ sơ bàn giao và quản lý thay đổi

- [ ] Cập nhật README: chuẩn bị môi trường, cấu hình, chạy, build, kiểm tra và xử lý lỗi thường gặp.
- [ ] Bàn giao SRS, quyết định nghiệp vụ, use case và ma trận quyền phiên bản cuối.
- [ ] Bàn giao ERD, từ điển dữ liệu, migration và hướng dẫn nhập dữ liệu.
- [ ] Bàn giao báo cáo kiểm thử, nghiệm thu và các giới hạn đã biết.
- [ ] Bàn giao hướng dẫn theo vai trò, triển khai, sao lưu/khôi phục và xử lý sự cố.
- [ ] Đánh tag phiên bản phát hành và ghi release notes.
- [ ] Quản lý yêu cầu mới bằng issue và đánh giá tác động đến dữ liệu, quyền, giao diện, kiểm thử trước khi làm.

## Quy tắc hoàn thành một chức năng

- [ ] Đúng phạm vi và tiêu chí chấp nhận đã chốt.
- [ ] Có validation, kiểm tra quyền tại máy chủ và xử lý ngoại lệ phù hợp.
- [ ] Có transaction, chống đồng thời/gửi lặp nếu chức năng cần.
- [ ] Có migration khi thay đổi CSDL, audit khi thay đổi nghiệp vụ quan trọng.
- [ ] Có kiểm chứng phù hợp rủi ro; dữ liệu kết quả được đối chiếu.
- [ ] Cập nhật tài liệu liên quan, không commit bí mật hoặc dữ liệu bệnh nhân thật.
- [ ] Build thành công và người thực hiện lưu bằng chứng/kết quả review.

## Các mốc theo dõi

| Mốc | Giai đoạn | Bằng chứng hoàn thành |
|---|---|---|
| M1 — Sẵn sàng thiết kế | 0–3 | Phạm vi, quyết định, trạng thái, use case, tiêu chí |
| M2 — Sẵn sàng lập trình | 4–6 | Quyền, nguyên mẫu, ERD, môi trường/CSDL nền tảng |
| M3 — Quản lý hồ sơ | 7–8 | Đăng nhập, quyền, kho và tra cứu metadata |
| M4 — Luồng xuyên suốt | 9–11 | Đăng ký → duyệt → lịch → giao → trả |
| M5 — Sẵn sàng nghiệm thu | 12–13 | Báo cáo, nhập dữ liệu, kết quả kiểm thử/dùng thử |
| M6 — Bàn giao | 14–15 | Triển khai, khôi phục thử, đào tạo, tài liệu và nghiệm thu |

Không ấn định thời gian cố định trước khi chốt phạm vi và nguồn lực. Ước lượng từng issue sau M1; theo dõi trên bảng Backlog → Ready → In progress → Review → Done.

## Công việc tiếp theo

**Tiếp theo: chốt thiết kế MRM-UC-04 — Nhập kho, cập nhật metadata và đối soát dữ liệu ban đầu**, theo P3 ở đầu file. Chuẩn bị danh mục UC-03 và vị trí tối thiểu của UC-06 để triển khai UC-04; không chuyển mục tiêu sang hoàn thiện toàn bộ UC-06 trước.

Đã có đăng nhập cơ bản, kết nối PostgreSQL và danh mục năm khoa. Trước khi code UC-04 cần chốt các quyết định trực tiếp ảnh hưởng form, khóa duy nhất, ngày đầu lưu và vị trí; xem [sổ quyết định](docs/requirements/decision-log.md). Các giai đoạn 0–15 giữ làm kế hoạch dài hạn, không phải yêu cầu hoàn thành tất cả tài liệu mở rộng trước từng lát cắt nhỏ.

## Thay đổi xác nhận ngày

[CR-001](docs/changes/CR-001_four-role-scope.md): chỉ READER/ARCHIVIST/DIRECTOR/ADMIN; giám sát chuyển DIRECTOR; UC-15/SCR-18 và thiết kế ủy quyền hoãn. Không coi thay đổi này là xác nhận tất cả D-xx khác. D-25 mỗi tài khoản một vai trò đã được xác nhận ngày (CR-002).

## Thiết kế đăng nhập ngày — chưa triển khai

**Ghi chú lịch sử:** nội dung mục này phản ánh ngày 19/10; phần đăng nhập cơ bản đã được triển khai và người dùng xác nhận về sau, xem P1. Phạm vi mở rộng vẫn chưa hoàn thành.

- [x] D-25: người dùng xác nhận mỗi tài khoản đúng một vai trò; đồng bộ ERD/dictionary và ma trận quyền.
- [x] Tách luồng UC-01.A login, UC-01.B logout/session, UC-01.C đổi password, chỉ A/B là ưu tiên lát cắt đầu.
- [x] Phác thảo form chung không chọn role, trạng thái lỗi và trang chào theo role.
- [x] Soạn dữ liệu auth tối thiểu, xử lý backend, audit/CSRF/session/counter và 28 ca kiểm thử NOT RUN.
- [x] Nhận xét biểu đồ do người dùng cung cấp và soạn sơ đồ gợi ý bằng PlantUML (chưa render).
- [ ] Người dùng rà soát thiết kế login, D-22 và các chính sách còn đề xuất trước code.
- [ ] Viết migration, backend, template và chạy kiểm thử ở đợt triển khai sau.

Tài liệu: [login-design.md](docs/design/authentication/login-design.md), [diagram-review.md](docs/design/authentication/diagram-review.md), [login-test-plan.md](docs/design/authentication/login-test-plan.md).

## Phạm vi ưu tiên D-26 — đăng nhập tối giản

- [x] Ghi nhận chỉ xác thực và điều hướng theo một role; hoãn quản lý phiên nâng cao, forced-change lần đầu, quên/reset password.
- [x] Soạn [thiết kế tối giản](docs/design/authentication/basic-login-design.md): hai bảng ROLE/APP_USER, một form, bốn trang chào, BASIC-01–09 NOT RUN.
- [x] Đã chuẩn bị kết nối database, migration, backend và template đăng nhập tối giản; người dùng xác nhận chức năng cơ bản thành công.

Các mục session/password nâng cao trước đó giữ cho đợt sau, không phải điều kiện hoàn tất lát cắt đầu. Thiết kế ban đầu ngày 19/10 chưa thêm code; trạng thái triển khai hiện tại xem P1.

## Cập nhật Java migration ngày

- [x] Người dùng xác nhận chưa chạy migration; chuyển SQL sang class Java theo yêu cầu.
- [x] Thêm `V1__CreateAuthenticationTables` và `V2__InsertRoles` trong `mrm.database.migration`; bỏ migration SQL cũ khỏi source resources.
- [x] Cấu hình location Java migration và checksum theo nội dung SQL; `mvnw.cmd -o -B -ntp clean compile` đạt BUILD SUCCESS.
- [x] Đã chạy ứng dụng và xác nhận Flyway V1–V4 thành công trên `mrm` tại `127.0.0.1:5433`; ngày kiểm tra lại đủ năm khoa, tên tiếng Việt đúng và active=true.

Tại thời điểm cập nhật ngày 19/10 chưa tạo bảng/thêm vai trò trong lượt đó; hiện đã áp dụng các migration và tạo dữ liệu như trạng thái ở đầu file. File trong `Note` giữ nguyên trong lượt cập nhật checklist này; [tài liệu triển khai hiện hành](docs/design/java-migrations.md) mô tả cách tổ chức Java migration.
