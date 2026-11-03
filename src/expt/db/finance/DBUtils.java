package expt.db.finance;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public final class DBUtils implements AutoCloseable {
    private final Properties config = new Properties();
    private char[] password;
    public DBUtils(Path path, char[] secret) throws Exception {
        try (Reader r=Files.newBufferedReader(path,StandardCharsets.UTF_8)) { config.load(r); }
        if(config.containsKey("PASSWORD")) throw new IllegalArgumentException("Use interactive password entry");
        if(!config.getProperty("DB_URL","").matches("jdbc:gaussdb://[^/]+/finance"))
            throw new IllegalArgumentException("Expected a GaussDB finance database URL");
        password=secret.clone();
        Class.forName(config.getProperty("JDBC_DRIVER"));
        DriverManager.setLoginTimeout(20);
    }
    public Connection open() throws SQLException {
        if(password==null) throw new IllegalStateException("Connection factory is closed");
        Connection c=DriverManager.getConnection(config.getProperty("DB_URL"),config.getProperty("USER"),new String(password));
        try(Statement s=c.createStatement()) { s.execute("SET search_path=finance"); }
        catch(SQLException e) { c.close(); throw e; }
        return c;
    }
    public static PreparedStatement bind(Connection c,String sql,Object... values) throws SQLException {
        PreparedStatement s=c.prepareStatement(sql);
        try { s.setQueryTimeout(45); for(int i=0;i<values.length;i++) s.setObject(i+1,values[i]); return s; }
        catch(SQLException e) { s.close(); throw e; }
    }
    public static int update(Connection c,String sql,Object... values) throws SQLException {
        try(PreparedStatement s=bind(c,sql,values)) { return s.executeUpdate(); }
    }
    public static List<Map<String,Object>> query(Connection c,String sql,Object... values) throws SQLException {
        try(PreparedStatement s=bind(c,sql,values);ResultSet r=s.executeQuery()) {
            List<Map<String,Object>> rows=new ArrayList<>(); int n=r.getMetaData().getColumnCount();
            while(r.next()) {
                Map<String,Object> row=new LinkedHashMap<>();
                for(int i=1;i<=n;i++) row.put(r.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT),r.getObject(i));
                rows.add(row);
            }
            return rows;
        }
    }
    public static long number(Connection c,String sql,Object... values) throws SQLException {
        List<Map<String,Object>> rows=query(c,sql,values);
        if(rows.size()!=1) throw new SQLException("Expected exactly one scalar row");
        Object value=rows.get(0).values().iterator().next();
        if(!(value instanceof Number)) throw new SQLException("Expected a numeric scalar");
        return ((Number)value).longValue();
    }
    public static void print(List<Map<String,Object>> rows) {
        if(rows.isEmpty()) { System.out.println("(0 rows)"); return; }
        System.out.println(String.join(" | ",rows.get(0).keySet()));
        for(Map<String,Object> row:rows) {
            StringJoiner text=new StringJoiner(" | ");
            for(Object value:row.values()) text.add(String.valueOf(value).trim());
            System.out.println(text);
        }
        System.out.println("("+rows.size()+" rows)");
    }
    public static DBUtils interactive(Path config) throws Exception {
        Console console=System.console();
        if(console==null) throw new IllegalStateException("An interactive terminal is required");
        char[] secret=console.readPassword("Database password (not echoed): ");
        if(secret==null || secret.length==0) throw new IllegalArgumentException("Password entry cancelled");
        try { return new DBUtils(config,secret); } finally { Arrays.fill(secret,'\0'); }
    }
    @Override public void close() { if(password!=null) Arrays.fill(password,'\0'); password=null; }
}
