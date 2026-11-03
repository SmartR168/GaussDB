package expt.db.finance;
import java.sql.SQLException;
public final class Checks {
    private static int total;
    public static void that(boolean ok,String label) {
        if(!ok) throw new AssertionError("FAIL "+label);
        System.out.println("PASS "+(++total)+" "+label);
    }
    @FunctionalInterface public interface Task { void run() throws Exception; }
    public static void rejects(Task task,String state,String label) throws Exception {
        try { task.run(); } catch(SQLException e) {
            that(state.equals(e.getSQLState()),label+" [SQLSTATE="+e.getSQLState()+"]"); return;
        }
        throw new AssertionError("Expected SQL failure: "+label);
    }
    public static int total() { return total; }
    public static void invalid(Task task,String label) throws Exception {
        try { task.run(); } catch(IllegalArgumentException e) { that(true,label); return; }
        throw new AssertionError("Expected invalid input rejection: "+label);
    }
}
