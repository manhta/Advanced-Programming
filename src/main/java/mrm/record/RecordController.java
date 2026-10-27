package mrm.record;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import mrm.catalog.CatalogException;
import mrm.catalog.CatalogService;
import mrm.common.BusinessException;
import mrm.department.DepartmentService;
import mrm.storage.StorageService;

@Controller
public class RecordController {
    private final RecordService service; private final DepartmentService departments;
    private final CatalogService catalogs; private final StorageService storage;
    private final mrm.electronic.ElectronicRecordService submissions;
    public RecordController(RecordService service,DepartmentService departments,CatalogService catalogs,StorageService storage,mrm.electronic.ElectronicRecordService submissions) {
        this.service=service; this.departments=departments; this.catalogs=catalogs; this.storage=storage;this.submissions=submissions;
    }
    private boolean archivist(Authentication auth) { return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ARCHIVIST")); }
    @GetMapping({"/archivist/records","/reader/records","/director/records"})
    public String list(@ModelAttribute("filter") RecordFilter filter,Authentication auth,Model model) {
        model.addAttribute("result",service.search(filter,auth.getName()));
        model.addAttribute("recordScope",filter.scope());
        model.addAttribute("departments",departments.findAll()); model.addAttribute("recordTypes",catalogs.recordTypes());
        return "records/list";
    }
    @GetMapping("/archivist/records/new") public String createForm(Model model) {
        model.addAttribute("recordForm",RecordForm.empty()); model.addAttribute("editId",null); model.addAttribute("stored",false);
        return form(model);
    }
    @PostMapping("/archivist/records")
    public String create(@Valid @ModelAttribute("recordForm") RecordForm recordForm,BindingResult errors,@RequestParam(defaultValue="DRAFT") String mode,
            Authentication auth,Model model,RedirectAttributes flash) {
        model.addAttribute("editId",null); model.addAttribute("stored",false);
        if(!errors.hasErrors()) try {
            UUID id=service.create(recordForm,mode,auth.getName()); flash.addFlashAttribute("successMessage",mode.equals("STORE") ? "Đã nhập kho hồ sơ." : "Đã lưu nháp, chưa công bố.");
            return "redirect:/archivist/records/"+id;
        } catch(DuplicateKeyException ex) { errors.reject("duplicate","Số lưu trữ/phiếu nhập bị trùng. Kiểm tra dữ liệu nguồn hoặc mở form mới."); }
        catch(BusinessException | CatalogException ex) { errors.reject("business",ex.getMessage()); }
        catch(DataIntegrityViolationException ex) { errors.reject("data","Dữ liệu tham chiếu hoặc giới hạn không hợp lệ. Kiểm tra danh mục và thử lại."); }
        return form(model);
    }
    @GetMapping({"/archivist/records/{id}","/reader/records/{id}","/director/records/{id}"})
    public String detail(@PathVariable UUID id,Authentication auth,Model model) {
        var identity=service.identity(id,auth.getName());
        boolean paper=Boolean.TRUE.equals(identity.get("has_paper_copy"));
        if(identity.get("submission_state")!=null && !paper && (!auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_READER")) || Boolean.TRUE.equals(identity.get("owned")))) {
            model.addAttribute("record",submissions.detail(id,auth.getName()));
            model.addAttribute("electronicBase","/"+auth.getAuthorities().stream().map(a->a.getAuthority()).filter(a->a.startsWith("ROLE_")).findFirst().orElseThrow().substring(5).toLowerCase(java.util.Locale.ROOT)+"/records");
            return "electronic/detail";
        }
        if(archivist(auth)) {
            model.addAttribute("birthDate",identity.get("birth_date"));
            var record=service.operationalDetail(id,auth.getName()); model.addAttribute("record",record);
            model.addAttribute("history",service.history(id)); model.addAttribute("locationHistory",service.locationHistory(id));
            var locations=storage.findAll(); model.addAttribute("locations",locations);
            model.addAttribute("locationPath",record.form().locationId()==null ? null : locations.stream().filter(l -> l.id().equals(record.form().locationId())).map(l -> l.path()).findFirst().orElse("Không xác định"));
            model.addAttribute("moveKey",UUID.randomUUID());
            if(identity.get("submission_state")!=null)model.addAttribute("submissionEvents",submissions.detail(id,auth.getName()).get("events")); return "records/detail";
        }
        boolean reader=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_READER"));
        model.addAttribute("readerHasPaper",reader && service.paperHandedToReader(id,auth.getName()));
        model.addAttribute("record",service.basicDetail(id,auth.getName())); return "records/basic-detail";
    }
    @GetMapping({"/archivist/records/{id}/edit","/archivist/records/{id}/attach-paper"}) public String edit(@PathVariable UUID id,Authentication auth,Model model) {
        var identity=service.identity(id,auth.getName());
        if(!Boolean.TRUE.equals(identity.get("has_paper_copy")) && !"PUBLISHED".equals(identity.get("record_state")))throw new BusinessException("Hồ sơ cần được Giám đốc duyệt trước khi bổ sung bản giấy.");
        model.addAttribute("attachingPaper",!Boolean.TRUE.equals(identity.get("has_paper_copy")));
        var record=service.details(id); model.addAttribute("recordForm",record.form()); model.addAttribute("editId",id);
        model.addAttribute("stored",record.state().equals("STORED")); return form(model);
    }
    @PostMapping("/archivist/records/{id}/edit")
    public String update(@PathVariable UUID id,@Valid @ModelAttribute("recordForm") RecordForm recordForm,BindingResult errors,
            @RequestParam(defaultValue="STORE") String mode,Authentication auth,Model model,RedirectAttributes flash) {
        model.addAttribute("editId",id); model.addAttribute("stored",service.details(id).state().equals("STORED"));
        model.addAttribute("attachingPaper",!Boolean.TRUE.equals(service.identity(id,auth.getName()).get("has_paper_copy")));
        if(!errors.hasErrors()) try {
            service.update(id,recordForm,mode,auth.getName()); flash.addFlashAttribute("successMessage","Đã lưu hồ sơ và lịch sử thay đổi."); return "redirect:/archivist/records/"+id;
        } catch(DuplicateKeyException ex) { errors.reject("duplicate","Số lưu trữ bị trùng."); }
        catch(BusinessException | CatalogException ex) { errors.reject("business",ex.getMessage()); }
        catch(DataIntegrityViolationException ex) { errors.reject("data","Dữ liệu không hợp lệ hoặc danh mục đã thay đổi."); }
        return form(model);
    }
    @PostMapping("/archivist/records/{id}/move")
    public String move(@PathVariable UUID id,@RequestParam UUID destination,@RequestParam long version,@RequestParam UUID operationKey,
            @RequestParam String reason,Authentication auth,RedirectAttributes flash) {
        try { service.move(id,destination,version,operationKey,reason,auth.getName()); flash.addFlashAttribute("successMessage","Đã chuyển hồ sơ và ghi lịch sử."); }
        catch(BusinessException ex) { flash.addFlashAttribute("errorMessage",ex.getMessage()); }
        return "redirect:/archivist/records/"+id;
    }
    @PostMapping("/archivist/records/{id}/availability")
    public String availability(@PathVariable UUID id,@RequestParam long version,@RequestParam String status,@RequestParam String reason,
            Authentication auth,RedirectAttributes flash) {
        try { service.availability(id,version,status,reason,auth.getName()); flash.addFlashAttribute("successMessage","Đã cập nhật khả dụng."); }
        catch(BusinessException ex) { flash.addFlashAttribute("errorMessage",ex.getMessage()); }
        return "redirect:/archivist/records/"+id;
    }
    private String form(Model model) {
        model.addAttribute("departments",departments.findAll()); model.addAttribute("recordTypes",catalogs.recordTypes());
        model.addAttribute("policies",catalogs.policies()); model.addAttribute("locations",storage.findAll());
        return "records/form";
    }
}
