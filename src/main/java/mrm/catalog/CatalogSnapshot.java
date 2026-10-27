package mrm.catalog;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Immutable values to persist with the future UC-04 record transaction. */
public record CatalogSnapshot(UUID departmentId, String departmentName,
        UUID recordTypeId, String recordTypeCode, String recordTypeName,
        List<PolicySnapshot> policies, int retentionYears, LocalDate firstStoredOn, LocalDate dueOn) {
    public CatalogSnapshot { policies = List.copyOf(policies); }
    public record PolicySnapshot(UUID id, String code, String name, int version, int years,
            LocalDate effectiveFrom, LocalDate effectiveTo, String basis) { }
}
