package expt.db.finance;
import java.io.Console;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public final class FlowControl {
    private final Service service;
    private final Reports reports;
    private final Console console;
    public FlowControl(DBUtils db) { service=new Service(db); reports=new Reports(db); console=System.console(); }
    private String read(String label) { String s=console.readLine("%s: ",label); if(s==null) throw new IllegalStateException("Input closed"); return s; }
    private int integer(String label) { return Integer.parseInt(read(label)); }
    public void run() {
        while(true) {
            System.out.println("\n1客户列表 2客户详情 3修改客户 4开户 5开卡 6销卡 7销户 8产品列表 9申购 10赎回 11客户资产汇总 12产品持仓 13月度统计 0退出");
            try {
                String menu=read("菜单");
                switch(menu) {
                    case "0": return;
                    case "1": DBUtils.print(service.clients()); break;
                    case "2": service.detail(integer("客户编号")).forEach((name,rows)->{ System.out.println(name); DBUtils.print(rows); }); break;
                    case "3": service.modify(integer("客户编号"),read("姓名"),read("手机号"),read("邮箱")); System.out.println("修改成功（1行）"); break;
                    case "4": service.openAccount(integer("客户编号"),read("姓名"),read("身份证号"),read("手机号"),read("邮箱")); System.out.println("开户成功（1行）"); break;
                    case "5": service.openCard(read("卡号"),read("类型：信用卡/储蓄卡"),integer("客户编号")); System.out.println("开卡成功（1行）"); break;
                    case "6": service.closeCard(read("卡号"),integer("所属客户编号")); System.out.println("销卡成功（1行）"); break;
                    case "7": service.closeAccount(integer("客户编号")); System.out.println("销户成功（1行）"); break;
                    case "8": DBUtils.print(service.products()); break;
                    case "9": System.out.println(service.buy(read("请求号"),integer("资产编号"),integer("客户编号"),integer("产品编号"),integer("申购金额"),java.sql.Date.valueOf(read("申购日期 YYYY-MM-DD")))); break;
                    case "10": System.out.println(service.redeem(read("请求号"),integer("资产编号"),integer("所属客户编号"))); break;
                    case "11": DBUtils.print(reports.run("client",null,null)); break;
                    case "12": DBUtils.print(reports.run("product",null,null)); break;
                    case "13": DBUtils.print(reports.run("month",LocalDate.parse(read("起日（含）")),LocalDate.parse(read("止日（不含）")))); break;
                    default: System.out.println("请选择菜单中存在的编号");
                }
            } catch(SQLException e) { System.out.println("操作失败 SQLSTATE="+e.getSQLState()+": "+e.getMessage()); }
            catch(Exception e) { System.out.println("操作失败: "+e.getMessage()); }
        }
    }
}
