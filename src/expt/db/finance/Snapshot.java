package expt.db.finance;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.util.*;
public final class Snapshot {
    public static void write(DBUtils db,boolean baseline) throws Exception {
        String[] tables={"client","bank_card","financial_product","financial_asset","command_request","transaction_event"};
        String[] keys={"c_id","b_number","p_id","a_id","request_id","request_id"};
        Path dir=Path.of("data",baseline?"before_switch":"after_switch"); Files.createDirectories(dir);
        try(Connection c=db.open()) {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); c.setAutoCommit(false);
            try {
                for(int i=0;i<tables.length;i++) {
                    Path out=dir.resolve(tables[i]+".csv");
                    Reports.csv(out,DBUtils.query(c,"SELECT * FROM finance."+tables[i]+" ORDER BY "+keys[i]));
                    byte[] bytes=Files.readAllBytes(out); StringBuilder hex=new StringBuilder();
                    for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes)) hex.append(String.format("%02x",b&255));
                    System.out.println("SNAPSHOT_SHA256 "+tables[i]+" "+hex);
                    if(!baseline) Checks.that(Arrays.equals(bytes,Files.readAllBytes(Path.of("data/before_switch",tables[i]+".csv"))),"Switchover preserves all ordered rows: "+tables[i]);
                }
                c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        }
    }
}
