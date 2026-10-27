# Ghi chú: kết nối Java/Spring Boot với PostgreSQL

Ngày tạo:. Dự án: Medical Record Management, package `mrm`.

> **Quy tắc của thư mục Note:** nội dung trong thư mục này chỉ được thêm, sửa hoặc xóa khi người dùng yêu cầu. Không tự đồng bộ Note theo thay đổi code/docs, không tự chỉnh khi refactor. Đây là ghi chú để đọc trước, không phải lệnh yêu cầu chạy các đoạn mã bên dưới.
>
> **Trạng thái:** toàn bộ SQL, cấu hình, Java và lệnh PowerShell dưới đây là mẫu tham khảo. Chưa sửa `pom.xml`, chưa tạo file Java/cấu hình thực tế, chưa tải dependency, chưa tạo role/database, chưa kết nối hoặc kiểm thử PostgreSQL. Mọi kết quả minh họa đều chưa được xác minh trên máy bằng mã này.

## 1. Hiểu đúng thứ đang kết nối

- **PostgreSQL server:** dịch vụ thực sự lưu dữ liệu và tiếp nhận kết nối.
- **psql:** chương trình dòng lệnh để gửi SQL tới server.
- **pgAdmin:** giao diện quản trị server và database.
- **JDBC driver:** thư viện giúp Java nói chuyện với PostgreSQL.

Java kết nối **PostgreSQL server**, không kết nối ứng dụng pgAdmin hay tiến trình psql. pgAdmin, psql và Java có thể là ba client của cùng server. Không cần mở pgAdmin để ứng dụng chạy, nhưng dịch vụ PostgreSQL phải hoạt động.

```text
Spring Boot → JdbcTemplate → DataSource/HikariCP → PostgreSQL JDBC → PostgreSQL server
pgAdmin -------------------------------↑
psql ---------------------------------↑
```

### Đọc hình pgAdmin bạn cung cấp

```text
Servers
└── PostgreSQL 18 ← tên đăng ký hiển thị trong pgAdmin
 ├── Databases
 │ └── postgres ← database đang có/được chọn trong ảnh
 │ └── Schemas
 └── Login/Group Roles
```

Tên đăng ký server có thể do người dùng đặt, không chứng minh phiên bản server. Ảnh không cho biết host/port/password. Trước đó bạn đã cung cấp kết quả server **16.5**, client psql **18.6**; khi làm thật hãy kiểm tra lại đúng server trong ảnh, không suy ra từ tên hiển thị.

`Login/Group Roles` trong pgAdmin là tài khoản/quyền **của PostgreSQL**. Bốn vai trò `READER`, `ARCHIVIST`, `DIRECTOR`, `ADMIN` là vai trò **người dùng website**, sau này lưu trong bảng ứng dụng; không cần tạo bốn PostgreSQL login để đại diện chúng.

## 2. Các thông số phải biết trước

Các giá trị bên dưới là đề xuất cho môi trường học tập, chưa có nghĩa đã tồn tại:

| Thông số | Ví dụ dùng trong hướng dẫn | Cách xác định |
|---|---|---|
| Host | `127.0.0.1` | Server cùng máy thì loopback; pgAdmin server Properties → Connection |
| Port | `5432` | Xem Connection; có thể khác nếu nhiều PostgreSQL instance |
| Database ứng dụng | `mrm` | Tạo riêng cho phát triển, chưa có trong ảnh |
| Database login | `mrm_app` | Tạo riêng cho ứng dụng, khác tài khoản website |
| Password database login | Tự đặt khi tạo `mrm_app` | Không phải mặc định password của postgres hoặc website |
| JDBC URL | `jdbc:postgresql://127.0.0.1:5432/mrm` | Ghép host, port, database thực tế |

Nếu Java chạy trong Docker/WSL/máy khác, `127.0.0.1` chỉ tới môi trường chạy Java đó, không nhất thiết là Windows host. Hướng dẫn này giả định Java và PostgreSQL đều chạy trực tiếp trên Windows hiện tại.

### Kiểm tra bằng pgAdmin — chỉ thực hiện khi bạn quyết định bắt đầu

