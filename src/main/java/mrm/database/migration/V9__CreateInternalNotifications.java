package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V9__CreateInternalNotifications extends BaseJavaMigration {
    private static final String SQL = """
        CREATE TABLE notifications (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            recipient_id UUID NOT NULL REFERENCES app_users(id),
            request_id UUID NOT NULL REFERENCES reading_requests(id),
            event_id UUID NOT NULL REFERENCES request_events(id),
            title VARCHAR(200) NOT NULL,
            message TEXT NOT NULL,
            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            read_at TIMESTAMPTZ,
            UNIQUE(recipient_id,event_id),
            CHECK(read_at IS NULL OR read_at >= created_at)
        );
        CREATE INDEX ix_notifications_inbox ON notifications(recipient_id,created_at DESC,id);
        CREATE INDEX ix_notifications_unread ON notifications(recipient_id) WHERE read_at IS NULL;
        """;
    @Override public void migrate(Context context)throws Exception {
        try(var statement=context.getConnection().createStatement()){statement.execute(SQL);}
    }
    @Override public Integer getChecksum(){var crc=new CRC32();crc.update(SQL.getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
}
