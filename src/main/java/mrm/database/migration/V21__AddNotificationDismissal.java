package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V21__AddNotificationDismissal extends BaseJavaMigration {
    private static final String SQL="""
        ALTER TABLE notifications ADD COLUMN deleted_at TIMESTAMPTZ;
        ALTER TABLE notifications ADD CONSTRAINT notification_deleted_time
            CHECK(deleted_at IS NULL OR deleted_at>=created_at);
        CREATE INDEX ix_notifications_visible ON notifications(recipient_id,created_at DESC,id) WHERE deleted_at IS NULL;
        INSERT INTO notifications(recipient_id,recipient_role,request_id,event_id,title,message,created_at)
        SELECT u.id,'ARCHIVIST',q.id,e.id,'Có yêu cầu mượn cần xác nhận lịch','',e.occurred_at
        FROM reading_requests q JOIN request_events e ON e.request_id=q.id AND e.action IN ('APPROVE','ARCHIVIST_APPROVE') AND e.revision=q.revision
        CROSS JOIN app_users u JOIN roles r ON r.id=u.role_id
        WHERE q.state='APPROVED' AND u.status='ACTIVE' AND r.code='ARCHIVIST'
        ON CONFLICT(recipient_id,event_id) DO NOTHING;
        """;
    @Override public void migrate(Context c)throws Exception{try(var s=c.getConnection().createStatement()){s.execute(SQL);}}
    @Override public Integer getChecksum(){var crc=new CRC32();crc.update(SQL.getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
}
