package mrm.department;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DepartmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public DepartmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Department> findAll() {
        String sql = """
                SELECT
                    d.id,
                    d.code,
                    d.name,
                    d.active,
                    u.username AS created_by_username,
                    d.created_at
                FROM departments d
                JOIN app_users u ON u.id = d.created_by
                ORDER BY d.code, d.id
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new Department(
                        rs.getObject("id", UUID.class),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getBoolean("active"),
                        rs.getString("created_by_username"),
                        rs.getTimestamp("created_at").toInstant()
                )
        );
    }

    public void insert(String code, String name, String creatorUsername) {
        String sql = """
                INSERT INTO departments (code, name, created_by)
                VALUES (
                    ?,
                    ?,
                    (SELECT id FROM app_users WHERE username = ?)
                )
                """;

        jdbcTemplate.update(sql, code, name, creatorUsername);
    }
}
