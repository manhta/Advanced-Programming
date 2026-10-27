package mrm.audit;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;
    public AuditService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public void log(String actor,String action,String type,Object id,String details) {
        jdbc.update("INSERT INTO audit_events(actor,action,entity_type,entity_id,outcome,details) VALUES(?,?,?,?, 'SUCCESS',?)",
            actor,action,type,Objects.toString(id,null),details);
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void failure(String actor,String action,String type,Object id,String details) {
        jdbc.update("INSERT INTO audit_events(actor,action,entity_type,entity_id,outcome,details) VALUES(?,?,?,?, 'FAILURE',?)",
            actor==null ? "anonymous" : actor.substring(0,Math.min(actor.length(),64)),action,type,Objects.toString(id,null),details);
    }
}
