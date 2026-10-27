package mrm.database.migration;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V13__CreateEmployees extends BaseJavaMigration {
    private static final String SQL="""
        CREATE TABLE employees (
            citizen_id VARCHAR(10) PRIMARY KEY CHECK(citizen_id ~ '^[0-9]{10}$'),
            account_id UUID NOT NULL UNIQUE REFERENCES app_users(id) ON DELETE CASCADE,
            full_name VARCHAR(200) NOT NULL CHECK(length(btrim(full_name))>0),
            birth_date DATE NOT NULL CHECK(birth_date>=DATE '1800-01-01'),
            gender VARCHAR(10) NOT NULL CHECK(gender IN ('MALE','FEMALE','OTHER')),
            role VARCHAR(16) NOT NULL REFERENCES roles(code)
        );
        COMMENT ON COLUMN employees.citizen_id IS 'Exactly ten digits per learning-project requirement; text preserves leading zeros.';
        CREATE FUNCTION validate_employee_account() RETURNS trigger LANGUAGE plpgsql AS $$
        BEGIN
            IF NOT EXISTS(SELECT 1 FROM app_users u JOIN roles r ON r.id=u.role_id
                WHERE u.id=NEW.account_id AND r.code=NEW.role AND u.display_name=NEW.full_name) THEN
                RAISE EXCEPTION 'Employee name/role must match linked account' USING ERRCODE='23514';
            END IF;
            RETURN NEW;
        END;
        $$;
        CREATE TRIGGER employees_account_consistency BEFORE INSERT OR UPDATE ON employees
            FOR EACH ROW EXECUTE FUNCTION validate_employee_account();
        CREATE FUNCTION sync_employee_from_account() RETURNS trigger LANGUAGE plpgsql AS $$
        BEGIN
            UPDATE employees SET full_name=NEW.display_name,role=(SELECT code FROM roles WHERE id=NEW.role_id)
                WHERE account_id=NEW.id;
            RETURN NEW;
        END;
        $$;
        CREATE TRIGGER app_users_sync_employee AFTER UPDATE OF display_name,role_id ON app_users
            FOR EACH ROW EXECUTE FUNCTION sync_employee_from_account();
        """;
    @Override public void migrate(Context context)throws Exception {try(var s=context.getConnection().createStatement()){s.execute(SQL);}}
    @Override public Integer getChecksum(){var crc=new CRC32();crc.update(SQL.getBytes(StandardCharsets.UTF_8));return (int)crc.getValue();}
}
