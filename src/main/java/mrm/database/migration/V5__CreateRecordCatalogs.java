package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V5__CreateRecordCatalogs extends BaseJavaMigration {
    private static final String SQL = """
            CREATE TABLE record_types (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                code VARCHAR(32) NOT NULL UNIQUE CHECK (code ~ '^[A-Z0-9_-]{2,32}$'),
                name VARCHAR(200) NOT NULL CHECK (length(btrim(name)) > 0),
                active BOOLEAN NOT NULL DEFAULT TRUE,
                created_by UUID REFERENCES app_users(id),
                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            CREATE TABLE retention_policies (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                code VARCHAR(32) NOT NULL CHECK (code ~ '^[A-Z0-9_-]{2,32}$'),
                name VARCHAR(200) NOT NULL CHECK (length(btrim(name)) > 0),
                version_no INTEGER NOT NULL CHECK (version_no > 0),
                years SMALLINT NOT NULL CHECK (years > 0),
                effective_from DATE NOT NULL,
                effective_to DATE,
                basis VARCHAR(2000) NOT NULL CHECK (length(btrim(basis)) > 0),
                status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'APPROVED')),
                active BOOLEAN NOT NULL DEFAULT FALSE,
                created_by UUID NOT NULL REFERENCES app_users(id),
                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                approved_by UUID REFERENCES app_users(id),
                approved_at TIMESTAMPTZ,
                UNIQUE (code, version_no),
                CHECK (effective_to IS NULL OR effective_to >= effective_from),
                CHECK (NOT active OR status = 'APPROVED'),
                CHECK ((status = 'DRAFT' AND approved_by IS NULL AND approved_at IS NULL)
                    OR (status = 'APPROVED' AND approved_by IS NOT NULL AND approved_at IS NOT NULL))
            );
            CREATE TABLE catalog_events (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                target_type VARCHAR(32) NOT NULL,
                target_id UUID NOT NULL,
                action VARCHAR(32) NOT NULL,
                actor_username VARCHAR(100) NOT NULL,
                details TEXT NOT NULL,
                occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            CREATE INDEX ix_catalog_events_target ON catalog_events(target_type, target_id, occurred_at);
            INSERT INTO record_types (code, name) VALUES
                ('INTERNAL_MEDICINE', 'Bệnh án Nội khoa'),
                ('SURGERY', 'Bệnh án Ngoại khoa'),
                ('PEDIATRICS', 'Bệnh án Nhi khoa'),
                ('INFECTIOUS', 'Bệnh án Truyền nhiễm'),
                ('OBSTETRICS', 'Bệnh án Sản khoa'),
                ('GYNECOLOGY', 'Bệnh án Phụ khoa');
            INSERT INTO catalog_events (target_type, target_id, action, actor_username, details)
                SELECT 'RECORD_TYPE', id, 'CREATE', 'SYSTEM:V5', code || ': ' || name
                FROM record_types;
            """;

    @Override
    public void migrate(Context context) throws Exception {
        try (var statement = context.getConnection().createStatement()) {
            statement.execute(SQL);
        }
    }

    @Override
    public Integer getChecksum() {
        var checksum = new CRC32();
        checksum.update(SQL.getBytes(StandardCharsets.UTF_8));
        return (int) checksum.getValue();
    }
}
