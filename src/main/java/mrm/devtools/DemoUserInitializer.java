package mrm.devtools;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev & seed-users")
public class DemoUserInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public DemoUserInitializer(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            Environment environment
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (seedingDisabled(jdbcTemplate)) {
            System.out.println("Demo seeding disabled after operational data reset.");
            return;
        }
        String[][] accounts = {
                {
                        "reader1",
                        "Reader Demo",
                        "READER",
                        "SEED_READER_PASSWORD"
                },
                {
                        "archivist1",
                        "Archivist Demo",
                        "ARCHIVIST",
                        "SEED_ARCHIVIST_PASSWORD"
                },
                {
                        "director1",
                        "Director Demo",
                        "DIRECTOR",
                        "SEED_DIRECTOR_PASSWORD"
                },
                {
                        "admin1",
                        "Admin Demo",
                        "ADMIN",
                        "SEED_ADMIN_PASSWORD"
                }
        };

        for (String[] account : accounts) {
            String username = account[0];
            String displayName = account[1];
            String roleCode = account[2];
            String passwordVariable = account[3];

            Integer existingCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM app_users WHERE username = ?",
                    Integer.class,
                    username
            );

            if (existingCount != null && existingCount > 0) {
                continue;
            }

            String rawPassword =
                    environment.getProperty(passwordVariable);

            if (rawPassword == null || rawPassword.isBlank()) {
                throw new IllegalStateException(
                        "Missing environment variable: " + passwordVariable
                );
            }

            String passwordHash =
                    passwordEncoder.encode(rawPassword);

            int inserted = jdbcTemplate.update(
                    """
                    INSERT INTO app_users (
                        username,
                        password_hash,
                        display_name,
                        role_id,
                        status
                    )
                    SELECT ?, ?, ?, id, 'ACTIVE'
                    FROM roles
                    WHERE code = ?
                    ON CONFLICT (username) DO NOTHING
                    """,
                    username,
                    passwordHash,
                    displayName,
                    roleCode
            );

            if (inserted == 0) {
                throw new IllegalStateException(
                        "Could not create demo account: " + username
                );
            }
        }

        System.out.println(
                "Demo accounts initialized; existing accounts were preserved."
        );
    }

    public static boolean seedingDisabled(JdbcTemplate jdbc) {
        Boolean exists = jdbc.queryForObject("SELECT to_regclass('data_reset_history') IS NOT NULL", Boolean.class);
        return Boolean.TRUE.equals(exists) && Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM data_reset_history WHERE reset_key='OPERATIONAL_BASELINE')", Boolean.class));
    }
}
