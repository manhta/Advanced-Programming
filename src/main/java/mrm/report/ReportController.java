package mrm.report;

import java.time.LocalDate;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import mrm.audit.AuditService;

@Controller
public class ReportController {
    private final ReportService reports;private final AuditService audit;
    public ReportController(ReportService reports,AuditService audit){this.reports=reports;this.audit=audit;}
    @GetMapping({"/archivist/reports","/admin/reports"}) public Object report(
            @RequestParam(defaultValue="audit") String type,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String actor,
            @RequestParam(defaultValue="") String action,@RequestParam(defaultValue="") String entity,@RequestParam(defaultValue="") String state,
            @RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue="false") boolean download,@RequestParam(defaultValue="false") boolean print,Authentication auth,Model model) {
        var filter=new ReportService.Filter(q,actor,action,entity,from,to,state);var rows=reports.rows(type,filter,auth.getName());
        audit.log(auth.getName(),download?"REPORT_EXPORT":print?"REPORT_PRINT":"REPORT_VIEW","REPORT",type,"Bộ lọc ngày "+from+" đến "+to+"; số dòng "+rows.size());
        if(download)return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"mrm-"+type+".csv\"").contentType(new MediaType("text","csv",java.nio.charset.StandardCharsets.UTF_8)).body(ReportService.csv(rows));
        model.addAttribute("rows",rows);model.addAttribute("columns",rows.isEmpty()?java.util.List.of():rows.getFirst().keySet());model.addAttribute("types",ReportService.TYPES);
        model.addAttribute("type",type);model.addAttribute("filter",filter);model.addAttribute("print",print);return "report/list";
    }
}
