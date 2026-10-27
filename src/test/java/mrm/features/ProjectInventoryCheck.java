package mrm.features;

import java.nio.file.*;
import java.sql.DriverManager;
import java.util.List;

/** Read-only schema inventory, never prints credentials or patient rows. */
public class ProjectInventoryCheck {
    public static void main(String[] args) throws Exception {
        String password=System.getenv("DB_PASSWORD");
        if(password==null || password.isBlank())password=Files.readString(Path.of(".secrets/db-password.txt")).replaceFirst("[\\r\\n]+$","");
        try(var connection=DriverManager.getConnection(System.getenv().getOrDefault("DB_URL","jdbc:postgresql://127.0.0.1:5433/mrm"),System.getenv().getOrDefault("DB_USERNAME","postgres"),password)) {
            connection.setReadOnly(true);
            try(var statement=connection.createStatement()) {
                statement.setQueryTimeout(10);
                try(var rows=statement.executeQuery("SELECT current_database(),current_schema(),current_setting('server_version')")) {
                    rows.next();System.out.println("DATABASE "+rows.getString(1)+" | schema="+rows.getString(2)+" | PostgreSQL "+rows.getString(3));
                }
                try(var rows=statement.executeQuery("SELECT version,description,success FROM flyway_schema_history ORDER BY installed_rank")) {
                    while(rows.next())System.out.println("MIGRATION "+rows.getString(1)+" | "+rows.getString(2)+" | "+rows.getBoolean(3));
                }
                for(String table:List.of("roles","app_users","employees","departments","record_types","retention_policies","catalog_events","storage_locations","medical_records","medical_record_policies","record_events","record_location_history","audit_events","reading_request_batches","reading_requests","reading_sessions","request_events","inventory_batches","inventory_items","notifications","staff_change_requests","staff_change_events")) {
                    try(var exists=connection.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
                        exists.setString(1,table);try(var result=exists.executeQuery()){result.next();if(!result.getBoolean(1)){System.out.println("TABLE "+table+" | absent");continue;}}
                    }
                    try(var rows=statement.executeQuery("SELECT COUNT(*) FROM "+table)){rows.next();System.out.println("TABLE "+table+" | rows="+rows.getLong(1));}
                }
            }
        }
    }
}
