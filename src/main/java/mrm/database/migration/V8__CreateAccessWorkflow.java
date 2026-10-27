package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Functional expansion; existing migrations and patient data are preserved. */
public class V8__CreateAccessWorkflow extends BaseJavaMigration {
    private static final String SQL = """
        ALTER TABLE app_users ADD COLUMN email VARCHAR(254), ADD COLUMN department_id UUID REFERENCES departments(id),
            ADD COLUMN approval_secret_hash TEXT, ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
        CREATE TABLE audit_events (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(), actor VARCHAR(64) NOT NULL,
            action VARCHAR(64) NOT NULL, entity_type VARCHAR(40) NOT NULL, entity_id VARCHAR(100),
            outcome VARCHAR(16) NOT NULL CHECK(outcome IN ('SUCCESS','FAILURE')),
            details TEXT NOT NULL, occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        CREATE INDEX ix_audit_lookup ON audit_events(actor,occurred_at DESC);
        CREATE TABLE reading_requests (
            id UUID PRIMARY KEY, submit_token UUID NOT NULL UNIQUE,
            record_id UUID NOT NULL REFERENCES medical_records(id), requester_id UUID NOT NULL REFERENCES app_users(id),
            purpose VARCHAR(2000) NOT NULL CHECK(length(btrim(purpose))>0),
            state VARCHAR(20) NOT NULL CHECK(state IN ('SUBMITTED','NEEDS_INFO','PENDING_APPROVAL','APPROVED','REJECTED','CONFIRMED','CANCELLED')),
            revision INTEGER NOT NULL DEFAULT 1, version BIGINT NOT NULL DEFAULT 0,
            reviewer_id UUID REFERENCES app_users(id), reviewed_at TIMESTAMPTZ, review_note VARCHAR(2000),
            approver_id UUID REFERENCES app_users(id), decided_at TIMESTAMPTZ, decision_note VARCHAR(2000),
            confirmation_note VARCHAR(2000), confirmed_by UUID REFERENCES app_users(id), confirmed_at TIMESTAMPTZ,
            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE reading_sessions (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(), request_id UUID NOT NULL REFERENCES reading_requests(id),
            record_id UUID NOT NULL REFERENCES medical_records(id), reading_on DATE NOT NULL,
            state VARCHAR(16) NOT NULL CHECK(state IN ('REQUESTED','CONFIRMED','READING','RETURNED','CANCELLED','NO_SHOW')),
            handed_at TIMESTAMPTZ, handed_by UUID REFERENCES app_users(id), received_by UUID REFERENCES app_users(id),
            returned_at TIMESTAMPTZ, returned_by UUID REFERENCES app_users(id),
            hand_condition VARCHAR(1000), return_condition VARCHAR(1000), return_location_id UUID REFERENCES storage_locations(id),
            exception_note VARCHAR(2000), exception_at TIMESTAMPTZ,
            UNIQUE(request_id,reading_on)
        );
        CREATE UNIQUE INDEX ux_daily_reading ON reading_sessions(record_id,reading_on) WHERE state IN ('CONFIRMED','READING','RETURNED');
        CREATE UNIQUE INDEX ux_one_open_handover ON reading_sessions(record_id) WHERE state='READING';
        CREATE TABLE request_events (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(), request_id UUID NOT NULL REFERENCES reading_requests(id),
            actor VARCHAR(64) NOT NULL, action VARCHAR(40) NOT NULL, revision INTEGER NOT NULL,
            details TEXT NOT NULL, occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE inventory_batches (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name VARCHAR(200) NOT NULL,
            location_id UUID REFERENCES storage_locations(id), state VARCHAR(12) NOT NULL CHECK(state IN ('OPEN','CLOSED')),
            created_by UUID NOT NULL REFERENCES app_users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            closed_at TIMESTAMPTZ
        );
        CREATE TABLE inventory_items (
            batch_id UUID NOT NULL REFERENCES inventory_batches(id), record_id UUID NOT NULL REFERENCES medical_records(id),
            expected_location_id UUID NOT NULL REFERENCES storage_locations(id),
            expected_path TEXT NOT NULL, observed VARCHAR(16) CHECK(observed IN ('FOUND','MISSING','WRONG_LOCATION','DAMAGED')),
            observed_location_id UUID REFERENCES storage_locations(id), note VARCHAR(2000),
            checked_by UUID REFERENCES app_users(id), checked_at TIMESTAMPTZ,
            PRIMARY KEY(batch_id,record_id)
        );
        CREATE INDEX ix_request_owner ON reading_requests(requester_id,created_at DESC);
        CREATE INDEX ix_sessions_day ON reading_sessions(reading_on,state);
        CREATE INDEX ix_request_events ON request_events(request_id,occurred_at);
        """;
    @Override public void migrate(Context context) throws Exception {
        try(var statement=context.getConnection().createStatement()) { statement.execute(SQL); }
    }
    @Override public Integer getChecksum() {
        var crc=new CRC32(); crc.update(SQL.getBytes(StandardCharsets.UTF_8)); return (int)crc.getValue();
    }
}
