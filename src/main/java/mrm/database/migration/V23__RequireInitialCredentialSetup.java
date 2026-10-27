package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Existing accounts retain their state; newly provisioned accounts must replace their initial password. */
public class V23__RequireInitialCredentialSetup extends BaseJavaMigration {
    private static final String SQL = """
        ALTER TABLE app_users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
        ALTER TABLE app_users ALTER COLUMN must_change_password SET DEFAULT TRUE;
        """;
    @Override public void migrate(Context context) throws Exception {
        try (var statement = context.getConnection().createStatement()) { statement.execute(SQL); }
    }
    @Override public Integer getChecksum() {
        var crc = new CRC32(); crc.update(SQL.getBytes(StandardCharsets.UTF_8)); return (int) crc.getValue();
    }
}
