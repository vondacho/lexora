-- More demo data of the local H2 database (profile "h2"), around the to-do list of the home page:
-- overdue invoices and requests, folders to invoice, paid invoices, advances and office expenses.
-- Dates are relative to the day the database is created, so the overdue cases and the current month always show.
-- Generated: amounts follow the application's rules (VAT 8.1 % truncated to 10 centimes, expenses VAT included).

-- Keller: invoice overdue for about two months, one reminder already sent
INSERT INTO invo_folders (fold_date, fold_name, fold_status, fold_first_date, fold_handled_by, fold_type, fold_comment, fold_mute_user, fold_mute_date)
  VALUES (DATEADD(DAY, -120, CURRENT_DATE), 'Keller', 'En cours', DATEADD(DAY, -98, CURRENT_DATE), 'dmo', 1, NULL, 'demo', CURRENT_DATE);
INSERT INTO invo_customers (cust_folder_id, cust_title, cust_first_name, cust_last_name, cust_address, cust_zip, cust_city, cust_mob_phone, cust_email, cust_is_billing, cust_mute_user, cust_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Keller'), 'Monsieur', 'Thomas', 'Keller', 'Rue du Seyon 8', '2000', 'Neuchâtel', '079 000 00 11', 'thomas.keller@example.ch', FALSE, 'demo', CURRENT_DATE);
INSERT INTO invo_invoices (invo_folder_id, invo_date, invo_hono_sum, invo_depo_sum, invo_disc_rate, invo_disc, invo_tax_rate, invo_tax, invo_amount, invo_payed_date, invo_delay, invo_mute_user, invo_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Keller'), DATEADD(DAY, -77, CURRENT_DATE), 400.00, 0, 0, 0, 0.081, 32.40, 432.40, NULL, 15, 'demo', CURRENT_DATE);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_amount, hono_invo_id, hono_mute_user, hono_mute_date)
  VALUES (DATEADD(DAY, -98, CURRENT_DATE), (SELECT fold_id FROM invo_folders WHERE fold_name = 'Keller'), 'Séance de médiation', 120, 200, 400.00, (SELECT invo_id FROM invo_invoices WHERE invo_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Keller')), 'demo', CURRENT_DATE);
INSERT INTO invo_invoices_recalls (reca_invo_id, reca_date, reca_degree, reca_amount, reca_mute_user, reca_mute_date)
  VALUES ((SELECT invo_id FROM invo_invoices WHERE invo_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Keller')), DATEADD(DAY, -14, CURRENT_DATE), 1, 432.40, 'demo', CURRENT_DATE);

-- Meyer: invoice overdue for five weeks, no reminder yet
INSERT INTO invo_folders (fold_date, fold_name, fold_status, fold_first_date, fold_handled_by, fold_type, fold_comment, fold_mute_user, fold_mute_date)
  VALUES (DATEADD(DAY, -90, CURRENT_DATE), 'Meyer', 'En cours', DATEADD(DAY, -62, CURRENT_DATE), 'dmo', 1, NULL, 'demo', CURRENT_DATE);
INSERT INTO invo_customers (cust_folder_id, cust_title, cust_first_name, cust_last_name, cust_address, cust_zip, cust_city, cust_mob_phone, cust_email, cust_is_billing, cust_mute_user, cust_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Meyer'), 'Madame', 'Andrea', 'Meyer', 'Avenue de la Gare 21', '2000', 'Neuchâtel', '079 000 00 12', 'andrea.meyer@example.ch', FALSE, 'demo', CURRENT_DATE);
INSERT INTO invo_invoices (invo_folder_id, invo_date, invo_hono_sum, invo_depo_sum, invo_disc_rate, invo_disc, invo_tax_rate, invo_tax, invo_amount, invo_payed_date, invo_delay, invo_mute_user, invo_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Meyer'), DATEADD(DAY, -55, CURRENT_DATE), 1000.00, 0, 0, 0, 0.081, 81.00, 1081.00, NULL, 20, 'demo', CURRENT_DATE);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_amount, hono_invo_id, hono_mute_user, hono_mute_date)
  VALUES (DATEADD(DAY, -76, CURRENT_DATE), (SELECT fold_id FROM invo_folders WHERE fold_name = 'Meyer'), 'Séance de médiation', 150, 200, 500.00, (SELECT invo_id FROM invo_invoices WHERE invo_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Meyer')), 'demo', CURRENT_DATE);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_amount, hono_invo_id, hono_mute_user, hono_mute_date)
  VALUES (DATEADD(DAY, -62, CURRENT_DATE), (SELECT fold_id FROM invo_folders WHERE fold_name = 'Meyer'), 'Séance de médiation', 150, 200, 500.00, (SELECT invo_id FROM invo_invoices WHERE invo_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Meyer')), 'demo', CURRENT_DATE);

-- Bernasconi: a recent overdue invoice, and a new session to invoice
INSERT INTO invo_folders (fold_date, fold_name, fold_status, fold_first_date, fold_handled_by, fold_type, fold_comment, fold_mute_user, fold_mute_date)
  VALUES (DATEADD(DAY, -70, CURRENT_DATE), 'Bernasconi', 'En cours', DATEADD(DAY, -46, CURRENT_DATE), 'dmo', 1, NULL, 'demo', CURRENT_DATE);
INSERT INTO invo_customers (cust_folder_id, cust_title, cust_first_name, cust_last_name, cust_address, cust_zip, cust_city, cust_mob_phone, cust_email, cust_is_billing, cust_mute_user, cust_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Bernasconi'), 'Madame', 'Rita', 'Bernasconi', 'Rue de la Côte 4', '2006', 'Neuchâtel', '079 000 00 13', 'rita.bernasconi@example.ch', FALSE, 'demo', CURRENT_DATE);
INSERT INTO invo_invoices (invo_folder_id, invo_date, invo_hono_sum, invo_depo_sum, invo_disc_rate, invo_disc, invo_tax_rate, invo_tax, invo_amount, invo_payed_date, invo_delay, invo_mute_user, invo_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Bernasconi'), DATEADD(DAY, -36, CURRENT_DATE), 200.00, 0, 0, 0, 0.081, 16.20, 216.20, NULL, 20, 'demo', CURRENT_DATE);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_amount, hono_invo_id, hono_mute_user, hono_mute_date)
  VALUES (DATEADD(DAY, -46, CURRENT_DATE), (SELECT fold_id FROM invo_folders WHERE fold_name = 'Bernasconi'), 'Séance de médiation', 60, 200, 200.00, (SELECT invo_id FROM invo_invoices WHERE invo_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Bernasconi')), 'demo', CURRENT_DATE);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_amount, hono_invo_id, hono_mute_user, hono_mute_date)
  VALUES (DATEADD(DAY, -12, CURRENT_DATE), (SELECT fold_id FROM invo_folders WHERE fold_name = 'Bernasconi'), 'Entretien téléphonique', 180, 150, 450.00, NULL, 'demo', CURRENT_DATE);

-- Favre: request overdue, half of it paid by Nadia
INSERT INTO invo_folders (fold_date, fold_name, fold_status, fold_first_date, fold_handled_by, fold_type, fold_comment, fold_mute_user, fold_mute_date)
  VALUES (DATEADD(DAY, -70, CURRENT_DATE), 'Favre', 'En cours', DATEADD(DAY, -60, CURRENT_DATE), 'dmo', 1, NULL, 'demo', CURRENT_DATE);
INSERT INTO invo_customers (cust_folder_id, cust_title, cust_first_name, cust_last_name, cust_address, cust_zip, cust_city, cust_mob_phone, cust_email, cust_is_billing, cust_mute_user, cust_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Favre'), 'Madame', 'Nadia', 'Favre', 'Rue des Parcs 30', '2000', 'Neuchâtel', '079 000 00 14', 'nadia.favre@example.ch', FALSE, 'demo', CURRENT_DATE);
INSERT INTO invo_customers (cust_folder_id, cust_title, cust_first_name, cust_last_name, cust_address, cust_zip, cust_city, cust_mob_phone, cust_email, cust_is_billing, cust_mute_user, cust_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Favre'), 'Monsieur', 'Paul', 'Favre', 'Rue Louis-Favre 2', '2017', 'Boudry', '079 000 00 15', 'paul.favre@example.ch', FALSE, 'demo', CURRENT_DATE);
INSERT INTO invo_requests (req_folder_id, req_date, req_amount, req_delay, req_hono_sum, req_depo_sum, req_mute_user, req_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Favre'), DATEADD(DAY, -67, CURRENT_DATE), 800, 30, 0, 0, 'demo', CURRENT_DATE);
INSERT INTO invo_requests_distrib (req_dist_req_id, req_dist_cust_id, req_dist_amount) VALUES ((SELECT req_id FROM invo_requests WHERE req_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Favre')), (SELECT cust_id FROM invo_customers WHERE cust_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Favre') AND cust_first_name = 'Nadia'), 400);
INSERT INTO invo_requests_distrib (req_dist_req_id, req_dist_cust_id, req_dist_amount) VALUES ((SELECT req_id FROM invo_requests WHERE req_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Favre')), (SELECT cust_id FROM invo_customers WHERE cust_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Favre') AND cust_first_name = 'Paul'), 400);
INSERT INTO invo_advances (adva_fold_id, adva_req_id, adva_date, adva_amount, adva_cust_id, adva_cash, adva_iban, adva_mute_user, adva_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Favre'), (SELECT req_id FROM invo_requests WHERE req_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Favre')), GREATEST(DATEADD(DAY, -5, CURRENT_DATE), DATEADD(DAY, 1 - DAY(CURRENT_DATE), CURRENT_DATE)), 400, (SELECT cust_id FROM invo_customers WHERE cust_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Favre') AND cust_first_name = 'Nadia'), FALSE, 'CH9300762011623852957', 'demo', CURRENT_DATE);

-- Girard: request overdue for ten days
INSERT INTO invo_folders (fold_date, fold_name, fold_status, fold_first_date, fold_handled_by, fold_type, fold_comment, fold_mute_user, fold_mute_date)
  VALUES (DATEADD(DAY, -30, CURRENT_DATE), 'Girard', 'En cours', DATEADD(DAY, -18, CURRENT_DATE), 'dmo', 1, NULL, 'demo', CURRENT_DATE);
INSERT INTO invo_customers (cust_folder_id, cust_title, cust_first_name, cust_last_name, cust_address, cust_zip, cust_city, cust_mob_phone, cust_email, cust_is_billing, cust_mute_user, cust_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Girard'), 'Madame', 'Hélène', 'Girard', 'Chemin des Pavés 9', '2000', 'Neuchâtel', '079 000 00 16', 'helene.girard@example.ch', FALSE, 'demo', CURRENT_DATE);
INSERT INTO invo_requests (req_folder_id, req_date, req_amount, req_delay, req_hono_sum, req_depo_sum, req_mute_user, req_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Girard'), DATEADD(DAY, -25, CURRENT_DATE), 500, 15, 0, 0, 'demo', CURRENT_DATE);

-- Rochat c. Rochat: four sessions not invoiced, no advance
INSERT INTO invo_folders (fold_date, fold_name, fold_status, fold_first_date, fold_handled_by, fold_type, fold_comment, fold_mute_user, fold_mute_date)
  VALUES (DATEADD(DAY, -60, CURRENT_DATE), 'Rochat c. Rochat', 'En cours', DATEADD(DAY, -48, CURRENT_DATE), 'dmo', 1, NULL, 'demo', CURRENT_DATE);
INSERT INTO invo_customers (cust_folder_id, cust_title, cust_first_name, cust_last_name, cust_address, cust_zip, cust_city, cust_mob_phone, cust_email, cust_is_billing, cust_mute_user, cust_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Rochat c. Rochat'), 'Madame', 'Sophie', 'Rochat', 'Rue du Château 5', '2034', 'Peseux', '079 000 00 17', 'sophie.rochat@example.ch', FALSE, 'demo', CURRENT_DATE);
INSERT INTO invo_customers (cust_folder_id, cust_title, cust_first_name, cust_last_name, cust_address, cust_zip, cust_city, cust_mob_phone, cust_email, cust_is_billing, cust_mute_user, cust_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Rochat c. Rochat'), 'Monsieur', 'Luc', 'Rochat', 'Rue de Neuchâtel 40', '2034', 'Peseux', '079 000 00 18', 'luc.rochat@example.ch', FALSE, 'demo', CURRENT_DATE);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_amount, hono_invo_id, hono_mute_user, hono_mute_date)
  VALUES (DATEADD(DAY, -48, CURRENT_DATE), (SELECT fold_id FROM invo_folders WHERE fold_name = 'Rochat c. Rochat'), 'Séance de médiation', 110, 200, 366.67, NULL, 'demo', CURRENT_DATE);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_amount, hono_invo_id, hono_mute_user, hono_mute_date)
  VALUES (DATEADD(DAY, -34, CURRENT_DATE), (SELECT fold_id FROM invo_folders WHERE fold_name = 'Rochat c. Rochat'), 'Séance de médiation', 110, 200, 366.67, NULL, 'demo', CURRENT_DATE);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_amount, hono_invo_id, hono_mute_user, hono_mute_date)
  VALUES (DATEADD(DAY, -20, CURRENT_DATE), (SELECT fold_id FROM invo_folders WHERE fold_name = 'Rochat c. Rochat'), 'Séance de médiation', 110, 200, 366.67, NULL, 'demo', CURRENT_DATE);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_amount, hono_invo_id, hono_mute_user, hono_mute_date)
  VALUES (DATEADD(DAY, -6, CURRENT_DATE), (SELECT fold_id FROM invo_folders WHERE fold_name = 'Rochat c. Rochat'), 'Séance de médiation', 110, 200, 366.67, NULL, 'demo', CURRENT_DATE);

-- Martin & Rossi: three sessions, partly covered by a spontaneous cash advance
INSERT INTO invo_folders (fold_date, fold_name, fold_status, fold_first_date, fold_handled_by, fold_type, fold_comment, fold_mute_user, fold_mute_date)
  VALUES (DATEADD(DAY, -50, CURRENT_DATE), 'Martin & Rossi', 'En cours', DATEADD(DAY, -41, CURRENT_DATE), 'dmo', 1, NULL, 'demo', CURRENT_DATE);
INSERT INTO invo_customers (cust_folder_id, cust_title, cust_first_name, cust_last_name, cust_address, cust_zip, cust_city, cust_mob_phone, cust_email, cust_is_billing, cust_mute_user, cust_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Martin & Rossi'), 'Madame', 'Julie', 'Martin', 'Rue de lEvole 15', '2000', 'Neuchâtel', '079 000 00 19', 'julie.martin@example.ch', FALSE, 'demo', CURRENT_DATE);
INSERT INTO invo_customers (cust_folder_id, cust_title, cust_first_name, cust_last_name, cust_address, cust_zip, cust_city, cust_mob_phone, cust_email, cust_is_billing, cust_mute_user, cust_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Martin & Rossi'), 'Monsieur', 'Enzo', 'Rossi', 'Rue Matile 3', '2000', 'Neuchâtel', '079 000 00 20', 'enzo.rossi@example.ch', FALSE, 'demo', CURRENT_DATE);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_amount, hono_invo_id, hono_mute_user, hono_mute_date)
  VALUES (DATEADD(DAY, -41, CURRENT_DATE), (SELECT fold_id FROM invo_folders WHERE fold_name = 'Martin & Rossi'), 'Séance de médiation', 112, 200, 373.33, NULL, 'demo', CURRENT_DATE);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_amount, hono_invo_id, hono_mute_user, hono_mute_date)
  VALUES (DATEADD(DAY, -25, CURRENT_DATE), (SELECT fold_id FROM invo_folders WHERE fold_name = 'Martin & Rossi'), 'Séance de médiation', 112, 200, 373.33, NULL, 'demo', CURRENT_DATE);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_amount, hono_invo_id, hono_mute_user, hono_mute_date)
  VALUES (DATEADD(DAY, -11, CURRENT_DATE), (SELECT fold_id FROM invo_folders WHERE fold_name = 'Martin & Rossi'), 'Séance de médiation', 112, 200, 373.33, NULL, 'demo', CURRENT_DATE);
INSERT INTO invo_advances (adva_fold_id, adva_date, adva_amount, adva_cust_id, adva_cash, adva_mute_user, adva_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Martin & Rossi'), DATEADD(DAY, -21, CURRENT_DATE), 500, (SELECT cust_id FROM invo_customers WHERE cust_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Martin & Rossi') AND cust_first_name = 'Julie'), TRUE, 'demo', CURRENT_DATE);

-- Pittet c. Pittet and Hug: invoices paid, by bank transfer and in cash
INSERT INTO invo_folders (fold_date, fold_name, fold_status, fold_first_date, fold_handled_by, fold_type, fold_comment, fold_mute_user, fold_mute_date)
  VALUES (DATEADD(DAY, -110, CURRENT_DATE), 'Pittet c. Pittet', 'En cours', DATEADD(DAY, -82, CURRENT_DATE), 'dmo', 1, NULL, 'demo', CURRENT_DATE);
INSERT INTO invo_customers (cust_folder_id, cust_title, cust_first_name, cust_last_name, cust_address, cust_zip, cust_city, cust_mob_phone, cust_email, cust_is_billing, cust_mute_user, cust_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Pittet c. Pittet'), 'Madame', 'Laure', 'Pittet', 'Rue des Moulins 7', '2000', 'Neuchâtel', '079 000 00 21', 'laure.pittet@example.ch', FALSE, 'demo', CURRENT_DATE);
INSERT INTO invo_invoices (invo_folder_id, invo_date, invo_hono_sum, invo_depo_sum, invo_disc_rate, invo_disc, invo_tax_rate, invo_tax, invo_amount, invo_payed_date, invo_delay, invo_mute_user, invo_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Pittet c. Pittet'), DATEADD(DAY, -54, CURRENT_DATE), 300.00, 0, 0, 0, 0.081, 24.30, 324.30, DATEADD(DAY, -24, CURRENT_DATE), 30, 'demo', CURRENT_DATE);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_amount, hono_invo_id, hono_mute_user, hono_mute_date)
  VALUES (DATEADD(DAY, -82, CURRENT_DATE), (SELECT fold_id FROM invo_folders WHERE fold_name = 'Pittet c. Pittet'), 'Séance de médiation', 90, 200, 300.00, (SELECT invo_id FROM invo_invoices WHERE invo_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Pittet c. Pittet')), 'demo', CURRENT_DATE);
INSERT INTO invo_payments (pay_invo_id, pay_date, pay_amount, pay_cust_id, pay_cash, pay_iban, pay_mute_user, pay_mute_date)
  VALUES ((SELECT invo_id FROM invo_invoices WHERE invo_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Pittet c. Pittet')), DATEADD(DAY, -24, CURRENT_DATE), 324.30, (SELECT cust_id FROM invo_customers WHERE cust_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Pittet c. Pittet') AND cust_first_name = 'Laure'), FALSE, 'CH5604835012345678009', 'demo', CURRENT_DATE);
INSERT INTO invo_folders (fold_date, fold_name, fold_status, fold_first_date, fold_handled_by, fold_type, fold_comment, fold_mute_user, fold_mute_date)
  VALUES (DATEADD(DAY, -45, CURRENT_DATE), 'Hug', 'En cours', DATEADD(DAY, -30, CURRENT_DATE), 'dmo', 1, NULL, 'demo', CURRENT_DATE);
INSERT INTO invo_customers (cust_folder_id, cust_title, cust_first_name, cust_last_name, cust_address, cust_zip, cust_city, cust_mob_phone, cust_email, cust_is_billing, cust_mute_user, cust_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Hug'), 'Monsieur', 'Marc', 'Hug', 'Faubourg du Lac 11', '2000', 'Neuchâtel', '079 000 00 22', 'marc.hug@example.ch', FALSE, 'demo', CURRENT_DATE);
INSERT INTO invo_invoices (invo_folder_id, invo_date, invo_hono_sum, invo_depo_sum, invo_disc_rate, invo_disc, invo_tax_rate, invo_tax, invo_amount, invo_payed_date, invo_delay, invo_mute_user, invo_mute_date)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Hug'), DATEADD(DAY, -20, CURRENT_DATE), 150.00, 0, 0, 0, 0.081, 12.10, 162.10, GREATEST(DATEADD(DAY, -2, CURRENT_DATE), DATEADD(DAY, 1 - DAY(CURRENT_DATE), CURRENT_DATE)), 30, 'demo', CURRENT_DATE);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_amount, hono_invo_id, hono_mute_user, hono_mute_date)
  VALUES (DATEADD(DAY, -30, CURRENT_DATE), (SELECT fold_id FROM invo_folders WHERE fold_name = 'Hug'), 'Séance de médiation', 45, 200, 150.00, (SELECT invo_id FROM invo_invoices WHERE invo_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Hug')), 'demo', CURRENT_DATE);
INSERT INTO invo_payments (pay_invo_id, pay_date, pay_amount, pay_cust_id, pay_cash, pay_mute_user, pay_mute_date)
  VALUES ((SELECT invo_id FROM invo_invoices WHERE invo_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Hug')), GREATEST(DATEADD(DAY, -2, CURRENT_DATE), DATEADD(DAY, 1 - DAY(CURRENT_DATE), CURRENT_DATE)), 162.10, (SELECT cust_id FROM invo_customers WHERE cust_folder_id = (SELECT fold_id FROM invo_folders WHERE fold_name = 'Hug') AND cust_first_name = 'Marc'), TRUE, 'demo', CURRENT_DATE);

UPDATE invo_folders SET fold_number = fold_id WHERE fold_number IS NULL;

-- office books: two standing orders, their payments of this month and the last, other expenses (VAT included)
INSERT INTO book_orders (ord_creditor, ord_title, ord_amount, ord_tax_rate, ord_tax, ord_day, ord_enabled, ord_mute_user, ord_mute_date) VALUES
  ('Gérance du Lac SA', 'Loyer du bureau', 2154, 0, 0, 1, TRUE, 'demo', CURRENT_DATE),
  ('Assurances du Jura', 'Assurance RC professionnelle', 86.40, 0, 0, 5, TRUE, 'demo', CURRENT_DATE);
INSERT INTO book_payments (pay_kind, pay_date, pay_no, pay_creditor, pay_title, pay_amount, pay_tax_rate, pay_tax, pay_payed_date, pay_mute_user, pay_mute_date)
  VALUES (3, DATEADD(DAY, 1 - DAY(CURRENT_DATE), CURRENT_DATE), NULL, 'Gérance du Lac SA', 'Loyer du bureau', 2154.00, 0, 0.00, DATEADD(DAY, 1 - DAY(CURRENT_DATE), CURRENT_DATE), 'demo', CURRENT_DATE);
INSERT INTO book_payments (pay_kind, pay_date, pay_no, pay_creditor, pay_title, pay_amount, pay_tax_rate, pay_tax, pay_payed_date, pay_mute_user, pay_mute_date)
  VALUES (3, LEAST(DATEADD(DAY, 4, DATEADD(DAY, 1 - DAY(CURRENT_DATE), CURRENT_DATE)), CURRENT_DATE), NULL, 'Assurances du Jura', 'Assurance RC professionnelle', 86.40, 0, 0.00, LEAST(DATEADD(DAY, 4, DATEADD(DAY, 1 - DAY(CURRENT_DATE), CURRENT_DATE)), CURRENT_DATE), 'demo', CURRENT_DATE);
INSERT INTO book_payments (pay_kind, pay_date, pay_no, pay_creditor, pay_title, pay_amount, pay_tax_rate, pay_tax, pay_payed_date, pay_mute_user, pay_mute_date)
  VALUES (3, DATEADD(MONTH, -1, DATEADD(DAY, 1 - DAY(CURRENT_DATE), CURRENT_DATE)), NULL, 'Gérance du Lac SA', 'Loyer du bureau', 2154.00, 0, 0.00, DATEADD(MONTH, -1, DATEADD(DAY, 1 - DAY(CURRENT_DATE), CURRENT_DATE)), 'demo', CURRENT_DATE);
INSERT INTO book_payments (pay_kind, pay_date, pay_no, pay_creditor, pay_title, pay_amount, pay_tax_rate, pay_tax, pay_payed_date, pay_mute_user, pay_mute_date)
  VALUES (3, DATEADD(DAY, 4, DATEADD(MONTH, -1, DATEADD(DAY, 1 - DAY(CURRENT_DATE), CURRENT_DATE))), NULL, 'Assurances du Jura', 'Assurance RC professionnelle', 86.40, 0, 0.00, DATEADD(DAY, 4, DATEADD(MONTH, -1, DATEADD(DAY, 1 - DAY(CURRENT_DATE), CURRENT_DATE))), 'demo', CURRENT_DATE);
INSERT INTO book_payments (pay_kind, pay_date, pay_no, pay_creditor, pay_title, pay_amount, pay_tax_rate, pay_tax, pay_payed_date, pay_mute_user, pay_mute_date)
  VALUES (1, DATEADD(DAY, -40, CURRENT_DATE), NULL, 'Swisscom', 'Téléphone et internet', 129.00, 0.081, 9.67, DATEADD(DAY, -40, CURRENT_DATE), 'demo', CURRENT_DATE);
INSERT INTO book_payments (pay_kind, pay_date, pay_no, pay_creditor, pay_title, pay_amount, pay_tax_rate, pay_tax, pay_payed_date, pay_mute_user, pay_mute_date)
  VALUES (1, GREATEST(DATEADD(DAY, -6, CURRENT_DATE), DATEADD(DAY, 1 - DAY(CURRENT_DATE), CURRENT_DATE)), NULL, 'Swisscom', 'Téléphone et internet', 129.00, 0.081, 9.67, GREATEST(DATEADD(DAY, -6, CURRENT_DATE), DATEADD(DAY, 1 - DAY(CURRENT_DATE), CURRENT_DATE)), 'demo', CURRENT_DATE);
INSERT INTO book_payments (pay_kind, pay_date, pay_no, pay_creditor, pay_title, pay_amount, pay_tax_rate, pay_tax, pay_payed_date, pay_mute_user, pay_mute_date)
  VALUES (2, GREATEST(DATEADD(DAY, -3, CURRENT_DATE), DATEADD(DAY, 1 - DAY(CURRENT_DATE), CURRENT_DATE)), '2026-114', 'Papeterie Payot', 'Classeurs et enveloppes', 54.10, 0.081, 4.05, GREATEST(DATEADD(DAY, -3, CURRENT_DATE), DATEADD(DAY, 1 - DAY(CURRENT_DATE), CURRENT_DATE)), 'demo', CURRENT_DATE);
INSERT INTO book_payments (pay_kind, pay_date, pay_no, pay_creditor, pay_title, pay_amount, pay_tax_rate, pay_tax, pay_payed_date, pay_mute_user, pay_mute_date)
  VALUES (1, DATEADD(DAY, -2, CURRENT_DATE), NULL, 'Fédération suisse des médiateurs', 'Cotisation annuelle', 350.00, 0, 0.00, NULL, 'demo', CURRENT_DATE);
