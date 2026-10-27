package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V16__ExtendElectronicRecordDetails extends BaseJavaMigration {
    private static final String SQL="""
        ALTER TABLE electronic_records
            ADD COLUMN department_id UUID REFERENCES departments(id),
            ADD COLUMN record_type_id UUID REFERENCES record_types(id),
            ADD COLUMN department_name_snapshot VARCHAR(200),
            ADD COLUMN record_type_name_snapshot VARCHAR(200),
            ADD COLUMN deceased BOOLEAN NOT NULL DEFAULT FALSE,
            ADD COLUMN admission_date DATE, ADD COLUMN discharge_date DATE,
            ADD COLUMN source_note VARCHAR(1000), ADD COLUMN note VARCHAR(2000),
            ADD CONSTRAINT electronic_treatment_dates CHECK(discharge_date IS NULL OR admission_date IS NULL OR discharge_date>=admission_date);
        """;
    @Override public void migrate(Context c)throws Exception{try(var s=c.getConnection().createStatement()){s.execute(SQL);}}
    @Override public Integer getChecksum(){var crc=new CRC32();crc.update(SQL.getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
}
