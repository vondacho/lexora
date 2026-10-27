-- Shares of the demo invoices and requests that had none: each single customer owes the whole amount.
-- Customer statements, receipts and QR-bills work per customer, from these shares.
INSERT INTO invo_invoices_distrib (invo_dist_invo_id, invo_dist_cust_id, invo_dist_amount)
  SELECT i.invo_id, c.cust_id, i.invo_amount
  FROM invo_invoices i
  JOIN invo_folders f ON f.fold_id = i.invo_folder_id
  JOIN invo_customers c ON c.cust_folder_id = f.fold_id
  WHERE f.fold_name IN ('Keller', 'Meyer', 'Bernasconi', 'Pittet c. Pittet', 'Hug')
    AND (SELECT COUNT(*) FROM invo_customers c2 WHERE c2.cust_folder_id = f.fold_id) = 1
    AND NOT EXISTS (SELECT 1 FROM invo_invoices_distrib d WHERE d.invo_dist_invo_id = i.invo_id);
INSERT INTO invo_requests_distrib (req_dist_req_id, req_dist_cust_id, req_dist_amount)
  SELECT r.req_id, c.cust_id, r.req_amount
  FROM invo_requests r
  JOIN invo_folders f ON f.fold_id = r.req_folder_id
  JOIN invo_customers c ON c.cust_folder_id = f.fold_id
  WHERE f.fold_name = 'Girard'
    AND (SELECT COUNT(*) FROM invo_customers c2 WHERE c2.cust_folder_id = f.fold_id) = 1
    AND NOT EXISTS (SELECT 1 FROM invo_requests_distrib d WHERE d.req_dist_req_id = r.req_id);
