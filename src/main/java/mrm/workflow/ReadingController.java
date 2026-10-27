package mrm.workflow;

import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import mrm.common.BusinessException;
import mrm.record.RecordService;
import mrm.storage.StorageService;

@Controller
public class ReadingController {
    private final ReadingService service;
    private final RecordService records;
    private final StorageService storage;
    public ReadingController(ReadingService service,RecordService records,StorageService storage){this.service=service;this.records=records;this.storage=storage;}
    private String base(Authentication auth){return "/"+auth.getAuthorities().stream().filter(a->a.getAuthority().startsWith("ROLE_")).findFirst().orElseThrow().getAuthority().substring(5).toLowerCase()+"/requests";}
    @GetMapping({"/reader/requests","/archivist/requests","/director/requests"})
    public String list(Authentication auth,Model model,@RequestParam(defaultValue="") String q,
            @RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="registered") String sort){
        if(base(auth).equals("/reader/requests")) {
            if(sort.equals("status") || sort.equals("status_desc"))sort="registered";
            ReadingService.RequestPage result;
            try{result=service.readerHistory(auth.getName(),q,from,to,page,status,sort);}
            catch(BusinessException ex){model.addAttribute("errorMessage",ex.getMessage());result=service.readerHistory(auth.getName(),"",null,null,0);}
            model.addAttribute("history",result);model.addAttribute("requests",result.items());
        }else model.addAttribute("requests",service.list(auth.getName()));
        model.addAttribute("requestBase",base(auth));return "reading/list";
    }
    @GetMapping("/reader/requests/records") @ResponseBody
    public RecordService.BorrowCandidates candidates(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Authentication auth){
        return records.borrowCandidates(q,page,auth.getName());
    }
    @GetMapping("/reader/requests/records/{id}") @ResponseBody public RecordService.RequestPreview candidateDetail(@PathVariable UUID id,Authentication auth){return records.borrowPreview(id,auth.getName());}
    @GetMapping("/reader/requests/new") public String form(@RequestParam(required=false) UUID recordId,Authentication auth,Model model){
        var selected=new java.util.ArrayList<RecordService.RequestPreview>();
        if(recordId!=null)try{selected.add(records.borrowPreview(recordId,auth.getName()));}catch(BusinessException ex){model.addAttribute("errorMessage",ex.getMessage());}
        model.addAttribute("selectedRecords",selected);model.addAttribute("token",UUID.randomUUID());return "reading/new";
    }
    @PostMapping("/reader/requests") public String submit(@RequestParam(required=false) java.util.List<UUID> recordIds,@RequestParam UUID token,
            @RequestParam String purpose,@RequestParam org.springframework.util.MultiValueMap<String,String> inputs,Authentication auth,Model model,RedirectAttributes flash) {
        try{
            rejectReaderDates(inputs);
            var requests=service.submitBatch(recordIds,purpose,token,auth.getName());
            flash.addFlashAttribute("successMessage","Đã gửi đăng ký mượn "+requests.size()+" hồ sơ; nhân viên lưu trữ sẽ chỉ định ngày nhận.");
            return requests.size()==1?"redirect:/reader/requests/"+requests.getFirst():"redirect:/reader/requests";
        }catch(BusinessException | DataIntegrityViolationException ex){
            model.addAttribute("errorMessage",ex instanceof BusinessException?ex.getMessage():"Hồ sơ đã thay đổi hoặc phiếu bị trùng. Tải lại và kiểm tra.");
            var selected=new java.util.ArrayList<RecordService.RequestPreview>();
            if(recordIds!=null && recordIds.size()<=20)for(UUID id:recordIds.stream().distinct().toList())try{selected.add(records.borrowPreview(id,auth.getName()));}catch(BusinessException unavailable){/* Do not preselect a record that is no longer borrowable. */}
            model.addAttribute("selectedRecords",selected);model.addAttribute("token",token);model.addAttribute("purpose",purpose);return "reading/new";
        }
    }
    private void rejectReaderDates(org.springframework.util.MultiValueMap<String,String> inputs){
        if(inputs.containsKey("dates") || inputs.containsKey("pickupOn"))throw new BusinessException("Ngày nhận do nhân viên lưu trữ quyết định; vui lòng dùng mẫu đăng ký mới.");
    }
    @GetMapping({"/reader/requests/{id}","/archivist/requests/{id}","/director/requests/{id}"})
    public String detail(@PathVariable UUID id,Authentication auth,Model model) {
        var request=service.detail(id,auth.getName());
        model.addAttribute("pickupMinDate",ReadingService.today());model.addAttribute("request",request);model.addAttribute("requestBase",base(auth));
        if(base(auth).equals("/reader/requests")) {
            model.addAttribute("progress",ReadingProgress.nodes(request));
            model.addAttribute("canOpenRecord","CONFIRMED".equals(request.get("state")) && records.readerMayView((UUID)request.get("record_id"),auth.getName()));
        }
        if(auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ARCHIVIST")))model.addAttribute("locations",storage.findAll());
        return base(auth).equals("/reader/requests") ? "reading/history" : "reading/detail";
    }
    private String action(UUID id,Authentication auth,RedirectAttributes flash,Runnable work){
        try{work.run();flash.addFlashAttribute("successMessage","Đã ghi nhận thao tác và lịch sử.");}
        catch(BusinessException ex){flash.addFlashAttribute("errorMessage",ex.getMessage());}
        catch(DataIntegrityViolationException ex){flash.addFlashAttribute("errorMessage","Lịch/trạng thái xung đột với thao tác khác. Tải lại và kiểm tra.");}
        return "redirect:"+base(auth)+"/"+id;
    }
    @PostMapping("/reader/requests/{id}/amend") public String amend(@PathVariable UUID id,@RequestParam long version,@RequestParam String purpose,@RequestParam org.springframework.util.MultiValueMap<String,String> inputs,Authentication auth,RedirectAttributes f){return action(id,auth,f,()->{rejectReaderDates(inputs);service.amend(id,version,purpose,auth.getName());});}
    @PostMapping({"/reader/requests/{id}/cancel","/archivist/requests/{id}/cancel"}) public String cancel(@PathVariable UUID id,@RequestParam long version,@RequestParam String note,Authentication auth,RedirectAttributes f){
        String result=action(id,auth,f,()->service.cancel(id,version,note,auth.getName()));
        return base(auth).equals("/reader/requests") ? "redirect:/reader/requests" : result;
    }
    @PostMapping("/archivist/requests/{id}/review")
    public String review(@PathVariable UUID id,@RequestParam long version,@RequestParam String result,
            @RequestParam(defaultValue="") String note,@RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate pickupOn,
            @RequestParam(required=false) String pickupRoom,@RequestParam(defaultValue="false") boolean approvalConfirmed,Authentication auth,RedirectAttributes f){
        return action(id,auth,f,()->service.reviewAndSchedule(id,version,result,note,pickupOn,pickupRoom,approvalConfirmed,auth.getName()));
    }
    @GetMapping("/archivist/requests/{id}/circulation")
    public String circulation(@PathVariable UUID id,Authentication auth,Model model){
        model.addAttribute("request",service.detail(id,auth.getName()));model.addAttribute("locations",storage.findAll());return "reading/circulation";
    }
    private String circulationAction(UUID id,Authentication auth,RedirectAttributes flash,Runnable work){
        action(id,auth,flash,work);return "redirect:/archivist/requests/"+id+"/circulation";
    }
    @PostMapping("/director/requests/{id}/decide") public String decide(@PathVariable UUID id,@RequestParam long version,@RequestParam boolean approve,@RequestParam String note,@RequestParam String secret,Authentication auth,RedirectAttributes f){return action(id,auth,f,()->service.decide(id,version,approve,note,secret,auth.getName()));}
    @PostMapping("/archivist/requests/{id}/confirm") public String confirm(@PathVariable UUID id,@RequestParam long version,@RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate pickupOn,@RequestParam String note,Authentication auth,RedirectAttributes f){return action(id,auth,f,()->service.confirm(id,version,pickupOn,note,auth.getName()));}
    // Session methods derive the owning request on the server; never trust a posted request ID for authorization.
    @PostMapping("/archivist/requests/{id}/sessions/{session}/handover") public String hand(@PathVariable UUID id,@PathVariable UUID session,@RequestParam long version,@RequestParam String condition,Authentication auth,RedirectAttributes f){return circulationAction(id,auth,f,()->{owns(id,session,auth);service.handover(session,version,condition,auth.getName());});}
    @PostMapping("/archivist/requests/{id}/sessions/{session}/return") public String receive(@PathVariable UUID id,@PathVariable UUID session,@RequestParam long version,@RequestParam UUID destination,@RequestParam String quality,@RequestParam String condition,Authentication auth,RedirectAttributes f){return circulationAction(id,auth,f,()->{owns(id,session,auth);service.receive(session,version,destination,quality,condition,auth.getName());});}
    @PostMapping("/archivist/requests/{id}/sessions/{session}/exception") public String exception(@PathVariable UUID id,@PathVariable UUID session,@RequestParam long version,@RequestParam String note,@RequestParam(defaultValue="false") boolean noShow,Authentication auth,RedirectAttributes f){return circulationAction(id,auth,f,()->{owns(id,session,auth);service.exception(session,version,note,noShow,auth.getName());});}
    private void owns(UUID id,UUID session,Authentication auth){
        var q=service.detail(id,auth.getName());
        @SuppressWarnings("unchecked") var sessions=(java.util.List<java.util.Map<String,Object>>)q.get("sessions");
        if(sessions.stream().noneMatch(s->s.get("id").equals(session)))throw new BusinessException("Phiên không thuộc yêu cầu đang mở.");
    }
}
