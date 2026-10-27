package mrm.storage;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import mrm.common.BusinessException;

@Controller
@RequestMapping("/archivist/locations")
public class StorageController {
    private final StorageService service;
    public StorageController(StorageService service) { this.service=service; }
    @GetMapping public String list(Model model) {
        model.addAttribute("locationForm",new LocationForm(null,null,"WAREHOUSE",null,null)); return page(model);
    }
    @PostMapping public String create(@Valid @ModelAttribute("locationForm") LocationForm form,BindingResult errors,
            Authentication auth,Model model,RedirectAttributes flash) {
        if(!errors.hasErrors()) try { service.create(form,auth.getName()); }
        catch(DuplicateKeyException ex) { errors.rejectValue("code","duplicate","Mã đã có trong cùng vị trí cha."); }
        catch(BusinessException ex) { errors.reject("business",ex.getMessage()); }
        if(errors.hasErrors()) return page(model);
        flash.addFlashAttribute("successMessage","Đã thêm vị trí."); return "redirect:/archivist/locations";
    }
    @PostMapping("/{id}") public String update(@PathVariable UUID id,@RequestParam String name,@RequestParam boolean active,
            @RequestParam(required=false) Integer capacity,@RequestParam long version,Authentication auth,RedirectAttributes flash) {
        try { service.update(id,name,active,capacity,version,auth.getName()); flash.addFlashAttribute("successMessage","Đã cập nhật vị trí."); }
        catch(BusinessException ex) { flash.addFlashAttribute("errorMessage",ex.getMessage()); }
        return "redirect:/archivist/locations";
    }
    private String page(Model model) { model.addAttribute("locations",service.findAll()); return "storage/list"; }
}
