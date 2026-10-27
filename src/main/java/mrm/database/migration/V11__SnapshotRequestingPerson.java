package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V11__SnapshotRequestingPerson extends BaseJavaMigration {
    private static final String SQL="""
        ALTER TABLE reading_requests ADD COLUMN requester_name_snapshot VARCHAR(200);
        UPDATE reading_requests q SET requester_name_snapshot=u.display_name FROM app_users u WHERE u.id=q.requester_id;
        ALTER TABLE reading_requests ALTER COLUMN requester_name_snapshot SET NOT NULL;
        ALTER TABLE reading_requests ADD CONSTRAINT ck_requester_name_nonblank CHECK(length(btrim(requester_name_snapshot))>0);
        COMMENT ON COLUMN reading_requests.requester_name_snapshot IS
            'Display name when submitted; pre-V11 requests backfilled with the current account name at migration time.';
        """;
    @Override public void migrate(Context context)throws Exception {try(var s=context.getConnection().createStatement()){s.execute(SQL);}}
    @Override public Integer getChecksum(){var crc=new CRC32();crc.update(SQL.getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
}
