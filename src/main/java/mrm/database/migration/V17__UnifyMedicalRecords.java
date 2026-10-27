package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Merge records, retaining identities and foreign keys; never infer patient identity from names. */
public class V17__UnifyMedicalRecords extends BaseJavaMigration {
    private static final String SQL="""
        ALTER TABLE medical_records
            ADD COLUMN record_code VARCHAR(64),
            ADD COLUMN has_paper_copy BOOLEAN NOT NULL DEFAULT TRUE,
            ADD COLUMN has_electronic_copy BOOLEAN NOT NULL DEFAULT TRUE,
            ADD COLUMN submission_state VARCHAR(20),
            ADD COLUMN reviewer_id UUID REFERENCES app_users(id), ADD COLUMN reviewed_at TIMESTAMPTZ, ADD COLUMN review_note VARCHAR(2000),
            ADD COLUMN approver_id UUID REFERENCES app_users(id), ADD COLUMN decided_at TIMESTAMPTZ, ADD COLUMN decision_note VARCHAR(2000), ADD COLUMN published_at TIMESTAMPTZ;
        UPDATE medical_records SET record_code=COALESCE(archive_number,'MRM-PAPER-'||upper(replace(id::text,'-','')));
        DO $$ DECLARE c RECORD; BEGIN
            FOR c IN SELECT conname FROM pg_constraint WHERE conrelid='medical_records'::regclass AND contype='c'
                AND (pg_get_constraintdef(oid) LIKE '%record_state%' OR pg_get_constraintdef(oid) LIKE '%physical_status%')
            LOOP EXECUTE format('ALTER TABLE medical_records DROP CONSTRAINT %I',c.conname); END LOOP;
            IF EXISTS(SELECT 1 FROM electronic_records e JOIN medical_records r ON r.id=e.id OR r.create_token=e.create_token OR r.record_code=e.record_code) THEN
                RAISE EXCEPTION 'Cannot merge: record ID, create token or record code conflicts; reconcile explicitly before upgrading.';
            END IF;
        END $$;
        ALTER TABLE medical_records ADD CONSTRAINT medical_record_code_unique UNIQUE(record_code);
        INSERT INTO medical_records(id,create_token,create_fingerprint,record_code,medical_record_number,patient_code,patient_name,birth_year,sex,
            department_id,record_type_id,department_name_snapshot,record_type_name_snapshot,deceased,admission_date,discharge_date,source_note,note,
            record_state,physical_status,quality_status,has_paper_copy,has_electronic_copy,submission_state,
            created_by,updated_by,reviewer_id,reviewed_at,review_note,approver_id,decided_at,decision_note,published_at,version,created_at,updated_at)
        SELECT id,create_token,create_fingerprint,record_code,record_code,patient_code,patient_name,birth_year,sex,
            department_id,record_type_id,department_name_snapshot,record_type_name_snapshot,deceased,admission_date,discharge_date,source_note,note,
            CASE WHEN state='APPROVED' THEN 'PUBLISHED' ELSE 'DRAFT' END,'NOT_APPLICABLE','NORMAL',FALSE,TRUE,state,
            created_by,updated_by,reviewer_id,reviewed_at,review_note,approver_id,decided_at,decision_note,published_at,version,created_at,updated_at
        FROM electronic_records;
        ALTER TABLE medical_records ALTER COLUMN record_code SET NOT NULL;
        ALTER TABLE medical_records ADD CONSTRAINT medical_record_format CHECK(has_paper_copy OR has_electronic_copy),
            ADD CONSTRAINT medical_record_state CHECK(record_state IN ('DRAFT','STORED','PUBLISHED')),
            ADD CONSTRAINT medical_record_physical CHECK(physical_status IN ('NOT_STORED','IN_STORAGE','UNAVAILABLE','READING','NOT_APPLICABLE')),
            ADD CONSTRAINT medical_record_storage CHECK(
                (has_paper_copy AND record_state='DRAFT' AND physical_status='NOT_STORED' AND current_location_id IS NULL)
                OR (has_paper_copy AND record_state='STORED' AND physical_status IN ('IN_STORAGE','UNAVAILABLE','READING')
                    AND archive_number IS NOT NULL AND medical_record_number IS NOT NULL AND birth_year IS NOT NULL AND department_id IS NOT NULL
                    AND record_type_id IS NOT NULL AND admission_date IS NOT NULL AND discharge_date IS NOT NULL AND first_stored_on IS NOT NULL
                    AND current_location_id IS NOT NULL AND retention_years_snapshot IS NOT NULL AND retention_due_on IS NOT NULL)
                OR (NOT has_paper_copy AND has_electronic_copy AND record_state IN ('DRAFT','PUBLISHED') AND physical_status='NOT_APPLICABLE'
                    AND archive_number IS NULL AND current_location_id IS NULL AND first_stored_on IS NULL AND retention_years_snapshot IS NULL AND retention_due_on IS NULL)),
            ADD CONSTRAINT medical_record_submission CHECK(submission_state IS NULL OR (
                submission_state IN ('SUBMITTED','NEEDS_INFO','PENDING_APPROVAL','APPROVED','REJECTED','CANCELLED') AND
                ((submission_state='APPROVED' AND record_state IN ('PUBLISHED','STORED') AND published_at IS NOT NULL AND approver_id IS NOT NULL
                    AND decided_at IS NOT NULL AND reviewer_id IS NOT NULL AND reviewed_at IS NOT NULL)
                OR (submission_state<>'APPROVED' AND record_state='DRAFT' AND published_at IS NULL))));
        CREATE FUNCTION mrm_assign_record_code() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN
            IF TG_OP='INSERT' AND NEW.record_code IS NULL THEN NEW.record_code=COALESCE(NEW.archive_number,'MRM-PAPER-'||upper(replace(NEW.id::text,'-',''))); END IF;
            IF TG_OP='UPDATE' AND NEW.record_code IS DISTINCT FROM OLD.record_code THEN RAISE EXCEPTION 'Record code is immutable'; END IF;
            RETURN NEW;
        END $$;
        CREATE TRIGGER assign_record_code BEFORE INSERT OR UPDATE ON medical_records FOR EACH ROW EXECUTE FUNCTION mrm_assign_record_code();
        ALTER TABLE electronic_record_events DROP CONSTRAINT electronic_record_events_record_id_fkey;
        ALTER TABLE electronic_record_events ADD CONSTRAINT electronic_record_events_record_id_fkey FOREIGN KEY(record_id) REFERENCES medical_records(id);
        ALTER TABLE notifications DROP CONSTRAINT notifications_electronic_record_id_fkey;
        ALTER TABLE notifications ADD CONSTRAINT notifications_electronic_record_id_fkey FOREIGN KEY(electronic_record_id) REFERENCES medical_records(id);
        DROP TABLE electronic_records;
        ALTER TABLE electronic_record_counters RENAME TO record_code_counters;
        CREATE INDEX ix_records_submission ON medical_records(submission_state,created_at DESC);
        -- A read-only compatibility projection, not a second storage table. Old links/history readers remain usable.
        CREATE VIEW electronic_records AS SELECT id,create_token,create_fingerprint,record_code,patient_name,birth_year,sex,patient_code,
            submission_state AS state,created_by,updated_by,reviewer_id,reviewed_at,review_note,approver_id,decided_at,decision_note,published_at,
            version,created_at,updated_at,department_id,record_type_id,department_name_snapshot,record_type_name_snapshot,deceased,admission_date,discharge_date,source_note,note
            FROM medical_records WHERE submission_state IS NOT NULL OFFSET 0;
        COMMENT ON VIEW electronic_records IS 'Read-only compatibility projection; all patient records are stored once in medical_records.';
        """;
    @Override public void migrate(Context c)throws Exception{try(var s=c.getConnection().createStatement()){s.execute(SQL);}}
    @Override public Integer getChecksum(){var crc=new CRC32();crc.update(SQL.getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
}
