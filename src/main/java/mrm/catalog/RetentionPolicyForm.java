package mrm.catalog;

import java.time.LocalDate;
import java.util.Locale;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

public record RetentionPolicyForm(
        @NotBlank(message = "Vui lòng nhập mã.")
        @Pattern(regexp = "[A-Z0-9_-]{2,32}", message = "Mã gồm 2–32 chữ không dấu, số, _ hoặc -.") String code,
        @NotBlank(message = "Vui lòng nhập tên.") @Size(max = 200) String name,
        @NotNull(message = "Vui lòng nhập phiên bản.") @Min(value = 1, message = "Phiên bản phải dương.") Integer versionNo,
        @NotNull(message = "Vui lòng nhập số năm.") @Min(value = 1, message = "Số năm phải dương.") @Max(32767) Integer years,
        @NotNull(message = "Vui lòng nhập ngày hiệu lực.") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveTo,
        @NotBlank(message = "Vui lòng ghi căn cứ và phạm vi áp dụng.") @Size(max = 2000) String basis) {
    public RetentionPolicyForm {
        code = code == null ? null : code.strip().toUpperCase(Locale.ROOT);
        name = name == null ? null : name.strip();
        basis = basis == null ? null : basis.strip();
    }
}
