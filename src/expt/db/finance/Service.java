package expt.db.finance;

import expt.db.finance.dao.*;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public final class Service {
    private final DBUtils db;
    public Service(DBUtils db) { this.db=db; }
    @FunctionalInterface interface Work<T> { T run(Connection c) throws Exception; }
    private <T> T transaction(Work<T> work) throws Exception {
        try(Connection c=db.open()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            c.setAutoCommit(false);
            try { T result=work.run(c); c.commit(); return result; }
            catch(Exception e) { try { c.rollback(); } catch(SQLException rollback) { e.addSuppressed(rollback); } throw e; }
        }
    }
    private static void positive(int id) { if(id<=0) throw new IllegalArgumentException("ID and quantity must be positive"); }
    private static void text(String value,int max,String label) {
        if(value==null || value.trim().isEmpty() || !value.equals(value.trim()) || value.length()>max)
            throw new IllegalArgumentException("Invalid "+label);
    }
    private static void contact(String name,String phone,String mail) {
        text(name,100,"name");
        if(phone==null || !phone.matches("[0-9]{11}")) throw new IllegalArgumentException("Expected an 11-digit phone");
        text(mail,30,"email");
        if(!mail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Invalid email");
    }
    private static void one(int affected) throws SQLException {
        if(affected!=1) throw new SQLException("Expected one affected row; actual="+affected,"P0002");
    }
    private static void lockClient(Connection c,int id) throws SQLException {
        if(DBUtils.query(c,"SELECT c_id FROM finance.client WHERE c_id=? FOR UPDATE",id).isEmpty())
            throw new SQLException("Client does not exist","P0002");
    }
    public List<Map<String,Object>> clients() throws SQLException { try(Connection c=db.open()) { return new Client(c).list(); } }
    public Map<String,List<Map<String,Object>>> detail(int id) throws SQLException {
        positive(id);
        try(Connection c=db.open()) {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); c.setAutoCommit(false);
            try {
                Map<String,List<Map<String,Object>>> rows=new LinkedHashMap<>();
                rows.put("client",new Client(c).byId(id));
                rows.put("bank_card",new BankCard(c).byClient(id));
                rows.put("financial_asset",new FinancialAsset(c).byClient(id)); c.commit(); return rows;
            } catch(SQLException e) { c.rollback(); throw e; }
        }
    }
    public List<Map<String,Object>> products() throws SQLException { try(Connection c=db.open()) { return new FinancialProduct(c).list(); } }
    public void openAccount(int id,String name,String identity,String phone,String mail) throws Exception {
        positive(id); contact(name,phone,mail);
        if(identity==null || !identity.matches("[0-9]{17}[0-9Xx]")) throw new IllegalArgumentException("Expected 18-character identity");
        transaction(c->{ one(new Client(c).insert(id,name,identity,phone,mail)); return null; });
    }
    public void modify(int id,String name,String phone,String mail) throws Exception {
        positive(id); contact(name,phone,mail);
        transaction(c->{ lockClient(c,id); one(new Client(c).update(id,name,phone,mail)); return null; });
    }
    public void closeAccount(int id) throws Exception {
        positive(id);
        transaction(c->{ lockClient(c,id);
            if(new BankCard(c).count(id)>0 || new FinancialAsset(c).count(id)>0)
                throw new SQLException("Close cards and redeem available assets before closing account","P0001");
            one(new Client(c).delete(id)); return null; });
    }
    public void openCard(String number,String type,int client) throws Exception {
        positive(client);
        if(number==null || !number.matches("[0-9]{16,30}")) throw new IllegalArgumentException("Expected 16-30 digit card number");
        if(!Arrays.asList("信用卡","储蓄卡").contains(type)) throw new IllegalArgumentException("Invalid card type");
        transaction(c->{ lockClient(c,client); one(new BankCard(c).insert(number,type,client)); return null; });
    }
    public void closeCard(String number,int client) throws Exception {
        positive(client); text(number,30,"card number");
        transaction(c->{ lockClient(c,client);
            one(DBUtils.update(c,"DELETE FROM finance.bank_card WHERE b_number=? AND b_client_id=?",number,client)); return null; });
    }
    public static final class Receipt {
        public final int assetId; public final boolean replay;
        Receipt(int id,boolean replay) { this.assetId=id; this.replay=replay; }
        @Override public String toString() { return "asset="+assetId+", replay="+replay; }
    }
    private static String digest(String payload) throws Exception {
        byte[] bytes=MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex=new StringBuilder(); for(byte b:bytes) hex.append(String.format("%02x",b&255)); return hex.toString();
    }
    private Receipt command(String request,String action,String payload,Work<Integer> body) throws Exception {
        if(request==null || !request.matches("[A-Za-z0-9_-]{1,80}")) throw new IllegalArgumentException("Invalid request ID");
        String hash=digest(payload);
        try(Connection c=db.open()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED); c.setAutoCommit(false);
            try {
                try {
                    one(DBUtils.update(c,"INSERT INTO finance.command_request(request_id,action,payload_hash,result_code) VALUES(?,?,?,'PENDING')",request,action,hash));
                } catch(SQLException duplicate) {
                    if(!"23505".equals(duplicate.getSQLState())) throw duplicate;
                    c.rollback();
                    List<Map<String,Object>> rows=DBUtils.query(c,"SELECT action,payload_hash,result_id,result_code FROM finance.command_request WHERE request_id=?",request);
                    if(rows.size()!=1) throw new SQLException("Conflicting request is not visible; retry using the SAME request ID",duplicate);
                    Map<String,Object> row=rows.get(0);
                    if(!action.equals(row.get("action").toString().trim()) || !hash.equals(row.get("payload_hash").toString().trim()))
                        throw new SQLException("Request ID reused with different parameters","P0001");
                    if(!"SUCCESS".equals(row.get("result_code").toString().trim())) throw new SQLException("Request has no completed result","P0001");
                    c.commit(); return new Receipt(((Number)row.get("result_id")).intValue(),true);
                }
                int id=body.run(c);
                one(DBUtils.update(c,"UPDATE finance.command_request SET result_id=?,result_code='SUCCESS' WHERE request_id=?",id,request));
                c.commit(); return new Receipt(id,false);
            } catch(Exception e) { try { c.rollback(); } catch(SQLException rollback) { e.addSuppressed(rollback); } throw e; }
        }
    }
    public Receipt buy(String request,int asset,int client,int product,int quantity,java.sql.Date date) throws Exception {
        return buyForTest(request,asset,client,product,quantity,date,false);
    }
    // Package-private fault injection is used only by the acceptance test.
    Receipt buyForTest(String request,int asset,int client,int product,int quantity,java.sql.Date date,boolean fail) throws Exception {
        positive(asset); positive(client); positive(product); positive(quantity); Objects.requireNonNull(date,"purchase date");
        String payload="BUY|"+asset+"|"+client+"|"+product+"|"+quantity+"|"+date;
        return command(request,"BUY",payload,c->{
            lockClient(c,client);
            if(new FinancialProduct(c).byId(product).isEmpty()) throw new SQLException("Product does not exist","P0002");
            one(new FinancialAsset(c).insert(asset,client,product,quantity,date));
            if(fail) throw new SQLException("Injected failure after asset insert","P0001");
            one(DBUtils.update(c,"INSERT INTO finance.transaction_event(request_id,action,asset_id,client_id,product_id,quantity,income) VALUES(?,'BUY',?,?,?,?,0)",request,asset,client,product,quantity));
            return asset;
        });
    }
    public Receipt redeem(String request,int asset,int client) throws Exception {
        positive(asset); positive(client);
        return command(request,"REDEEM","REDEEM|"+asset+"|"+client,c->{
            lockClient(c,client);
            List<Map<String,Object>> rows=DBUtils.query(c,"SELECT * FROM finance.financial_asset WHERE a_id=? AND a_client_id=? FOR UPDATE",asset,client);
            if(rows.isEmpty()) throw new SQLException("Asset does not exist or is not owned by this client","P0002");
            Map<String,Object> a=rows.get(0);
            if(!"可用".equals(a.get("a_status").toString().trim())) throw new SQLException("Frozen asset cannot be redeemed","P0001");
            one(new FinancialAsset(c).delete(asset));
            one(DBUtils.update(c,"INSERT INTO finance.transaction_event(request_id,action,asset_id,client_id,product_id,quantity,income) VALUES(?,'REDEEM',?,?,?,?,?)",request,asset,client,a.get("a_product_id"),a.get("a_quantity"),a.get("a_income")));
            return asset;
        });
    }
}
