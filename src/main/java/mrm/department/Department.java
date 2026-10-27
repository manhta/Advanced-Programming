package mrm.department;

import java.time.Instant;
import java.util.UUID;

public record Department(
        UUID id,
        String code,
        String name,
        boolean active,
        String createdBy,
        Instant createdAt
) {
}
