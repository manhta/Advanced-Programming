package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Retire premature approval results, backfill completed applications, preserve Admin work and rejection notices. */
public class V25__NotifyStaffAfterApplication extends BaseJavaMigration {
    private static final String SQL="""
        UPDATE notifications n SET deleted_at=GREATEST(clock_timestamp(),n.created_at)
        FROM staff_change_requests q,staff_change_events e
        WHERE n.staff_request_id=q.id AND n.staff_event_id=e.id AND e.request_id=q.id
            AND n.recipient_id=q.requester_id AND e.action='APPROVE' AND n.deleted_at IS NULL;
        INSERT INTO notifications(recipient_id,recipient_role,staff_request_id,staff_event_id,title,message,created_at)
        SELECT u.id,r.code,q.id,e.id,'Đề nghị thay đổi đã được phê duyệt','',e.occurred_at
        FROM staff_change_requests q JOIN staff_change_events e ON e.request_id=q.id AND e.action='APPLY'
        JOIN app_users u ON u.id=q.requester_id JOIN roles r ON r.id=u.role_id WHERE q.state='APPLIED'
        ON CONFLICT(recipient_id,staff_event_id) DO NOTHING;
        """;
    @Override public void migrate(Context c)throws Exception {try(var s=c.getConnection().createStatement()){s.execute(SQL);}}
    @Override public Integer getChecksum(){var crc=new CRC32();crc.update(SQL.getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
}
