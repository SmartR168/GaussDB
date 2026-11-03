package expt.db.finance;
import java.io.Console;
import java.sql.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Provision {
    static String host(String value) {
        if(!value.matches("192\\.168\\.[0-9]{1,3}\\.[0-9]{1,3}")) throw new IllegalArgumentException("Expected verified sandbox private IPv4");
        return value;
    }
    static char[] secret(Console console,String label) {
        char[] p=console.readPassword("%s: ",label);
        if(p==null || p.length==0) throw new IllegalArgumentException("Password entry cancelled"); return p;
    }
    static Connection connect(String host,String database,String user,char[] secret) throws Exception {
        Class.forName("com.huawei.gaussdb.jdbc.Driver"); DriverManager.setLoginTimeout(20);
        return DriverManager.getConnection("jdbc:gaussdb://"+host+":8000/"+database,user,new String(secret));
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=1) throw new IllegalArgumentException("Provision IP");
        String host=host(args[0]); Console console=System.console(); if(console==null) throw new IllegalStateException("Interactive terminal required");
        char[] root=secret(console,"Database password for root");
        try {
            try(Connection c=connect(host,"postgres","root",root)) {
                if(DBUtils.number(c,"SELECT count(*) FROM pg_database WHERE datname='finance'")!=0) throw new IllegalStateException("finance exists; inspect it instead of overwriting");
                DBUtils.update(c,"CREATE DATABASE finance WITH ENCODING='UTF8'");
                System.out.println("CREATE_DATABASE finance UTF8");
            }
            try(Connection c=connect(host,"finance","root",root)) {
                for(String user:new String[]{"db_dev","test"}) {
                    if(DBUtils.number(c,"SELECT count(*) FROM pg_roles WHERE rolname=?",user)!=0) throw new IllegalStateException(user+" already exists; inspect before proceeding");
                    char[] first=secret(console,"Set NEW password for "+user),second=null;
                    try {
                        second=secret(console,"Confirm NEW password for "+user);
                        if(!Arrays.equals(first,second)) throw new IllegalArgumentException("Passwords do not match");
                        System.out.println("This creates ordinary sandbox user "+user+". No administrator attributes are granted.");
                        if(!"CREATE".equals(console.readLine("Type CREATE to submit this new credential: "))) throw new IllegalArgumentException("Creation cancelled");
                        // Utility statements do not accept a password bind parameter. Escape the SQL literal; never print it.
                        String quoted=new String(first).replace("\"","\"\"");
                        try(Statement s=c.createStatement()) { s.execute("CREATE USER "+user+" IDENTIFIED BY \""+quoted+"\""); }
                        catch(SQLException e) { throw new SQLException("User creation rejected; SQLSTATE="+e.getSQLState(),e.getSQLState()); }
                        System.out.println("CREATE_USER "+user);
                    } finally { Arrays.fill(first,'\0'); if(second!=null) Arrays.fill(second,'\0'); }
                }
                DBUtils.update(c,"CREATE SCHEMA finance AUTHORIZATION db_dev");
                DBUtils.update(c,"GRANT CONNECT ON DATABASE finance TO db_dev,test");
                DBUtils.print(DBUtils.query(c,"SELECT rolname,rolsuper,rolcreatedb,rolcreaterole FROM pg_roles WHERE rolname IN ('db_dev','test') ORDER BY rolname"));
            }
            Files.createDirectories(Path.of("config"));
            Files.writeString(Path.of("config/db.properties"),"JDBC_DRIVER=com.huawei.gaussdb.jdbc.Driver\nDB_URL=jdbc:gaussdb://"+host+":8000/finance\nUSER=db_dev\n",StandardCharsets.UTF_8);
            System.out.println("PROVISION_COMPLETE; credentials were not written to config");
        } finally { Arrays.fill(root,'\0'); }
    }
}
