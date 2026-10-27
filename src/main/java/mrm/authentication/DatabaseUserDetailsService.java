package mrm.authentication;

import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        String normalizedUsername =
                username.strip().toLowerCase(Locale.ROOT);

        String sql = """
                SELECT
                    u.username,
                    u.password_hash,
                    u.status,
                    u.deleted_at,
                    r.code AS role_code
                FROM app_users u
                JOIN roles r ON r.id = u.role_id
                WHERE u.username = ?
                """;

        List<UserDetails> users = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    String status = rs.getString("status");

                    return User.withUsername(rs.getString("username"))
                            .password(rs.getString("password_hash"))
                            .roles(rs.getString("role_code"))
                            .disabled("DISABLED".equals(status) || rs.getTimestamp("deleted_at") != null)
                            .accountLocked("LOCKED".equals(status))
                            .build();
                },
                normalizedUsername
        );

        if (users.isEmpty()) {
            throw new UsernameNotFoundException(
                    "Invalid login information"
            );
        }

        return users.getFirst();
    }
}
