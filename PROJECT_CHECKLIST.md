# Medical Record Management — Lộ trình xây dựng chương trình

Ngày lập: 18/10/2026. Mục đích: quản lý công việc từ phân tích đến bàn giao website quản lý lưu trữ và khai thác hồ sơ bệnh án (HSBA).

## Cách sử dụng

- Đánh dấu `[x]` khi có kết quả kiểm chứng được; giữ `[ ]` khi chưa hoàn thành.
- Thực hiện theo thứ tự các giai đoạn. Mỗi giai đoạn có điều kiện để chuyển sang bước tiếp theo.
- Mỗi công việc có thể tách thành GitHub Issue; ghi người phụ trách, liên kết tài liệu và kết quả kiểm tra.
- Các phương án trong SRS dự thảo và hai báo cáo nhận xét chưa mặc nhiên là yêu cầu đã được duyệt.
- Tên file, folder, class và biến dùng tiếng Anh. Nội dung hướng dẫn và giao diện có thể dùng tiếng Việt.
- File này là kế hoạch; không khởi tạo chức năng hoặc thay đổi cơ sở dữ liệu.

Luồng mục tiêu: **Tra cứu → đăng ký đọc → NVLT kiểm tra/trình → duyệt → xác nhận lịch → giao tại phòng đọc → nhận trả → tra cứu lịch sử/báo cáo.**

## 0. Kiểm kê nền tảng hiện có

- [x] Có dự án Maven với `pom.xml` và Maven Wrapper.
- [x] Có cấu hình Spring Boot 4.1.1 và Java 26 trong `pom.xml`.
- [x] Đã đổi tên artifact thành `medical-record-management`.
- [x] Xác nhận package gốc `mrm`; file `src/main/java/mrm/MedicalRecordApplication.java` khai báo class `MedicalRecordApplication` và `package mrm;`.
- [x] Đồng bộ README với `pom.xml`: `target/medical-record-management-0.0.1-SNAPSHOT.jar`; bổ sung quy ước package và file khởi động.
- [x] Chạy lại build và ứng dụng ngày 18/10/2026: BUILD SUCCESS, HTTP 200; xem `docs/verification/BASELINE_VERIFICATION.md`.
- [ ] Kiểm tra phiên bản PostgreSQL server bằng truy vấn sau khi kết nối; không suy ra từ phiên bản `psql`.
  - Đã thử localhost:5432; server yêu cầu mật khẩu. Chờ kết quả `SELECT version();` từ phiên đã đăng nhập.
- [x] Ghi nhận danh sách file được quản lý và tạo commit mốc nền tảng chạy được; xem `docs/verification/BASELINE_VERIFICATION.md` và `git log`.

**Hoàn thành khi:** chạy được khung ứng dụng bằng hướng dẫn và xác định rõ các chức năng nghiệp vụ chưa được xây dựng.

## 1. Chốt phạm vi phiên bản đầu

- [ ] Đọc và đối chiếu SRS v0.2 với hai báo cáo nhận xét; lập danh sách điểm thống nhất/mâu thuẫn.
- [ ] Xác định ai xác nhận nghiệp vụ: Giám đốc, KHNV, nhân viên lưu trữ (NVLT), CNTT.
- [ ] Xác định người dùng: bác sĩ/nhân viên y tế, NVLT, KHNV, Giám đốc/người được ủy quyền, quản trị kỹ thuật.
- [ ] Chốt có đưa nhân viên soát HSBA và bước tiếp nhận từ khoa vào phần mềm hay không.
- [ ] Chốt điểm bắt đầu quản lý: từ nhập kho hay từ tiếp nhận/soát hồ sơ.
- [ ] Liệt kê chức năng bắt buộc của bản đầu và chức năng để sau.
- [ ] Ghi rõ chỉ quản lý metadata, vị trí, trạng thái, lịch đọc và lịch sử HSBA giấy.
- [ ] Ghi rõ ngoài phạm vi hiện tại: bản quét/toàn văn, HIS, chữ ký số, yêu cầu bên ngoài, workflow khai thác HSBA tử vong, tiêu hủy.
- [ ] Phân biệt quản lý metadata HSBA tử vong với workflow xin khai thác nằm ngoài hệ thống.
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
- [ ] Chốt duyệt toàn yêu cầu hay từng chi tiết/ngày; có duyệt một phần hay không.
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
- [ ] Chốt ủy quyền: người cấp, phạm vi, bắt đầu/kết thúc, thu hồi và kiểm tra khi lưu quyết định.
- [ ] Chốt phạm vi tra cứu và trường được xem theo từng vai trò, kể cả báo cáo/xuất tệp.
- [ ] Chốt SMTP nếu dùng email, người nhận, sự kiện, nội dung, thử lại và mốc đo 5 phút.
- [ ] Chốt sự kiện kiểm toán, quyền xem, bảo toàn và chính sách lưu 10 năm theo tài liệu nghiệp vụ.

