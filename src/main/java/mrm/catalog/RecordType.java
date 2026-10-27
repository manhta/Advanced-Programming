package mrm.catalog;

import java.util.UUID;

public record RecordType(UUID id, String code, String name, boolean active, long version) { }
