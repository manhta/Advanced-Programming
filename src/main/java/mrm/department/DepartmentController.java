package mrm.department;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/archivist/departments")
public class DepartmentController {
    private final DepartmentService service;
    public DepartmentController(DepartmentService service) { this.service = service; }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("departments", service.findAll());
        return "department/list";
    }

    @PostMapping
    public String create() {
        throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED,
                "Department catalog is fixed for the learning project.");
    }
}