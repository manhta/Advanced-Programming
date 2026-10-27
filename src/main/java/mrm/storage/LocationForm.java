package mrm.storage;

import java.util.Locale;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record LocationForm(
        @NotBlank @Pattern(regexp="[A-Z0-9_-]{2,32}", message="Mã gồm 2–32 chữ không dấu, số, _ hoặc -.") String code,
        @NotBlank(message="Cần tên vị trí.") @Size(max=200) String name,
        @NotBlank @Pattern(regexp="WAREHOUSE|SHELF|COMPARTMENT") String type,
        UUID parentId, @Min(value=1, message="Sức chứa phải dương.") Integer capacity) {
    public LocationForm {
        code = code == null ? null : code.strip().toUpperCase(Locale.ROOT);
        name = name == null ? null : name.strip();
    }
}
