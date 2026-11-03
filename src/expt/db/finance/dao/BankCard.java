package expt.db.finance.dao;
import expt.db.finance.DBUtils;
import java.sql.*;
import java.util.*;

public final class BankCard {
    private final Connection c;
    public BankCard(Connection c) { this.c=c; }
    public List<Map<String,Object>> list() throws SQLException { return DBUtils.query(c,"SELECT * FROM finance.bank_card ORDER BY b_number"); }
    public List<Map<String,Object>> byNumber(String number) throws SQLException { return DBUtils.query(c,"SELECT * FROM finance.bank_card WHERE b_number=?",number); }
    public List<Map<String,Object>> byClient(int id) throws SQLException { return DBUtils.query(c,"SELECT * FROM finance.bank_card WHERE b_client_id=? ORDER BY b_number",id); }
    public long count(int id) throws SQLException { return DBUtils.number(c,"SELECT count(*) FROM finance.bank_card WHERE b_client_id=?",id); }
    public int insert(String number,String type,int id) throws SQLException { return DBUtils.update(c,"INSERT INTO finance.bank_card(b_number,b_type,b_client_id) VALUES(?,?,?)",number,type,id); }
    public int delete(String number) throws SQLException { return DBUtils.update(c,"DELETE FROM finance.bank_card WHERE b_number=?",number); }
}
