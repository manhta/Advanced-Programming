package mrm.inventory;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mrm.account.AccountService;
import mrm.audit.AuditService;
import mrm.common.BusinessException;
import mrm.storage.StorageRepository;

@Service
public class InventoryService {
    private final JdbcTemplate jdbc;private final AccountService accounts;private final AuditService audit;private final StorageRepository storage;
    public InventoryService(JdbcTemplate jdbc,AccountService accounts,AuditService audit,StorageRepository storage){this.jdbc=jdbc;this.accounts=accounts;this.audit=audit;this.storage=storage;}
    public List<Map<String,Object>> list(String by){accounts.actor(by,"ARCHIVIST");return jdbc.queryForList("SELECT b.*,u.username,(SELECT COUNT(*) FROM inventory_items i WHERE i.batch_id=b.id) AS total,(SELECT COUNT(*) FROM inventory_items i WHERE i.batch_id=b.id AND i.observed IS NOT NULL) AS checked FROM inventory_batches b JOIN app_users u ON u.id=b.created_by ORDER BY created_at DESC");}
    public Map<String,Object> detail(UUID id,String by){accounts.actor(by,"ARCHIVIST");var rows=jdbc.queryForList("SELECT * FROM inventory_batches WHERE id=?",id);if(rows.isEmpty())throw new BusinessException("Không có đợt kiểm kê.");var batch=rows.getFirst();batch.put("items",jdbc.queryForList("SELECT i.*,r.archive_number,r.patient_name,l.code AS observed_code FROM inventory_items i JOIN medical_records r ON r.id=i.record_id LEFT JOIN storage_locations l ON l.id=i.observed_location_id WHERE i.batch_id=? ORDER BY r.archive_number",id));return batch;}
    @Transactional public UUID create(String name,UUID location,String by){
        var actor=accounts.actor(by,"ARCHIVIST");if(name==null || name.isBlank() || name.strip().length()>200)throw new BusinessException("Tên đợt cần 1–200 ký tự.");
        storage.lockTree();if(location!=null)storage.get(location);
        UUID id=UUID.randomUUID();jdbc.update("INSERT INTO inventory_batches(id,name,location_id,state,created_by) VALUES(?,?,?,'OPEN',?)",id,name.strip(),location,actor.get("id"));
        var rows=jdbc.queryForList("""
            WITH RECURSIVE scope AS (SELECT id FROM storage_locations WHERE id=CAST(? AS UUID)
                UNION ALL SELECT l.id FROM storage_locations l JOIN scope s ON l.parent_id=s.id)
            SELECT id,current_location_id FROM medical_records WHERE record_state='STORED' AND physical_status<>'READING'
                AND (CAST(? AS UUID) IS NULL OR current_location_id IN (SELECT id FROM scope))
            """,location,location);
        for(var r:rows)jdbc.update("INSERT INTO inventory_items(batch_id,record_id,expected_location_id,expected_path) VALUES(?,?,?,?)",id,r.get("id"),r.get("current_location_id"),storage.get((UUID)r.get("current_location_id")).path());
        audit.log(by,"INVENTORY_CREATE","INVENTORY",id,"Chụp danh sách "+rows.size()+" hồ sơ trong kho tại thời điểm tạo; không gồm hồ sơ đang giao đọc.");return id;
    }
    private void open(UUID id){var rows=jdbc.queryForList("SELECT state FROM inventory_batches WHERE id=? FOR UPDATE",id);if(rows.isEmpty() || !rows.getFirst().get("state").equals("OPEN"))throw new BusinessException("Đợt kiểm kê không mở.");}
    @Transactional public void check(UUID batch,UUID record,String observed,UUID location,String note,String by){
        var actor=accounts.actor(by,"ARCHIVIST");open(batch);
        if(!List.of("FOUND","MISSING","WRONG_LOCATION","DAMAGED").contains(observed))throw new BusinessException("Kết quả kiểm kê không hợp lệ.");
        if(note!=null && note.length()>2000)throw new BusinessException("Ghi chú tối đa 2000 ký tự.");
        if(!observed.equals("FOUND") && (note==null || note.isBlank()))throw new BusinessException("Cần mô tả chênh lệch.");
        if(observed.equals("WRONG_LOCATION") && location==null)throw new BusinessException("Cần ngăn quan sát thực tế.");
        if(location!=null)storage.requireActive(location,true);
        var previous=jdbc.queryForList("SELECT observed,note,observed_location_id,expected_location_id FROM inventory_items WHERE batch_id=? AND record_id=?",batch,record);
        if(previous.isEmpty())throw new BusinessException("Hồ sơ không thuộc đợt kiểm kê.");
        if(observed.equals("FOUND") && location!=null && !location.equals(previous.getFirst().get("expected_location_id")))throw new BusinessException("Ngăn thực tế khác danh sách gốc; chọn kết quả Sai vị trí.");
        jdbc.update("UPDATE inventory_items SET observed=?,observed_location_id=?,note=?,checked_by=?,checked_at=CURRENT_TIMESTAMP WHERE batch_id=? AND record_id=?",observed,location,note,actor.get("id"),batch,record);
        audit.log(by,"INVENTORY_CHECK","INVENTORY",batch,"Hồ sơ "+record+"; trước "+previous.getFirst()+"; sau "+observed+"; ngăn quan sát "+location+"; "+Objects.toString(note,""));
    }
    @Transactional public void close(UUID id,String by){accounts.actor(by,"ARCHIVIST");open(id);if(jdbc.queryForObject("SELECT COUNT(*) FROM inventory_items WHERE batch_id=? AND observed IS NULL",Integer.class,id)>0)throw new BusinessException("Còn hồ sơ chưa đối chiếu.");jdbc.update("UPDATE inventory_batches SET state='CLOSED',closed_at=CURRENT_TIMESTAMP WHERE id=?",id);audit.log(by,"INVENTORY_CLOSE","INVENTORY",id,"Chốt biên bản; chênh lệch cần sửa metadata/chuyển vị trí bằng chức năng tương ứng, không tự sửa số liệu kho.");}
}