**Đầu ra dự kiến:** `docs/requirements/business-rules.md`, sổ quyết định có người/ngày xác nhận.

**Hoàn thành khi:** các quyết định ảnh hưởng cấu trúc dữ liệu, quyền và luồng chính đã chốt trước khi lập trình phần tương ứng.

## 3. Đặc tả quy trình, trạng thái và nghiệm thu

- [ ] Vẽ luồng xuyên suốt, chỉ rõ người thao tác tại từng bước.
- [ ] Tách trạng thái yêu cầu, phiên đọc và HSBA vật lý; thông báo có trạng thái riêng.
- [ ] Với mỗi chuyển trạng thái, ghi tác nhân, điều kiện, dữ liệu bắt buộc, kết quả và nhật ký.
- [ ] Mô tả nhánh bổ sung, từ chối, hủy, không đến, không khả dụng và chưa trả cuối ngày.
- [ ] Chuẩn hóa danh mục mã use case; giải quyết khác biệt UC-07 giữa các tài liệu.
- [ ] Viết use case cho tài khoản/quyền, HSBA/kho, yêu cầu, duyệt/ủy quyền, lịch, giao–trả, báo cáo, nhập dữ liệu.
- [ ] Viết tiêu chí chấp nhận có đầu vào, thao tác và kết quả quan sát được cho từng chức năng.
- [ ] Lập ma trận yêu cầu → quy tắc → use case → màn hình/chức năng → ca kiểm thử.
- [ ] Chốt tải đồng thời, quy mô dữ liệu, ngưỡng đáp ứng và cấu hình đo.
- [ ] Chốt RPO (mất dữ liệu tối đa), RTO (thời gian khôi phục), thời gian phục vụ và cách làm khi gián đoạn.

**Đầu ra dự kiến:** `docs/use-cases/`, `docs/requirements/acceptance-criteria.md`, `docs/testing/traceability.md`.

**Hoàn thành khi:** có thể viết ca kiểm thử mà không phải tự đoán quy tắc. Khoảng 100 HSBA chỉ là quy mô thử, không phải tải đồng thời.

## 4. Thiết kế quyền và giao diện

- [ ] Lập ma trận hành động × vai trò × phạm vi bản ghi × trường dữ liệu.
- [ ] Tách quản trị kỹ thuật khỏi quyền đọc dữ liệu nhạy cảm và quyền duyệt.
- [ ] Thiết kế đăng nhập, đăng xuất, đổi mật khẩu, khóa/đặt lại tài khoản và hết phiên.
- [ ] Phác thảo menu và trang tổng quan cho từng vai trò.
- [ ] Phác thảo danh sách/chi tiết/thêm/sửa HSBA, bộ lọc và phân trang.
- [ ] Phác thảo vị trí kho, chuyển vị trí và lịch sử.
- [ ] Phác thảo đăng ký đọc, yêu cầu của tôi, bổ sung/hủy.
- [ ] Phác thảo hàng đợi NVLT, hàng đợi duyệt, xác thực và quản lý ủy quyền.
- [ ] Phác thảo xác nhận lịch, giao–trả và đối soát cuối ngày.
- [ ] Phác thảo báo cáo, nhật ký, nhập dữ liệu và quản trị danh mục.
- [ ] Thiết kế thông báo lỗi, trạng thái rỗng, không có quyền, mất kết nối và dữ liệu đã bị người khác thay đổi.
- [ ] Rà soát nguyên mẫu theo từng vai trò với người nghiệp vụ.

