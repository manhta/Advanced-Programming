package mrm.department;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    @Transactional
    public void create(String code, String name, String creatorUsername) {
        String normalizedCode = code.strip().toUpperCase(Locale.ROOT);
        String normalizedName = name.strip();

        departmentRepository.insert(
                normalizedCode,
                normalizedName,
                creatorUsername
        );
    }
}
