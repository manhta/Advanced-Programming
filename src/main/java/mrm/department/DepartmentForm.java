package mrm.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DepartmentForm {

    @NotBlank(message = "Vui lòng nhập mã khoa/phòng.")
    @Size(min = 2, max = 32, message = "Mã phải có từ 2 đến 32 ký tự.")
    @Pattern(
            regexp = "[A-Za-z0-9_-]+",
            message = "Mã chỉ gồm chữ không dấu, số, dấu _ hoặc -."
    )
    private String code;

    @NotBlank(message = "Vui lòng nhập tên khoa/phòng.")
    @Size(max = 200, message = "Tên không được vượt quá 200 ký tự.")
    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