1. Chuột phải vào mục server `PostgreSQL 18` → **Properties** → **Connection**, đọc host, port và username đã cấu hình. Không cần gửi password cho trợ lý.
2. Chọn database `postgres`, mở **Query Tool**.
3. Chạy truy vấn chỉ đọc sau, đối chiếu đúng server và database:

```sql
SELECT
 version() AS server_version,
 current_database() AS database_name,
 current_user AS database_user,
 inet_server_addr() AS server_address,
 inet_server_port() AS server_port;
```

Nếu kết nối không qua TCP, địa chỉ/port có thể null; trên Windows với host `127.0.0.1` thông thường là TCP. pgAdmin có thể lưu password cho nó, Java không tự đọc được password đã lưu của pgAdmin.

## 3. Chuẩn bị role và database riêng — SQL mẫu, CHƯA CHẠY

Nên để database `postgres` phục vụ quản trị/thử kiểm tra, dữ liệu ứng dụng đặt trong `mrm`. Dùng tài khoản PostgreSQL riêng để tránh ứng dụng chạy bằng superuser `postgres`.

### 3.1. Tạo database login

Trong Query Tool kết nối bằng tài khoản quản trị có quyền tạo role/database, ví dụ `postgres`, chọn và chạy **riêng câu lệnh này** sau khi thay placeholder password:

```sql
CREATE ROLE mrm_app
 WITH LOGIN
 PASSWORD 'REPLACE_WITH_YOUR_OWN_DATABASE_PASSWORD'
 NOSUPERUSER
 NOCREATEDB
 NOCREATEROLE;
```

`REPLACE_WITH_YOUR_OWN_DATABASE_PASSWORD` chỉ là placeholder, không dùng nguyên văn. Nếu password có dấu nháy đơn trong SQL cần escape đúng, hoặc dùng hộp thoại tạo Login/Group Role của pgAdmin để nhập thay vì ghép SQL thủ công. Không lưu password thực trong Note, file `.sql` đưa lên Git hoặc ảnh chụp chia sẻ.

Không chạy lại CREATE ROLE nếu role đã tồn tại; kiểm tra trước:

```sql
SELECT rolname, rolcanlogin
FROM pg_roles
WHERE rolname = 'mrm_app';
```

### 3.2. Tạo database phát triển

Bật **Auto-commit** trong Query Tool, chạy **riêng** câu lệnh sau, không bọc `BEGIN`/transaction và không chọn cả CREATE ROLE lẫn CREATE DATABASE để chạy một lần:

```sql
CREATE DATABASE mrm
 WITH OWNER = mrm_app
 ENCODING = 'UTF8';
```

PostgreSQL không cho CREATE DATABASE chạy trong transaction block. Nếu có lỗi tương ứng, kiểm tra auto-commit và chỉ thực thi một câu lệnh CREATE DATABASE.

Không chạy lại nếu database đã có; kiểm tra:

```sql
SELECT datname, pg_get_userbyid(datdba) AS owner
FROM pg_database
WHERE datname = 'mrm';
```

Chuột phải **Databases** → Refresh. Khi đó mới kỳ vọng thấy `mrm` bên cạnh `postgres`. Muốn query database mới thì mở Query Tool từ **mrm**, không chỉ đổi tên tab cũ.

Ở database mới PostgreSQL 16 chuẩn, owner có thể quản lý schema `public` thông qua `pg_database_owner`. Chưa cần GRANT ALL trên mọi database/table. Với database cũ/import có quyền schema riêng, phải kiểm tra đúng quyền thay vì dùng GRANT rộng để thử. Cho môi trường học tập, owner `mrm_app` dùng được để phát triển; khi triển khai thật có thể tách migration owner và tài khoản runtime có quyền hạn chế hơn.

### 3.3. Thử bằng psql trước khi sửa Java

Trong PowerShell, dùng thông số thực tế:

```powershell
psql -h 127.0.0.1 -p 5432 -U mrm_app -d mrm -W
```

