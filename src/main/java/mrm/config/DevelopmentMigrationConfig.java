package mrm.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Bootstrap the V4 prerequisite before migrating a fresh development database. */
@Configuration
@Profile("dev & seed-users")
public class DevelopmentMigrationConfig {
    @Bean
    FlywayMigrationStrategy developmentMigrationStrategy(Environment environment, PasswordEncoder encoder) {
        return flyway -> {
            var current = flyway.info().current();
            if (current == null || current.getVersion().compareTo(org.flywaydb.core.api.MigrationVersion.fromVersion("3")) < 0) {
                Flyway.configure().configuration(flyway.getConfiguration()).target("3").load().migrate();
            }
            var jdbc = new JdbcTemplate(flyway.getConfiguration().getDataSource());
            if (mrm.devtools.DemoUserInitializer.seedingDisabled(jdbc)) {
                flyway.migrate();
                return;
            }
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM app_users WHERE username = 'archivist1'", Integer.class);
            if (count != null && count == 0) {
                String password = environment.getProperty("SEED_ARCHIVIST_PASSWORD");
                if (password == null || password.isBlank()) throw new IllegalStateException("Fresh development database requires SEED_ARCHIVIST_PASSWORD before V4.");
                jdbc.update("""
                        INSERT INTO app_users (username, password_hash, display_name, role_id, status)
                        SELECT 'archivist1', ?, 'Archivist Demo', id, 'ACTIVE' FROM roles WHERE code = 'ARCHIVIST'
                        ON CONFLICT (username) DO NOTHING
                        """, encoder.encode(password));
            }
            flyway.migrate();
            // A fresh Archivist is inserted before V4, so V23 sees it as an existing account.
            // Mark only that just-bootstrapped account as first-login after all migrations finish.
            if (count != null && count == 0) {
                jdbc.update("UPDATE app_users SET must_change_password=TRUE WHERE username='archivist1'");
            }
        };
    }
}
