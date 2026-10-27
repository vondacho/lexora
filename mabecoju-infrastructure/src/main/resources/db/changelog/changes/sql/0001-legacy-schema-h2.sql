-- H2 equivalent of 0001-legacy-schema-mysql.sql, for tests and local development.
-- Type mapping: int(11) -> INT, tinyint(1) flags -> BOOLEAN, tinyint(n) -> TINYINT, float -> REAL, text -> VARCHAR.

CREATE TABLE all_grants (
  gran_id INT AUTO_INCREMENT PRIMARY KEY,
  gran_user_name VARCHAR(16),
  gran_module_name VARCHAR(20),
  gran_page_name VARCHAR(50)
);

CREATE TABLE all_users (
  user_name VARCHAR(16) NOT NULL PRIMARY KEY,
  user_pswd VARCHAR(80),
  user_first_name VARCHAR(20),
  user_last_name VARCHAR(30),
  user_shortcut VARCHAR(3)
);

CREATE TABLE book_defaults (
  defa_id INT AUTO_INCREMENT PRIMARY KEY,
  defa_income_tax_rate REAL DEFAULT 0 NOT NULL
);

CREATE TABLE book_orders (
  ord_id INT AUTO_INCREMENT PRIMARY KEY,
  ord_folder_id INT,
  ord_creditor VARCHAR(80),
  ord_title VARCHAR(80),
  ord_amount REAL DEFAULT 0 NOT NULL,
  ord_tax_rate REAL DEFAULT 0 NOT NULL,
  ord_tax REAL DEFAULT 0 NOT NULL,
  ord_day TINYINT DEFAULT 0 NOT NULL,
  ord_enabled BOOLEAN DEFAULT TRUE NOT NULL,
  ord_mute_date DATE,
  ord_mute_user VARCHAR(10)
);

CREATE TABLE book_payments (
  pay_id INT AUTO_INCREMENT PRIMARY KEY,
  pay_folder_id INT,
  pay_kind TINYINT DEFAULT 0 NOT NULL,
  pay_date DATE,
  pay_no VARCHAR(20),
  pay_creditor VARCHAR(80),
  pay_title VARCHAR(80),
  pay_amount REAL DEFAULT 0 NOT NULL,
  pay_tax_rate REAL DEFAULT 0 NOT NULL,
  pay_tax REAL DEFAULT 0 NOT NULL,
  pay_ticket VARCHAR(80),
  pay_payed_date DATE,
  pay_mute_date DATE,
  pay_mute_user VARCHAR(10)
);

CREATE TABLE invo_advances (
  adva_id INT AUTO_INCREMENT PRIMARY KEY,
  adva_fold_id INT,
  adva_req_id INT,
  adva_date DATE,
  adva_tick_id INT,
  adva_amount REAL DEFAULT 0 NOT NULL,
  adva_invo_id INT,
  adva_mute_date DATE,
  adva_mute_user VARCHAR(10),
  adva_cust_id INT DEFAULT 0,
  adva_cash BOOLEAN DEFAULT FALSE,
  adva_iban VARCHAR(25)
);

CREATE TABLE invo_customers (
  cust_id INT AUTO_INCREMENT PRIMARY KEY,
  cust_folder_id INT DEFAULT 0 NOT NULL,
  cust_title VARCHAR(20),
  cust_first_name VARCHAR(50),
  cust_last_name VARCHAR(50),
  cust_address VARCHAR(128),
  cust_city VARCHAR(30),
  cust_zip VARCHAR(10),
  cust_priv_phone VARCHAR(20),
  cust_prof_phone VARCHAR(20),
  cust_mob_phone VARCHAR(20),
  cust_mob_phone2 VARCHAR(20),
  cust_email VARCHAR(50),
  cust_email2 VARCHAR(50),
  cust_email3 VARCHAR(50),
  cust_comment VARCHAR(65535),
  cust_is_billing BOOLEAN DEFAULT FALSE NOT NULL,
  cust_ibs DATE,
  cust_abs DATE,
  cust_mute_date DATE,
  cust_mute_user VARCHAR(10)
);

