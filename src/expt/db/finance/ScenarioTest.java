package expt.db.finance;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

public final class ScenarioTest {
    private final DBUtils db;
    private final Service service;
    public ScenarioTest(DBUtils db) { this.db=db; service=new Service(db); }
    private long count(String table,String where,Object... args) throws Exception {
        try(Connection c=db.open()) { return DBUtils.number(c,"SELECT count(*) FROM finance."+table+" WHERE "+where,args); }
    }
    private void cleanRequest(String id) throws Exception {
        Checks.that(count("command_request","request_id=?",id)==0 && count("transaction_event","request_id=?",id)==0,"Failed request leaves no request/event: "+id);
    }
    private List<Object> race(Callable<Service.Receipt> a,Callable<Service.Receipt> b) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);
        List<Future<Object>> futures=new ArrayList<>();
        try {
            for(Callable<Service.Receipt> job:Arrays.asList(a,b)) futures.add(pool.submit(()->{
                ready.countDown(); if(!start.await(10,TimeUnit.SECONDS)) throw new TimeoutException("Start gate");
                try { return job.call(); } catch(SQLException e) { return e; }
            }));
            if(!ready.await(10,TimeUnit.SECONDS)) throw new TimeoutException("Workers not ready"); start.countDown();
            List<Object> results=new ArrayList<>(); for(Future<Object> f:futures) results.add(f.get(100,TimeUnit.SECONDS));
            System.out.println("CONCURRENT_RESULTS "+results); return results;
        } finally { start.countDown(); for(Future<?> f:futures) f.cancel(true); pool.shutdownNow(); pool.awaitTermination(5,TimeUnit.SECONDS); }
    }
    public void run() throws Exception {
        Checks.that(count("client","c_id>=900000")==0 && count("financial_asset","a_id>=900000")==0 && count("command_request","request_id LIKE 'lab5_%'")==0,"Reserved test namespace is empty");
        service.openAccount(910001,"业务测试","990000000000910001","19900910001","flow@example.test");
        Checks.that(service.detail(910001).get("client").size()==1,"Open account and query detail");
        service.modify(910001,"业务更新","19900910001","flow2@example.test");
        Checks.that("业务更新".equals(service.detail(910001).get("client").get(0).get("c_name").toString().trim()),"Modify client persisted");
        Checks.rejects(()->service.modify(999999,"不存在","19900999999","none@example.test"),"P0002","Missing modify fails");
        service.openCard("990000000000910001","储蓄卡",910001);
        Checks.rejects(()->service.closeAccount(910001),"P0001","Account with card cannot close");
        Checks.rejects(()->service.closeCard("990000000000910001",1),"P0002","Cannot close another client's card");
        java.sql.Date day=java.sql.Date.valueOf("2026-11-03");
        Checks.invalid(()->service.buy("lab5_badamount",920001,910001,1,0,day),"Zero purchase amount rejected before transaction"); cleanRequest("lab5_badamount");
        Checks.invalid(()->service.buy("bad request!",920001,910001,1,100,day),"Invalid request ID rejected");
        Checks.invalid(()->service.modify(910001,"业务更新","bad-phone","flow2@example.test"),"Invalid contact rejected before update");
        Checks.rejects(()->service.buy("lab5_badclient",920001,999999,1,100,day),"P0002","Unknown buy client rejected"); cleanRequest("lab5_badclient");
        Checks.rejects(()->service.buy("lab5_badproduct",920001,910001,999999,100,day),"P0002","Unknown buy product rejected"); cleanRequest("lab5_badproduct");
        Checks.rejects(()->service.buyForTest("lab5_rollback",920001,910001,1,100,day,true),"P0001","Injected failure rolls back"); cleanRequest("lab5_rollback");
        Checks.that(count("financial_asset","a_id=920001")==0,"Injected failure leaves no asset");
        Service.Receipt first=service.buy("lab5_buy",920001,910001,1,100,day);
        Service.Receipt replay=service.buy("lab5_buy",920001,910001,1,100,day);
        Checks.that(!first.replay && replay.replay && replay.assetId==920001,"Same buy request replays original receipt");
        Checks.rejects(()->service.buy("lab5_buy",920001,910001,1,101,day),"P0001","Same request with changed quantity rejected");
        Checks.rejects(()->service.buy("lab5_buy",920001,910001,1,100,java.sql.Date.valueOf("2026-11-04")),"P0001","Same request with changed purchase date rejected");
        Checks.rejects(()->service.redeem("lab5_buy",920001,910001),"P0001","Same request with changed action rejected");
        Checks.rejects(()->service.redeem("lab5_wrongowner",920001,1),"P0002","Cannot redeem another client's asset"); cleanRequest("lab5_wrongowner");
        Checks.that(count("financial_asset","a_id=920001")==1 && count("transaction_event","request_id='lab5_buy'")==1,"Replay creates one asset and one event");
        Checks.rejects(()->service.closeAccount(910001),"P0001","Account with assets cannot close");
        Checks.rejects(()->service.redeem("lab5_frozen",4,4),"P0001","Frozen seed asset cannot redeem"); cleanRequest("lab5_frozen");
        Service.Receipt redemption=service.redeem("lab5_redeem",920001,910001);
        Checks.that(!redemption.replay && service.redeem("lab5_redeem",920001,910001).replay,"Redeem replay works after holding deletion");
        Checks.rejects(()->service.redeem("lab5_again",920001,910001),"P0002","Different request cannot redeem deleted holding"); cleanRequest("lab5_again");
        List<Object> buys=race(()->service.buy("lab5_race_buy",920002,910001,1,200,day),()->service.buy("lab5_race_buy",920002,910001,1,200,day));
        Checks.that(buys.stream().allMatch(x->x instanceof Service.Receipt) && buys.stream().filter(x->((Service.Receipt)x).replay).count()==1,"Concurrent same-key buy: one commit, one replay");
        Checks.that(count("financial_asset","a_id=920002")==1 && count("command_request","request_id='lab5_race_buy'")==1 && count("transaction_event","request_id='lab5_race_buy'")==1,"Concurrent buy database cardinalities 1/1/1");
        List<Object> redeems=race(()->service.redeem("lab5_race_ra",920002,910001),()->service.redeem("lab5_race_rb",920002,910001));
        Checks.that(redeems.stream().filter(x->x instanceof Service.Receipt).count()==1 && redeems.stream().filter(x->x instanceof SQLException && "P0002".equals(((SQLException)x).getSQLState())).count()==1,"Concurrent different-key redeem: exactly one success");
        Checks.that(count("financial_asset","a_id=920002")==0 && count("transaction_event","request_id IN ('lab5_race_ra','lab5_race_rb')")==1 && count("command_request","request_id IN ('lab5_race_ra','lab5_race_rb')")==1,"Redeem loser rolled back; one committed audit event");
        service.closeCard("990000000000910001",910001); service.closeAccount(910001);
        Checks.that(service.detail(910001).get("client").isEmpty(),"Close card then account succeeds");
        Checks.that(count("transaction_event","client_id=910001")==4,"Transaction history survives account closure");
        reportFixture();
        Checks.that(count("client","1=1")==10 && count("bank_card","1=1")==10 && count("financial_product","1=1")==4 && count("financial_asset","1=1")==4,"Final four business tables retain 10/10/4/4 seeds");
    }
    private static long n(Map<String,Object> row,String key) { return ((Number)row.get(key)).longValue(); }
    void reportFixture() throws Exception {
        try(Connection c=db.open()) {
            c.setAutoCommit(false);
            try {
                DBUtils.update(c,"INSERT INTO finance.bank_card VALUES('990000000000930001','储蓄卡',1)");
                DBUtils.update(c,"INSERT INTO finance.financial_asset VALUES(930001,1,1,1,'可用',100,7,?)",java.sql.Date.valueOf("2018-07-31"));
                DBUtils.update(c,"INSERT INTO finance.financial_asset VALUES(930002,1,1,1,'冻结',200,9,?)",java.sql.Date.valueOf("2018-08-01"));
                List<Map<String,Object>> clients=DBUtils.query(c,Reports.sql("client"));
                Map<String,Object> one=clients.stream().filter(r->n(r,"c_id")==1).findFirst().orElseThrow();
                Checks.that(n(one,"card_count")==2 && n(one,"asset_count")==3 && n(one,"quantity_total")==304 && n(one,"income_total")==8017 && n(one,"frozen_quantity")==200,"Report preaggregation: two cards/three assets, amount 304 not 608");
                Map<String,Object> zero=clients.stream().filter(r->n(r,"c_id")==10).findFirst().orElseThrow();
                Checks.that(clients.size()==10 && n(zero,"asset_count")==0 && n(zero,"quantity_total")==0,"Report preserves zero-holding client");
                List<Map<String,Object>> products=DBUtils.query(c,Reports.sql("product"));
                Map<String,Object> p=products.get(0);
                Checks.that(n(p,"client_count")==1 && n(p,"asset_count")==3 && n(p,"quantity_total")==304,"Product distinct holder count and amount");
                List<Map<String,Object>> july=DBUtils.query(c,Reports.sql("month"),java.sql.Date.valueOf("2018-07-01"),java.sql.Date.valueOf("2018-08-01"));
                Checks.that(july.size()==1 && n(july.get(0),"purchase_count")==5 && n(july.get(0),"quantity_total")==110 && n(july.get(0),"income_total")==32017,"July includes July 31, excludes August 1; amount 110");
                List<Map<String,Object>> aug=DBUtils.query(c,Reports.sql("month"),java.sql.Date.valueOf("2018-08-01"),java.sql.Date.valueOf("2018-09-01"));
                Checks.that(aug.size()==1 && n(aug.get(0),"purchase_count")==1 && n(aug.get(0),"quantity_total")==200,"August includes lower boundary once");
                DBUtils.print(clients); DBUtils.print(july); DBUtils.print(aug);
            } finally { c.rollback(); }
        }
    }
}
