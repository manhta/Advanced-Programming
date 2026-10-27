package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.util.zip.CRC32;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V3__CreateDepartments extends BaseJavaMigration {

    private static final String CREATE_DEPARTMENTS = """
            CREATE TABLE departments (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                code VARCHAR(32) NOT NULL,
                name VARCHAR(200) NOT NULL,

                active BOOLEAN NOT NULL DEFAULT TRUE,

                created_by UUID NOT NULL,
                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                CONSTRAINT uq_departments_code UNIQUE (code),

                CONSTRAINT ck_departments_code CHECK (
                    code ~ '^[A-Z0-9_-]{2,32}$'
                ),

                CONSTRAINT ck_departments_name CHECK (
                    length(btrim(name)) > 0
                ),

                CONSTRAINT fk_departments_created_by
                    FOREIGN KEY (created_by)
                    REFERENCES app_users(id)
            )
            """;

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement =
                     context.getConnection().createStatement()) {

            statement.execute(CREATE_DEPARTMENTS);
        }
    }

    @Override
    public Integer getChecksum() {
        CRC32 checksum = new CRC32();

        checksum.update(
                CREATE_DEPARTMENTS.getBytes(StandardCharsets.UTF_8)
        );

        return (int) checksum.getValue();
    }
}