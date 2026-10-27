package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds a reversible archival marker while preserving account references in clinical history. */
public class V26__AddAccountArchival extends BaseJavaMigration {
    private static final String SQL = """
        ALTER TABLE app_users ADD COLUMN deleted_at TIMESTAMPTZ;
        CREATE INDEX ix_app_users_visible_accounts ON app_users(username) WHERE deleted_at IS NULL;
        COMMENT ON COLUMN app_users.deleted_at IS 'Account soft-deletion timestamp; preserves user identity for historical foreign-key references.';
        """;

    @Override
    public void migrate(Context context) throws Exception {
        try (var statement = context.getConnection().createStatement()) {
            statement.execute(SQL);
        }
    }

    @Override
    public Integer getChecksum() {
        var crc = new CRC32();
        crc.update(SQL.getBytes(StandardCharsets.UTF_8));
        return (int) crc.getValue();
    }
}