**Đầu ra dự kiến:** `docs/requirements/permission-matrix.md`, `docs/ui/`.

**Hoàn thành khi:** người dùng xác nhận được thao tác cần làm, trường được thấy và phản hồi của hệ thống.

## 5. Thiết kế PostgreSQL và kiến trúc ứng dụng

- [ ] Vẽ ERD theo mô hình yêu cầu đã chốt; xác định quan hệ một/nhiều.
- [ ] Thiết kế tài khoản, vai trò/quyền, khoa/phòng và vị trí kho.
- [ ] Thiết kế HSBA, chính sách thời hạn và lịch sử vị trí.
- [ ] Thiết kế yêu cầu/phiên bản, chi tiết nếu cần, quyết định và ủy quyền.
- [ ] Thiết kế phiên theo ngày, lượt giao–trả và ngoại lệ.
- [ ] Thiết kế audit; lô nhập/lỗi và outbox thông báo nếu các chức năng này được chọn.
- [ ] Viết từ điển dữ liệu: ý nghĩa, kiểu, độ dài, bắt buộc, mặc định và giá trị hợp lệ.
- [ ] Xác định khóa chính/ngoại, unique, check constraint và chính sách xóa/vô hiệu hóa.
- [ ] Thiết kế ràng buộc một HSBA tối đa một lượt giao chưa trả.
- [ ] Chốt transaction/khóa hoặc phiên bản dữ liệu khi duyệt, xác nhận lịch, giao và trả.
- [ ] Chốt chống gửi lặp: cùng thao tác gửi lại không sinh giao dịch trùng.
- [ ] Chọn chỉ mục theo tra cứu thực tế; quyết định tìm tiếng Việt/có dấu/không dấu nếu cần.
- [ ] Phân biệt ngày nghiệp vụ với timestamp sự kiện; thống nhất múi giờ và thời gian máy chủ.
- [ ] Chọn DTO/Form để giới hạn dữ liệu đầu vào/đầu ra theo quyền.
- [ ] Giữ package `mrm` đơn giản ban đầu; chỉ tách package con khi số lớp/chức năng cần tổ chức thêm.
- [ ] Quy định Controller nhận/trả web, Service xử lý nghiệp vụ/transaction, Repository truy cập dữ liệu.

**Đầu ra dự kiến:** `docs/database/erd.*`, `docs/database/data-dictionary.md`, `docs/architecture.md`.

**Hoàn thành khi:** mô hình đáp ứng cả luồng chính, ngoại lệ và truy vết; không chỉ đủ cho CRUD.

## 6. Chuẩn bị nền tảng triển khai chức năng

Thực hiện sau khi hoàn tất các quyết định thiết kế liên quan.

- [ ] Chốt phiên bản dependency trong Maven; dùng BOM Spring Boot cho thư viện được quản lý.
- [ ] Bổ sung JPA, PostgreSQL JDBC, Flyway PostgreSQL, Security và thư viện kiểm thử theo thiết kế.
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

- [ ] Đăng nhập/đăng xuất bằng session; mật khẩu được băm phù hợp.
- [ ] Quản lý cấp/khóa tài khoản, đổi/đặt lại mật khẩu theo chính sách đã chốt.
- [ ] Quản lý vai trò/quyền, nhật ký thay đổi và tài khoản khởi tạo an toàn.
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
- [ ] Quản lý ủy quyền có thời hạn/thu hồi; kiểm tra lại hiệu lực lúc lưu quyết định.
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
- [ ] Liệt kê chưa trả cuối ngày; ghi nguyên nhân, người xử lý và kết quả.
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
- [ ] Kiểm tra quyền bị thu hồi/hết hạn ngay khi lưu quyết định.
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

**Bắt đầu với giai đoạn 0 và 1.** Sau đó ưu tiên chốt khóa HSBA, mô hình một/nhiều hồ sơ mỗi yêu cầu, phạm vi duyệt, trạng thái, quyền và xác thực cấp 2 trước khi khóa ERD hoặc viết nghiệp vụ.
