package mrm.catalog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

/** Extended P2 checks; uses rollback plus an isolated, disposable schema for fresh boot. */
public class CompleteCatalogCheck {
    private static int passed;
    public static void main(String[] args) throws Exception {
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isBlank()) password = Files.readString(Path.of(".secrets/db-password.txt")).replaceFirst("[\\r\\n]+$", "");
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://127.0.0.1:5433/mrm");
        String user = System.getenv().getOrDefault("DB_USERNAME", "postgres");
        var ds = new DriverManagerDataSource(url, user, password);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DataSource.class, () -> ds);
            context.registerBean(JdbcTemplate.class, () -> new JdbcTemplate(ds));
            context.registerBean(DataSourceTransactionManager.class, () -> new DataSourceTransactionManager(ds));
            context.register(CatalogIntegrationCheck.TransactionConfig.class, CatalogRepository.class, CatalogService.class, CatalogSelectionService.class);
            context.refresh();
            var jdbc = context.getBean(JdbcTemplate.class);
            var service = context.getBean(CatalogService.class);
            var selection = context.getBean(CatalogSelectionService.class);
            var tx = new TransactionTemplate(context.getBean(DataSourceTransactionManager.class));
            check(jdbc.queryForObject("SELECT COUNT(*) FROM departments", Integer.class) == 5, "five fixed departments");
            check(jdbc.queryForObject("SELECT COUNT(*) FROM retention_policies WHERE code IN ('GENERAL','ACCIDENT','PSYCHIATRIC','DECEASED') AND version_no=1", Integer.class) == 4,
                    "four simulation policies exist; preserve user approval state");
            for (String mutation : List.of("INSERT INTO departments(code,name,created_by) SELECT 'TEST_BLOCK','Test',id FROM app_users WHERE username='archivist1'",
                    "UPDATE departments SET name=name WHERE code='EMERGENCY'", "DELETE FROM departments WHERE code='EMERGENCY'")) {
                expect(DataIntegrityViolationException.class, () -> tx.executeWithoutResult(status -> jdbc.update(mutation)));
            }
            tx.executeWithoutResult(status -> {
                try {
                    UUID department = jdbc.queryForObject("SELECT id FROM departments WHERE code='INTERNAL_MEDICINE'", UUID.class);
                    String code = "P2_" + UUID.randomUUID().toString().replace("-", "").substring(0,12).toUpperCase();
                    service.createType(new RecordTypeForm(code, "Snapshot type"), "archivist1");
                    var type = service.recordTypes().stream().filter(t -> t.code().equals(code)).findFirst().orElseThrow();
                    for (int years : List.of(10, 15, 20, 30)) service.createPolicy(new RetentionPolicyForm(code + "_" + years, "Simulation", 1, years,
                            LocalDate.of(2026,10,20), null, "TEST; rolled back"), "archivist1");
                    var policies = service.policies().stream().filter(p -> p.code().startsWith(code + "_")).toList();
                    expect(CatalogException.class, () -> selection.select(department, type.id(), List.of(policies.getFirst().id()), LocalDate.of(2028,2,29)));
                    for (var policy : policies) service.approve(policy.id(), "director1");
                    var ids = policies.stream().map(RetentionPolicy::id).toList();
                    var snapshot = selection.select(department, type.id(), ids, LocalDate.of(2028,2,29));
                    check(snapshot.retentionYears() == 30, "longest applicable retention group selected");
                    check(snapshot.dueOn().equals(LocalDate.of(2058,2,28)), "leap day adjusts to February 28");
                    var twentyYears = policies.stream().filter(p -> p.years() == 20).findFirst().orElseThrow();
                    check(selection.select(department, type.id(), List.of(twentyYears.id()), LocalDate.of(2028,2,29)).dueOn().equals(LocalDate.of(2048,2,28)),
                            "agreed February 28 rule also applies when target year is leap");
                    check(selection.eligiblePolicyIds(LocalDate.of(2028,2,29)).containsAll(ids), "eligible approved policy lookup");
                    check(!selection.eligiblePolicyIds(LocalDate.of(2020,1,1)).contains(ids.getFirst()), "policies not yet effective excluded");
                    service.updateType(type.id(), "Changed", false, type.version(), "archivist1");
                    for (var policy : policies) service.deactivatePolicy(policy.id(), "director1");
                    check(snapshot.recordTypeName().equals("Snapshot type") && snapshot.retentionYears() == 30 && snapshot.policies().size() == 4,
                            "snapshot unchanged after catalog deactivation/rename");
                    expect(UnsupportedOperationException.class, () -> snapshot.policies().clear());
                    check(selection.activeRecordTypes().stream().noneMatch(t -> t.id().equals(type.id())), "inactive type excluded from new choices");
                    expect(CatalogException.class, () -> selection.select(department, type.id(), ids, LocalDate.of(2028,2,29)));
                    expect(CatalogException.class, () -> service.updateType(type.id(), "Stale overwrite", true, type.version(), "archivist1"));
                    expect(CatalogException.class, () -> selection.select(UUID.randomUUID(), UUID.randomUUID(), ids, LocalDate.of(2028,2,29)));
                } finally { status.setRollbackOnly(); }
            });
            concurrency(jdbc, service);
            bootstrap(jdbc, url, user, password);
        }
        System.out.println("PASSED " + passed + " extended P2 checks.");
    }

    private static void concurrency(JdbcTemplate jdbc, CatalogService service) throws Exception {
        String code = "RACE_" + UUID.randomUUID().toString().replace("-", "").substring(0,12).toUpperCase();
        UUID id = null;
        try {
            service.createType(new RecordTypeForm(code, "Race"), "archivist1");
            var type = service.recordTypes().stream().filter(t -> t.code().equals(code)).findFirst().orElseThrow();
            id = type.id();
            var start = new CountDownLatch(1);
            var successes = new AtomicInteger(); var conflicts = new AtomicInteger();
            try (var executor = Executors.newFixedThreadPool(2)) {
                var tasks = java.util.stream.IntStream.range(0,2).mapToObj(n -> executor.submit(() -> {
                    start.await();
                    try { service.updateType(type.id(), "Writer " + n, true, type.version(), "archivist1"); successes.incrementAndGet(); }
                    catch (CatalogException ex) { conflicts.incrementAndGet(); }
                    return null;
                })).toList();
                start.countDown();
                for (var task : tasks) task.get(20, java.util.concurrent.TimeUnit.SECONDS);
            }
            check(successes.get() == 1 && conflicts.get() == 1, "two concurrent edits: one success, one stale conflict");
            check(jdbc.queryForObject("SELECT version FROM record_types WHERE id=?", Long.class, id) == 1L, "version incremented exactly once");
        } finally {
            if (id != null) { jdbc.update("DELETE FROM catalog_events WHERE target_id=?", id); jdbc.update("DELETE FROM record_types WHERE id=?", id); }
        }
    }

    private static void bootstrap(JdbcTemplate root, String url, String user, String password) {
        String schema = "p2_bootstrap_" + UUID.randomUUID().toString().replace("-", "");
        if (!schema.matches("p2_bootstrap_[a-f0-9]{32}")) throw new IllegalStateException("Unsafe test schema");
        String isolatedUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        String secret = UUID.randomUUID().toString();
        root.execute("CREATE SCHEMA " + schema);
        try {
            var app = new SpringApplication(mrm.MedicalRecordApplication.class);
            app.setAdditionalProfiles("dev", "seed-users");
            app.addInitializers(context -> context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolatedP2Verification", Map.of(
                    "spring.datasource.url", isolatedUrl, "spring.datasource.username", user, "spring.datasource.password", password,
                    "spring.flyway.default-schema", schema, "spring.flyway.schemas", schema, "server.port", "0",
                    "SEED_READER_PASSWORD", secret, "SEED_ARCHIVIST_PASSWORD", secret,
                    "SEED_DIRECTOR_PASSWORD", secret, "SEED_ADMIN_PASSWORD", secret))));
            try (var boot = app.run()) {
                var jdbc = boot.getBean(JdbcTemplate.class);
                check(schema.equals(jdbc.queryForObject("SELECT current_schema()", String.class)), "fresh boot confined to disposable schema");
                check("21".equals(jdbc.queryForObject("SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1", String.class)), "fresh V1-V21 migrations succeed");
                check(jdbc.queryForObject("SELECT COUNT(*) FROM app_users", Integer.class) == 4, "four demo accounts bootstrapped with supplied passwords");
                check(jdbc.queryForObject("SELECT COUNT(*) FROM departments", Integer.class) == 5, "fresh schema has five departments");
                check(jdbc.queryForObject("SELECT COUNT(*) FROM record_types", Integer.class) == 6, "fresh schema has six record types");
                check(jdbc.queryForObject("SELECT COUNT(*) FROM retention_policies", Integer.class) == 4, "fresh schema has four simulation drafts");
            }
        } finally { root.execute("DROP SCHEMA " + schema + " CASCADE"); }
        check(root.queryForObject("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name=?", Integer.class, schema) == 0, "disposable schema removed");
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
        passed++; System.out.println("PASS: " + message);
    }
}
