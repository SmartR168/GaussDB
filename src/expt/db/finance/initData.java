package expt.db.finance;
import java.sql.*;
public final class initData {
    public static void run(DBUtils db) throws Exception {
        try(Connection c=db.open()) {
            c.setAutoCommit(false);
            try {
                for(String table:new String[]{"client","bank_card","financial_product","financial_asset","command_request","transaction_event"})
                    if(DBUtils.number(c,"SELECT count(*) FROM finance."+table)!=0)
                        throw new IllegalStateException("Seed requires empty tables; stopped without deleting data");
                SqlFiles.execute(c,"scripts/02_seed.sql");
                int[] expected={10,10,4,4}; int i=0;
                for(String table:new String[]{"client","bank_card","financial_product","financial_asset"}) {
                    long count=DBUtils.number(c,"SELECT count(*) FROM finance."+table);
                    if(count!=expected[i++]) throw new SQLException("Unexpected seed count: "+table);
                    System.out.println("SEED "+table+"="+count);
                }
                c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        }
    }
}
