package expt.db.finance;
import expt.db.finance.dao.*;
import java.sql.*;
public final class testDAO {
    public static void run(DBUtils db) throws Exception {
        try(Connection c=db.open()) {
            c.setAutoCommit(false);
            try {
                Client client=new Client(c); BankCard card=new BankCard(c);
                FinancialProduct product=new FinancialProduct(c); FinancialAsset asset=new FinancialAsset(c);
                Checks.that(client.list().size()==10 && card.list().size()==10 && product.list().size()==4 && asset.list().size()==4,"DAO seed lists 10/10/4/4");
                Checks.that(client.byId(1).size()==1 && product.byId(1).size()==1 && card.byNumber("6222021302020000001").size()==1,"DAO primary-key lookups");
                Checks.that(client.byId(999999).isEmpty() && product.byId(999999).isEmpty(),"DAO missing keys return empty");
                Checks.that(client.insert(900001,"DAO测试","990000000000900001","19900900001","dao@example.test")==1,"DAO insert client");
                Checks.that(client.update(900001,"DAO更新","19900900001","dao2@example.test")==1,"DAO update client");
                Checks.that(client.update(999999,"无记录","19900999999","none@example.test")==0,"DAO missing update reports zero");
                Checks.that(card.insert("990000000000900001","储蓄卡",900001)==1 && card.count(900001)==1,"DAO insert and count card");
                Checks.that(asset.insert(900001,900001,1,100,java.sql.Date.valueOf("2026-11-03"))==1 && asset.count(900001)==1,"DAO insert and count asset");
                Savepoint sp=c.setSavepoint();
                Checks.rejects(()->client.insert(900001,"重复","990000000000900002","19900900002","dup@example.test"),"23505","Primary key duplicate rejected"); c.rollback(sp);
                sp=c.setSavepoint();
                Checks.rejects(()->card.insert("990000000000900002","储蓄卡",999999),"23503","Foreign key orphan rejected"); c.rollback(sp);
                sp=c.setSavepoint();
                Checks.rejects(()->asset.insert(900002,900001,1,0,java.sql.Date.valueOf("2026-11-03")),"23514","Nonpositive quantity rejected"); c.rollback(sp);
                Checks.that(asset.delete(900001)==1 && card.delete("990000000000900001")==1 && client.delete(900001)==1,"DAO delete in dependency order");
                Checks.that(asset.delete(900001)==0 && client.delete(900001)==0,"DAO missing deletes report zero");
            } finally { c.rollback(); }
        }
        try(Connection c=db.open()) {
            Checks.that(DBUtils.number(c,"SELECT count(*) FROM finance.client")==10 && DBUtils.number(c,"SELECT count(*) FROM finance.financial_asset")==4,"DAO test rolled back; seed intact");
        }
    }
}
