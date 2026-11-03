WITH cards AS (
 SELECT b_client_id,COUNT(*) AS card_count
 FROM finance.bank_card GROUP BY b_client_id
), assets AS (
 SELECT a_client_id,COUNT(*) AS asset_count,
 SUM(a_quantity) AS quantity_total,SUM(a_income) AS income_total,
 SUM(CASE WHEN a_status='可用' THEN a_quantity ELSE 0 END) AS available_quantity,
 SUM(CASE WHEN a_status='冻结' THEN a_quantity ELSE 0 END) AS frozen_quantity
 FROM finance.financial_asset GROUP BY a_client_id
)
SELECT c.c_id,c.c_name,COALESCE(b.card_count,0) AS card_count,
 COALESCE(a.asset_count,0) AS asset_count,
 COALESCE(a.quantity_total,0) AS quantity_total,
 COALESCE(a.income_total,0) AS income_total,
 COALESCE(a.available_quantity,0) AS available_quantity,
 COALESCE(a.frozen_quantity,0) AS frozen_quantity
FROM finance.client c LEFT JOIN cards b ON b.b_client_id=c.c_id
LEFT JOIN assets a ON a.a_client_id=c.c_id
ORDER BY quantity_total DESC,c.c_id;
