SELECT TO_CHAR(a_purchase_time,'YYYY-MM') AS purchase_month,
 COUNT(*) AS purchase_count,COUNT(DISTINCT a_client_id) AS client_count,
 SUM(a_quantity) AS quantity_total,SUM(a_income) AS income_total
FROM finance.financial_asset
WHERE a_purchase_time>=? AND a_purchase_time<?
GROUP BY TO_CHAR(a_purchase_time,'YYYY-MM')
ORDER BY purchase_month;
