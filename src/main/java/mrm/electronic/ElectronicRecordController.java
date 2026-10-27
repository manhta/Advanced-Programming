package mrm.electronic;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;
import mrm.common.BusinessException;

@Controller
public class ElectronicRecordController {
    private final ElectronicRecordService service;
    private final mrm.department.DepartmentService departments;
    private final mrm.catalog.CatalogService catalogs;
    public ElectronicRecordController(ElectronicRecordService service,mrm.department.DepartmentService departments,mrm.catalog.CatalogService catalogs){this.service=service;this.departments=departments;this.catalogs=catalogs;}
    private String patientForm(Model m){m.addAttribute("departments",departments.findAll());m.addAttribute("recordTypes",catalogs.recordTypes());return "electronic/form";}
    private java.time.LocalDate date(Object value){return value==null?null:((java.sql.Date)value).toLocalDate();}
    private String base(Authentication a){return "/"+a.getAuthorities().stream().map(x->x.getAuthority()).filter(x->x.startsWith("ROLE_")).findFirst().orElseThrow().substring(5).toLowerCase(java.util.Locale.ROOT)+"/records";}
    @GetMapping({"/reader/electronic-records","/archivist/electronic-records","/director/electronic-records"})
    public String list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String state,@RequestParam(defaultValue="0") int page,Authentication a,Model m){
        return "redirect:"+org.springframework.web.util.UriComponentsBuilder.fromPath(base(a)).queryParam("scope","DECLARATIONS").queryParam("q",q).queryParam("submissionState",state).build().encode().toUriString();
    }
    @GetMapping({"/reader/records/new","/reader/electronic-records/new"}) public String form(Model m){m.addAttribute("electronicForm",ElectronicRecordForm.empty());return patientForm(m);}
    @PostMapping({"/reader/records","/reader/electronic-records"})
    public String create(@Valid @ModelAttribute("electronicForm") ElectronicRecordForm form,BindingResult errors,Authentication a,Model m,RedirectAttributes f){
        if(!errors.hasErrors())try{UUID id=service.create(form,a.getName());f.addFlashAttribute("successMessage","Đã gửi hồ sơ; mã được tạo tự động, chờ nhân viên lưu trữ kiểm tra.");return "redirect:"+base(a)+"/"+id;}
        catch(BusinessException ex){errors.reject("business",ex.getMessage());}catch(DataIntegrityViolationException ex){errors.reject("data","Dữ liệu đã thay đổi hoặc trùng phiếu. Kiểm tra và gửi lại.");}
        return patientForm(m);
    }
    @GetMapping({"/reader/electronic-records/{id}","/archivist/electronic-records/{id}","/director/electronic-records/{id}"})
    public String detail(@PathVariable UUID id,Authentication a,Model m){service.detail(id,a.getName());return "redirect:"+base(a)+"/"+id;}
    @GetMapping({"/reader/records/{id}/edit","/reader/electronic-records/{id}/edit"}) public String edit(@PathVariable UUID id,Authentication a,Model m){
        var r=service.detail(id,a.getName());if(!java.util.List.of("SUBMITTED","NEEDS_INFO").contains(r.get("state")))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Không thể sửa ở trạng thái hiện tại.");
        m.addAttribute("editId",id);m.addAttribute("electronicForm",new ElectronicRecordForm(UUID.randomUUID(),((Number)r.get("version")).longValue(),(String)r.get("patient_name"),((Number)r.get("birth_year")).intValue(),(String)r.get("sex"),(String)r.get("patient_code"),(UUID)r.get("department_id"),(UUID)r.get("record_type_id"),Boolean.TRUE.equals(r.get("deceased")),date(r.get("admission_date")),date(r.get("discharge_date")),(String)r.get("source_note"),(String)r.get("note"),date(r.get("birth_date"))==null?null:date(r.get("birth_date")).getDayOfMonth(),date(r.get("birth_date"))==null?null:date(r.get("birth_date")).getMonthValue()));return patientForm(m);
    }
    @PostMapping({"/reader/records/{id}/edit","/reader/electronic-records/{id}/edit"}) public String revise(@PathVariable UUID id,@Valid @ModelAttribute("electronicForm") ElectronicRecordForm form,BindingResult errors,Authentication a,Model m,RedirectAttributes f){
        service.detail(id,a.getName());m.addAttribute("editId",id);
        if(!errors.hasErrors())try{service.revise(id,form,a.getName());f.addFlashAttribute("successMessage","Đã gửi lại hồ sơ, giữ nguyên mã.");return "redirect:"+base(a)+"/"+id;}catch(BusinessException ex){errors.reject("business",ex.getMessage());}
        return patientForm(m);
    }
    private String action(UUID id,Authentication a,RedirectAttributes f,Runnable work){
        try{work.run();f.addFlashAttribute("successMessage","Đã cập nhật hồ sơ và lịch sử xử lý.");}
        catch(BusinessException ex){f.addFlashAttribute("errorMessage",ex.getMessage());}
        catch(DataIntegrityViolationException ex){f.addFlashAttribute("errorMessage","Dữ liệu đã thay đổi; tải lại trước khi xử lý.");}
        return "redirect:"+base(a)+"/"+id;
    }
    @PostMapping({"/archivist/records/{id}/review","/archivist/electronic-records/{id}/review"}) public String review(@PathVariable UUID id,@RequestParam long version,@RequestParam boolean ready,@RequestParam String note,Authentication a,RedirectAttributes f){return action(id,a,f,()->service.review(id,version,ready,note,a.getName()));}
    @PostMapping({"/director/records/{id}/decide","/director/electronic-records/{id}/decide"}) public String decide(@PathVariable UUID id,@RequestParam long version,@RequestParam boolean approve,@RequestParam String note,@RequestParam String secret,Authentication a,RedirectAttributes f){return action(id,a,f,()->service.decide(id,version,approve,note,secret,a.getName()));}
    @PostMapping({"/reader/records/{id}/cancel","/reader/electronic-records/{id}/cancel"}) public String cancel(@PathVariable UUID id,@RequestParam long version,Authentication a,RedirectAttributes f){return action(id,a,f,()->service.cancel(id,version,a.getName()));}
}
