package mrm.record;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public record RecordFilter(String q, UUID departmentId, UUID recordTypeId, String physicalStatus, String state,
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
        Integer page, Integer size, String sort, Integer birthYear,String format,String submissionState,String scope) {
    public RecordFilter(String q,UUID departmentId,UUID recordTypeId,String physicalStatus,String state,LocalDate from,LocalDate to,Integer page,Integer size,String sort,Integer birthYear,String format,String submissionState){this(q,departmentId,recordTypeId,physicalStatus,state,from,to,page,size,sort,birthYear,format,submissionState,"OFFICIAL");}
    public RecordFilter(String q,UUID departmentId,UUID recordTypeId,String physicalStatus,String state,LocalDate from,LocalDate to,Integer page,Integer size,String sort,Integer birthYear){this(q,departmentId,recordTypeId,physicalStatus,state,from,to,page,size,sort,birthYear,null,null);}
    public RecordFilter(String q, UUID departmentId, UUID recordTypeId, String physicalStatus, String state,
            LocalDate from, LocalDate to, Integer page, Integer size, String sort) {
        this(q, departmentId, recordTypeId, physicalStatus, state, from, to, page, size, sort, null);
    }
    public RecordFilter {
        q=q==null ? "" : q.strip(); page=page==null ? 0 : page; size=size==null ? 10 : size;
        sort=sort==null ? "newest" : sort;
        format=format==null?"":format;submissionState=submissionState==null?"":submissionState;
        scope=scope==null || scope.isBlank()?"OFFICIAL":scope;
    }
}
