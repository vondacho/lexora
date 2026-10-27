-- Demo data of the local H2 database (profile "h2"). Identifiers are generated: no explicit ids.

-- user demo / demo, with every grant
INSERT INTO all_users (user_name, user_pswd, user_first_name, user_last_name, user_shortcut)
  VALUES ('demo', 'fe01ce2a7fbac8fafaed7c982a04e229', 'Demo', 'User', 'dmo');
INSERT INTO all_grants (gran_user_name, gran_module_name, gran_page_name) VALUES
  ('demo', 'admin', 'users'),
  ('demo', 'booking', 'payments'),
  ('demo', 'booking', 'payments_list'),
  ('demo', 'booking', 'payment_display'),
  ('demo', 'booking', 'payment_create'),
  ('demo', 'booking', 'payment_modify'),
  ('demo', 'booking', 'orders'),
  ('demo', 'booking', 'orders_list'),
  ('demo', 'booking', 'order_create'),
  ('demo', 'booking', 'order_modify'),
  ('demo', 'booking', 'tax_summary'),
  ('demo', 'booking', 'overview'),
  ('demo', 'booking', 'statistics_search'),
  ('demo', 'booking', 'parameters_modify'),
  ('demo', 'invoicing', 'folder_search'),
  ('demo', 'invoicing', 'folder_create'),
  ('demo', 'invoicing', 'folder_display'),
  ('demo', 'invoicing', 'folder_modify'),
  ('demo', 'invoicing', 'folder_status_modify'),
  ('demo', 'invoicing', 'folder_content'),
  ('demo', 'invoicing', 'folder_requests'),
  ('demo', 'invoicing', 'folder_invoices'),
  ('demo', 'invoicing', 'folder_honoraries'),
  ('demo', 'invoicing', 'customer_create'),
  ('demo', 'invoicing', 'customer_modify'),
  ('demo', 'invoicing', 'customer_display'),
  ('demo', 'invoicing', 'customer_folder'),
  ('demo', 'invoicing', 'customer_honoraries'),
  ('demo', 'invoicing', 'customer_invoices'),
  ('demo', 'invoicing', 'customer_advances'),
  ('demo', 'invoicing', 'customer_requests'),
  ('demo', 'invoicing', 'honorary_create'),
  ('demo', 'invoicing', 'honorary_modify'),
  ('demo', 'invoicing', 'invoice_create'),
  ('demo', 'invoicing', 'invoice_modify'),
  ('demo', 'invoicing', 'invoice_display'),
  ('demo', 'invoicing', 'request_create'),
  ('demo', 'invoicing', 'request_modify'),
  ('demo', 'invoicing', 'request_display'),
  ('demo', 'invoicing', 'debtor_search'),
  ('demo', 'invoicing', 'debtor_receipt'),
  ('demo', 'invoicing', 'advance_receipt'),
  ('demo', 'invoicing', 'debtors_payed'),
  ('demo', 'invoicing', 'debtors_open'),
  ('demo', 'invoicing', 'parameters_modify'),
  ('demo', 'invoicing', 'wordings_modify'),
  ('demo', 'invoicing', 'models');

-- defaults
INSERT INTO invo_defaults (defa_tariff_meeting, defa_tariff_other, defa_invo_delay, defa_req_delay, defa_tva_no, defa_tva_rate)
  VALUES (200, 150, 30, 10, '000 000', 0.081);
INSERT INTO book_defaults (defa_income_tax_rate) VALUES (0.059);

INSERT INTO invo_folders_status (fold_stat_name) VALUES ('En cours'), ('récupéré');
INSERT INTO invo_wordings (word_wording) VALUES ('Séance de médiation'), ('Entretien téléphonique'), ('Correspondance');

-- a folder with two customers and open honoraries
INSERT INTO invo_folders (fold_date, fold_name, fold_status, fold_first_date, fold_handled_by, fold_type, fold_comment)
  VALUES (DATE '2026-09-01', 'Dupont c. Dupont', 'En cours', DATE '2026-09-08', 'dmo', 1, 'Dossier de démonstration');
UPDATE invo_folders SET fold_number = fold_id WHERE fold_name = 'Dupont c. Dupont';
INSERT INTO invo_customers (cust_folder_id, cust_title, cust_first_name, cust_last_name, cust_address, cust_zip, cust_city, cust_mob_phone, cust_email, cust_is_billing)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Dupont c. Dupont'), 'Madame', 'Anne', 'Dupont', 'Rue des Alpes 12', '2000', 'Neuchâtel', '079 000 00 01', 'anne.dupont@example.ch', FALSE);
INSERT INTO invo_customers (cust_folder_id, cust_title, cust_first_name, cust_last_name, cust_address, cust_zip, cust_city, cust_mob_phone, cust_email, cust_is_billing)
  VALUES ((SELECT fold_id FROM invo_folders WHERE fold_name = 'Dupont c. Dupont'), 'Monsieur', 'Paul', 'Dupont', 'Chemin du Lac 3', '2001', 'Neuchâtel', '079 000 00 02', 'paul.dupont@example.ch', FALSE);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_expense_1, hono_expense_desc_1, hono_amount)
  VALUES (DATE '2026-09-08', (SELECT fold_id FROM invo_folders WHERE fold_name = 'Dupont c. Dupont'), 'Séance de médiation', 90, 200, 0, NULL, 300);
INSERT INTO invo_honoraries (hono_date, hono_folder_id, hono_desc, hono_minutes, hono_tariff, hono_expense_1, hono_expense_desc_1, hono_amount)
  VALUES (DATE '2026-09-22', (SELECT fold_id FROM invo_folders WHERE fold_name = 'Dupont c. Dupont'), 'Séance de médiation', 120, 200, 12.5, 'Frais de copies', 412.5);

-- letter templates (placeholders as in the legacy templates)
INSERT INTO invo_models (mode_name, mode_title, mode_content, mode_adr_count, mode_type, mode_layout_1, mode_layout_2)
  VALUES ('Mémoire d''honoraires - partage', 'Mémoire d''honoraires', '$title_1, $title_2,

Vous trouverez ci-joint le décompte de mes honoraires pour la médiation en cours. Votre part s''élève à frs. $amount_1 pour $firstName_1 et à frs. $amount_2 pour $firstName_2, payable dans les $delay jours.

Je vous prie d''agréer mes meilleures salutations.', 2, 1, 'layout_invoice_double.php', 'layout_invoice_double_detail.php');
INSERT INTO invo_models (mode_name, mode_title, mode_content, mode_adr_count, mode_type, mode_layout_1)
  VALUES ('Provision - Simple', 'Demande de provision', 'Madame, Monsieur,

Par la présente, je me permets de vous demander de bien vouloir me verser à titre de provision un montant de frs. $amount chacun.

En vous remerciant par avance de votre versement, je vous prie d''agréer, Madame, Monsieur, mes meilleures salutations.', 2, 2, 'layout_request.php');
INSERT INTO invo_models (mode_name, mode_title, mode_content, mode_adr_count, mode_type, mode_layout_1)
  VALUES ('Rappel', 'Rappel', '$title_1,

Sauf erreur de notre part, notre facture du $invoiceDate reste impayée. Nous vous saurions gré de bien vouloir nous verser le solde de frs. $amount_1 dans les prochains jours.

Avec nos meilleures salutations.', 1, 3, 'layout_recall.php');
