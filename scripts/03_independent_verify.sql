\set ON_ERROR_STOP on
\pset pager off
\conninfo
SELECT current_database(),current_user,version(),current_timestamp;
SHOW server_encoding;
SHOW autoanalyze;
SELECT nspname,pg_get_userbyid(nspowner) AS owner FROM pg_namespace WHERE nspname='finance';
SELECT tablename,tableowner FROM pg_tables WHERE schemaname='finance' ORDER BY tablename;
SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname='finance' ORDER BY tablename,indexname;
SELECT 'client' AS table_name,count(*) AS rows FROM finance.client UNION ALL
SELECT 'bank_card',count(*) FROM finance.bank_card UNION ALL
SELECT 'financial_product',count(*) FROM finance.financial_product UNION ALL
SELECT 'financial_asset',count(*) FROM finance.financial_asset UNION ALL
SELECT 'command_request',count(*) FROM finance.command_request UNION ALL
SELECT 'transaction_event',count(*) FROM finance.transaction_event;
SELECT a_id,a_client_id,a_product_id,a_type,a_status,a_quantity,a_income,a_purchase_time FROM finance.financial_asset ORDER BY a_id;
SELECT sum(a_quantity) AS total_quantity,sum(a_income) AS total_income FROM finance.financial_asset;
SELECT request_id,action,result_id,result_code FROM finance.command_request ORDER BY request_id;
SELECT * FROM finance.transaction_event ORDER BY request_id;
SELECT count(*) AS incomplete_requests FROM finance.command_request WHERE result_code<>'SUCCESS';
SELECT count(*) AS missing_event FROM finance.command_request r LEFT JOIN finance.transaction_event e ON e.request_id=r.request_id WHERE e.request_id IS NULL;
SELECT count(*) AS orphan_assets FROM finance.financial_asset a LEFT JOIN finance.client c ON a.a_client_id=c.c_id LEFT JOIN finance.financial_product p ON a.a_product_id=p.p_id WHERE c.c_id IS NULL OR p.p_id IS NULL;
