package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Preserve existing usernames and allow the hospital's email-shaped account convention. */
public class V22__AllowEmailUsernames extends BaseJavaMigration {
    private static final String SQL = """
        ALTER TABLE app_users DROP CONSTRAINT ck_app_users_username;
        ALTER TABLE app_users ADD CONSTRAINT ck_app_users_username CHECK (
            length(username) BETWEEN 3 AND 64 AND
            (username ~ '^[a-z0-9._-]{3,64}$' OR username ~ '^[a-z0-9._-]+@[a-z0-9.-]+[.][a-z]{2,}$')
        );
        """;
    @Override public void migrate(Context context) throws Exception {
        try (var statement = context.getConnection().createStatement()) { statement.execute(SQL); }
    }
    @Override public Integer getChecksum() {
        var crc = new CRC32(); crc.update(SQL.getBytes(StandardCharsets.UTF_8)); return (int) crc.getValue();
    }
}
