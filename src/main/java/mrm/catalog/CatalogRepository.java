package mrm.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {
    private final JdbcTemplate jdbc;
    public CatalogRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<RecordType> recordTypes() {
        return jdbc.query("SELECT id, code, name, active, version FROM record_types ORDER BY code",
                (rs, n) -> new RecordType(rs.getObject("id", UUID.class), rs.getString("code"),
                        rs.getString("name"), rs.getBoolean("active"), rs.getLong("version")));
    }

    public List<RetentionPolicy> policies() {
        return jdbc.query("""
                SELECT p.*, creator.username AS creator_name, approver.username AS approver_name
                FROM retention_policies p JOIN app_users creator ON creator.id = p.created_by
                LEFT JOIN app_users approver ON approver.id = p.approved_by
                ORDER BY p.code, p.version_no DESC
                """, (rs, n) -> new RetentionPolicy(rs.getObject("id", UUID.class), rs.getString("code"),
                rs.getString("name"), rs.getInt("version_no"), rs.getInt("years"),
                rs.getObject("effective_from", java.time.LocalDate.class),
                rs.getObject("effective_to", java.time.LocalDate.class), rs.getString("basis"),
                rs.getString("status"), rs.getBoolean("active"), rs.getString("creator_name"), rs.getString("approver_name")));
    }

    public UUID createType(RecordTypeForm form, String username) {
        return jdbc.queryForObject("""
                INSERT INTO record_types (code, name, created_by)
                VALUES (?, ?, (SELECT id FROM app_users WHERE username = ?)) RETURNING id
                """, UUID.class, form.code(), form.name(), username);
    }

    public UUID createPolicy(RetentionPolicyForm form, String username) {
        return jdbc.queryForObject("""
                INSERT INTO retention_policies (code, name, version_no, years, effective_from, effective_to, basis, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, (SELECT id FROM app_users WHERE username = ?)) RETURNING id
                """, UUID.class, form.code(), form.name(), form.versionNo(), form.years(),
                form.effectiveFrom(), form.effectiveTo(), form.basis(), username);
    }

    public RecordType lockType(UUID id) {
        var rows = jdbc.query("SELECT id, code, name, active, version FROM record_types WHERE id = ? FOR UPDATE",
                (rs, n) -> new RecordType(rs.getObject("id", UUID.class), rs.getString("code"),
                        rs.getString("name"), rs.getBoolean("active"), rs.getLong("version")), id);
        if (rows.isEmpty()) throw new CatalogException("Loại bệnh án không còn tồn tại.");
        return rows.getFirst();
    }

    public void updateType(UUID id, String name, boolean active, long version) {
        int updated = jdbc.update("UPDATE record_types SET name = ?, active = ?, version = version + 1 WHERE id = ? AND version = ?", name, active, id, version);
        if (updated != 1) throw new CatalogException("Loại bệnh án đã được người khác thay đổi. Vui lòng tải lại trang.");
    }

    public RetentionPolicy lockPolicy(UUID id) {
        var codes = jdbc.queryForList("SELECT code FROM retention_policies WHERE id = ?", String.class, id);
        if (codes.isEmpty()) throw new CatalogException("Chính sách không còn tồn tại.");
        // Serialize approvals of different versions of the same policy code.
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> { }, "retention:" + codes.getFirst());
        jdbc.queryForList("SELECT id FROM retention_policies WHERE id = ? FOR UPDATE", UUID.class, id);
        return policies().stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }

    public boolean overlaps(RetentionPolicy policy) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM retention_policies
                WHERE code = ? AND id <> ? AND status = 'APPROVED' AND active
                AND effective_from <= COALESCE(CAST(? AS DATE), 'infinity'::date)
                AND COALESCE(effective_to, 'infinity'::date) >= CAST(? AS DATE))
                """, Boolean.class, policy.code(), policy.id(), policy.effectiveTo(), policy.effectiveFrom()));
    }

    public void approve(UUID id, String username) {
        jdbc.update("""
                UPDATE retention_policies SET status = 'APPROVED', active = TRUE,
                approved_by = (SELECT id FROM app_users WHERE username = ?), approved_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, username, id);
    }

    public void deactivatePolicy(UUID id) {
        jdbc.update("UPDATE retention_policies SET active = FALSE WHERE id = ?", id);
    }

    public void event(String type, UUID id, String action, String username, String details) {
        jdbc.update("INSERT INTO catalog_events (target_type, target_id, action, actor_username, details) VALUES (?, ?, ?, ?, ?)",
                type, id, action, username, details);
    }
}
