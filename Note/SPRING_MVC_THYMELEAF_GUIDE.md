# Ghi chú: Java, HTML, điều hướng và truyền dữ liệu trong MRM

Ngày ghi:. Đối chiếu mã nguồn hiện tại của dự án Medical Record Management.

> Nội dung trong thư mục Note chỉ được thêm, sửa hoặc xóa khi người dùng yêu cầu. File này được tạo theo yêu cầu ghi lại câu trả lời cho câu hỏi 1 (liên kết Java–HTML và điều hướng đăng nhập/đăng xuất), câu hỏi 2 (truyền dữ liệu Java–HTML), cùng giải thích bổ sung về classpath. Đây là tài liệu học tập; các đoạn minh họa không phải yêu cầu tự sửa mã nguồn hoặc chạy ứng dụng.

## 1. Những file cần đọc

| File | Vai trò |
|---|---|
| [pom.xml](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/pom.xml>) | Khai báo thư viện Spring MVC, Thymeleaf, Security |
| [MedicalRecordApplication.java](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/src/main/java/mrm/MedicalRecordApplication.java>) | Khởi động Spring Boot; có `@SpringBootApplication` |
| [HomeController.java](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/src/main/java/mrm/authentication/HomeController.java>) | Nhận GET, chuyển hướng theo role, chuẩn bị dữ liệu trang chủ |
| [SecurityConfig.java](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/src/main/java/mrm/config/SecurityConfig.java>) | Xác thực, phân quyền URL, xử lý POST đăng nhập/đăng xuất |
| [DatabaseUserDetailsService.java](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/src/main/java/mrm/authentication/DatabaseUserDetailsService.java>) | Đọc tài khoản, mật khẩu băm và role từ PostgreSQL |
| [application.properties](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/src/main/resources/application.properties>) | Cấu hình chung; hiện chưa ghi đè prefix/suffix của Thymeleaf |
| [login.html](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/src/main/resources/templates/login.html>) | Mẫu trang đăng nhập |
| [home.html](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/src/main/resources/templates/home.html>) | Mẫu trang chủ dùng chung cho bốn role |

## 2. Vì sao chỉ trả về "login" mà vẫn tìm đúng HTML?

### 2.1. Chuỗi "login" là tên view

```java
@GetMapping("/login")
public String login() {
 return "login";
}
```

`HomeController` có `@Controller`. Với phương thức này, không có `@ResponseBody`, Spring MVC hiểu chuỗi trả về là tên view cần hiển thị. Nó không gửi nguyên chữ `login` cho trình duyệt.

Nếu dùng `@RestController` hoặc `@ResponseBody`, chuỗi thường được ghi vào nội dung HTTP response; đó là cơ chế khác. Xem [Spring MVC: kiểu giá trị trả về](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/return-types.html).

### 2.2. Spring Boot đã chuẩn bị các thành phần Thymeleaf khi khởi động

Trong dự án, `pom.xml` có `spring-boot-starter-thymeleaf`; lớp khởi động có `@SpringBootApplication`, bao gồm cơ chế tự cấu hình.

Khi các điều kiện phù hợp được đáp ứng và không có cấu hình tùy chỉnh thay thế, Boot thiết lập tích hợp Thymeleaf cho Spring MVC. Những thành phần cần phân biệt:

- `ThymeleafViewResolver`: nhận tên view từ Spring MVC, tạo/chọn Thymeleaf view để render.
- Template engine: xử lý mẫu và dữ liệu.
- Template resolver: xác định tài nguyên mẫu từ tên mẫu và cấu hình đường dẫn.

Controller không tự đi tìm file HTML. Việc tìm và render được giao cho các thành phần trên.

### 2.3. Prefix và suffix là quy tắc đã có sẵn

Giá trị mặc định:

```properties
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
```

Quy tắc cho tên mẫu thông thường trong cấu hình hiện tại:

```text
Tên view: login
Prefix: classpath:/templates/
Suffix:.html

Tài nguyên cần đọc: classpath:/templates/login.html
```

