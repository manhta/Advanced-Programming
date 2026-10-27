package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.util.zip.CRC32;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V1__CreateAuthenticationTables extends BaseJavaMigration {

    private static final String CREATE_ROLES = """
            CREATE TABLE roles (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                code VARCHAR(32) NOT NULL UNIQUE,
                name VARCHAR(100) NOT NULL,
                CONSTRAINT ck_roles_code CHECK (
                    code IN ('READER', 'ARCHIVIST', 'DIRECTOR', 'ADMIN')
                )
            )
            """;

    private static final String CREATE_USERS = """
            CREATE TABLE app_users (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                username VARCHAR(64) NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                display_name VARCHAR(200) NOT NULL,
                role_id UUID NOT NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_app_users_role
                    FOREIGN KEY (role_id) REFERENCES roles(id),
                CONSTRAINT ck_app_users_status CHECK (
                    status IN ('ACTIVE', 'LOCKED', 'DISABLED')
                ),
                CONSTRAINT ck_app_users_username CHECK (
                    username ~ '^[a-z0-9._-]{3,64}$'
                )
            )
            """;

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute(CREATE_ROLES);
            statement.execute(CREATE_USERS);
        }
    }

    @Override
    public Integer getChecksum() {
        CRC32 checksum = new CRC32();
        checksum.update((CREATE_ROLES + "\n" + CREATE_USERS)
                .getBytes(StandardCharsets.UTF_8));
        return (int) checksum.getValue();
    }
}
