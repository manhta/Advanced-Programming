package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Preserve unknown day/month in legacy records; never fabricate January 1. */
public class V18__AddPatientBirthDate extends BaseJavaMigration {
    private static final String SQL="""
        ALTER TABLE medical_records ADD COLUMN birth_date DATE;
        ALTER TABLE medical_records ADD CONSTRAINT medical_record_birth_date CHECK(birth_date IS NULL OR (
            birth_year IS NOT NULL AND EXTRACT(YEAR FROM birth_date)=birth_year
            AND (admission_date IS NULL OR birth_date<=admission_date)
            AND (discharge_date IS NULL OR birth_date<=discharge_date)));
        COMMENT ON COLUMN medical_records.birth_date IS 'Full patient birth date when known. NULL preserves legacy year-only precision.';
        CREATE OR REPLACE VIEW electronic_records AS SELECT id,create_token,create_fingerprint,record_code,patient_name,birth_year,sex,patient_code,
            submission_state AS state,created_by,updated_by,reviewer_id,reviewed_at,review_note,approver_id,decided_at,decision_note,published_at,
            version,created_at,updated_at,department_id,record_type_id,department_name_snapshot,record_type_name_snapshot,deceased,admission_date,discharge_date,source_note,note,
            birth_date FROM medical_records WHERE submission_state IS NOT NULL OFFSET 0;
        """;
    @Override public void migrate(Context c)throws Exception{try(var s=c.getConnection().createStatement()){s.execute(SQL);}}
    @Override public Integer getChecksum(){var crc=new CRC32();crc.update(SQL.getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
}
