package mrm.storage;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mrm.common.BusinessException;

@Service
public class StorageService {
    private final StorageRepository repository;
    public StorageService(StorageRepository repository) { this.repository=repository; }
    public List<StorageLocation> findAll() { return repository.findAll(); }
    @Transactional
    public UUID create(LocationForm form, String username) {
        repository.lockTree();
        if (!form.type().equals("WAREHOUSE")) {
            var parent=repository.requireActive(form.parentId(),false);
            String expected=form.type().equals("SHELF") ? "WAREHOUSE" : "SHELF";
            if (!parent.type().equals(expected)) throw new BusinessException("Sai cấp: Kho → Kệ → Ngăn.");
        } else if (form.parentId()!=null) throw new BusinessException("Kho không có vị trí cha.");
        if (form.capacity()!=null && (!form.type().equals("COMPARTMENT") || form.capacity()<1)) throw new BusinessException("Chỉ ngăn có sức chứa dương.");
        UUID id=repository.insert(form,username);
        repository.event(id,"CREATE",username,form.toString()); return id;
    }
    @Transactional
    public void update(UUID id,String name,boolean active,Integer capacity,long version,String username) {
        repository.lockTree(); var old=repository.get(id);
        String normalized=name==null ? "" : name.strip();
        if(normalized.isEmpty() || normalized.length()>200) throw new BusinessException("Tên vị trí phải có 1–200 ký tự.");
        if(old.version()!=version) throw new BusinessException("Vị trí đã thay đổi. Vui lòng tải lại.");
        if(!active && (old.occupied()>0 || repository.hasActiveChildren(id))) throw new BusinessException("Còn hồ sơ hoặc vị trí con đang hoạt động; chưa thể ngừng dùng.");
        if(active && old.parentId()!=null) repository.requireActive(old.parentId(),false);
        if(capacity!=null && (!old.type().equals("COMPARTMENT") || capacity<1 || capacity<old.occupied())) throw new BusinessException("Sức chứa không hợp lệ hoặc nhỏ hơn số hồ sơ hiện có.");
        repository.update(id,normalized,active,capacity,version);
        repository.event(id,"UPDATE",username,old+" -> name="+normalized+", active="+active+", capacity="+capacity);
    }
}
