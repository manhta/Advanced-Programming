package mrm.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final CatalogRepository repository;
    public CatalogService(CatalogRepository repository) { this.repository = repository; }
    public List<RecordType> recordTypes() { return repository.recordTypes(); }
    public List<RetentionPolicy> policies() { return repository.policies(); }

    @Transactional
    public void createType(RecordTypeForm form, String username) {
        UUID id = repository.createType(form, username);
        repository.event("RECORD_TYPE", id, "CREATE", username, form.toString());
    }

    @Transactional
    public void updateType(UUID id, String name, boolean active, long version, String username) {
        String normalized = name == null ? "" : name.strip();
        if (normalized.isEmpty() || normalized.length() > 200) throw new CatalogException("Tên phải có 1–200 ký tự.");
        var previous = repository.lockType(id);
        if (previous.version() != version) throw new CatalogException("Loại bệnh án đã được người khác thay đổi. Vui lòng tải lại trang.");
        repository.updateType(id, normalized, active, version);
        repository.event("RECORD_TYPE", id, "UPDATE", username,
                previous + " -> name=" + normalized + ", active=" + active);
    }

    @Transactional
    public void createPolicy(RetentionPolicyForm form, String username) {
        if (form.effectiveTo() != null && form.effectiveTo().isBefore(form.effectiveFrom())) {
            throw new CatalogException("Ngày kết thúc không được trước ngày hiệu lực.");
        }
        UUID id = repository.createPolicy(form, username);
        repository.event("RETENTION_POLICY", id, "CREATE_DRAFT", username, form.toString());
    }

    @Transactional
    public void approve(UUID id, String username) {
        var policy = repository.lockPolicy(id);
        if (!policy.status().equals("DRAFT")) throw new CatalogException("Chính sách đã được xử lý. Vui lòng tải lại trang.");
        if (repository.overlaps(policy)) throw new CatalogException("Có phiên bản đang dùng cùng mã bị chồng thời gian hiệu lực. Cần ngừng dùng phiên bản cũ trước.");
        repository.approve(id, username);
        repository.event("RETENTION_POLICY", id, "APPROVE", username, policy.toString());
    }

    @Transactional
    public void deactivatePolicy(UUID id, String username) {
        var policy = repository.lockPolicy(id);
        if (!policy.active()) throw new CatalogException("Chính sách hiện không được sử dụng.");
        repository.deactivatePolicy(id);
        repository.event("RETENTION_POLICY", id, "DEACTIVATE", username, policy.toString());
    }
}
