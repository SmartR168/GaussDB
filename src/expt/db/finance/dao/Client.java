package expt.db.finance.dao;
import expt.db.finance.DBUtils;
import java.sql.*;
import java.util.*;

public final class Client {
    private final Connection c;
    public Client(Connection c) { this.c=c; }
    public List<Map<String,Object>> list() throws SQLException { return DBUtils.query(c,"SELECT * FROM finance.client ORDER BY c_id"); }
    public List<Map<String,Object>> byId(int id) throws SQLException { return DBUtils.query(c,"SELECT * FROM finance.client WHERE c_id=?",id); }
    public int insert(int id,String name,String identity,String phone,String mail) throws SQLException {
        return DBUtils.update(c,"INSERT INTO finance.client(c_id,c_name,c_id_card,c_phone,c_mail) VALUES(?,?,?,?,?)",id,name,identity,phone,mail);
    }
    public int update(int id,String name,String phone,String mail) throws SQLException {
        return DBUtils.update(c,"UPDATE finance.client SET c_name=?,c_phone=?,c_mail=? WHERE c_id=?",name,phone,mail,id);
    }
    public int delete(int id) throws SQLException { return DBUtils.update(c,"DELETE FROM finance.client WHERE c_id=?",id); }
}
