package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V6__CompleteReferenceCatalogs extends BaseJavaMigration {
    private static final String SQL = """
            ALTER TABLE record_types ADD COLUMN version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0);
            CREATE FUNCTION protect_department_catalog() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN
                RAISE EXCEPTION 'Department catalog is fixed for the learning project' USING ERRCODE = '23514';
            END;
            $$;
            CREATE TRIGGER departments_fixed_catalog BEFORE INSERT OR UPDATE OR DELETE ON departments
                FOR EACH ROW EXECUTE FUNCTION protect_department_catalog();
            INSERT INTO retention_policies (code, name, version_no, years, effective_from, basis, created_by)
            SELECT source.code, source.name, 1, source.years, DATE '2026-10-20',
                'MÔ PHỎNG HỌC TẬP. Theo tài liệu QT.01.KHNV Khánh Sơn tháng 11/2024 và phương án người dùng xác nhận 20/10/2026. '
                || 'Không xác nhận pháp luật hiện hành. Tính từ ngày đầu lưu kho; 29/2 về 28/2 ở năm đích; '
                || 'nhiều nhóm lấy dài nhất. Nguồn: https://trungtamytekhanhson.khanhhoa.gov.vn/wp-content/uploads/2024/11/quy-trinh-tiep-nhan-benh-an.pdf',
                owner.id
            FROM (VALUES
                ('GENERAL', 'Hồ sơ thông thường — mô phỏng', 10),
                ('ACCIDENT', 'Hồ sơ tai nạn — mô phỏng', 15),
                ('PSYCHIATRIC', 'Hồ sơ tâm thần — mô phỏng', 20),
                ('DECEASED', 'Hồ sơ tử vong — mô phỏng', 30)
            ) AS source(code, name, years)
            CROSS JOIN app_users owner WHERE owner.username = 'archivist1';
            INSERT INTO catalog_events (target_type, target_id, action, actor_username, details)
                SELECT 'RETENTION_POLICY', id, 'CREATE_DRAFT', 'SYSTEM:V6',
                    'Learning-project defaults confirmed by user; pending Director catalog confirmation: ' || code
                FROM retention_policies WHERE code IN ('GENERAL', 'ACCIDENT', 'PSYCHIATRIC', 'DECEASED') AND version_no = 1;
            """;

    @Override public void migrate(Context context) throws Exception {
        try (var statement = context.getConnection().createStatement()) { statement.execute(SQL); }
    }
    @Override public Integer getChecksum() {
        var checksum = new CRC32(); checksum.update(SQL.getBytes(StandardCharsets.UTF_8)); return (int) checksum.getValue();
    }
}
