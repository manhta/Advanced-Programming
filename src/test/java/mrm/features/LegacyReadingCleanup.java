package mrm.features;

import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Explicit, narrowly scoped cleanup of the two legacy demo registrations requested by the project owner. */
public class LegacyReadingCleanup {
    public static void main(String[] args)throws Exception {
        String password=System.getenv("DB_PASSWORD");
        if(password==null || password.isBlank())password=Files.readString(Path.of(".secrets/db-password.txt")).replaceFirst("[\\r\\n]+$","");
        try(var c=DriverManager.getConnection(System.getenv().getOrDefault("DB_URL","jdbc:postgresql://127.0.0.1:5433/mrm"),System.getenv().getOrDefault("DB_USERNAME","postgres"),password)) {
            c.setAutoCommit(false);
            try {
                String target="SELECT q.id FROM public.reading_requests q JOIN public.medical_records r ON r.id=q.record_id JOIN public.app_users u ON u.id=q.requester_id WHERE r.archive_number IN ('DEMO-MRM-007','DEMO-MRM-012') AND q.scheduling_mode='LEGACY' AND u.username='reader1'";
                var ids=new ArrayList<UUID>();
                try(var s=c.createStatement();var rs=s.executeQuery(target+" FOR UPDATE OF q")){while(rs.next())ids.add(rs.getObject(1,UUID.class));}
                System.out.println("Matching legacy Reader registrations: "+ids.size());
                if(ids.size()>2)throw new IllegalStateException("More than two matching registrations; no changes made.");
                String condition="request_id IN ("+target+")";
                try(var s=c.createStatement();var rs=s.executeQuery("SELECT COUNT(*) FROM public.reading_sessions WHERE "+condition+" AND (handed_at IS NOT NULL OR returned_at IS NOT NULL OR state='READING')")){
                    rs.next();if(rs.getInt(1)>0)throw new IllegalStateException("Contains handover history; no changes made.");
                }
                var snapshots=new ArrayList<String>();
                for(String table:List.of("reading_requests","reading_sessions","request_events","notifications")) {
                    String predicate=table.equals("reading_requests")?"id IN ("+target+")":condition;
                    try(var s=c.createStatement();var rs=s.executeQuery("SELECT COALESCE(jsonb_agg(to_jsonb(t)),'[]'::jsonb)::text FROM (SELECT * FROM public."+table+" WHERE "+predicate+") t")){
                        rs.next();snapshots.add("\""+table+"\":"+rs.getString(1));
                    }
                }
                if(args.length==0 || !args[0].equals("--apply")){System.out.println("Inspection only; no changes made.");c.rollback();return;}
                if(ids.isEmpty()){System.out.println("Already clean; no changes made.");c.rollback();return;}
                Path backup=Path.of("target/verification/legacy-reading-backup-"+UUID.randomUUID()+".json");
                Files.createDirectories(backup.getParent());Files.writeString(backup,"{"+String.join(",",snapshots)+"}",StandardOpenOption.CREATE_NEW);
                System.out.println("Backup: "+backup.toAbsolutePath());
                for(String table:List.of("notifications","reading_sessions","request_events","reading_requests")) {
                    String predicate=table.equals("reading_requests")?"id IN ("+target+")":condition;
                    try(var s=c.createStatement()){System.out.println("Deleted "+table+": "+s.executeUpdate("DELETE FROM public."+table+" WHERE "+predicate));}
                }
                try(var s=c.prepareStatement("INSERT INTO public.audit_events(actor,action,entity_type,entity_id,outcome,details) VALUES('admin1','CLEANUP_LEGACY','REQUEST',?,'SUCCESS','User requested removal of legacy demo registrations 007 and 012; original medical records preserved; local backup created.')")){
                    for(UUID id:ids){s.setString(1,id.toString());s.executeUpdate();}
                }
                c.commit();System.out.println("Cleanup committed; medical records preserved.");
            }catch(Exception ex){c.rollback();throw ex;}
        }
    }
}
