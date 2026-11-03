package expt.db.finance.dao;
import expt.db.finance.DBUtils;
import java.sql.*;
import java.util.*;

public final class FinancialAsset {
    private final Connection c;
    public FinancialAsset(Connection c) { this.c=c; }
    public List<Map<String,Object>> list() throws SQLException { return DBUtils.query(c,"SELECT * FROM finance.financial_asset ORDER BY a_id"); }
    public List<Map<String,Object>> byId(int id) throws SQLException { return DBUtils.query(c,"SELECT * FROM finance.financial_asset WHERE a_id=?",id); }
    public List<Map<String,Object>> byClient(int id) throws SQLException { return DBUtils.query(c,"SELECT * FROM finance.financial_asset WHERE a_client_id=? ORDER BY a_id",id); }
    public long count(int id) throws SQLException { return DBUtils.number(c,"SELECT count(*) FROM finance.financial_asset WHERE a_client_id=?",id); }
    public int insert(int id,int client,int product,int quantity,java.sql.Date date) throws SQLException {
        return DBUtils.update(c,"INSERT INTO finance.financial_asset(a_id,a_client_id,a_product_id,a_type,a_status,a_quantity,a_income,a_purchase_time) VALUES(?,?,?,1,'可用',?,0,?)",id,client,product,quantity,date);
    }
    public int delete(int id) throws SQLException { return DBUtils.update(c,"DELETE FROM finance.financial_asset WHERE a_id=?",id); }
}
