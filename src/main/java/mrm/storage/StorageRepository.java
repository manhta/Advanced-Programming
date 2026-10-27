package mrm.storage;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import mrm.common.BusinessException;

@Repository
public class StorageRepository {
    private final JdbcTemplate jdbc;
    public StorageRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void lockTree() { jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended('mrm:storage',0))", rs -> { }); }

    public List<StorageLocation> findAll() {
        return jdbc.query("""
                WITH RECURSIVE tree AS (
                    SELECT l.*, (l.code || ' · ' || l.name)::TEXT AS path FROM storage_locations l WHERE parent_id IS NULL
                    UNION ALL SELECT child.*, tree.path || ' / ' || child.code || ' · ' || child.name
                    FROM storage_locations child JOIN tree ON child.parent_id=tree.id
                ) SELECT tree.*, (SELECT COUNT(*) FROM medical_records r WHERE r.current_location_id=tree.id AND r.record_state='STORED') AS occupied
                FROM tree ORDER BY path
                """, (rs,n) -> new StorageLocation(rs.getObject("id", UUID.class), rs.getObject("parent_id", UUID.class),
                rs.getString("code"), rs.getString("name"), rs.getString("type"), rs.getBoolean("active"),
                rs.getObject("capacity", Integer.class), rs.getLong("version"), rs.getString("path"), rs.getLong("occupied")));
    }
    public StorageLocation get(UUID id) {
        return findAll().stream().filter(l -> l.id().equals(id)).findFirst().orElseThrow(() -> new BusinessException("Vị trí không tồn tại."));
    }
    public StorageLocation requireActive(UUID id, boolean compartment) {
        if (id == null) throw new BusinessException("Cần chọn vị trí lưu trữ.");
        var locations = findAll();
        var current = locations.stream().filter(l -> l.id().equals(id)).findFirst().orElseThrow(() -> new BusinessException("Vị trí không tồn tại."));
        if (compartment && !current.type().equals("COMPARTMENT")) throw new BusinessException("Hồ sơ phải đặt trong một ngăn.");
        var cursor = current;
        while (true) {
            if (!cursor.active()) throw new BusinessException("Vị trí hoặc vị trí cha đã ngừng hoạt động.");
            if (cursor.parentId() == null) break;
            UUID parent = cursor.parentId();
            cursor = locations.stream().filter(l -> l.id().equals(parent)).findFirst().orElseThrow();
        }
        return current;
    }
    public UUID insert(LocationForm form, String username) {
        return jdbc.queryForObject("""
                INSERT INTO storage_locations(parent_id,code,name,type,capacity,created_by)
                VALUES(?,?,?,?,?,(SELECT id FROM app_users WHERE username=?)) RETURNING id
                """, UUID.class, form.parentId(), form.code(), form.name(), form.type(), form.capacity(), username);
    }
    public void update(UUID id, String name, boolean active, Integer capacity, long version) {
        if (jdbc.update("UPDATE storage_locations SET name=?, active=?, capacity=?, version=version+1 WHERE id=? AND version=?", name, active, capacity, id, version) != 1)
            throw new BusinessException("Vị trí đã thay đổi. Vui lòng tải lại trang.");
    }
    public boolean hasActiveChildren(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM storage_locations WHERE parent_id=? AND active)", Boolean.class,id));
    }
    public void event(UUID id, String action, String username, String details) {
        jdbc.update("INSERT INTO location_events(location_id,action,actor_username,details) VALUES(?,?,?,?)", id,action,username,details);
    }
}