Nếu PATH chưa có psql, dùng đường dẫn cài đặt đã biết, ví dụ:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h 127.0.0.1 -p 5432 -U mrm_app -d mrm -W
```

Tên thư mục `18` ở đây là client, không khẳng định server 18. `-W` yêu cầu hỏi password; không đặt password trên command line. Sau khi đăng nhập:

```sql
SELECT current_database(), current_user;
SELECT 1 AS connection_ok;
```

Kỳ vọng database `mrm`, user `mrm_app`, connection_ok bằng 1. Thoát psql bằng `\q` (gõ một dấu gạch chéo ngược: `q`). Nếu psql kết nối thất bại thì sửa host/port/account/password/database trước, chưa sửa Java để bù lỗi kết nối server.

## 4. Thêm thư viện vào Maven — chỉ là nội dung để bạn đọc

Vì mục tiêu hiện tại chỉ kiểm tra kết nối, hướng dẫn dùng **Spring JDBC + JdbcTemplate**. Chưa cần Entity/Repository JPA, Hibernate, bảng ROLE/APP_USER hoặc Flyway để thực hiện SELECT kiểm tra. Điều này không phủ nhận định hướng dùng JPA/Flyway ở bước triển khai dữ liệu sau.

Hai dependency mới cần bổ sung vào `<dependencies>`:

```xml
<dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>

<dependency>
 <groupId>org.postgresql</groupId>
 <artifactId>postgresql</artifactId>
 <scope>runtime</scope>
</dependency>
```

Starter JDBC cung cấp hỗ trợ JDBC, JdbcTemplate và connection pool. Driver PostgreSQL thực hiện giao tiếp với server. Spring Boot parent quản lý phiên bản các dependency tương thích; không tự thêm version driver chỉ vì số server/client đang là 16 hoặc 18. Version JDBC driver là hệ phiên bản khác.

### Toàn bộ pom.xml mẫu theo khung hiện có

Đây là nội dung đề xuất **sau khi bạn chọn áp dụng**, chưa thay file thật. Chỉ thêm hai dependency so với khung đã đọc, không cần cài Spring Boot riêng hoặc tự tải driver JAR.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
 xmlns:xsi="http://www.w3.org//XMLSchema-instance"
 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
 <modelVersion>4.0.0</modelVersion>

 <parent>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-parent</artifactId>
 <version>4.1.1</version>
 <relativePath/>
 </parent>

 <groupId>vn.edu.hust</groupId>
 <artifactId>medical-record-management</artifactId>
 <version>0.0.1-SNAPSHOT</version>
 <name>Medical Record Management</name>
 <description>Medical record archive and access management website</description>

 <properties>
 <java.version>26</java.version>
 </properties>

 <dependencies>
 <dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-webmvc</artifactId>
 </dependency>
 <dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-thymeleaf</artifactId>
 </dependency>
 <dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-validation</artifactId>
 </dependency>
 <dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-jdbc</artifactId>
 </dependency>
 <dependency>
 <groupId>org.postgresql</groupId>
 <artifactId>postgresql</artifactId>
 <scope>runtime</scope>
 </dependency>
 </dependencies>

 <build>
 <plugins>
 <plugin>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-maven-plugin</artifactId>
 </plugin>
 </plugins>
 </build>
</project>
```

Nếu sau này pom.xml đã có thay đổi khác, không ghi đè bằng bản mẫu cũ; chỉ tích hợp dependency còn thiếu. Maven tải thư viện khi chạy build, không dùng `pip install requirement.txt`. Bước tải/build **chưa được thực hiện cho mẫu trong Note này**.

## 5. Cấu hình datasource qua biến môi trường

Tách môi trường dev để không ghi password trong mã nguồn. Có hai file cấu hình dự kiến:

### 5.1. src/main/resources/application.properties

Giữ cấu hình chung hiện tại:

```properties
spring.application.name=medical-record-management
```

### 5.2. src/main/resources/application-dev.properties

File này hiện chỉ được mô tả trong Note, chưa tạo bên ngoài:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://127.0.0.1:5433/mrm}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}

spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.connection-timeout=10000
spring.jdbc.template.query-timeout=10

