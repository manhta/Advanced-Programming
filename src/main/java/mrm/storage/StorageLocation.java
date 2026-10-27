package mrm.storage;

import java.util.UUID;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public record StorageLocation(UUID id, UUID parentId, String code, String name, String type,
        boolean active, Integer capacity, long version, String path, long occupied) {
    private static final Map<String,String> VIETNAMESE_DEPARTMENTS = Map.ofEntries(
            Map.entry("GYNECOLOGY", "Sản phụ khoa"),
            Map.entry("INFECTIOUS", "Truyền nhiễm"),
            Map.entry("INFECTIOUS_DISEASES", "Truyền nhiễm"),
            Map.entry("INTERNAL_MEDICINE", "Nội tổng hợp"),
            Map.entry("OBSTETRICS", "Sản khoa"),
            Map.entry("PEDIATRICS", "Nhi khoa"),
            Map.entry("PEDIATRICS_RH", "Nhi và sức khỏe sinh sản"),
            Map.entry("SURGERY", "Ngoại khoa"),
            Map.entry("GENERAL_SURGERY", "Ngoại tổng hợp"),
            Map.entry("EMERGENCY", "Cấp cứu"));

    /** Human-facing path for the circulation form; database codes remain unchanged. */
    public String displayPath() {
        return Arrays.stream(path.split(" / "))
                .map(StorageLocation::displaySegment)
                .collect(Collectors.joining(" / "));
    }

    private static String displaySegment(String segment) {
        int separator = segment.indexOf(" · ");
        if (separator < 0) return cleanName(segment);
        String locationCode = segment.substring(0, separator);
        if (locationCode.equals("DEMO_WAREHOUSE")) return "Kho lưu trữ";
        if (locationCode.equals("DEMO_SHELF")) return "Kệ hồ sơ";
        if (locationCode.startsWith("DEMO_")) {
            String departmentCode = locationCode.substring("DEMO_".length());
            return "Ngăn " + VIETNAMESE_DEPARTMENTS.getOrDefault(departmentCode, "lưu trữ");
        }
        return cleanName(segment.substring(separator + 3));
    }

    private static String cleanName(String value) {
        return value.replaceAll("(?i)DEMO[_ -]*", "")
                .replace("mẫu — dữ liệu giả lập", "lưu trữ")
                .replace("mẫu", "lưu trữ")
                .trim();
    }
}
