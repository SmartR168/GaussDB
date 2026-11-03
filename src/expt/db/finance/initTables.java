package expt.db.finance;
import java.sql.*;
public final class initTables {
    public static void run(DBUtils db) throws Exception {
        try(Connection c=db.open()) {
            if(DBUtils.number(c,"SELECT count(*) FROM pg_tables WHERE schemaname='finance'")!=0)
                throw new IllegalStateException("finance already contains tables; initialization stopped without dropping data");
            c.setAutoCommit(false);
            try { SqlFiles.execute(c,"scripts/01_schema.sql"); c.commit(); System.out.println("SCHEMA_CREATED: 4 business tables + 2 extension tables"); }
            catch(Exception e) { c.rollback(); throw e; }
        }
    }
}
