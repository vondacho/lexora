-- Legacy schema of the mabecoju database as of the July 2023 dump (backup/backup_23072023.sql of mabecoju-php),
-- shared with the PHP application. Only applied to an empty database: see 0001-legacy-schema.yaml.

CREATE TABLE `all_grants` (
  `gran_id` int(11) NOT NULL AUTO_INCREMENT,
  `gran_user_name` varchar(16) DEFAULT NULL,
  `gran_module_name` varchar(20) DEFAULT NULL,
  `gran_page_name` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`gran_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `all_users` (
  `user_name` varchar(16) NOT NULL,
  `user_pswd` varchar(80) DEFAULT NULL,
  `user_first_name` varchar(20) DEFAULT NULL,
  `user_last_name` varchar(30) DEFAULT NULL,
  `user_shortcut` varchar(3) DEFAULT NULL,
  PRIMARY KEY (`user_name`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `book_defaults` (
  `defa_id` int(11) NOT NULL AUTO_INCREMENT,
  `defa_income_tax_rate` float NOT NULL DEFAULT '0',
  PRIMARY KEY (`defa_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `book_orders` (
  `ord_id` int(11) NOT NULL AUTO_INCREMENT,
  `ord_folder_id` int(11) DEFAULT NULL,
  `ord_creditor` varchar(80) DEFAULT NULL,
  `ord_title` varchar(80) DEFAULT NULL,
  `ord_amount` float NOT NULL DEFAULT '0',
  `ord_tax_rate` float NOT NULL DEFAULT '0',
  `ord_tax` float NOT NULL DEFAULT '0',
  `ord_day` tinyint(2) NOT NULL DEFAULT '0',
  `ord_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `ord_mute_date` date DEFAULT NULL,
  `ord_mute_user` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`ord_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `book_payments` (
  `pay_id` int(11) NOT NULL AUTO_INCREMENT,
  `pay_folder_id` int(11) DEFAULT NULL,
  `pay_kind` tinyint(1) NOT NULL DEFAULT '0',
  `pay_date` date DEFAULT NULL,
  `pay_no` varchar(20) DEFAULT NULL,
  `pay_creditor` varchar(80) DEFAULT NULL,
  `pay_title` varchar(80) DEFAULT NULL,
  `pay_amount` float NOT NULL DEFAULT '0',
  `pay_tax_rate` float NOT NULL DEFAULT '0',
  `pay_tax` float NOT NULL DEFAULT '0',
  `pay_ticket` varchar(80) DEFAULT NULL,
  `pay_payed_date` date DEFAULT NULL,
  `pay_mute_date` date DEFAULT NULL,
  `pay_mute_user` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`pay_id`),
  KEY `pay_date` (`pay_date`),
  KEY `pay_folder_id` (`pay_folder_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_advances` (
  `adva_id` int(11) NOT NULL AUTO_INCREMENT,
  `adva_fold_id` int(11) DEFAULT NULL,
  `adva_req_id` int(11) DEFAULT NULL,
  `adva_date` date DEFAULT NULL,
  `adva_tick_id` int(11) DEFAULT NULL,
  `adva_amount` float(8,2) NOT NULL DEFAULT '0.00',
  `adva_invo_id` int(11) DEFAULT NULL,
  `adva_mute_date` date DEFAULT NULL,
  `adva_mute_user` varchar(10) DEFAULT NULL,
  `adva_cust_id` int(11) DEFAULT '0',
  `adva_cash` tinyint(1) DEFAULT '0',
  `adva_iban` varchar(25) DEFAULT NULL,
  PRIMARY KEY (`adva_id`),
  KEY `adva_tick_id` (`adva_tick_id`),
  KEY `adva_fold_id` (`adva_fold_id`),
  KEY `adva_req_id` (`adva_req_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_customers` (
  `cust_id` int(11) NOT NULL AUTO_INCREMENT,
  `cust_folder_id` int(11) NOT NULL DEFAULT '0',
  `cust_title` varchar(20) DEFAULT NULL,
  `cust_first_name` varchar(50) DEFAULT NULL,
  `cust_last_name` varchar(50) DEFAULT NULL,
  `cust_address` varchar(128) DEFAULT NULL,
  `cust_city` varchar(30) DEFAULT NULL,
  `cust_zip` varchar(10) DEFAULT NULL,
  `cust_priv_phone` varchar(20) DEFAULT NULL,
  `cust_prof_phone` varchar(20) DEFAULT NULL,
  `cust_mob_phone` varchar(20) DEFAULT NULL,
  `cust_mob_phone2` varchar(20) DEFAULT NULL,
  `cust_email` varchar(50) DEFAULT NULL,
  `cust_email2` varchar(50) DEFAULT NULL,
  `cust_email3` varchar(50) DEFAULT NULL,
  `cust_comment` text,
  `cust_is_billing` tinyint(1) NOT NULL DEFAULT '0',
  `cust_ibs` date DEFAULT NULL,
  `cust_abs` date DEFAULT NULL,
  `cust_mute_date` date DEFAULT NULL,
  `cust_mute_user` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`cust_id`),
  KEY `cust_last_name` (`cust_last_name`),
  KEY `cust_folder_id` (`cust_folder_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_defaults` (
  `defa_id` int(11) NOT NULL AUTO_INCREMENT,
  `defa_tariff_meeting` float NOT NULL DEFAULT '0',
  `defa_tariff_other` float NOT NULL DEFAULT '0',
  `defa_invo_delay` tinyint(2) NOT NULL DEFAULT '0',
  `defa_req_delay` tinyint(2) NOT NULL DEFAULT '0',
  `defa_tva_no` varchar(10) DEFAULT NULL,
  `defa_tva_rate` float NOT NULL DEFAULT '0',
  PRIMARY KEY (`defa_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_folders` (
  `fold_id` int(11) NOT NULL AUTO_INCREMENT,
  `fold_date` date DEFAULT NULL,
  `fold_name` varchar(50) DEFAULT NULL,
  `fold_path` varchar(128) DEFAULT NULL,
  `fold_status` varchar(80) DEFAULT NULL,
  `fold_number` int(11) DEFAULT NULL,
  `fold_first_date` date DEFAULT NULL,
  `fold_handled_by` varchar(10) DEFAULT NULL,
  `fold_comment` text,
  `fold_type` tinyint(1) NOT NULL DEFAULT '0',
  `fold_mute_user` varchar(10) DEFAULT NULL,
  `fold_mute_date` date DEFAULT NULL,
  `fold_wayToOurOffice` text,
  PRIMARY KEY (`fold_id`),
  KEY `fold_name` (`fold_name`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_folders_status` (
  `fold_stat_id` int(11) NOT NULL AUTO_INCREMENT,
  `fold_stat_name` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`fold_stat_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_honoraries` (
  `hono_id` int(11) NOT NULL AUTO_INCREMENT,
  `hono_date` date DEFAULT NULL,
  `hono_folder_id` int(11) NOT NULL DEFAULT '0',
  `hono_desc` varchar(255) DEFAULT NULL,
  `hono_minutes` int(11) DEFAULT '0',
  `hono_tariff` float(5,2) DEFAULT '0.00',
  `hono_expense_1` float(8,2) DEFAULT '0.00',
  `hono_expense_2` float(8,2) DEFAULT '0.00',
  `hono_expense_3` float(8,2) DEFAULT '0.00',
  `hono_expense_4` float(8,2) DEFAULT '0.00',
  `hono_expense_desc_1` varchar(50) DEFAULT NULL,
  `hono_expense_desc_2` varchar(50) DEFAULT NULL,
  `hono_expense_desc_3` varchar(50) DEFAULT NULL,
  `hono_expense_desc_4` varchar(50) DEFAULT NULL,
  `hono_amount` float(8,2) NOT NULL DEFAULT '0.00',
  `hono_invo_id` int(11) DEFAULT NULL,
  `hono_mute_date` date DEFAULT NULL,
  `hono_mute_user` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`hono_id`),
  KEY `hono_folder_id` (`hono_folder_id`),
  KEY `hono_invo_id` (`hono_invo_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_invoices` (
  `invo_id` int(11) NOT NULL AUTO_INCREMENT,
  `invo_folder_id` int(11) NOT NULL DEFAULT '0',
  `invo_date` date DEFAULT NULL,
  `invo_hono_sum` float(8,2) NOT NULL DEFAULT '0.00',
  `invo_depo_sum` float(8,2) DEFAULT NULL,
  `invo_disc_rate` float NOT NULL DEFAULT '0',
  `invo_disc` float NOT NULL DEFAULT '0',
  `invo_tax_rate` float NOT NULL DEFAULT '0',
  `invo_tax` float NOT NULL DEFAULT '0',
  `invo_amount` float NOT NULL DEFAULT '0',
  `invo_payed_date` date DEFAULT NULL,
  `invo_delay` tinyint(2) NOT NULL DEFAULT '0',
  `invo_comment` text,
  `invo_mute_date` date DEFAULT NULL,
  `invo_mute_user` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`invo_id`),
  KEY `invo_folder_id_idx` (`invo_folder_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_invoices_distrib` (
  `invo_dist_id` int(11) NOT NULL AUTO_INCREMENT,
  `invo_dist_invo_id` int(11) NOT NULL DEFAULT '0',
  `invo_dist_cust_id` int(11) NOT NULL DEFAULT '0',
  `invo_dist_amount` float NOT NULL DEFAULT '0',
  PRIMARY KEY (`invo_dist_id`),
  KEY `invo_dist_invo_id` (`invo_dist_invo_id`),
  KEY `invo_dist_cust_id` (`invo_dist_cust_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_invoices_recalls` (
  `reca_id` int(11) NOT NULL AUTO_INCREMENT,
  `reca_invo_id` int(11) NOT NULL DEFAULT '0',
  `reca_date` date DEFAULT NULL,
  `reca_degree` smallint(1) NOT NULL DEFAULT '0',
  `reca_delay` date DEFAULT NULL,
  `reca_amount` float NOT NULL DEFAULT '0',
  `reca_mute_user` varchar(10) DEFAULT NULL,
  `reca_mute_date` date DEFAULT NULL,
  PRIMARY KEY (`reca_id`),
  KEY `reca_invo_id` (`reca_invo_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_invoices_tickets` (
  `invo_tick_id` int(11) NOT NULL AUTO_INCREMENT,
  `invo_tick_invo_id` int(11) NOT NULL DEFAULT '0',
  `invo_tick_cust_id` int(11) NOT NULL DEFAULT '0',
  `invo_tick_tick_id` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`invo_tick_id`),
  KEY `invo_tick_invo_id` (`invo_tick_invo_id`),
  KEY `invo_tick_tick_id` (`invo_tick_tick_id`),
  KEY `invo_tick_cust_id` (`invo_tick_cust_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_models` (
  `mode_id` int(11) NOT NULL AUTO_INCREMENT,
  `mode_name` varchar(80) DEFAULT NULL,
  `mode_title` varchar(80) DEFAULT NULL,
  `mode_content` text NOT NULL,
  `mode_adr_count` tinyint(4) NOT NULL DEFAULT '0',
  `mode_type` tinyint(4) NOT NULL DEFAULT '0',
  `mode_layout_1` varchar(50) DEFAULT NULL,
  `mode_layout_2` varchar(50) DEFAULT NULL,
  `mode_layout_3` varchar(50) DEFAULT NULL,
  `mode_layout_4` varchar(50) DEFAULT NULL,
  `mode_mute_date` date DEFAULT NULL,
  `mode_mute_user` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`mode_id`),
  UNIQUE KEY `repo_name` (`mode_name`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_payments` (
  `pay_id` int(11) NOT NULL AUTO_INCREMENT,
  `pay_invo_id` int(11) NOT NULL DEFAULT '0',
  `pay_date` date DEFAULT NULL,
  `pay_tick_id` int(11) DEFAULT NULL,
  `pay_amount` float NOT NULL DEFAULT '0',
  `pay_mute_date` date DEFAULT NULL,
  `pay_mute_user` varchar(10) DEFAULT NULL,
  `pay_iban` varchar(25) DEFAULT NULL,
  `pay_cash` tinyint(1) DEFAULT '0',
  `pay_cust_id` int(11) DEFAULT '0',
  PRIMARY KEY (`pay_id`),
  KEY `pay_tick_id` (`pay_tick_id`),
  KEY `pay_invo_id` (`pay_invo_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_recalls` (
  `reca_id` int(11) NOT NULL AUTO_INCREMENT,
  `reca_invo_id` int(11) NOT NULL DEFAULT '0',
  `reca_date` date DEFAULT NULL,
  `reca_degree` smallint(1) NOT NULL DEFAULT '0',
  `reca_delay` date DEFAULT NULL,
  `reca_amount` float NOT NULL DEFAULT '0',
  `reca_mute_user` varchar(10) DEFAULT NULL,
  `reca_mute_date` date DEFAULT NULL,
  PRIMARY KEY (`reca_id`),
  KEY `reca_invo_id` (`reca_invo_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_requests` (
  `req_id` int(11) NOT NULL AUTO_INCREMENT,
  `req_folder_id` int(11) NOT NULL DEFAULT '0',
  `req_date` date DEFAULT NULL,
  `req_amount` float NOT NULL DEFAULT '0',
  `req_payed_date` date DEFAULT NULL,
  `req_model_id` smallint(1) DEFAULT NULL,
  `req_meeting_date` date DEFAULT NULL,
  `req_delay` tinyint(2) NOT NULL DEFAULT '0',
  `req_hono_sum` float(8,2) NOT NULL DEFAULT '0.00',
  `req_depo_sum` float(8,2) DEFAULT NULL,
  `req_comment` text,
  `req_mute_date` date DEFAULT NULL,
  `req_mute_user` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`req_id`),
  KEY `req_folder_id` (`req_folder_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_requests_distrib` (
  `req_dist_id` int(11) NOT NULL AUTO_INCREMENT,
  `req_dist_req_id` int(11) NOT NULL DEFAULT '0',
  `req_dist_cust_id` int(11) NOT NULL DEFAULT '0',
  `req_dist_amount` float NOT NULL DEFAULT '0',
  PRIMARY KEY (`req_dist_id`),
  KEY `req_dist_req_id` (`req_dist_req_id`),
  KEY `req_dist_cust_id` (`req_dist_cust_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_requests_recalls` (
  `reca_id` int(11) NOT NULL AUTO_INCREMENT,
  `reca_req_id` int(11) NOT NULL DEFAULT '0',
  `reca_date` date DEFAULT NULL,
  `reca_degree` smallint(1) NOT NULL DEFAULT '0',
  `reca_delay` date DEFAULT NULL,
  `reca_amount` float NOT NULL DEFAULT '0',
  `reca_mute_user` varchar(10) DEFAULT NULL,
  `reca_mute_date` date DEFAULT NULL,
  PRIMARY KEY (`reca_id`),
  KEY `reca_req_id` (`reca_req_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_requests_tickets` (
  `req_tick_id` int(11) NOT NULL AUTO_INCREMENT,
  `req_tick_req_id` int(11) NOT NULL DEFAULT '0',
  `req_tick_cust_id` int(11) NOT NULL DEFAULT '0',
  `req_tick_tick_id` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`req_tick_id`),
  KEY `req_tick_req_id` (`req_tick_req_id`),
  KEY `req_tick_tick_id` (`req_tick_tick_id`),
  KEY `req_tick_cust_id` (`req_tick_cust_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_tickets` (
  `tick_id` int(11) NOT NULL AUTO_INCREMENT,
  `tick_number` varchar(128) NOT NULL DEFAULT '0',
  `tick_folder_id` int(11) DEFAULT NULL,
  `tick_customer_id` int(11) DEFAULT NULL,
  `tick_mute_date` date DEFAULT NULL,
  `tick_mute_user` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`tick_id`),
  KEY `tick_folder_id` (`tick_folder_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `invo_wordings` (
  `word_id` int(11) NOT NULL AUTO_INCREMENT,
  `word_wording` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`word_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