spring.sql.init.mode=never
```

Giải thích:

- `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}` đọc từ environment của tiến trình Java; không có password mặc định giấu trong file.
- URL ví dụ `jdbc:postgresql://127.0.0.1:5432/mrm`: JDBC → PostgreSQL → host → port → database. Không trỏ tới `Schemas/public` trong đường dẫn URL.
- Pool tối đa 5 kết nối là lựa chọn đơn giản để học, không phải đã đo hiệu năng.
- `connection-timeout=10000` là thời gian tối đa đợi pool cấp kết nối, đơn vị milliseconds; không phải cam kết toàn bộ TCP/query luôn kết thúc trong 10 giây.
- `query-timeout=10` là query timeout theo giây qua JdbcTemplate.
- `spring.sql.init.mode=never` không cho Boot tự chạy schema.sql/data.sql ở bước chỉ kiểm tra kết nối. Java mẫu bên dưới chỉ SELECT.
- Không cần `Class.forName("org.postgresql.Driver")`; driver hiện đại tự đăng ký. Có thể khai báo `spring.datasource.driver-class-name=org.postgresql.Driver` nếu cần, nhưng URL và dependency đã đủ cho auto-detection.
- Chưa có JPA nên không cần `spring.jpa.*`, dialect hoặc `ddl-auto=update`.

Sau khi thêm JDBC starter, nếu chạy mà không bật dev/không có datasource tương ứng, Boot có thể báo thiếu URL/driver embedded. Vì vậy lệnh chạy ở bước dưới phải cấp environment và active profile đúng.

## 6. Java mẫu kiểm tra kết nối bằng truy vấn chỉ đọc

### 6.1. File khởi động hiện tại, không cần đổi chức năng

Đường dẫn: `src/main/java/mrm/MedicalRecordApplication.java`.

```java
package mrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MedicalRecordApplication {

 public static void main(String[] args) {
 SpringApplication.run(MedicalRecordApplication.class, args);
 }
}
```

### 6.2. File mới dự kiến: DatabaseConnectionCheck.java

Đường dẫn dự kiến: `src/main/java/mrm/DatabaseConnectionCheck.java`. Không đặt vào Note dưới dạng `.java` chạy được; đoạn dưới chỉ nằm trong tài liệu để bạn đọc.

```java
package mrm;

import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("dev & db-check")
public class DatabaseConnectionCheck implements ApplicationRunner {

 private final JdbcTemplate jdbcTemplate;

 public DatabaseConnectionCheck(JdbcTemplate jdbcTemplate) {
 this.jdbcTemplate = jdbcTemplate;
 }

 @Override
 public void run(ApplicationArguments args) {
 Integer result = jdbcTemplate.queryForObject(
 "SELECT 1",
 Integer.class
 );

 if (!Integer.valueOf(1).equals(result)) {
 throw new IllegalStateException(
 "Unexpected result from database connection check"
 );
 }

 Map<String, Object> info = jdbcTemplate.queryForMap("""
 SELECT
 current_database() AS database_name,
 current_user AS database_user,
 current_setting('server_version') AS server_version,
 inet_server_port() AS server_port
 """);

 System.out.println("=== PostgreSQL connection verified ===");
 System.out.println("Database: " + info.get("database_name"));
 System.out.println("User: " + info.get("database_user"));
 System.out.println("Server version: " + info.get("server_version"));
 System.out.println("Server port: " + info.get("server_port"));
 }
}
```

### Cách đoạn Java hoạt động

1. Boot khởi tạo DataSource từ environment và cấu hình dev, cùng JDBC driver/pool.
2. Vì class có `@Component` và nằm dưới package `mrm`, Boot tìm thấy nó.
3. Profile expression `dev & db-check` yêu cầu cả **dev** và **db-check** cùng active mới chạy class này.
4. Constructor nhận JdbcTemplate do Spring cung cấp; không tạo connection tay hoặc hardcode password.
5. ApplicationRunner chạy sau khi application context được tạo, gửi SELECT 1 rồi đọc tên database/user/server.
6. Nếu kết nối hoặc truy vấn lỗi, exception được đưa ra, startup thất bại; không nuốt lỗi và vẫn in “thành công”.
7. JdbcTemplate quản lý lấy/trả connection qua DataSource. Không giữ connection trong biến static hoặc quên đóng kết nối tự tạo.

Thông báo “Started MedicalRecordApplication” hoặc nhìn thấy trang HTML **chưa đủ chứng minh database kết nối được**; phải có kết quả query đúng database/user. Runner có thể chạy sau thông báo khởi động ban đầu. Mẫu in metadata chỉ dùng local dev, không đưa version/user database ra endpoint công khai.

## 7. PowerShell mẫu để chạy khi bạn đã chọn áp dụng

