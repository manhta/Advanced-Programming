package mrm.catalog;

import java.time.LocalDate;
import java.util.UUID;

public record RetentionPolicy(UUID id, String code, String name, int versionNo,
        int years, LocalDate effectiveFrom, LocalDate effectiveTo, String basis,
        String status, boolean active, String createdBy, String approvedBy) { }
