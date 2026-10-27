package mrm.record;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

public record RecordForm(
        @NotNull UUID operationKey, @NotNull @Min(0) Long version,
        @Size(max=64) @Pattern(regexp="[A-Z0-9/_-]{2,64}|",message="Số lưu trữ gồm 2–64 chữ không dấu, số, /, _ hoặc -.") String archiveNumber,
        @Size(max=64) String medicalRecordNumber, @Size(max=64) String patientCode,
        @NotBlank(message="Cần họ tên người bệnh.") @Size(max=200) String patientName,
        @Min(1800) @Max(9999) Integer birthYear, @Pattern(regexp="MALE|FEMALE|UNKNOWN") String sex,
        UUID departmentId, UUID recordTypeId, boolean deceased,
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate admissionDate,
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dischargeDate,
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate firstStoredOn,
        List<@NotNull UUID> policyIds, UUID locationId,
        @Pattern(regexp="NORMAL|DAMAGED|INCOMPLETE") String qualityStatus,
        @Size(max=1000) String sourceNote, @Size(max=2000) String note, @Size(max=1000) String reason) {
    public RecordForm {
        archiveNumber=clean(archiveNumber); if(archiveNumber!=null) archiveNumber=archiveNumber.toUpperCase(Locale.ROOT);
        medicalRecordNumber=clean(medicalRecordNumber); patientCode=clean(patientCode); patientName=clean(patientName);
        sex=sex==null ? "UNKNOWN" : sex; qualityStatus=qualityStatus==null ? "NORMAL" : qualityStatus;
        sourceNote=clean(sourceNote); note=clean(note); reason=clean(reason);
        policyIds=policyIds==null ? List.of() : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(policyIds));
    }
    private static String clean(String value) { return value==null || value.isBlank() ? null : value.strip(); }
    public static RecordForm empty() {
        return new RecordForm(UUID.randomUUID(),0L,null,null,null,null,null,"UNKNOWN",null,null,false,
                null,null,null,List.of(),null,"NORMAL",null,null,null);
    }
}
