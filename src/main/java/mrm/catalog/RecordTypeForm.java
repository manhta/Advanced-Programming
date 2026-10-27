package mrm.catalog;

import java.util.Locale;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecordTypeForm(
        @NotBlank(message = "Vui lòng nhập mã.")
        @Pattern(regexp = "[A-Z0-9_-]{2,32}", message = "Mã gồm 2–32 chữ không dấu, số, _ hoặc -.") String code,
        @NotBlank(message = "Vui lòng nhập tên.")
        @Size(max = 200, message = "Tên tối đa 200 ký tự.") String name) {
    public RecordTypeForm {
        code = code == null ? null : code.strip().toUpperCase(Locale.ROOT);
        name = name == null ? null : name.strip();
    }
}
