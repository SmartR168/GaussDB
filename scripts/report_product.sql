SELECT p.p_id,p.p_name,COUNT(a.a_id) AS asset_count,
 COUNT(DISTINCT a.a_client_id) AS client_count,
 COALESCE(SUM(a.a_quantity),0) AS quantity_total,
 COALESCE(SUM(a.a_income),0) AS income_total,
 COALESCE(SUM(CASE WHEN a.a_status='冻结' THEN a.a_quantity ELSE 0 END),0) AS frozen_quantity
FROM finance.financial_product p
LEFT JOIN finance.financial_asset a ON a.a_product_id=p.p_id
GROUP BY p.p_id,p.p_name ORDER BY p.p_id;