Các lệnh dưới đây chưa được thực thi. Chạy trong thư mục **Code** có `pom.xml` và `mvnw.cmd` sau khi đã bổ sung đúng file/dependency.

### 7.1. Cấp thông số kết nối cho terminal hiện tại

```powershell
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:5433/mrm'
$env:DB_USERNAME = 'postgres'

$dbPasswordSecure = Read-Host 'Nhap password PostgreSQL cua mrm_app' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $dbPasswordSecure).Password
```

Read-Host giúp password không hiện trên màn hình/lịch sử lệnh như một literal. Khi gán cho environment, password vẫn tồn tại trong bộ nhớ của tiến trình để driver sử dụng, không phải được mã hóa đầu-cuối bởi SecureString. Không in `$env:DB_PASSWORD` hoặc chụp environment để chia sẻ.

Biến này chỉ thuộc terminal/process và các tiến trình con; F5 của VS Code đang chạy từ tiến trình khác có thể không nhận. Bước này nên chạy Maven từ chính terminal vừa đặt biến. Spring Boot không tự đọc file `.env` chỉ vì bạn tạo nó.

### 7.2. Build và chạy qua Maven Wrapper

```powershell
.\mvnw.cmd -B -ntp package
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev,db-check'
```

Lần đầu có thể phải tải JDBC starter/driver nên không dùng `-o` offline khi chưa có cache. Nếu lệnh package thất bại, dừng để sửa lỗi, không coi lệnh run sau đó đã dùng đúng bản build mới.

### 7.3. Cách chạy JAR thay thế

Đây là **lựa chọn thay thế**, không chạy đồng thời với Maven run trên cùng cổng:

```powershell
java -jar target/medical-record-management-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev,db-check
```

Các biến DB vẫn phải có trong cùng terminal. Có thể dùng `-server.port=18080` nếu 8080 bị chiếm; đây là cổng HTTP, khác với port PostgreSQL 5432.

### 7.4. Kết quả minh họa

```text
=== PostgreSQL connection verified ===
Database: mrm
User: mrm_app
Server version: <actual-server-version>
Server port: 5432
```

`<actual-server-version>` phải là dữ liệu truy vấn server; không mặc định 18 chỉ vì pgAdmin đặt tên PostgreSQL 18. Trên máy đã xác nhận trước đó có thể là 16.5, nhưng phải kiểm tra lại đúng instance.

Dừng ứng dụng bằng Ctrl+C. Sau khi dừng và không cần dùng biến nữa, có thể xóa biến password khỏi terminal:

```powershell
Remove-Item Env:\DB_PASSWORD -ErrorAction SilentlyContinue
Remove-Variable dbPasswordSecure -ErrorAction SilentlyContinue
```

Lần chạy kiểm tra tiếp phải nhập lại. Đóng terminal cũng kết thúc environment tạm đó. Không dùng setx để lưu password cố định.

