package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Preserve historical approval routes; new batches explicitly choose their route. */
public class V19__RouteBorrowingApproval extends BaseJavaMigration {
    private static final String SQL="""
        ALTER TABLE reading_request_batches ADD COLUMN requires_director BOOLEAN NOT NULL DEFAULT TRUE;
        ALTER TABLE reading_requests ADD COLUMN requires_director BOOLEAN NOT NULL DEFAULT TRUE;
        ALTER TABLE reading_request_batches ADD CONSTRAINT batch_approval_route UNIQUE(id,requires_director);
        ALTER TABLE reading_requests ADD CONSTRAINT request_batch_approval_route
            FOREIGN KEY(batch_id,requires_director) REFERENCES reading_request_batches(id,requires_director);
        CREATE FUNCTION mrm_keep_borrowing_route() RETURNS trigger LANGUAGE plpgsql AS $body$
        BEGIN
            IF NEW.requires_director IS DISTINCT FROM OLD.requires_director THEN
                RAISE EXCEPTION 'Borrowing approval route cannot be changed after submission';
            END IF;
            RETURN NEW;
        END $body$;
        CREATE TRIGGER keep_batch_approval_route BEFORE UPDATE ON reading_request_batches
            FOR EACH ROW EXECUTE FUNCTION mrm_keep_borrowing_route();
        CREATE TRIGGER keep_request_approval_route BEFORE UPDATE ON reading_requests
            FOR EACH ROW EXECUTE FUNCTION mrm_keep_borrowing_route();
        COMMENT ON COLUMN reading_requests.requires_director IS
            'Immutable batch approval route: any deceased record requires Director for every child. Historical requests retain Director route.';
        """;
    @Override public void migrate(Context c)throws Exception{try(var s=c.getConnection().createStatement()){s.execute(SQL);}}
    @Override public Integer getChecksum(){var crc=new CRC32();crc.update(SQL.getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
}
