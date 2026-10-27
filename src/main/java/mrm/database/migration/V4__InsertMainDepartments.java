package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.zip.CRC32;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V4__InsertMainDepartments extends BaseJavaMigration {

    // Bootstrap catalog ownership; this is not an interactive creation event.
    private static final String CREATOR_USERNAME = "archivist1";

    private static final String FIND_CREATOR = """
            SELECT u.id
            FROM app_users u
            JOIN roles r ON r.id = u.role_id
            WHERE u.username = ?
                AND u.status = 'ACTIVE'
                AND r.code = 'ARCHIVIST'
            """;

    private static final String INSERT_DEPARTMENT = """
            INSERT INTO departments (code, name, active, created_by)
            SELECT ?, ?, TRUE, ?
            WHERE NOT EXISTS (
                SELECT 1 FROM departments
                WHERE upper(btrim(code)) = ?
                    OR lower(btrim(name)) = lower(?)
            )
            ON CONFLICT (code) DO NOTHING
            """;

    private static final String[][] DEPARTMENTS = {
            {"INTERNAL_MEDICINE", "Nội tổng hợp - Y học cổ truyền - Phục hồi chức năng"},
            {"GENERAL_SURGERY", "Ngoại tổng hợp - Liên chuyên khoa"},
            {"PEDIATRICS_RH", "Nhi - Chăm sóc sức khỏe sinh sản"},
            {"EMERGENCY", "Khám bệnh - Cấp cứu - Hồi sức tích cực - Chống độc"},
            {"INFECTIOUS_DISEASES", "Truyền nhiễm"}
    };

    @Override
    public void migrate(Context context) throws Exception {
        UUID creatorId;
        try (PreparedStatement statement = context.getConnection().prepareStatement(FIND_CREATOR)) {
            statement.setString(1, CREATOR_USERNAME);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException(
                            "Main department initialization requires the active ARCHIVIST account: "
                                    + CREATOR_USERNAME
                    );
                }
                creatorId = result.getObject("id", UUID.class);
            }
        }

        try (PreparedStatement statement = context.getConnection().prepareStatement(INSERT_DEPARTMENT)) {
            for (String[] department : DEPARTMENTS) {
                statement.setString(1, department[0]);
                statement.setString(2, department[1]);
                statement.setObject(3, creatorId);
                statement.setString(4, department[0]);
                statement.setString(5, department[1]);
                statement.executeUpdate();
            }
        }
    }

    @Override
    public Integer getChecksum() {
        CRC32 checksum = new CRC32();
        updateChecksum(checksum, FIND_CREATOR);
        updateChecksum(checksum, INSERT_DEPARTMENT);
        updateChecksum(checksum, CREATOR_USERNAME);
        for (String[] department : DEPARTMENTS) {
            updateChecksum(checksum, department[0]);
            updateChecksum(checksum, department[1]);
        }
        return (int) checksum.getValue();
    }

    private static void updateChecksum(CRC32 checksum, String value) {
        checksum.update(value.getBytes(StandardCharsets.UTF_8));
        checksum.update(0);
    }
}
