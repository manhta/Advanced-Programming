package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Old work notices remain in history; cancelled requests receive a replacement notice. */
public class V20__BackfillArchivistCancellationNotices extends BaseJavaMigration {
    private static final String SQL="""
        INSERT INTO notifications(recipient_id,recipient_role,request_id,event_id,title,message,created_at)
        SELECT u.id,'ARCHIVIST',cancel.request_id,cancel.id,
            'Yêu cầu với hồ sơ '||mr.record_code||' đã hủy','',cancel.occurred_at
        FROM (SELECT DISTINCT ON (e.request_id) e.id,e.request_id,e.occurred_at
            FROM request_events e JOIN reading_requests q ON q.id=e.request_id
            WHERE e.action='CANCEL' AND q.state='CANCELLED'
            ORDER BY e.request_id,e.occurred_at DESC,e.id) cancel
        JOIN reading_requests q ON q.id=cancel.request_id JOIN medical_records mr ON mr.id=q.record_id
        CROSS JOIN app_users u JOIN roles role ON role.id=u.role_id
        WHERE u.status='ACTIVE' AND role.code='ARCHIVIST'
        ON CONFLICT(recipient_id,event_id) DO NOTHING;
        """;
    @Override public void migrate(Context c)throws Exception{try(var s=c.getConnection().createStatement()){s.execute(SQL);}}
    @Override public Integer getChecksum(){var crc=new CRC32();crc.update(SQL.getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
}
