package io.lexora.infrastructure.persistence;

import liquibase.command.CommandScope;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * On the production database the legacy tables already exist: the first migration must leave them untouched, only
 * record the baseline as applied, then copy the legacy slip-to-document links into the tickets.
 */
class LiquibaseBaselineTest {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.yaml";

    @Test
    void adopts_an_existing_legacy_database_without_changing_it() throws Exception {
        String url = "jdbc:h2:mem:legacy;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement sql = connection.createStatement()) {
            // the database as the PHP application left it, with a row to keep
            sql.execute("RUNSCRIPT FROM 'classpath:db/changelog/changes/sql/0001-legacy-schema-h2.sql'");
            sql.execute("ALTER TABLE invo_models ADD COLUMN mode_single_page_1 BOOLEAN DEFAULT FALSE");
            sql.execute("ALTER TABLE invo_models ADD COLUMN mode_single_page_2 BOOLEAN DEFAULT FALSE");
            sql.execute("ALTER TABLE invo_models ADD COLUMN mode_single_page_3 BOOLEAN DEFAULT FALSE");
            sql.execute("ALTER TABLE invo_models ADD COLUMN mode_single_page_4 BOOLEAN DEFAULT FALSE");
            sql.execute("INSERT INTO invo_folders (fold_id, fold_name, fold_type) VALUES (1, 'Existing', 1)");
            sql.execute("INSERT INTO all_grants (gran_user_name, gran_module_name, gran_page_name) VALUES "
                    + "('olivia', 'booking', 'tax_summary'), ('olivia', 'booking', 'overview'), ('max', 'booking', 'overview')");
            // legacy links: slip 10 issued for invoice 5, slip 11 for request 7, slip 12 for a deleted invoice
            sql.execute("INSERT INTO invo_invoices (invo_id, invo_folder_id) VALUES (5, 1)");
            sql.execute("INSERT INTO invo_requests (req_id, req_folder_id) VALUES (7, 1)");
            sql.execute("INSERT INTO invo_tickets (tick_id, tick_number, tick_folder_id, tick_customer_id) VALUES "
                    + "(10, 'A', 1, 3), (11, 'B', 1, 3), (12, 'C', 1, 3)");
            sql.execute("INSERT INTO invo_invoices_tickets (invo_tick_invo_id, invo_tick_cust_id, invo_tick_tick_id) "
                    + "VALUES (5, 3, 10), (99, 3, 12)");
            sql.execute("INSERT INTO invo_requests_tickets (req_tick_req_id, req_tick_cust_id, req_tick_tick_id) "
                    + "VALUES (7, 3, 11)");

            update(connection);
            update(connection); // idempotent

            Map<String, String> applied = new LinkedHashMap<>();
            try (ResultSet rows = sql.executeQuery("SELECT id, exectype FROM databasechangelog ORDER BY orderexecuted")) {
                while (rows.next()) {
                    applied.put(rows.getString(1), rows.getString(2));
                }
            }
            assertThat(applied).isEqualTo(Map.of(
                    "0001-legacy-schema-h2", "MARK_RAN",
                    "0002-invo-models-single-page", "MARK_RAN",
                    "0003-tickets-issued-for-documents", "EXECUTED",
                    "0004-migrate-legacy-ticket-links", "EXECUTED",
                    "0005-vat-payments", "EXECUTED",
                    "0005-vat-payment-grant", "EXECUTED",
                    "0006-vat-payment-expense", "EXECUTED",
                    "0007-bank-entries", "EXECUTED"));
            // whoever could see the VAT report may now record VAT payments, once
            try (ResultSet rows = sql.executeQuery("SELECT gran_user_name FROM all_grants WHERE gran_page_name = 'vat_payment'")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("olivia");
                assertThat(rows.next()).isFalse();
            }
            Map<Integer, String> links = new LinkedHashMap<>();
            try (ResultSet rows = sql.executeQuery("SELECT tick_id, tick_invo_id, tick_req_id FROM invo_tickets ORDER BY tick_id")) {
                while (rows.next()) {
                    links.put(rows.getInt(1), rows.getObject(2) + "/" + rows.getObject(3));
                }
            }
            assertThat(links).containsExactly(Map.entry(10, "5/null"), Map.entry(11, "null/7"), Map.entry(12, "null/null"));
            try (ResultSet rows = sql.executeQuery("SELECT fold_name FROM invo_folders")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("Existing");
            }
        }
    }

    private static void update(Connection connection) throws Exception {
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        new CommandScope(UpdateCommandStep.COMMAND_NAME)
                .addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database)
                .addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, CHANGELOG)
                .execute();
    }
}