CREATE TABLE invo_defaults (
  defa_id INT AUTO_INCREMENT PRIMARY KEY,
  defa_tariff_meeting REAL DEFAULT 0 NOT NULL,
  defa_tariff_other REAL DEFAULT 0 NOT NULL,
  defa_invo_delay TINYINT DEFAULT 0 NOT NULL,
  defa_req_delay TINYINT DEFAULT 0 NOT NULL,
  defa_tva_no VARCHAR(10),
  defa_tva_rate REAL DEFAULT 0 NOT NULL
);

CREATE TABLE invo_folders (
  fold_id INT AUTO_INCREMENT PRIMARY KEY,
  fold_date DATE,
  fold_name VARCHAR(50),
  fold_path VARCHAR(128),
  fold_status VARCHAR(80),
  fold_number INT,
  fold_first_date DATE,
  fold_handled_by VARCHAR(10),
  fold_comment VARCHAR(65535),
  fold_type TINYINT DEFAULT 0 NOT NULL,
  fold_mute_user VARCHAR(10),
  fold_mute_date DATE,
  fold_wayToOurOffice VARCHAR(65535)
);

CREATE TABLE invo_folders_status (
  fold_stat_id INT AUTO_INCREMENT PRIMARY KEY,
  fold_stat_name VARCHAR(80)
);

CREATE TABLE invo_honoraries (
  hono_id INT AUTO_INCREMENT PRIMARY KEY,
  hono_date DATE,
  hono_folder_id INT DEFAULT 0 NOT NULL,
  hono_desc VARCHAR(1024),
  hono_minutes INT DEFAULT 0,
  hono_tariff REAL DEFAULT 0,
  hono_expense_1 REAL DEFAULT 0,
  hono_expense_2 REAL DEFAULT 0,
  hono_expense_3 REAL DEFAULT 0,
  hono_expense_4 REAL DEFAULT 0,
  hono_expense_desc_1 VARCHAR(50),
  hono_expense_desc_2 VARCHAR(50),
  hono_expense_desc_3 VARCHAR(50),
  hono_expense_desc_4 VARCHAR(50),
  hono_amount REAL DEFAULT 0 NOT NULL,
  hono_invo_id INT,
  hono_mute_date DATE,
  hono_mute_user VARCHAR(10)
);

CREATE TABLE invo_invoices (
  invo_id INT AUTO_INCREMENT PRIMARY KEY,
  invo_folder_id INT DEFAULT 0 NOT NULL,
  invo_date DATE,
  invo_hono_sum REAL DEFAULT 0 NOT NULL,
  invo_depo_sum REAL,
  invo_disc_rate REAL DEFAULT 0 NOT NULL,
  invo_disc REAL DEFAULT 0 NOT NULL,
  invo_tax_rate REAL DEFAULT 0 NOT NULL,
  invo_tax REAL DEFAULT 0 NOT NULL,
  invo_amount REAL DEFAULT 0 NOT NULL,
  invo_payed_date DATE,
  invo_delay TINYINT DEFAULT 0 NOT NULL,
  invo_comment VARCHAR(65535),
  invo_mute_date DATE,
  invo_mute_user VARCHAR(10)
);

CREATE TABLE invo_invoices_distrib (
  invo_dist_id INT AUTO_INCREMENT PRIMARY KEY,
  invo_dist_invo_id INT DEFAULT 0 NOT NULL,
  invo_dist_cust_id INT DEFAULT 0 NOT NULL,
  invo_dist_amount REAL DEFAULT 0 NOT NULL
);

CREATE TABLE invo_invoices_recalls (
  reca_id INT AUTO_INCREMENT PRIMARY KEY,
  reca_invo_id INT DEFAULT 0 NOT NULL,
  reca_date DATE,
  reca_degree SMALLINT DEFAULT 0 NOT NULL,
  reca_delay DATE,
  reca_amount REAL DEFAULT 0 NOT NULL,
  reca_mute_user VARCHAR(10),
  reca_mute_date DATE
);

CREATE TABLE invo_invoices_tickets (
  invo_tick_id INT AUTO_INCREMENT PRIMARY KEY,
  invo_tick_invo_id INT DEFAULT 0 NOT NULL,
  invo_tick_cust_id INT DEFAULT 0 NOT NULL,
  invo_tick_tick_id INT DEFAULT 0 NOT NULL
);

