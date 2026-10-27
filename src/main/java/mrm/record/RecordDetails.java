package mrm.record;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import mrm.catalog.CatalogSnapshot;

public record RecordDetails(UUID id, RecordForm form, String state, String physicalStatus,
        String departmentName, String recordTypeName, String locationPath, Integer retentionYears, LocalDate retentionDueOn,
        List<CatalogSnapshot.PolicySnapshot> policies, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) { }
