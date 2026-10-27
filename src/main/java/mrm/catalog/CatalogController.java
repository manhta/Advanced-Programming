package mrm.catalog;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CatalogController {
    private final CatalogService service;
    public CatalogController(CatalogService service) { this.service = service; }

    @GetMapping("/archivist/record-types")
    public String types(Model model) {
        model.addAttribute("recordTypeForm", new RecordTypeForm(null, null));
        return typePage(model);
    }

    @PostMapping("/archivist/record-types")
    public String createType(@Valid @ModelAttribute("recordTypeForm") RecordTypeForm form,
            BindingResult errors, Authentication auth, Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try { service.createType(form, auth.getName()); }
            catch (DuplicateKeyException ex) { errors.rejectValue("code", "duplicate", "Mã loại bệnh án đã tồn tại."); }
        }
        if (errors.hasErrors()) return typePage(model);
        flash.addFlashAttribute("successMessage", "Đã thêm loại bệnh án.");
        return "redirect:/archivist/record-types";
    }

    @PostMapping("/archivist/record-types/{id}")
    public String updateType(@PathVariable UUID id, @RequestParam String name, @RequestParam boolean active, @RequestParam long version,
            Authentication auth, RedirectAttributes flash) {
        try {
            service.updateType(id, name, active, version, auth.getName());
            flash.addFlashAttribute("successMessage", "Đã cập nhật loại bệnh án.");
        } catch (CatalogException ex) { flash.addFlashAttribute("errorMessage", ex.getMessage()); }
        return "redirect:/archivist/record-types";
    }

    @GetMapping({"/archivist/retention-policies", "/director/retention-policies"})
    public String policies(Authentication auth, Model model) {
        model.addAttribute("retentionPolicyForm", new RetentionPolicyForm(null, null, 1, null, null, null, null));
        return policyPage(auth, model);
    }

    @PostMapping("/archivist/retention-policies")
    public String createPolicy(@Valid @ModelAttribute("retentionPolicyForm") RetentionPolicyForm form,
            BindingResult errors, Authentication auth, Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try { service.createPolicy(form, auth.getName()); }
            catch (DuplicateKeyException ex) { errors.rejectValue("versionNo", "duplicate", "Mã và phiên bản đã tồn tại."); }
            catch (CatalogException ex) { errors.rejectValue("effectiveTo", "dateRange", ex.getMessage()); }
        }
        if (errors.hasErrors()) return policyPage(auth, model);
        flash.addFlashAttribute("successMessage", "Đã tạo chính sách dự thảo, chờ Giám đốc xác nhận.");
        return "redirect:/archivist/retention-policies";
    }

    @PostMapping("/director/retention-policies/{id}/approve")
    public String approve(@PathVariable UUID id, Authentication auth, RedirectAttributes flash) {
        try {
            service.approve(id, auth.getName());
            flash.addFlashAttribute("successMessage", "Đã xác nhận chính sách.");
        } catch (CatalogException ex) { flash.addFlashAttribute("errorMessage", ex.getMessage()); }
        return "redirect:/director/retention-policies";
    }

    @PostMapping("/director/retention-policies/{id}/deactivate")
    public String deactivate(@PathVariable UUID id, Authentication auth, RedirectAttributes flash) {
        try {
            service.deactivatePolicy(id, auth.getName());
            flash.addFlashAttribute("successMessage", "Đã ngừng dùng chính sách cho lựa chọn mới; lịch sử vẫn được giữ.");
        } catch (CatalogException ex) { flash.addFlashAttribute("errorMessage", ex.getMessage()); }
        return "redirect:/director/retention-policies";
    }

    private String typePage(Model model) {
        model.addAttribute("recordTypes", service.recordTypes());
        return "catalog/record-types";
    }

    private String policyPage(Authentication auth, Model model) {
        model.addAttribute("policies", service.policies());
        model.addAttribute("director", auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DIRECTOR")));
        return "catalog/retention-policies";
    }
}