CREATE TABLE invo_models (
  mode_id INT AUTO_INCREMENT PRIMARY KEY,
  mode_name VARCHAR(80),
  mode_title VARCHAR(80),
  mode_content VARCHAR(65535) NOT NULL,
  mode_adr_count TINYINT DEFAULT 0 NOT NULL,
  mode_type TINYINT DEFAULT 0 NOT NULL,
  mode_layout_1 VARCHAR(50),
  mode_layout_2 VARCHAR(50),
  mode_layout_3 VARCHAR(50),
  mode_layout_4 VARCHAR(50),
  mode_mute_date DATE,
  mode_mute_user VARCHAR(10),
  CONSTRAINT repo_name UNIQUE (mode_name)
);

CREATE TABLE invo_payments (
  pay_id INT AUTO_INCREMENT PRIMARY KEY,
  pay_invo_id INT DEFAULT 0 NOT NULL,
  pay_date DATE,
  pay_tick_id INT,
  pay_amount REAL DEFAULT 0 NOT NULL,
  pay_mute_date DATE,
  pay_mute_user VARCHAR(10),
  pay_iban VARCHAR(25),
  pay_cash BOOLEAN DEFAULT FALSE,
  pay_cust_id INT DEFAULT 0
);

CREATE TABLE invo_recalls (
  reca_id INT AUTO_INCREMENT PRIMARY KEY,
  reca_invo_id INT DEFAULT 0 NOT NULL,
  reca_date DATE,
  reca_degree SMALLINT DEFAULT 0 NOT NULL,
  reca_delay DATE,
  reca_amount REAL DEFAULT 0 NOT NULL,
  reca_mute_user VARCHAR(10),
  reca_mute_date DATE
);

CREATE TABLE invo_requests (
  req_id INT AUTO_INCREMENT PRIMARY KEY,
  req_folder_id INT DEFAULT 0 NOT NULL,
  req_date DATE,
  req_amount REAL DEFAULT 0 NOT NULL,
  req_payed_date DATE,
  req_model_id SMALLINT,
  req_meeting_date DATE,
  req_delay TINYINT DEFAULT 0 NOT NULL,
  req_hono_sum REAL DEFAULT 0 NOT NULL,
  req_depo_sum REAL,
  req_comment VARCHAR(65535),
  req_mute_date DATE,
  req_mute_user VARCHAR(10)
);

CREATE TABLE invo_requests_distrib (
  req_dist_id INT AUTO_INCREMENT PRIMARY KEY,
  req_dist_req_id INT DEFAULT 0 NOT NULL,
  req_dist_cust_id INT DEFAULT 0 NOT NULL,
  req_dist_amount REAL DEFAULT 0 NOT NULL
);

CREATE TABLE invo_requests_recalls (
  reca_id INT AUTO_INCREMENT PRIMARY KEY,
  reca_req_id INT DEFAULT 0 NOT NULL,
  reca_date DATE,
  reca_degree SMALLINT DEFAULT 0 NOT NULL,
  reca_delay DATE,
  reca_amount REAL DEFAULT 0 NOT NULL,
  reca_mute_user VARCHAR(10),
  reca_mute_date DATE
);

CREATE TABLE invo_requests_tickets (
  req_tick_id INT AUTO_INCREMENT PRIMARY KEY,
  req_tick_req_id INT DEFAULT 0 NOT NULL,
  req_tick_cust_id INT DEFAULT 0 NOT NULL,
  req_tick_tick_id INT DEFAULT 0 NOT NULL
);

CREATE TABLE invo_tickets (
  tick_id INT AUTO_INCREMENT PRIMARY KEY,
  tick_number VARCHAR(128) DEFAULT '0' NOT NULL,
  tick_folder_id INT,
  tick_customer_id INT,
  tick_mute_date DATE,
  tick_mute_user VARCHAR(10)
);

CREATE TABLE invo_wordings (
  word_id INT AUTO_INCREMENT PRIMARY KEY,
  word_wording VARCHAR(80)
);
