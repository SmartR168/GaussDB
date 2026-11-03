package expt.db.finance;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
public final class Acceptance {
    public static void main(String[] args) throws Exception {
        if(args.length!=2 || !Arrays.asList("init","verify","resume-reports").contains(args[1])) throw new IllegalArgumentException("Usage: Acceptance config/db.properties init|verify");
        System.out.println("RUN_START "+OffsetDateTime.now());
        try(DBUtils db=DBUtils.interactive(Path.of(args[0]))) {
            try(Connection c=db.open()) {
                System.out.println("JDBC "+c.getMetaData().getDriverName()+" "+c.getMetaData().getDriverVersion());
                DBUtils.print(DBUtils.query(c,"SELECT current_database(),current_user,version(),current_timestamp"));
                Checks.that("db_dev".equals(c.getMetaData().getUserName()),"Ordinary development user db_dev connected");
            }
            if("init".equals(args[1])) {
                initTables.run(db); initData.run(db); testDAO.run(db); new ScenarioTest(db).run();
            }
            if("resume-reports".equals(args[1])) new ScenarioTest(db).reportFixture();
            try(Connection c=db.open()) {
                DBUtils.print(DBUtils.query(c,"SELECT tablename,tableowner FROM pg_tables WHERE schemaname='finance' ORDER BY tablename"));
                DBUtils.print(DBUtils.query(c,"SELECT tablename,indexname FROM pg_indexes WHERE schemaname='finance' ORDER BY tablename,indexname"));
                int[] expected={10,10,4,4,4,4}; int i=0;
                for(String table:new String[]{"client","bank_card","financial_product","financial_asset","command_request","transaction_event"}) {
                    long count=DBUtils.number(c,"SELECT count(*) FROM finance."+table); System.out.println("FINAL_COUNT "+table+"="+count);
                    Checks.that(count==expected[i++],"Expected final row count: "+table);
                }
                Checks.that(DBUtils.number(c,"SELECT count(*) FROM finance.command_request WHERE result_code<>'SUCCESS'")==0,"No incomplete committed requests");
                Checks.that(DBUtils.number(c,"SELECT count(*) FROM finance.command_request")==DBUtils.number(c,"SELECT count(*) FROM finance.transaction_event"),"One audit event for every committed command");
                DBUtils.print(DBUtils.query(c,"SELECT * FROM finance.transaction_event ORDER BY request_id"));
            }
            Reports reports=new Reports(db);
            for(String name:Arrays.asList("client","product","month")) {
                List<Map<String,Object>> rows=reports.run(name,LocalDate.of(2018,7,1),LocalDate.of(2018,8,1));
                System.out.println("REPORT "+name); DBUtils.print(rows); Reports.csv(Path.of("data/report_"+name+".csv"),rows);
            }
            Snapshot.write(db,!"verify".equals(args[1]));
            System.out.println("ALL_CHECKS_PASSED="+Checks.total());
        }
        System.out.println("RUN_END "+OffsetDateTime.now());
    }
}
