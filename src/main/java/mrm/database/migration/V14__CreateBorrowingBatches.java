package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V14__CreateBorrowingBatches extends BaseJavaMigration {
    private static final String SQL="""
        CREATE TABLE reading_request_batches (
            id UUID PRIMARY KEY,
            submit_token UUID NOT NULL UNIQUE,
            requester_id UUID NOT NULL REFERENCES app_users(id),
            purpose VARCHAR(2000) NOT NULL CHECK(length(btrim(purpose))>0),
            registered_on DATE NOT NULL,
            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        ALTER TABLE reading_requests ADD COLUMN batch_id UUID REFERENCES reading_request_batches(id);
        ALTER TABLE reading_requests ADD COLUMN registered_on DATE;
        UPDATE reading_requests SET registered_on=(created_at AT TIME ZONE 'Asia/Bangkok')::date;
        ALTER TABLE reading_requests ALTER COLUMN registered_on SET NOT NULL;
        ALTER TABLE reading_requests ALTER COLUMN registered_on SET DEFAULT ((CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Bangkok')::date);
        ALTER TABLE reading_requests ADD COLUMN pickup_deadline DATE GENERATED ALWAYS AS (registered_on+2) STORED;
        ALTER TABLE reading_requests ADD COLUMN scheduling_mode VARCHAR(16) NOT NULL DEFAULT 'LEGACY'
            CHECK(scheduling_mode IN ('LEGACY','ARCHIVIST'));
        ALTER TABLE reading_requests ALTER COLUMN scheduling_mode SET DEFAULT 'ARCHIVIST';
        ALTER TABLE reading_requests ADD CONSTRAINT ux_request_batch_record UNIQUE(batch_id,record_id);
        CREATE INDEX ix_batches_requester ON reading_request_batches(requester_id,created_at DESC);
        CREATE FUNCTION check_session_pickup_window() RETURNS trigger LANGUAGE plpgsql AS $$
        DECLARE request_row reading_requests%ROWTYPE;
        BEGIN
            SELECT * INTO request_row FROM reading_requests WHERE id=NEW.request_id;
            IF request_row.scheduling_mode='ARCHIVIST' AND
                (NEW.reading_on<request_row.registered_on OR NEW.reading_on>request_row.pickup_deadline) THEN
                RAISE EXCEPTION 'Pickup must be within two calendar days of registration' USING ERRCODE='23514';
            END IF;
            RETURN NEW;
        END;
        $$;
        CREATE TRIGGER reading_sessions_pickup_window BEFORE INSERT OR UPDATE OF reading_on,request_id ON reading_sessions
            FOR EACH ROW EXECUTE FUNCTION check_session_pickup_window();
        """;
    @Override public void migrate(Context context)throws Exception {try(var s=context.getConnection().createStatement()){s.execute(SQL);}}
    @Override public Integer getChecksum(){var crc=new CRC32();crc.update(SQL.getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
}