Hai giá trị mặc định được nêu trong [Spring Boot: Common Application Properties](https://docs.spring.io/spring-boot/appendix/application-properties/index.html#application-properties.templating.spring.thymeleaf.prefix). Không cần viết lại vào `application.properties` nếu giữ nguyên thư mục hiện tại.

Đây là phép áp dụng quy tắc, không phải tìm ngẫu nhiên mọi file có tên giống `login`. Đổi cấu hình hoặc đổi tên view sẽ làm thay đổi tài nguyên được tìm.

### 2.4. Classpath thực chất là gì?

Classpath là tập hợp các vị trí mà môi trường chạy Java có thể tìm lớp và tài nguyên của ứng dụng, gồm thư mục và thư viện JAR. Tiền tố `classpath:` yêu cầu Spring đọc tài nguyên thông qua cơ chế đó.

`classpath:` không có nghĩa cố định là `src/main/resources`, cũng không phải thư mục hiện tại của PowerShell.

Theo cấu trúc Maven mặc định, tài nguyên trong `src/main/resources` được sao chép vào thư mục đầu ra, giữ nguyên đường dẫn tương đối:

```text
File bạn chỉnh trong mã nguồn:
src/main/resources/templates/login.html

Sau bước xử lý resources của Maven:
target/classes/templates/login.html

Đường dẫn tương đối từ gốc classpath:
templates/login.html
```

Khi chạy bằng Maven, `target/classes` nằm trong classpath của ứng dụng. Khi chạy từ IDE, thư mục đầu ra có thể khác, nhưng tài nguyên vẫn cần được đưa vào classpath đúng cấu trúc. Xem [Maven: cấu trúc thư mục chuẩn](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html).

Nếu đóng gói thành executable JAR bằng Spring Boot Maven Plugin, tài nguyên ứng dụng nằm trong `BOOT-INF/classes/`; cơ chế nạp của Boot vẫn cho phép truy cập theo `templates/login.html`. Không viết thêm `BOOT-INF/classes` vào prefix cấu hình.

Vì thế, tên ổ đĩa và đường dẫn workspace của máy phát triển không cần xuất hiện trong controller.

### 2.5. Toàn bộ chuỗi xử lý

```text
Trình duyệt yêu cầu GET /login
 → đi qua Spring Security (URL này được phép truy cập)
 → DispatcherServlet của Spring MVC
 → HomeController.login()
 → tên view "login"
 → ThymeleafViewResolver
 → Thymeleaf view / template engine / template resolver
 → classpath:/templates/login.html
 → xử lý mẫu với dữ liệu
 → HTTP response chứa HTML gửi về trình duyệt
```

Ví dụ khác, với cùng prefix/suffix:

| Controller trả về | Mẫu trong mã nguồn |
|---|---|
| `"home"` | `src/main/resources/templates/home.html` |
| `"department/list"` | `src/main/resources/templates/department/list.html` |
| `"reader/home"` | `src/main/resources/templates/reader/home.html` — chỉ dùng được nếu có mẫu này |

Tên hàm Java không quyết định tên mẫu. URL cũng không bắt buộc giống tên mẫu.

### 2.6. Phân biệt URL, tên view và đường dẫn tài nguyên

| Khái niệm | Ví dụ | Ai sử dụng? |
|---|---|---|
| URL | `/login` | Trình duyệt và bộ định tuyến HTTP |
| Tên view | `login` | Spring MVC và view resolver |
| Đường dẫn tài nguyên | `classpath:/templates/login.html` | Spring/Thymeleaf đọc mẫu |
| File nguồn | `src/main/resources/templates/login.html` | Lập trình viên chỉnh sửa |

`templates` không mặc nhiên là đường dẫn công khai. Trình duyệt không cần truy cập `/templates/login.html` để xem trang.

## 3. Câu hỏi 1: Đăng nhập, chuyển trang theo role và đăng xuất

### 3.1. Hiển thị trang đăng nhập: GET /login

`@GetMapping("/login")` trong `HomeController` trả về view `login`. Phần 2 giải thích cách tìm mẫu tương ứng.

### 3.2. Gửi thông tin đăng nhập: POST /login

Form HTML:

```html
<form th:action="@{/login}" method="post">
 <input name="username" type="text" required>
 <input name="password" type="password" required>
 <button type="submit">Đăng nhập</button>
</form>
```

Cấu hình đang dùng trong `SecurityConfig`:

```java
.formLogin(login -> login
.loginPage("/login")
.loginProcessingUrl("/login")
.defaultSuccessUrl("/home", true)
.failureUrl("/login?error")
.permitAll()
)
```

GET và POST cùng URL nhưng khác phương thức HTTP. GET hiển thị trang; POST gửi thông tin xác thực.

POST được bộ lọc của Spring Security tiếp nhận, nên không cần `@PostMapping("/login")` trong `HomeController`. `DatabaseUserDetailsService` đọc tài khoản và role từ PostgreSQL; authentication provider dùng password encoder kiểm tra mật khẩu với giá trị băm. Thất bại chuyển về `/login?error`, thành công chuyển tới `/home`.

Tham khảo [Spring Security: Form Login](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html).

### 3.3. Chọn URL trang chủ theo role

Trong `HomeController.home()`:

```java
return switch (role) {
 case "READER" -> "redirect:/reader/home";
 case "ARCHIVIST" -> "redirect:/archivist/home";
 case "DIRECTOR" -> "redirect:/director/home";
 case "ADMIN" -> "redirect:/admin/home";
 default -> throw new AccessDeniedException("Invalid role");
};
```

`redirect:` yêu cầu chuyển hướng HTTP; không tìm file `templates/redirect:....html`. Trình duyệt gửi một yêu cầu mới tới URL đích.

Ở mã nguồn hiện tại, bốn URL trang chủ gọi chung `roleHome()`, cùng trả về `"home"`. Vì vậy chúng dùng chung `home.html`, với `title`, `username`, `role` khác nhau. Chưa có bốn file trang chủ riêng.

Security kiểm tra quyền URL độc lập với chuyển hướng: Reader nhập trực tiếp `/admin/home` vẫn bị chặn. Role lấy từ thông tin xác thực trên server; người dùng không tự chọn role trong form đăng nhập.

### 3.4. Đăng xuất: POST /logout

Trong `home.html`:

```html
<form th:action="@{/logout}" method="post">
 <button type="submit">Đăng xuất</button>
</form>
```

Cấu hình:

```java
.logout(logout -> logout
.logoutSuccessUrl("/login?logout")
.permitAll()
)
```

Spring Security có sẵn endpoint `/logout`. POST hợp lệ xóa trạng thái xác thực và vô hiệu hóa phiên HTTP hiện tại theo mặc định, rồi chuyển đến `/login?logout`. Không cần controller logout hoặc `logout.html`. Với CSRF đang bật, GET `/logout` mặc định có thể hiện trang xác nhận; form POST hiện tại thực hiện đăng xuất trực tiếp.

Tích hợp Thymeleaf/Spring Security tự đưa CSRF token vào form POST dùng `th:action`. Khi sửa HTML cần giữ cơ chế này. Xem [Spring Security: Handling Logouts](https://docs.spring.io/spring-security/reference/servlet/authentication/logout.html).

```text
GET /login → login.html
POST /login → Spring Security → PostgreSQL + kiểm tra mật khẩu
 ├─ thất bại → /login?error
 └─ thành công → /home → URL theo role → home.html
POST /logout → Spring Security → /login?logout
```

## 4. Câu hỏi 2: HTML nhận dữ liệu Java như thế nào?

### 4.1. Java chuẩn bị Model

Mã đang có trong `HomeController.roleHome()`:

```java
model.addAttribute("username", authentication.getName());
model.addAttribute("role", role);
model.addAttribute("title", title);
return "home";
```

`Model` mang dữ liệu của lần render này. Ví dụ tài khoản `reader1` có role `READER` thì ba khóa lần lượt có thể chứa `reader1`, `READER`, `Trang người đọc`.

### 4.2. Thymeleaf điền dữ liệu vào HTML trên server

```html
<h1 th:text="${title}">Trang chủ</h1>
<strong th:text="${username}">username</strong>
```

Kết quả hiển thị tương ứng là tiêu đề và tên tài khoản. Phần văn bản mẫu bên trong phần tử được thay thế khi render. `th:text` escape ký tự HTML; phù hợp để hiển thị dữ liệu người dùng nhập.

Trình duyệt nhận HTML đã xử lý, không nhận đối tượng `Model` hay quyền trực tiếp gọi phương thức Java. Xem [Thymeleaf: Using Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html).

### 4.3. Các thuộc tính thường gặp trong dự án

| Thuộc tính | Tác dụng |
|---|---|
| `th:text="${username}"` | Hiển thị giá trị |
| `th:if="${role == 'ARCHIVIST'}"` | Chỉ render phần tử khi điều kiện đúng |
| `th:href="@{/archivist/departments}"` | Tạo URL liên kết có xét context path |
| `th:action="@{/login}"` | Tạo URL gửi form |
| `th:each="department: ${departments}"` | Lặp qua danh sách |
| `th:object="${departmentForm}"` | Chọn object gắn với form |
| `th:field="*{code}"` | Gắn ô nhập với thuộc tính của object form |

Ví dụ thực tế trong `home.html`:

```html
<div th:if="${role == 'ARCHIVIST'}">
 <a th:href="@{/archivist/departments}">Quản lý khoa/phòng</a>
</div>
```

Điều kiện HTML chỉ quyết định hiển thị. Quyền truy cập vẫn phải được kiểm tra bằng Spring Security ở backend.

### 4.4. Thông báo lấy từ tham số URL

```html
<p th:if="${param.error}" role="alert">
 Không thể đăng nhập bằng thông tin đã cung cấp.
</p>
<p th:if="${param.logout}">
 Bạn đã đăng xuất.
</p>
```

`param.error` và `param.logout` lấy từ tham số URL, không cần `model.addAttribute()` trong controller. Đây chỉ là cờ hiển thị thông báo, không phải bằng chứng xác thực hay phân quyền.

### 4.5. Chiều ngược lại: HTML gửi dữ liệu về Java

```html
<input id="username" name="username" type="text">
```

- `name` là tên khóa dữ liệu được gửi khi submit form.
- `id` dùng để liên kết label, chọn phần tử bằng CSS/JavaScript; không thay thế `name`.

Spring Security mặc định chờ `username` và `password`, nên form đăng nhập cần giữ đúng hai tên này nếu chưa cấu hình lại backend. Với form nghiệp vụ, Spring MVC có thể bind các trường vào form object; controller kiểm tra dữ liệu rồi gọi service/repository để ghi database.

Không cần JavaScript `fetch()` để truyền dữ liệu trong luồng form và render HTML hiện tại. JavaScript có thể hỗ trợ trải nghiệm, còn xác thực, kiểm tra dữ liệu và phân quyền vẫn ở backend.

## 5. Phân biệt các kiểu trả về dễ nhầm

| Mã | Ý nghĩa trong controller hiện tại |
|---|---|
| `return "login";` | Render mẫu `login.html` |
| `return "home";` | Render mẫu `home.html`, URL hiện tại giữ nguyên |
| `return "department/list";` | Render mẫu trong thư mục con `department` |
| `return "redirect:/home";` | Trình duyệt gửi yêu cầu mới đến `/home` |

Model thường phục vụ lần render hiện tại; không tự đi theo redirect. Khi cần thông báo sau redirect, có thể dùng flash attribute qua `RedirectAttributes`, như luồng thêm khoa/phòng.

## 6. Những hiểu nhầm cần tránh

- `xmlns:th="http://www.thymeleaf.org"` khai báo namespace; không tải template hay cấu hình đường dẫn qua Internet.
- Package Java `mrm.authentication` không quyết định thư mục HTML. View name và template resolver mới quyết định mẫu.
- Đổi tên hàm Java không tự đổi URL hoặc tên HTML.
- Tài nguyên trong `templates` cần được render; mở HTML trực tiếp từ ổ đĩa không chạy Thymeleaf.
- File đã sửa trong nguồn cần được đưa vào đầu ra chạy ứng dụng. Chạy một JAR cũ sẽ vẫn dùng tài nguyên đóng gói trong JAR đó.
- Nếu không tìm thấy mẫu, kiểm tra tên view, đường dẫn/hoa thường, prefix/suffix và tài nguyên trong đầu ra build; không suy ra ngay rằng controller bị lỗi.

Tham khảo thêm: [Thymeleaf tích hợp Spring MVC](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html).

## 7. Bổ sung cấu hình để trang đăng nhập render đầy đủ

Ngày bổ sung:. Người dùng đã áp dụng cấu hình và xác nhận chạy lại thành công; đây là kết quả do người dùng kiểm thử, không phải lần chạy tự động khi cập nhật ghi chú.

### 7.1. Triệu chứng và nguyên nhân đã xác định từ log

Trang đăng nhập chỉ hiển thị một phần, thiếu ô nhập/nút. Log gốc cho biết lỗi khi xử lý `th:action="@{/login}"`:

```text
Error during execution of processor 'org.thymeleaf.spring6.processor.SpringActionTagProcessor'
Caused by: java.lang.IllegalStateException: Cannot create a session after the response has been committed
```

Stack trace đi qua `HttpSessionCsrfTokenRepository.saveToken` và `CsrfRequestDataValueProcessor.getExtraHiddenFields`: khi Thymeleaf xử lý form POST, Spring Security cần tạo session để lưu CSRF token, nhưng response đã được gửi trước đó nên không thể tạo session mới.

Nội dung HTML/CSS dài trước form có thể làm bộ đệm response đầy và được gửi trong quá trình render. Việc hiển thị dở trang trong trường hợp này không có nghĩa file thiếu nút. Hai biểu thức `th:attr` của ô nhập không phải nguyên nhân được log này xác định.

Dòng sau là lỗi tiếp theo khi Spring cố hiển thị trang báo lỗi sau khi response đã được gửi:

```text
Cannot render error page for request [/login] as the response has already been committed
```

### 7.2. Cấu hình đã giúp xử lý lỗi

Trong [application.properties](</C:/Users/admin/Desktop/Project/HUST/Master/SUB/Advanced Programming/Code/src/main/resources/application.properties>), bổ sung:

```properties
spring.thymeleaf.servlet.produce-partial-output-while-processing=false
```

Ví dụ kết hợp với tên ứng dụng:

```properties
spring.application.name=medical-record-management
spring.thymeleaf.servlet.produce-partial-output-while-processing=false
```

Thuộc tính mặc định là `true`. Đặt `false` yêu cầu Thymeleaf đệm đầu ra tới khi xử lý xong mẫu thay vì gửi từng phần trong lúc render. Trong luồng này, session và CSRF token được chuẩn bị trước khi gửi nội dung trang. Tham khảo [Spring Boot: cấu hình partial output](https://docs.spring.io/spring-boot/appendix/application-properties/index.html#application-properties.templating.spring.thymeleaf.servlet.produce-partial-output-while-processing).

Cấu hình này phù hợp với các trang HTML thông thường của MRM. Nó thay đổi cách đệm kết quả render, không vô hiệu hóa CSRF hoặc bỏ yêu cầu xác thực. Giữ nguyên `th:action="@{/login}"` và `method="post"`.

### 7.3. Cách kiểm tra khi áp dụng hoặc tái hiện lỗi

1. Lưu cấu hình, dừng ứng dụng bằng `Ctrl + C`.
2. Chạy lại trong terminal đã thiết lập biến môi trường database:

 ```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
 ```

3. Mở `/login` trong cửa sổ ẩn danh để kiểm tra trường hợp chưa có session.
4. Kiểm tra trang có đủ ô username, password và nút đăng nhập; thử đăng nhập và đăng xuất.

Nếu chạy bằng JAR, cần build lại sau khi thay đổi properties rồi chạy JAR mới. Nếu còn lỗi, đọc exception đầu tiên và `Caused by` thay vì chỉ dựa vào dòng không render được trang báo lỗi.
