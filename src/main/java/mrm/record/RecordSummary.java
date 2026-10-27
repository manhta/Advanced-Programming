package mrm.record;

import java.time.LocalDate;
import java.util.UUID;

/** Deliberately excludes location, internal notes, policy/audit and creator details. */
public record RecordSummary(UUID id, String archiveNumber, String medicalRecordNumber, String patientCode,
        String patientName, Integer birthYear, String sex, String departmentName, String recordTypeName,
        LocalDate admissionDate, LocalDate dischargeDate, boolean deceased, String state, String physicalStatus,
        String recordCode,boolean hasPaperCopy,boolean hasElectronicCopy,String submissionState,boolean borrowable,String borrowingStatus,LocalDate birthDate) {
    public String displayCode(){return recordCode;}
    public String birthLabel(){return birthDate!=null ? birthDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : (birthYear==null ? "Chưa có" : birthYear+" (chỉ biết năm)");}
    public String formatLabel(){return hasPaperCopy ? (hasElectronicCopy ? "Giấy và điện tử" : "Giấy") : "Điện tử";}
    public String borrowingLabel(){
        return switch(borrowingStatus){
            case "BORROWED" -> "Đã được mượn";
            case "SCHEDULED" -> "Đã xác nhận lịch mượn";
            case "RESERVED" -> "Đã đăng ký mượn";
            default -> "Chưa đủ điều kiện";
        };
    }
}
