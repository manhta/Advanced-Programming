package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V7__CreateMedicalRecordStorage extends BaseJavaMigration {
    private static final String SQL = """
            CREATE TABLE storage_locations (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                parent_id UUID REFERENCES storage_locations(id),
                code VARCHAR(32) NOT NULL CHECK(code ~ '^[A-Z0-9_-]{2,32}$'),
                name VARCHAR(200) NOT NULL CHECK(length(btrim(name)) > 0),
                type VARCHAR(20) NOT NULL CHECK(type IN ('WAREHOUSE','SHELF','COMPARTMENT')),
                active BOOLEAN NOT NULL DEFAULT TRUE,
                capacity INTEGER CHECK(capacity > 0),
                version BIGINT NOT NULL DEFAULT 0 CHECK(version >= 0),
                created_by UUID NOT NULL REFERENCES app_users(id),
                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE NULLS NOT DISTINCT(parent_id, code),
                CHECK((type='WAREHOUSE' AND parent_id IS NULL) OR (type<>'WAREHOUSE' AND parent_id IS NOT NULL)),
                CHECK(capacity IS NULL OR type='COMPARTMENT')
            );
            CREATE FUNCTION check_storage_parent() RETURNS trigger LANGUAGE plpgsql AS $$
            DECLARE parent_type TEXT;
            BEGIN
                IF TG_OP='UPDATE' AND (NEW.parent_id IS DISTINCT FROM OLD.parent_id OR NEW.type<>OLD.type OR NEW.code<>OLD.code) THEN
                    RAISE EXCEPTION 'Storage code, type and parent cannot change' USING ERRCODE='23514';
                END IF;
                IF NEW.parent_id IS NOT NULL THEN
                    SELECT type INTO parent_type FROM storage_locations WHERE id=NEW.parent_id;
                    IF (NEW.type='SHELF' AND parent_type IS DISTINCT FROM 'WAREHOUSE')
                        OR (NEW.type='COMPARTMENT' AND parent_type IS DISTINCT FROM 'SHELF') THEN
                        RAISE EXCEPTION 'Invalid storage hierarchy' USING ERRCODE='23514';
                    END IF;
                END IF;
                RETURN NEW;
            END;
            $$;
            CREATE TRIGGER storage_parent_guard BEFORE INSERT OR UPDATE ON storage_locations
                FOR EACH ROW EXECUTE FUNCTION check_storage_parent();
            CREATE TABLE archive_counters (period CHAR(7) PRIMARY KEY, last_value INTEGER NOT NULL CHECK(last_value BETWEEN 1 AND 999999));
            CREATE TABLE medical_records (
                id UUID PRIMARY KEY,
                create_token UUID NOT NULL UNIQUE,
                create_fingerprint VARCHAR(64) NOT NULL,
                archive_number VARCHAR(64) UNIQUE CHECK(archive_number ~ '^[A-Z0-9/_-]{2,64}$'),
                medical_record_number VARCHAR(64), patient_code VARCHAR(64),
                patient_name VARCHAR(200) NOT NULL CHECK(length(btrim(patient_name)) > 0),
                birth_year SMALLINT CHECK(birth_year BETWEEN 1800 AND 9999),
                sex VARCHAR(10) NOT NULL CHECK(sex IN ('MALE','FEMALE','UNKNOWN')),
                department_id UUID REFERENCES departments(id),
                record_type_id UUID REFERENCES record_types(id),
                deceased BOOLEAN NOT NULL DEFAULT FALSE,
                admission_date DATE, discharge_date DATE, first_stored_on DATE,
                current_location_id UUID REFERENCES storage_locations(id),
                department_name_snapshot VARCHAR(200), record_type_code_snapshot VARCHAR(32), record_type_name_snapshot VARCHAR(200),
                retention_years_snapshot INTEGER CHECK(retention_years_snapshot > 0), retention_due_on DATE,
                record_state VARCHAR(16) NOT NULL CHECK(record_state IN ('DRAFT','STORED')),
                physical_status VARCHAR(20) NOT NULL CHECK(physical_status IN ('NOT_STORED','IN_STORAGE','UNAVAILABLE','READING')),
                quality_status VARCHAR(20) NOT NULL CHECK(quality_status IN ('NORMAL','DAMAGED','INCOMPLETE')),
                source_note VARCHAR(1000), note VARCHAR(2000),
                draft_policy_ids UUID[] NOT NULL DEFAULT '{}',
                version BIGINT NOT NULL DEFAULT 0 CHECK(version >= 0),
                created_by UUID NOT NULL REFERENCES app_users(id), updated_by UUID NOT NULL REFERENCES app_users(id),
                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CHECK(discharge_date IS NULL OR admission_date IS NULL OR discharge_date >= admission_date),
                CHECK(first_stored_on IS NULL OR discharge_date IS NULL OR first_stored_on >= discharge_date),
                CHECK((record_state='DRAFT' AND physical_status='NOT_STORED' AND current_location_id IS NULL)
                    OR (record_state='STORED' AND physical_status<>'NOT_STORED' AND archive_number IS NOT NULL
                        AND medical_record_number IS NOT NULL AND birth_year IS NOT NULL AND department_id IS NOT NULL
                        AND record_type_id IS NOT NULL AND admission_date IS NOT NULL AND discharge_date IS NOT NULL
                        AND first_stored_on IS NOT NULL AND current_location_id IS NOT NULL
                        AND retention_years_snapshot IS NOT NULL AND retention_due_on IS NOT NULL))
            );
            CREATE TABLE medical_record_policies (
                record_id UUID NOT NULL REFERENCES medical_records(id),
                policy_id UUID NOT NULL REFERENCES retention_policies(id),
                code_snapshot VARCHAR(32) NOT NULL, name_snapshot VARCHAR(200) NOT NULL,
                version_snapshot INTEGER NOT NULL, years_snapshot INTEGER NOT NULL,
                effective_from_snapshot DATE NOT NULL, effective_to_snapshot DATE, basis_snapshot VARCHAR(2000) NOT NULL,
                PRIMARY KEY(record_id, policy_id)
            );
            CREATE TABLE record_events (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(), record_id UUID NOT NULL REFERENCES medical_records(id),
                action VARCHAR(32) NOT NULL, actor_username VARCHAR(64) NOT NULL,
                reason VARCHAR(1000) NOT NULL, before_data JSONB, after_data JSONB,
                occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            CREATE TABLE location_events (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(), location_id UUID NOT NULL REFERENCES storage_locations(id),
                action VARCHAR(32) NOT NULL, actor_username VARCHAR(64) NOT NULL,
                details TEXT NOT NULL, occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            CREATE TABLE record_location_history (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(), record_id UUID NOT NULL REFERENCES medical_records(id),
                from_location_id UUID REFERENCES storage_locations(id), to_location_id UUID NOT NULL REFERENCES storage_locations(id),
                from_path TEXT, to_path TEXT NOT NULL, actor_username VARCHAR(64) NOT NULL,
                reason VARCHAR(1000) NOT NULL, operation_key UUID NOT NULL UNIQUE,
                occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            CREATE FUNCTION mrm_search_key(value TEXT) RETURNS TEXT LANGUAGE SQL IMMUTABLE PARALLEL SAFE AS $$
                SELECT regexp_replace(translate(normalize(lower(coalesce(value, '')), NFD), 'đ', 'd'), U&'[\\0300-\\036F]', '', 'g');
            $$;
            CREATE INDEX ix_records_patient_code ON medical_records(patient_code);
            CREATE INDEX ix_records_department_discharge ON medical_records(department_id, discharge_date DESC, id);
            CREATE INDEX ix_records_location ON medical_records(current_location_id) WHERE record_state='STORED';
            CREATE INDEX ix_record_events ON record_events(record_id, occurred_at, id);
            CREATE INDEX ix_record_location_history ON record_location_history(record_id, occurred_at, id);
            """;
    @Override public void migrate(Context context) throws Exception {
        try (var statement = context.getConnection().createStatement()) { statement.execute(SQL); }
    }
    @Override public Integer getChecksum() {
        var checksum = new CRC32(); checksum.update(SQL.getBytes(StandardCharsets.UTF_8)); return (int) checksum.getValue();
    }
}
