package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V10__RouteWorkflowNotifications extends BaseJavaMigration {
    private static final String SQL="""
        ALTER TABLE notifications ADD COLUMN recipient_role VARCHAR(16) NOT NULL DEFAULT 'READER'
            CHECK(recipient_role IN ('READER','ARCHIVIST','DIRECTOR'));
        -- Existing pending work should be visible after this upgrade, without resending old completed work.
        INSERT INTO notifications(recipient_id,recipient_role,request_id,event_id,title,message)
        SELECT u.id,r.code,q.id,e.id,
            CASE WHEN q.state='SUBMITTED' THEN 'Có yêu cầu đọc cần kiểm tra' ELSE 'Có yêu cầu đọc chờ phê duyệt' END,
            'Yêu cầu ' || q.id || '. Công việc chờ xử lý được bổ sung khi nâng cấp; mở yêu cầu để xem trạng thái hiện tại.'
        FROM reading_requests q
        JOIN LATERAL (SELECT id FROM request_events WHERE request_id=q.id AND revision=q.revision
            AND ((q.state='SUBMITTED' AND action IN ('SUBMIT','AMEND'))
              OR (q.state='PENDING_APPROVAL' AND action='REVIEW_PENDING_APPROVAL'))
            ORDER BY occurred_at DESC,id DESC LIMIT 1) e ON TRUE
        JOIN roles r ON r.code=CASE WHEN q.state='SUBMITTED' THEN 'ARCHIVIST' ELSE 'DIRECTOR' END
        JOIN app_users u ON u.role_id=r.id AND u.status='ACTIVE'
        WHERE q.state IN ('SUBMITTED','PENDING_APPROVAL') ON CONFLICT(recipient_id,event_id) DO NOTHING;
        """;
    @Override public void migrate(Context context)throws Exception {try(var s=context.getConnection().createStatement()){s.execute(SQL);}}
    @Override public Integer getChecksum(){var crc=new CRC32();crc.update(SQL.getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
}
