package mrm.electronic;

import java.util.UUID;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.*;

public record ElectronicRecordForm(@NotNull UUID token,@NotNull @Min(0) Long version,
        @NotBlank @Size(max=200) String patientName,@NotNull @Min(1800) @Max(9999) Integer birthYear,
        @NotNull @Pattern(regexp="MALE|FEMALE|UNKNOWN") String sex,@Size(max=64) String patientCode,
        UUID departmentId,UUID recordTypeId,Boolean deceased,
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate admissionDate,
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dischargeDate,
        @Size(max=1000) String sourceNote,@Size(max=2000) String note,
        @NotNull @Min(1) @Max(31) Integer birthDay,@NotNull @Min(1) @Max(12) Integer birthMonth) {
    public ElectronicRecordForm {
        patientName=patientName==null?null:patientName.strip();
        patientCode=patientCode==null || patientCode.isBlank()?null:patientCode.strip();
        sourceNote=clean(sourceNote);note=clean(note);
        deceased=Boolean.TRUE.equals(deceased);
    }
    private static String clean(String text){return text==null || text.isBlank()?null:text.strip();}
    public ElectronicRecordForm(UUID token,Long version,String patientName,Integer birthYear,String sex,String patientCode,
            UUID departmentId,UUID recordTypeId,Boolean deceased,LocalDate admissionDate,LocalDate dischargeDate,String sourceNote,String note){
        this(token,version,patientName,birthYear,sex,patientCode,departmentId,recordTypeId,deceased,admissionDate,dischargeDate,sourceNote,note,null,null);
    }
    public ElectronicRecordForm(UUID token,Long version,String patientName,Integer birthYear,String sex,String patientCode){this(token,version,patientName,birthYear,sex,patientCode,null,null,false,null,null,null,null,null,null);}
    public static ElectronicRecordForm empty(){return new ElectronicRecordForm(UUID.randomUUID(),0L,null,null,"UNKNOWN",null);}
}
