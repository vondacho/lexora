-- Test fixtures: 'tester' holds every grant, 'reader' none. Both passwords are 'secret' (legacy MD5).
INSERT INTO all_users (user_name, user_pswd, user_first_name, user_last_name, user_shortcut) VALUES
  ('tester', '5ebe2294ecd0e0f08eab7690d2a6ee69', 'Test', 'User', 'tst'),
  ('reader', '5ebe2294ecd0e0f08eab7690d2a6ee69', 'Read', 'Only', 'rdr');

INSERT INTO all_grants (gran_user_name, gran_module_name, gran_page_name) VALUES
  ('tester', 'admin', 'users'),
  ('tester', 'booking', 'payments'),
  ('tester', 'booking', 'payments_list'),
  ('tester', 'booking', 'payment_display'),
  ('tester', 'booking', 'payment_create'),
  ('tester', 'booking', 'payment_modify'),
  ('tester', 'booking', 'orders'),
  ('tester', 'booking', 'orders_list'),
  ('tester', 'booking', 'order_create'),
  ('tester', 'booking', 'order_modify'),
  ('tester', 'booking', 'tax_summary'),
  ('tester', 'booking', 'vat_payment'),
  ('tester', 'booking', 'overview'),
  ('tester', 'booking', 'statistics_search'),
  ('tester', 'booking', 'parameters_modify'),
  ('tester', 'invoicing', 'folder_search'),
  ('tester', 'invoicing', 'folder_create'),
  ('tester', 'invoicing', 'folder_display'),
  ('tester', 'invoicing', 'folder_modify'),
  ('tester', 'invoicing', 'folder_status_modify'),
  ('tester', 'invoicing', 'folder_content'),
  ('tester', 'invoicing', 'folder_requests'),
  ('tester', 'invoicing', 'folder_invoices'),
  ('tester', 'invoicing', 'folder_honoraries'),
  ('tester', 'invoicing', 'customer_create'),
  ('tester', 'invoicing', 'customer_modify'),
  ('tester', 'invoicing', 'customer_display'),
  ('tester', 'invoicing', 'customer_folder'),
  ('tester', 'invoicing', 'customer_honoraries'),
  ('tester', 'invoicing', 'customer_invoices'),
  ('tester', 'invoicing', 'customer_advances'),
  ('tester', 'invoicing', 'customer_requests'),
  ('tester', 'invoicing', 'honorary_create'),
  ('tester', 'invoicing', 'honorary_modify'),
  ('tester', 'invoicing', 'invoice_create'),
  ('tester', 'invoicing', 'invoice_modify'),
  ('tester', 'invoicing', 'invoice_display'),
  ('tester', 'invoicing', 'request_create'),
  ('tester', 'invoicing', 'request_modify'),
  ('tester', 'invoicing', 'request_display'),
  ('tester', 'invoicing', 'debtor_search'),
  ('tester', 'invoicing', 'debtor_receipt'),
  ('tester', 'invoicing', 'advance_receipt'),
  ('tester', 'invoicing', 'debtors_payed'),
  ('tester', 'invoicing', 'debtors_open'),
  ('tester', 'invoicing', 'parameters_modify'),
  ('tester', 'invoicing', 'wordings_modify'),
  ('tester', 'invoicing', 'models');

INSERT INTO invo_defaults (defa_tariff_meeting, defa_tariff_other, defa_invo_delay, defa_req_delay, defa_tva_no, defa_tva_rate)
  VALUES (200, 150, 30, 10, '000 000', 0.077);

INSERT INTO book_defaults (defa_income_tax_rate) VALUES (0.059);
