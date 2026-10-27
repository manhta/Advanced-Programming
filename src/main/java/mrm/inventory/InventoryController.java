package mrm.inventory;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import mrm.common.BusinessException;
import mrm.storage.StorageService;

@Controller
public class InventoryController {
    private final InventoryService service;private final StorageService storage;
    public InventoryController(InventoryService service,StorageService storage){this.service=service;this.storage=storage;}
    @GetMapping("/archivist/inventories") public String list(Authentication auth,Model model){model.addAttribute("batches",service.list(auth.getName()));if(arch(auth))model.addAttribute("locations",storage.findAll());return "inventory/list";}
    @GetMapping("/archivist/inventories/{id}") public String detail(@PathVariable UUID id,Authentication auth,Model model){
        if(!arch(auth))throw new org.springframework.security.access.AccessDeniedException("Chi tiết vị trí thuộc Archivist; Director xem báo cáo kiểm kê tổng hợp.");
        model.addAttribute("batch",service.detail(id,auth.getName()));model.addAttribute("locations",storage.findAll());return "inventory/detail";
    }
    @PostMapping("/archivist/inventories") public String create(@RequestParam String name,@RequestParam(required=false) UUID location,Authentication auth){return "redirect:/archivist/inventories/"+service.create(name,location,auth.getName());}
    @PostMapping("/archivist/inventories/{id}/check") public String check(@PathVariable UUID id,@RequestParam UUID record,@RequestParam String observed,@RequestParam(required=false) UUID location,@RequestParam(required=false) String note,Authentication auth,RedirectAttributes flash){try{service.check(id,record,observed,location,note,auth.getName());flash.addFlashAttribute("successMessage","Đã ghi nhận kết quả kiểm kê.");}catch(BusinessException ex){flash.addFlashAttribute("errorMessage",ex.getMessage());}return "redirect:/archivist/inventories/"+id;}
    @PostMapping("/archivist/inventories/{id}/close") public String close(@PathVariable UUID id,Authentication auth,RedirectAttributes flash){try{service.close(id,auth.getName());flash.addFlashAttribute("successMessage","Đã chốt đợt kiểm kê.");}catch(BusinessException ex){flash.addFlashAttribute("errorMessage",ex.getMessage());}return "redirect:/archivist/inventories/"+id;}
    private boolean arch(Authentication auth){return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ARCHIVIST"));}
}
