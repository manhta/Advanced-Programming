package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V24__CreateStaffChangeRequests extends BaseJavaMigration {
    private static final String SQL="""
        CREATE TABLE staff_change_requests (
            id UUID PRIMARY KEY, submit_token UUID NOT NULL UNIQUE,
            requester_id UUID NOT NULL REFERENCES app_users(id),
            original_role VARCHAR(16) NOT NULL REFERENCES roles(code),
            original_department_id UUID REFERENCES departments(id),
            requested_role VARCHAR(16) NOT NULL CHECK(requested_role IN ('READER','ARCHIVIST')),
            requested_department_id UUID NOT NULL REFERENCES departments(id),
            reason VARCHAR(2000) NOT NULL CHECK(length(btrim(reason))>0),
            state VARCHAR(16) NOT NULL DEFAULT 'SUBMITTED' CHECK(state IN ('SUBMITTED','APPROVED','REJECTED','APPLIED')),
            version BIGINT NOT NULL DEFAULT 0,
            decided_by UUID REFERENCES app_users(id), decided_at TIMESTAMPTZ, decision_note VARCHAR(2000),
            applied_by UUID REFERENCES app_users(id), applied_at TIMESTAMPTZ,
            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            CHECK(requested_role<>original_role OR requested_department_id IS DISTINCT FROM original_department_id),
            CHECK((state='SUBMITTED' AND decided_by IS NULL AND decided_at IS NULL AND decision_note IS NULL)
                OR (state<>'SUBMITTED' AND decided_by IS NOT NULL AND decided_by<>requester_id AND decided_at IS NOT NULL)),
            CHECK(state<>'REJECTED' OR (decision_note IS NOT NULL AND length(btrim(decision_note))>0)),
            CHECK((state='APPLIED' AND applied_by IS NOT NULL AND applied_at IS NOT NULL)
                OR (state<>'APPLIED' AND applied_by IS NULL AND applied_at IS NULL))
        );
        CREATE UNIQUE INDEX ux_staff_change_open ON staff_change_requests(requester_id) WHERE state IN ('SUBMITTED','APPROVED');
        CREATE TABLE staff_change_events (
            id UUID PRIMARY KEY, request_id UUID NOT NULL REFERENCES staff_change_requests(id),
            actor_id UUID NOT NULL REFERENCES app_users(id),
            action VARCHAR(16) NOT NULL CHECK(action IN ('SUBMIT','APPROVE','REJECT','APPLY')),
            details VARCHAR(2000) NOT NULL, occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            UNIQUE(id,request_id)
        );
        ALTER TABLE notifications ADD COLUMN staff_request_id UUID REFERENCES staff_change_requests(id),
            ADD COLUMN staff_event_id UUID;
        ALTER TABLE notifications DROP CONSTRAINT notifications_recipient_role_check;
        ALTER TABLE notifications ADD CONSTRAINT notifications_recipient_role_check CHECK(
            recipient_role IN ('READER','ARCHIVIST','DIRECTOR','ADMIN') AND (recipient_role<>'ADMIN' OR staff_request_id IS NOT NULL));
        ALTER TABLE notifications ADD CONSTRAINT notification_staff_event_fk FOREIGN KEY(staff_event_id,staff_request_id) REFERENCES staff_change_events(id,request_id);
        ALTER TABLE notifications ADD CONSTRAINT notification_staff_event_unique UNIQUE(recipient_id,staff_event_id);
        ALTER TABLE notifications DROP CONSTRAINT notification_target_check;
        ALTER TABLE notifications ADD CONSTRAINT notification_target_check CHECK(
            (request_id IS NOT NULL AND event_id IS NOT NULL AND electronic_record_id IS NULL AND electronic_event_id IS NULL AND staff_request_id IS NULL AND staff_event_id IS NULL)
            OR (request_id IS NULL AND event_id IS NULL AND electronic_record_id IS NOT NULL AND electronic_event_id IS NOT NULL AND staff_request_id IS NULL AND staff_event_id IS NULL)
            OR (request_id IS NULL AND event_id IS NULL AND electronic_record_id IS NULL AND electronic_event_id IS NULL AND staff_request_id IS NOT NULL AND staff_event_id IS NOT NULL));
        CREATE INDEX ix_staff_change_queue ON staff_change_requests(state,created_at,id);
        """;
    @Override public void migrate(Context context)throws Exception {try(var s=context.getConnection().createStatement()){s.execute(SQL);}}
    @Override public Integer getChecksum(){var crc=new CRC32();crc.update(SQL.getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
}