### 7.5. Chạy bình thường sau khi đã kiểm tra

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
```

Lúc này bean kiểm tra không chạy do thiếu db-check, datasource vẫn có cấu hình dev. Có thể giữ class check trong source cho môi trường dev có kiểm soát, nhưng không bật profile db-check trong môi trường vận hành.

## 8. Những lỗi thường gặp và cách đọc

| Lỗi | Nguyên nhân cần kiểm tra | Cách xử lý |
|---|---|---|
| `Connection refused` | Server chưa chạy, host/port sai, nhầm instance | Kiểm tra dịch vụ và Properties của server, thử psql cùng thông số |
| `password authentication failed` | Sai password/user hoặc policy xác thực | Dùng password của mrm_app, không mặc định password postgres; thử psql |
| `database mrm does not exist` | Chưa tạo hoặc tạo ở instance khác | Đối chiếu host/port, refresh pgAdmin, query pg_database |
| `role mrm_app does not exist` | Login chưa có trên đúng server | Kiểm tra pg_roles trên cùng host/port |
| `Could not resolve placeholder DB_PASSWORD` | Process Java không nhận environment | Đặt biến và run Maven từ cùng terminal; không in password |
| `Failed to determine a suitable driver class` | Thiếu JDBC driver/dependency hoặc profile URL | Kiểm tra pom và profile dev, Maven đã resolve dependency chưa |
| `No suitable driver` | Driver không ở runtime classpath hoặc URL sai | Dùng Maven/JAR Boot đúng, URL bắt đầu jdbc:postgresql: |
| `CREATE DATABASE cannot run inside a transaction block` | Chạy chung block/transaction hoặc tắt auto-commit | Bật auto-commit và chạy riêng CREATE DATABASE |
| `permission denied for schema public` | Schema/owner của database cũ khác dự kiến | Kiểm tra đúng database owner/schema; không sửa bằng grant superuser rộng |
| `Port 8080 was already in use` | HTTP app khác đang chạy | Dừng app cũ hoặc đổi server.port; không đổi DB_URL port để chữa HTTP |
| `Class not found: mrm.DatabaseConnectionCheck` hoặc import đỏ | File/package sai hoặc IDE chưa nạp Maven | File dưới src/main/java/mrm, package mrm, reload Maven/Java workspace |

Không tắt password authentication hoặc chuyển toàn bộ pg_hba.conf sang trust chỉ để làm mất thông báo lỗi. Nếu là server khác máy, cần thiết kế mạng/TLS/quyền riêng, không dùng nguyên cấu hình localhost như bản triển khai thực tế.

## 9. Phạm vi thay đổi dự kiến khi bạn đồng ý triển khai

```text
Code/
├── pom.xml # thêm JDBC starter và PostgreSQL driver
└── src/main/
 ├── java/mrm/
 │ ├── MedicalRecordApplication.java # giữ nguyên main
 │ └── DatabaseConnectionCheck.java # mẫu kiểm tra chỉ SELECT
 └── resources/
 ├── application.properties # cấu hình chung
 └── application-dev.properties # DB environment, pool
```

Ngoài Java còn có database/login được tạo thủ công theo bước 3. Chưa tạo bảng ROLE/APP_USER, chưa cài đăng nhập, chưa xử lý password website, chưa có migration hay quyền ứng dụng. Chứng minh kết nối DB chỉ là nền tảng cho bước đó.

## 10. Checklist để tự thực hiện sau khi đã đọc

- [ ] Hiểu PostgreSQL server khác psql và pgAdmin.
- [ ] Xác định host/port/server version đúng instance.
- [ ] Chọn database phát triển mrm và database login mrm_app.
- [ ] Tạo login/database bằng từng lệnh riêng sau khi quyết định áp dụng.
- [ ] Thử psql bằng mrm_app tới mrm.
- [ ] Tích hợp dependency vào pom hiện hành, không ghi đè thay đổi mới ngoài ý muốn.
- [ ] Tạo cấu hình dev không chứa password thật.
- [ ] Thêm class check theo package mrm, chỉ SELECT.
- [ ] Đặt environment và chạy profile dev,db-check.
- [ ] Đối chiếu đúng database/user/version/port, không chỉ nhìn trang web hoặc log Started.
- [ ] Dừng app và xóa biến password tạm khi xong.
- [ ] Sau đó mới bàn tiếp schema/account/đăng nhập.

Tất cả ô để trống: chưa có bước triển khai hoặc kiểm chứng nào được thực hiện bởi việc tạo file ghi chú này. Mã mẫu được rà soát bằng đọc, chưa biên dịch/chạy trên môi trường dự án.

## 11. Tài liệu chính thức để đọc thêm

- [Spring Boot — SQL Databases](https://docs.spring.io/spring-boot/reference/data/sql.html): datasource properties, JDBC, pool, JdbcTemplate.
- [pgJDBC — kết nối và URL](https://jdbc.postgresql.org/documentation/use/): driver PostgreSQL, URL và thông số kết nối.
- [PostgreSQL 16 — CREATE ROLE](https://www.postgresql.org/docs/16/sql-createrole.html): tạo database login.
- [PostgreSQL 16 — CREATE DATABASE](https://www.postgresql.org/docs/16/sql-createdatabase.html): database owner và giới hạn transaction.
- [pgAdmin — Server Dialog](https://www.pgadmin.org/docs/pgadmin4/latest/server_dialog.html): thông số đăng ký/kết nối server.

Các nguồn trên giải thích cơ chế. Tên database/user, profile, pool và class check trong Note là phương án đề xuất riêng cho dự án học tập này.
