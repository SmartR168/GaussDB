package expt.db.finance;
import java.io.Console;
import java.sql.*;
import java.util.*;
public final class PermissionCheck {
    private static void select(String host,char[] password,boolean allowed) throws Exception {
        try(Connection c=Provision.connect(host,"finance","test",password)) {
            DBUtils.print(DBUtils.query(c,"SELECT current_user,current_database()"));
            if(allowed) {
                Checks.that(DBUtils.number(c,"SELECT count(*) FROM finance.client")==10,"test SELECT succeeds after explicit table grant");
                Checks.rejects(()->DBUtils.query(c,"SELECT count(*) FROM finance.bank_card"),"42501","Unrelated table remains denied");
            } else Checks.rejects(()->DBUtils.query(c,"SELECT count(*) FROM finance.client"),"42501","test SELECT denied at expected permission stage");
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=1) throw new IllegalArgumentException("PermissionCheck IP");
        String host=Provision.host(args[0]); Console console=System.console();
        char[] dev=Provision.secret(console,"Existing password for db_dev"),test=null;
        try {
            test=Provision.secret(console,"Existing password for test");
            try(Connection owner=Provision.connect(host,"finance","db_dev",dev)) {
                System.out.println("STAGE_1 no schema and no table permission"); select(host,test,false);
                DBUtils.update(owner,"GRANT USAGE ON SCHEMA finance TO test");
                System.out.println("STAGE_2 schema USAGE only"); select(host,test,false);
                DBUtils.update(owner,"GRANT SELECT ON finance.client TO test");
                System.out.println("STAGE_3 schema USAGE + client SELECT"); select(host,test,true);
                try(Connection restricted=Provision.connect(host,"finance","test",test)) {
                    Checks.rejects(()->DBUtils.update(restricted,"UPDATE finance.client SET c_name=c_name WHERE c_id=1"),"42501","test remains unable to update client");
                }
                DBUtils.print(DBUtils.query(owner,"SELECT grantee,table_schema,table_name,privilege_type FROM information_schema.role_table_grants WHERE grantee='test' ORDER BY table_name,privilege_type"));
            }
            System.out.println("PERMISSION_CHECKS_PASSED="+Checks.total());
        } finally { Arrays.fill(dev,'\0'); if(test!=null) Arrays.fill(test,'\0'); }
    }
}
