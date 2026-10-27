package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V12__UseRequesterUsernameSnapshot extends BaseJavaMigration {
    private static final String SQL="""
        UPDATE reading_requests q SET requester_name_snapshot=u.username
        FROM app_users u WHERE u.id=q.requester_id;
        COMMENT ON COLUMN reading_requests.requester_name_snapshot IS
            'Login username of the requester, captured on submission. V12 replaces V11 display names with linked account usernames per user request.';
        """;
    @Override public void migrate(Context context)throws Exception {try(var s=context.getConnection().createStatement()){s.execute(SQL);}}
    @Override public Integer getChecksum(){var crc=new CRC32();crc.update(SQL.getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
}
