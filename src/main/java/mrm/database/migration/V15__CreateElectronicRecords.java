package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V15__CreateElectronicRecords extends BaseJavaMigration {
    private static final String SQL="""
        CREATE TABLE electronic_record_counters(year INTEGER PRIMARY KEY,last_value BIGINT NOT NULL CHECK(last_value>0));
        CREATE TABLE electronic_records (
            id UUID PRIMARY KEY, create_token UUID UNIQUE NOT NULL, create_fingerprint VARCHAR(64) NOT NULL,
            record_code VARCHAR(64) UNIQUE NOT NULL,
            patient_name VARCHAR(200) NOT NULL CHECK(length(btrim(patient_name))>0),
            birth_year INTEGER NOT NULL CHECK(birth_year BETWEEN 1800 AND 9999),
            sex VARCHAR(10) NOT NULL CHECK(sex IN ('MALE','FEMALE','UNKNOWN')), patient_code VARCHAR(64),
            state VARCHAR(20) NOT NULL CHECK(state IN ('SUBMITTED','NEEDS_INFO','PENDING_APPROVAL','APPROVED','REJECTED','CANCELLED')),
            created_by UUID NOT NULL REFERENCES app_users(id), updated_by UUID NOT NULL REFERENCES app_users(id),
            reviewer_id UUID REFERENCES app_users(id), reviewed_at TIMESTAMPTZ, review_note VARCHAR(2000),
            approver_id UUID REFERENCES app_users(id), decided_at TIMESTAMPTZ, decision_note VARCHAR(2000), published_at TIMESTAMPTZ,
            version BIGINT NOT NULL DEFAULT 0 CHECK(version>=0),
            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            CHECK((state='APPROVED' AND published_at IS NOT NULL AND approver_id IS NOT NULL AND decided_at IS NOT NULL AND reviewer_id IS NOT NULL AND reviewed_at IS NOT NULL)
                OR (state<>'APPROVED' AND published_at IS NULL))
        );
        CREATE INDEX ix_electronic_owner ON electronic_records(created_by,created_at DESC);
        CREATE INDEX ix_electronic_state ON electronic_records(state,created_at DESC);
        CREATE TABLE electronic_record_events (
            id UUID PRIMARY KEY, record_id UUID NOT NULL REFERENCES electronic_records(id),
            actor VARCHAR(64) NOT NULL, action VARCHAR(40) NOT NULL, details VARCHAR(2000) NOT NULL,
            version BIGINT NOT NULL, snapshot JSONB NOT NULL, occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            UNIQUE(id,record_id)
        );
        CREATE INDEX ix_electronic_events ON electronic_record_events(record_id,occurred_at,id);
        ALTER TABLE notifications ALTER COLUMN request_id DROP NOT NULL, ALTER COLUMN event_id DROP NOT NULL;
        ALTER TABLE notifications ADD COLUMN electronic_record_id UUID REFERENCES electronic_records(id), ADD COLUMN electronic_event_id UUID;
        ALTER TABLE notifications ADD CONSTRAINT notification_electronic_event_fk FOREIGN KEY(electronic_event_id,electronic_record_id) REFERENCES electronic_record_events(id,record_id);
        ALTER TABLE notifications ADD CONSTRAINT notification_target_check CHECK(
            (request_id IS NOT NULL AND event_id IS NOT NULL AND electronic_record_id IS NULL AND electronic_event_id IS NULL)
            OR (request_id IS NULL AND event_id IS NULL AND electronic_record_id IS NOT NULL AND electronic_event_id IS NOT NULL));
        ALTER TABLE notifications ADD CONSTRAINT notification_electronic_unique UNIQUE(recipient_id,electronic_event_id);
        """;
    @Override public void migrate(Context context)throws Exception {try(var s=context.getConnection().createStatement()){s.execute(SQL);}}
    @Override public Integer getChecksum(){var crc=new CRC32();crc.update(SQL.getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
}
