package expt.db.finance.dao;
import expt.db.finance.DBUtils;
import java.sql.*;
import java.util.*;

public final class FinancialProduct {
    private final Connection c;
    public FinancialProduct(Connection c) { this.c=c; }
    public List<Map<String,Object>> list() throws SQLException { return DBUtils.query(c,"SELECT * FROM finance.financial_product ORDER BY p_id"); }
    public List<Map<String,Object>> byId(int id) throws SQLException { return DBUtils.query(c,"SELECT * FROM finance.financial_product WHERE p_id=?",id); }
}
