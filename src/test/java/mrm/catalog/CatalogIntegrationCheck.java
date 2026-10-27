package mrm.catalog;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import jakarta.validation.Validation;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/** Explicit PostgreSQL/HTTP checks, not automatically run by Surefire. See docs/testing/CATALOG_VERIFICATION.md. */
public class CatalogIntegrationCheck {
    @Configuration
    @EnableTransactionManagement
    static class TransactionConfig { }

    private static int passed;

    public static void main(String[] args) throws Exception {
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isBlank()) {
            password = Files.readString(Path.of(".secrets/db-password.txt")).replaceFirst("[\\r\\n]+$", "");
        }
        var dataSource = new DriverManagerDataSource(
                System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://127.0.0.1:5433/mrm"),
                System.getenv().getOrDefault("DB_USERNAME", "postgres"), password);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DataSource.class, () -> dataSource);
            context.registerBean(JdbcTemplate.class, () -> new JdbcTemplate(dataSource));
            context.registerBean(DataSourceTransactionManager.class, () -> new DataSourceTransactionManager(dataSource));
            context.register(TransactionConfig.class, CatalogRepository.class, CatalogService.class);
            context.refresh();
            var jdbc = context.getBean(JdbcTemplate.class);
            var service = context.getBean(CatalogService.class);
            var transactions = new TransactionTemplate(context.getBean(DataSourceTransactionManager.class));
            long beforeTypes = count(jdbc, "record_types");
            long beforePolicies = count(jdbc, "retention_policies");
            long beforeEvents = count(jdbc, "catalog_events");
            try (var factory = Validation.buildDefaultValidatorFactory()) {
                var validator = factory.getValidator();
                check(!validator.validate(new RecordTypeForm("!", " ")).isEmpty(), "invalid type fields rejected");
                check(!validator.validate(new RetentionPolicyForm("T1", "Policy", 0, 0, null, null, " ")).isEmpty(), "invalid policy fields rejected");
            }
            rollback(transactions, ignored -> {
                String code = uniqueCode();
                service.createType(new RecordTypeForm(code.toLowerCase(), " Tên thử nghiệm "), "archivist1");
                var type = service.recordTypes().stream().filter(t -> t.code().equals(code)).findFirst().orElseThrow();
                check(type.name().equals("Tên thử nghiệm"), "type normalization and Unicode preserved");
                service.updateType(type.id(), "Tên mới", false, type.version(), "archivist1");
                check(service.recordTypes().stream().anyMatch(t -> t.id().equals(type.id()) && !t.active()), "type soft deactivation preserves row");
                check(count(jdbc, "catalog_events") == beforeEvents + 2, "type creation and update audited");
            });
            rollback(transactions, ignored -> {
                String code = uniqueCode();
                service.createType(new RecordTypeForm(code, "First"), "archivist1");
                expect(DuplicateKeyException.class, () -> service.createType(new RecordTypeForm(code, "Duplicate"), "archivist1"));
            });
            rollback(transactions, ignored -> {
                expect(CatalogException.class, () -> service.createPolicy(policy(uniqueCode(), 1,
                        LocalDate.of(2050, 2, 2), LocalDate.of(2050, 1, 1)), "archivist1"));
            });
            rollback(transactions, ignored -> {
                String code = uniqueCode();
                service.createPolicy(policy(code, 1, LocalDate.of(2050, 1, 1), null), "archivist1");
                var first = service.policies().stream().filter(p -> p.code().equals(code)).findFirst().orElseThrow();
                check(first.status().equals("DRAFT") && !first.active(), "new policy remains inactive draft");
                service.approve(first.id(), "director1");
                var approved = service.policies().stream().filter(p -> p.id().equals(first.id())).findFirst().orElseThrow();
                check(approved.active() && approved.approvedBy().equals("director1"), "approval records director and activates version");
                service.deactivatePolicy(first.id(), "director1");
                check(service.policies().stream().anyMatch(p -> p.id().equals(first.id()) && !p.active() && p.status().equals("APPROVED")), "deactivation preserves approved policy history");
            });
            rollback(transactions, ignored -> {
                String code = uniqueCode();
                service.createPolicy(policy(code, 1, LocalDate.of(2050, 1, 1), null), "archivist1");
                service.createPolicy(policy(code, 2, LocalDate.of(2051, 1, 1), null), "archivist1");
                var first = service.policies().stream().filter(p -> p.code().equals(code) && p.versionNo() == 1).findFirst().orElseThrow();
                var second = service.policies().stream().filter(p -> p.code().equals(code) && p.versionNo() == 2).findFirst().orElseThrow();
                service.approve(first.id(), "director1");
                expect(CatalogException.class, () -> service.approve(second.id(), "director1"));
            });
            rollback(transactions, ignored -> {
                String code = uniqueCode();
                service.createPolicy(policy(code, 1, LocalDate.of(2050, 1, 1), LocalDate.of(2050, 12, 31)), "archivist1");
                service.createPolicy(policy(code, 2, LocalDate.of(2051, 1, 1), null), "archivist1");
                for (var policy : service.policies().stream().filter(p -> p.code().equals(code)).toList()) service.approve(policy.id(), "director1");
                check(service.policies().stream().filter(p -> p.code().equals(code) && p.active()).count() == 2, "non-overlapping policy versions allowed");
            });
            rollback(transactions, ignored -> {
                var form = policy(uniqueCode(), 1, LocalDate.of(2050, 1, 1), null);
                service.createPolicy(form, "archivist1");
                expect(DuplicateKeyException.class, () -> service.createPolicy(form, "archivist1"));
            });
            expect(DataIntegrityViolationException.class, () -> service.createType(new RecordTypeForm(uniqueCode(), "Rollback audit"), "x".repeat(101)));
            check(count(jdbc, "record_types") == beforeTypes && count(jdbc, "retention_policies") == beforePolicies
                    && count(jdbc, "catalog_events") == beforeEvents, "all checks rolled back, including failed audit transaction");
            if (args.length > 0) {
                httpChecks(jdbc, args[0]);
                check(count(jdbc, "record_types") == beforeTypes && count(jdbc, "retention_policies") == beforePolicies
                        && count(jdbc, "catalog_events") == beforeEvents, "HTTP verification data cleaned up");
            }
            System.out.println("PASSED " + passed + " catalog checks; temporary data removed/rolled back.");
        }
    }

    private static void httpChecks(JdbcTemplate jdbc, String base) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String rawPassword = UUID.randomUUID().toString();
        String passwordHash = new mrm.config.SecurityConfig().passwordEncoder().encode(rawPassword);
        var users = Map.of("reader", "READER", "archivist", "ARCHIVIST", "director", "DIRECTOR", "admin", "ADMIN");
        var browsers = new HashMap<String, HttpClient>();
        String httpTypeCode = "HTTP_TYPE_" + suffix.toUpperCase();
        String httpPolicyCode = "HTTP_POLICY_" + suffix.toUpperCase();
        try {
            for (var item : users.entrySet()) {
                jdbc.update("""
                        INSERT INTO app_users (username, password_hash, display_name, role_id, status)
                        SELECT ?, ?, 'Temporary catalog verification', id, 'ACTIVE' FROM roles WHERE code = ?
                        """, "cat_" + item.getKey() + "_" + suffix, passwordHash, item.getValue());
            }
            var anonymous = client();
            check(get(anonymous, base + "/archivist/record-types").statusCode() == 302, "anonymous redirected to login");
            for (var item : users.entrySet()) {
                var browser = client();
                browsers.put(item.getKey(), browser);
                var login = get(browser, base + "/login");
                check(login.statusCode() == 200, "login page renders for " + item.getKey());
                var match = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(login.body());
                if (!match.find()) throw new AssertionError("CSRF token missing from login form");
                var response = post(browser, base + "/login", "username=" + encode("cat_" + item.getKey() + "_" + suffix)
                        + "&password=" + encode(rawPassword) + "&_csrf=" + encode(match.group(1)));
                check(response.statusCode() == 302 && response.headers().firstValue("location").orElse("").endsWith("/home"), "authentication succeeds for " + item.getKey());
                if (item.getKey().equals("archivist")) {
                    var types = get(browser, base + "/archivist/record-types");
                    check(types.statusCode() == 200 && types.body().contains("INTERNAL_MEDICINE"), "record type template renders list and forms");
                    var policies = get(browser, base + "/archivist/retention-policies");
                    check(policies.statusCode() == 200 && policies.body().contains("retention"), "policy creation template renders");
                    check(get(browser, base + "/director/retention-policies").statusCode() == 403, "archivist cannot access director approval");
                    check(post(browser, base + "/archivist/record-types", "code=INVALID&name=Test").statusCode() == 403, "POST without CSRF rejected");
                    var csrf = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(types.body());
                    if (!csrf.find()) throw new AssertionError("Catalog form CSRF missing");
                    var invalid = post(browser, base + "/archivist/record-types", "code=%21&name=&_csrf=" + encode(csrf.group(1)));
                    check(invalid.statusCode() == 200 && invalid.body().contains("role=\"alert\""), "invalid form redisplays validation errors");
                } else {
                    check(get(browser, base + "/archivist/record-types").statusCode() == 403, item.getKey() + " denied archivist catalog");
                    var policies = get(browser, base + "/director/retention-policies");
                    check(policies.statusCode() == (item.getKey().equals("director") ? 200 : 403), item.getKey() + " director page permissions");
                }
            }
            var archivist = browsers.get("archivist");
            var director = browsers.get("director");
            String token = csrf(get(archivist, base + "/archivist/record-types").body());
            check(post(archivist, base + "/archivist/record-types", "code=" + httpTypeCode + "&name=HTTP+verification&_csrf=" + encode(token)).statusCode() == 302,
                    "valid type form binds and redirects");
            UUID typeId = jdbc.queryForObject("SELECT id FROM record_types WHERE code = ?", UUID.class, httpTypeCode);
            check(post(archivist, base + "/archivist/record-types/" + typeId, "name=HTTP+updated&active=false&version=0&_csrf=" + encode(token)).statusCode() == 302,
                    "type edit form binds and redirects");
            check(!jdbc.queryForObject("SELECT active FROM record_types WHERE id = ?", Boolean.class, typeId), "HTTP type deactivation persisted");
            check(post(archivist, base + "/archivist/record-types/" + typeId, "name=Stale&active=true&version=0&_csrf=" + encode(token)).statusCode() == 302,
                    "stale type form redirects with conflict message");
            check("HTTP updated".equals(jdbc.queryForObject("SELECT name FROM record_types WHERE id = ?", String.class, typeId)), "stale HTTP form cannot overwrite newer type");
            var departments = get(archivist, base + "/archivist/departments");
            check(departments.statusCode() == 200 && !departments.body().contains("departmentForm"), "fixed department page renders without creation form");
            check(post(archivist, base + "/archivist/departments", "code=UNWANTED&name=Test&_csrf=" + encode(token)).statusCode() == 405,
                    "fixed department POST rejected");
            String policyToken = csrf(get(archivist, base + "/archivist/retention-policies").body());
            String policyBody = "code=" + httpPolicyCode + "&name=HTTP+verification&versionNo=1&years=10&effectiveFrom=2050-01-01&effectiveTo=2050-12-31&basis=TEST+ONLY&_csrf=" + encode(policyToken);
            check(post(archivist, base + "/archivist/retention-policies", policyBody).statusCode() == 302, "valid policy form binds dates and redirects");
            UUID policyId = jdbc.queryForObject("SELECT id FROM retention_policies WHERE code = ?", UUID.class, httpPolicyCode);
            var directorPage = get(director, base + "/director/retention-policies");
            check(directorPage.statusCode() == 200 && directorPage.body().contains(httpPolicyCode), "nonempty policy table and approval forms render");
            String directorToken = csrf(directorPage.body());
            check(post(archivist, base + "/director/retention-policies/" + policyId + "/approve", "_csrf=" + encode(policyToken)).statusCode() == 403,
                    "archivist cannot POST director approval");
            check(post(director, base + "/director/retention-policies/" + policyId + "/approve", "_csrf=" + encode(directorToken)).statusCode() == 302,
                    "director approval POST succeeds");
            check(get(director, base + "/director/retention-policies").statusCode() == 200, "approved policy table renders");
            check(post(director, base + "/director/retention-policies/" + policyId + "/deactivate", "_csrf=" + encode(directorToken)).statusCode() == 302,
                    "director deactivation POST succeeds");
        } finally {
            jdbc.update("DELETE FROM catalog_events WHERE target_id IN (SELECT id FROM record_types WHERE code = ?) OR target_id IN (SELECT id FROM retention_policies WHERE code = ?)", httpTypeCode, httpPolicyCode);
            jdbc.update("DELETE FROM retention_policies WHERE code = ?", httpPolicyCode);
            jdbc.update("DELETE FROM record_types WHERE code = ?", httpTypeCode);
            for (String role : users.keySet()) jdbc.update("DELETE FROM app_users WHERE username = ?", "cat_" + role + "_" + suffix);
        }
    }

    private static HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private static HttpResponse<String> post(HttpClient client, String url, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String csrf(String html) {
        var match = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        if (!match.find()) throw new AssertionError("Form CSRF missing");
        return match.group(1);
    }
    private static String uniqueCode() { return "TEST_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(); }
    private static long count(JdbcTemplate jdbc, String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }
    private static RetentionPolicyForm policy(String code, int version, LocalDate from, LocalDate to) {
        return new RetentionPolicyForm(code, "Policy verification", version, 10, from, to, "TEST ONLY; rolled back");
    }
    private static void rollback(TransactionTemplate transactions, Consumer<Void> action) {
        transactions.executeWithoutResult(status -> { try { action.accept(null); } finally { status.setRollbackOnly(); } });
    }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable failure) {
            if (type.isInstance(failure)) { check(true, "expected " + type.getSimpleName()); return; }
            throw failure;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        passed++;
        System.out.println("PASS: " + message);
    }
}
