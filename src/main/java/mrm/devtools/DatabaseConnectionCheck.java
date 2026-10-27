package mrm.devtools;

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