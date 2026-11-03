package expt.db.finance;
import java.sql.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.time.LocalDate;

public final class Reports {
    private final DBUtils db;
    public Reports(DBUtils db) { this.db=db; }
    public static String sql(String name) throws IOException {
        if(!Arrays.asList("client","product","month").contains(name)) throw new IllegalArgumentException("Unknown report");
        return Files.readString(Path.of("scripts/report_"+name+".sql"),StandardCharsets.UTF_8);
    }
    public List<Map<String,Object>> run(String name,LocalDate start,LocalDate end) throws Exception {
        if("month".equals(name) && (start==null || end==null || !start.isBefore(end))) throw new IllegalArgumentException("Expected start < end");
        try(Connection c=db.open()) {
            return "month".equals(name)?DBUtils.query(c,sql(name),java.sql.Date.valueOf(start),java.sql.Date.valueOf(end)):DBUtils.query(c,sql(name));
        }
    }
    private static String cell(Object value) {
        String s=value==null?"":value.toString().trim();
        // Protect spreadsheet users from formula execution in exported text cells.
        if(!(value instanceof Number) && s.matches("^[=+@-].*")) s="'"+s;
        return "\""+s.replace("\"","\"\"")+"\"";
    }
    public static void csv(Path path,List<Map<String,Object>> rows) throws IOException {
        if(rows.isEmpty()) throw new IllegalArgumentException("No report rows to export");
        Files.createDirectories(path.toAbsolutePath().getParent());
        try(BufferedWriter w=Files.newBufferedWriter(path,StandardCharsets.UTF_8)) {
            w.write(String.join(",",rows.get(0).keySet())); w.newLine();
            for(Map<String,Object> row:rows) {
                List<String> cells=new ArrayList<>(); for(Object value:row.values()) cells.add(cell(value));
                w.write(String.join(",",cells)); w.newLine();
            }
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=1) throw new IllegalArgumentException("Usage: Reports config/db.properties");
        try(DBUtils db=DBUtils.interactive(Path.of(args[0]))) {
            Reports reports=new Reports(db);
            for(String name:Arrays.asList("client","product","month")) {
                System.out.println("REPORT "+name);
                List<Map<String,Object>> rows=reports.run(name,LocalDate.of(2018,7,1),LocalDate.of(2018,8,1));
                DBUtils.print(rows); csv(Path.of("data/report_"+name+".csv"),rows);
            }
        }
    }
}
