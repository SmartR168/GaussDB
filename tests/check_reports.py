"""Local SQLite fixture for report SQL logic; this is NOT a GaussDB execution test."""
from pathlib import Path
import sqlite3, re, json
root = Path(__file__).resolve().parents[1]
db = sqlite3.connect(':memory:')
# Formatting adapter for the local fixture; actual TO_CHAR behavior is tested on GaussDB.
db.create_function('TO_CHAR', 2, lambda value, mask: str(value)[:7] if mask == 'YYYY-MM' else None)
db.row_factory = sqlite3.Row
db.execute("ATTACH DATABASE ':memory:' AS finance")
schema = (root/'scripts/01_schema.sql').read_text(encoding='utf-8')
for statement in schema.split(';'):
    if statement.strip().startswith('CREATE TABLE'):
        # SQLite resolves unqualified foreign-key targets within the same attached database.
        db.execute(re.sub(r'REFERENCES finance\.', 'REFERENCES ', statement))
seed = (root/'scripts/02_seed.sql').read_text(encoding='utf-8')
seed = re.sub(r"TO_DATE\('([^']+)','YYYY-MM-DD'\)", r"'\1'", seed)
db.executescript(seed)
checks=[]
def check(ok, label):
    assert ok, label
    checks.append(label)
    print('PASS', len(checks), label)
def report(name, *args):
    return [dict(row) for row in db.execute((root/f'scripts/report_{name}.sql').read_text(encoding='utf-8'), args)]
check([db.execute('SELECT count(*) FROM finance.'+t).fetchone()[0] for t in ['client','bank_card','financial_product','financial_asset']] == [10,10,4,4], 'Word seed cardinalities 10/10/4/4')
original=report('client')
check(sum(r['quantity_total'] for r in original)==10 and sum(r['income_total'] for r in original)==32010, 'Seed amount 10 and income 32010')
db.execute("INSERT INTO finance.bank_card VALUES ('990000000000930001','储蓄卡',1)")
db.execute("INSERT INTO finance.financial_asset VALUES (930001,1,1,1,'可用',100,7,'2018-07-31')")
db.execute("INSERT INTO finance.financial_asset VALUES (930002,1,1,1,'冻结',200,9,'2018-08-01')")
clients=report('client'); one=next(r for r in clients if r['c_id']==1)
check((one['card_count'],one['asset_count'],one['quantity_total'],one['income_total'],one['available_quantity'],one['frozen_quantity'])==(2,3,304,8017,104,200),'Multiple cards/assets are preaggregated without multiplying amounts')
zero=next(r for r in clients if r['c_id']==10)
check(len(clients)==10 and zero['quantity_total']==0 and zero['asset_count']==0,'Zero-holding client is retained')
p=report('product')[0]
check((p['asset_count'],p['client_count'],p['quantity_total'],p['frozen_quantity'])==(3,1,304,200),'Distinct product holders and frozen amount')
july=report('month','2018-07-01','2018-08-01'); august=report('month','2018-08-01','2018-09-01')
check(len(july)==1 and (july[0]['purchase_count'],july[0]['quantity_total'],july[0]['income_total'])==(5,110,32017),'July interval includes July 31 and excludes August 1')
check(len(august)==1 and august[0]['purchase_count']==1 and august[0]['quantity_total']==200,'August lower boundary is included once')
naive=db.execute('SELECT SUM(a_quantity) FROM finance.client c JOIN finance.bank_card b ON b_client_id=c_id JOIN finance.financial_asset a ON a_client_id=c_id WHERE c_id=1').fetchone()[0]
check(naive==608 and one['quantity_total']==304,'Counterexample demonstrates doubled amount from naive join')
check(report('month','2027-01-01','2027-02-01')==[],'Empty reporting interval has no rows')
print('LOCAL_SQLITE_LOGIC_CHECKS_PASSED='+str(len(checks)))
(root/'logs/local_report_fixture.json').write_text(json.dumps({'engine':'SQLite local fixture; not GaussDB','checks':checks,'client1':one,'july':july,'august':august},ensure_ascii=False,indent=2),encoding='utf-8')
