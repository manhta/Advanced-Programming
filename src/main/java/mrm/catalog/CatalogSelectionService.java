package mrm.catalog;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogSelectionService {
    private final JdbcTemplate jdbc;
    public CatalogSelectionService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<RecordType> activeRecordTypes() {
        return jdbc.query("SELECT id, code, name, active, version FROM record_types WHERE active ORDER BY code",
                (rs, n) -> new RecordType(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"), true, rs.getLong("version")));
    }

    public List<UUID> eligiblePolicyIds(LocalDate date) {
        if (date == null) throw new CatalogException("Cần ngày đầu lưu kho để chọn chính sách.");
        return jdbc.queryForList("""
                SELECT id FROM retention_policies WHERE status = 'APPROVED' AND active
                AND effective_from <= ? AND (effective_to IS NULL OR effective_to >= ?) ORDER BY code, version_no
                """, UUID.class, date, date);
    }

    @Transactional
    public CatalogSnapshot select(UUID departmentId, UUID recordTypeId, List<UUID> policyIds, LocalDate firstStoredOn) {
        if (departmentId == null || recordTypeId == null || firstStoredOn == null || policyIds == null || policyIds.isEmpty()
                || policyIds.stream().anyMatch(java.util.Objects::isNull)) throw new CatalogException("Cần khoa, loại hồ sơ, chính sách và ngày đầu lưu kho.");
        var department = jdbc.queryForList("SELECT name FROM departments WHERE id = ? AND active FOR SHARE", String.class, departmentId);
        var type = jdbc.query("SELECT code, name FROM record_types WHERE id = ? AND active FOR SHARE",
                (rs, n) -> List.of(rs.getString("code"), rs.getString("name")), recordTypeId);
        if (department.isEmpty() || type.isEmpty()) throw new CatalogException("Khoa hoặc loại hồ sơ không tồn tại/đã ngừng hoạt động.");
        var snapshots = new ArrayList<CatalogSnapshot.PolicySnapshot>();
        for (UUID id : policyIds.stream().distinct().sorted().toList()) {
            var policies = jdbc.query("""
                    SELECT * FROM retention_policies WHERE id = ? AND status = 'APPROVED' AND active
                    AND effective_from <= ? AND (effective_to IS NULL OR effective_to >= ?) FOR SHARE
                    """, (rs, n) -> new CatalogSnapshot.PolicySnapshot(rs.getObject("id", UUID.class), rs.getString("code"),
                    rs.getString("name"), rs.getInt("version_no"), rs.getInt("years"),
                    rs.getObject("effective_from", LocalDate.class), rs.getObject("effective_to", LocalDate.class), rs.getString("basis")),
                    id, firstStoredOn, firstStoredOn);
            if (policies.isEmpty()) throw new CatalogException("Chính sách không tồn tại, chưa xác nhận, ngừng dùng hoặc ngoài ngày hiệu lực.");
            snapshots.add(policies.getFirst());
        }
        int years = snapshots.stream().mapToInt(CatalogSnapshot.PolicySnapshot::years).max().orElseThrow();
        LocalDate dueOn;
        try {
            dueOn = firstStoredOn.plusYears(years);
            if (firstStoredOn.getMonthValue() == 2 && firstStoredOn.getDayOfMonth() == 29) {
                dueOn = dueOn.withDayOfMonth(28);
            }
        }
        catch (DateTimeException ex) { throw new CatalogException("Ngày tính hạn nằm ngoài phạm vi hỗ trợ."); }
        return new CatalogSnapshot(departmentId, department.getFirst(), recordTypeId,
                type.getFirst().get(0), type.getFirst().get(1), snapshots, years, firstStoredOn, dueOn);
    }
}
