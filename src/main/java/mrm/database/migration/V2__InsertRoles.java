package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.util.zip.CRC32;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2__InsertRoles extends BaseJavaMigration {

    private static final String INSERT_ROLES = """
            INSERT INTO roles (code, name)
            VALUES
                ('READER', 'Người đọc'),
                ('ARCHIVIST', 'Nhân viên lưu trữ'),
                ('DIRECTOR', 'Giám đốc'),
                ('ADMIN', 'Quản trị hệ thống')
            """;

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.executeUpdate(INSERT_ROLES);
        }
    }

    @Override
    public Integer getChecksum() {
        CRC32 checksum = new CRC32();
        checksum.update(INSERT_ROLES.getBytes(StandardCharsets.UTF_8));
        return (int) checksum.getValue();
    }
}
