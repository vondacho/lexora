package io.lexora.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end scenario over the REST API, on an H2 copy of the legacy schema validated against the JPA mappings.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvoicingApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void from_folder_opening_to_paid_invoice() throws Exception {
        // open a folder with its first customer, add a second one
        String opened = call(post("/api/folders"), """
                {"folder": {"type": "INVOICING", "openingDate": "2026-01-10", "name": "Dupont c. Dupont"},
                 "firstCustomer": {"title": "Madame", "firstName": "Anne", "lastName": "Dupont"}}""")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long folderId = id(opened, "$.folder.id");
        long anne = id(opened, "$.firstCustomer.id");
        call(get("/api/folders/" + folderId), null).andExpect(jsonPath("$.number").value(folderId));
        long paul = id(call(post("/api/folders/" + folderId + "/customers"), """
                {"title": "Monsieur", "firstName": "Paul", "lastName": "Dupont"}""")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");

        // 90 minutes at 200.-/h plus 12.50 of expenses
        call(post("/api/folders/" + folderId + "/honoraries"), """
                {"date": "2026-01-20", "description": "Séance", "minutes": 90, "hourlyRate": 200,
                 "expenses": [{"amount": 12.50, "description": "Frais"}]}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(312.50));

        // a spontaneous cash advance
        call(post("/api/folders/" + folderId + "/advances"), """
                {"customerId": %d, "date": "2026-01-25", "amount": 100, "cash": true}""".formatted(anne))
                .andExpect(status().isCreated());

        call(get("/api/folders/" + folderId + "/invoice-draft"), null)
                .andExpect(jsonPath("$.honorariesSum").value(312.50))
                .andExpect(jsonPath("$.advancedSum").value(100.00))
                .andExpect(jsonPath("$.defaultDelayDays").value(30));

        // subtotal 312.50, VAT 7.7 % = 24.0625 truncated to 24.00, minus 100 of advance
        String invoice = call(post("/api/folders/" + folderId + "/invoices"), """
                {"date": "2026-02-01", "delayDays": 30, "taxRatePercent": 7.7,
                 "shares": [{"customerId": %d, "amount": 118.27}, {"customerId": %d, "amount": 118.23}]}"""
                .formatted(anne, paul))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tax").value(24.00))
                .andExpect(jsonPath("$.amount").value(236.50))
                .andExpect(jsonPath("$.shares[0].amount").value(118.25))
                .andExpect(jsonPath("$.dueDate").value("2026-03-03"))
                .andReturn().getResponse().getContentAsString();
        long invoiceId = id(invoice, "$.id");

        call(get("/api/folders/" + folderId + "/honoraries?invoiced=false"), null).andExpect(jsonPath("$", hasSize(0)));
        call(get("/api/folders/" + folderId + "/advances?invoiced=true"), null)
                .andExpect(jsonPath("$[0].invoiceId").value(invoiceId));
        call(get("/api/debtors/overview"), null).andExpect(jsonPath("$.openInvoices[0].documentId").value(invoiceId));

        // Anne pays everything at once: settled within the one franc tolerance
        call(post("/api/folders/" + folderId + "/customers/" + anne + "/receipts"), """
                {"invoices": [{"documentId": %d, "date": "2026-02-15", "amount": 236.00}]}""".formatted(invoiceId))
                .andExpect(status().isOk());
        String paid = call(get("/api/invoices/" + invoiceId), null)
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.paidOn").value("2026-02-15"))
                .andReturn().getResponse().getContentAsString();
        long paymentId = id(paid, "$.payments[0].id");

        // cancelling the payment reopens the invoice; a reminder claims the whole amount
        call(delete("/api/invoices/" + invoiceId + "/payments/" + paymentId), null)
                .andExpect(jsonPath("$.paid").value(false));
        call(post("/api/invoices/" + invoiceId + "/recalls"), """
                {"date": "2026-03-10"}""")
                .andExpect(jsonPath("$.recalls[0].degree").value(1))
                .andExpect(jsonPath("$.recalls[0].amount").value(236.50));

        // deleting the invoice makes its honoraries and advances billable again
        call(delete("/api/invoices/" + invoiceId), null).andExpect(status().isNoContent());
        call(get("/api/folders/" + folderId + "/honoraries?invoiced=false"), null).andExpect(jsonPath("$", hasSize(1)));
        call(get("/api/folders/" + folderId + "/advances?invoiced=false"), null).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void advance_requests_are_settled_by_the_advances_received() throws Exception {
        String opened = call(post("/api/folders"), """
                {"folder": {"type": "INVOICING", "name": "Martin"},
                 "firstCustomer": {"firstName": "Luc", "lastName": "Martin"}}""")
                .andReturn().getResponse().getContentAsString();
        long folderId = id(opened, "$.folder.id");
        long luc = id(opened, "$.firstCustomer.id");

        long requestId = id(call(post("/api/folders/" + folderId + "/advance-requests"), """
                {"date": "2026-04-01", "delayDays": 10, "amount": 1000, "shares": [{"customerId": %d, "amount": 1000}]}"""
                .formatted(luc)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");

        call(post("/api/folders/" + folderId + "/customers/" + luc + "/receipts"), """
                {"requests": [{"documentId": %d, "date": "2026-04-05", "amount": 999.50}]}""".formatted(requestId))
                .andExpect(jsonPath("$.requests", hasSize(0)));
        call(get("/api/advance-requests/" + requestId), null)
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.paidSum").value(999.50))
                .andExpect(jsonPath("$.advances", hasSize(1)));
    }

    @Test
    void standing_orders_are_executed_into_paid_permanent_payments() throws Exception {
        call(post("/api/booking/standing-orders"), """
                {"creditor": "Régie", "title": "Loyer", "amount": 2154, "taxRatePercent": 0, "dayOfMonth": 31, "enabled": true}""")
                .andExpect(status().isCreated());

        call(post("/api/booking/standing-orders/executions"), """
                {"month": "2025-02"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].kind").value("PERMANENT"))
                .andExpect(jsonPath("$[0].paidOn").value("2025-02-28"));
        call(get("/api/booking/standing-orders/next-execution"), null)
                .andExpect(jsonPath("$.month").value("2025-03"));
        call(get("/api/reports/expenses?from=2025-02-01&to=2025-02-28"), null)
                .andExpect(jsonPath("$.total").value(2154.00));
    }

    @Test
    void one_off_expenses_are_editable_but_standing_order_payments_only_deletable() throws Exception {
        long expenseId = id(call(post("/api/booking/payments"), """
                {"kind": "EXPENSE", "date": "2024-03-04", "creditor": "Papeterie", "title": "Classeurs", "amount": 54.10}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.editable").value(true))
                .andReturn().getResponse().getContentAsString(), "$.id");
        call(put("/api/booking/payments/" + expenseId), """
                {"kind": "EXPENSE", "date": "2024-03-04", "creditor": "Papeterie", "title": "Classeurs et enveloppes",
                 "amount": 61.30, "taxRatePercent": 8.1, "paidOn": "2024-03-10"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(61.30))
                .andExpect(jsonPath("$.paidOn").value("2024-03-10"));
        call(post("/api/booking/payments"), """
                {"kind": "PERMANENT", "creditor": "Régie", "amount": 100}""")
                .andExpect(status().isUnprocessableContent());

        long orderId = id(call(post("/api/booking/standing-orders"), """
                {"creditor": "Telco", "title": "Téléphone", "amount": 80, "dayOfMonth": 5, "enabled": true}""")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        long permanentId = id(call(post("/api/booking/standing-orders/executions"), """
                {"month": "2024-03"}""")
                .andExpect(jsonPath("$[0].editable").value(false))
                .andReturn().getResponse().getContentAsString(), "$[0].id");
        call(put("/api/booking/payments/" + permanentId), """
                {"kind": "EXPENSE", "creditor": "Telco", "amount": 90}""")
                .andExpect(status().isUnprocessableContent());
        call(delete("/api/booking/payments/" + permanentId), null).andExpect(status().isNoContent());
        // the test database is shared: do not leave an active order behind
        call(delete("/api/booking/standing-orders/" + orderId), null).andExpect(status().isNoContent());
    }

    @Test
    void vat_payments_keep_the_figures_of_their_period_and_cannot_overlap() throws Exception {
        long folderId = id(call(post("/api/folders"), """
                {"folder": {"type": "INVOICING", "name": "TVA"}}""")
                .andReturn().getResponse().getContentAsString(), "$.folder.id");
        long customerId = id(call(post("/api/folders/" + folderId + "/customers"), """
                {"title": "Madame", "firstName": "Eva", "lastName": "Tva"}""")
                .andReturn().getResponse().getContentAsString(), "$.id");
        call(post("/api/folders/" + folderId + "/advances"), """
                {"customerId": %d, "date": "2023-02-10", "amount": 1000, "cash": true}""".formatted(customerId))
                .andExpect(status().isCreated());
        call(get("/api/settings/booking"), null).andExpect(jsonPath("$.vatRatePercent").value(5.9));
        call(get("/api/reports/income?from=2023-01-01&to=2023-06-30"), null)
                .andExpect(jsonPath("$.total").value(1000.00))
                .andExpect(jsonPath("$.vatDue").value(59.00));

        String recorded = call(post("/api/booking/vat-payments"), """
                {"from": "2023-01-01", "to": "2023-06-30", "paidOn": "2023-08-25"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.revenue").value(1000.00))
                .andExpect(jsonPath("$.vatRatePercent").value(5.9))
                .andExpect(jsonPath("$.vatDue").value(59.00))
                .andExpect(jsonPath("$.amount").value(59.00))
                .andExpect(jsonPath("$.expenseId").isNumber())
                .andReturn().getResponse().getContentAsString();
        long paymentId = id(recorded, "$.id");
        // the money paid is an office expense, which follows the VAT payment
        long expenseId = id(recorded, "$.expenseId");
        call(get("/api/booking/payments/" + expenseId), null)
                .andExpect(jsonPath("$.kind").value("BANK"))
                .andExpect(jsonPath("$.creditor").value("Administration fédérale des contributions"))
                .andExpect(jsonPath("$.title").value("TVA du 01.01.2023 au 30.06.2023"))
                .andExpect(jsonPath("$.amount").value(59.00))
                .andExpect(jsonPath("$.tax").value(0.00))
                .andExpect(jsonPath("$.paidOn").value("2023-08-25"))
                .andExpect(jsonPath("$.editable").value(false))
                .andExpect(jsonPath("$.vatPaymentId").value(paymentId));
        call(get("/api/reports/expenses?from=2023-08-01&to=2023-08-31"), null)
                .andExpect(jsonPath("$.total").value(59.00));
        call(put("/api/booking/payments/" + expenseId), """
                {"kind": "BANK", "creditor": "AFC", "amount": 1}""")
                .andExpect(status().isUnprocessableContent());
        call(delete("/api/booking/payments/" + expenseId), null).andExpect(status().isUnprocessableContent());
        call(post("/api/booking/vat-payments"), """
                {"from": "2023-06-01", "to": "2023-12-31", "paidOn": "2024-02-20", "amount": 10}""")
                .andExpect(status().isUnprocessableContent())
                // a code and its values, for the clients to translate; the detail stays in English
                .andExpect(jsonPath("$.code").value("VAT_PERIOD_OVERLAP"))
                .andExpect(jsonPath("$.params.from").value("2023-01-01"))
                .andExpect(jsonPath("$.params.to").value("2023-06-30"))
                .andExpect(jsonPath("$.params.paidOn").value("2023-08-25"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("periods cannot overlap")));
        call(post("/api/booking/vat-payments"), """
                {"from": "2023-07-01", "to": "2023-12-31", "paidOn": "2024-02-20", "amount": 0}""")
                .andExpect(status().isCreated());
        call(get("/api/booking/vat-payments"), null)
                .andExpect(jsonPath("$[0].from").value("2023-07-01"))
                .andExpect(jsonPath("$[1].id").value(paymentId));

        call(delete("/api/booking/vat-payments/" + paymentId), null).andExpect(status().isNoContent());
        call(delete("/api/booking/vat-payments/" + paymentId), null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.params.id").value(String.valueOf(paymentId)));
        call(get("/api/booking/payments/" + expenseId), null).andExpect(status().isNotFound());
        call(get("/api/reports/expenses?from=2023-08-01&to=2023-08-31"), null).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void a_customer_with_a_history_is_kept_one_entered_by_mistake_can_be_removed() throws Exception {
        String opened = call(post("/api/folders"), """
                {"folder": {"type": "INVOICING", "name": "Historique"},
                 "firstCustomer": {"firstName": "Ida", "lastName": "Histo"}}""")
                .andReturn().getResponse().getContentAsString();
        long folderId = id(opened, "$.folder.id");
        long ida = id(opened, "$.firstCustomer.id");
        long typo = id(call(post("/api/folders/" + folderId + "/customers"), """
                {"firstName": "Idda", "lastName": "Histo"}""").andReturn().getResponse().getContentAsString(), "$.id");
        call(post("/api/folders/" + folderId + "/honoraries"), """
                {"date": "2026-04-01", "description": "Séance", "minutes": 60, "hourlyRate": 200}""");
        call(post("/api/folders/" + folderId + "/invoices"), """
                {"date": "2026-04-02", "delayDays": 30, "shares": [{"customerId": %d, "amount": 200}]}""".formatted(ida))
                .andExpect(status().isCreated());

        call(delete("/api/customers/" + ida), null).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CUSTOMER_HAS_HISTORY"))
                .andExpect(jsonPath("$.params.customerId").value(ida));
        call(delete("/api/customers/" + typo), null).andExpect(status().isNoContent());
        call(get("/api/folders/" + folderId + "/customers"), null).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void invoiced_honoraries_cannot_be_changed() throws Exception {
        long folderId = id(call(post("/api/folders"), """
                {"folder": {"type": "INVOICING", "name": "Bloqué"}}""")
                .andReturn().getResponse().getContentAsString(), "$.folder.id");
        String honorary = """
                {"date": "2026-05-01", "description": "Séance", "minutes": 60, "hourlyRate": 200}""";
        long honoraryId = id(call(post("/api/folders/" + folderId + "/honoraries"), honorary)
                .andReturn().getResponse().getContentAsString(), "$.id");
        call(post("/api/folders/" + folderId + "/invoices"), """
                {"date": "2026-05-02", "delayDays": 30}""").andExpect(status().isCreated());

        call(put("/api/honoraries/" + honoraryId), honorary).andExpect(status().isUnprocessableContent());
        call(delete("/api/honoraries/" + honoraryId), null).andExpect(status().isUnprocessableContent());
    }

    @Test
    void deleting_a_folder_deletes_everything_it_holds() throws Exception {
        String opened = call(post("/api/folders"), """
                {"folder": {"type": "INVOICING", "name": "A supprimer"},
                 "firstCustomer": {"firstName": "Eva", "lastName": "Muster"}}""")
                .andReturn().getResponse().getContentAsString();
        long folderId = id(opened, "$.folder.id");
        long eva = id(opened, "$.firstCustomer.id");
        String base = "/api/folders/" + folderId;

        long ticketId = id(call(post(base + "/tickets"), """
                {"reference": "210000000003139471430009017", "customerId": %d}""".formatted(eva))
                .andReturn().getResponse().getContentAsString(), "$.id");
        call(post(base + "/honoraries"), """
                {"date": "2026-05-01", "minutes": 60, "hourlyRate": 200}""").andExpect(status().isCreated());
        long invoiceId = id(call(post(base + "/invoices"), """
                {"date": "2026-05-02", "delayDays": 30}""").andReturn().getResponse().getContentAsString(), "$.id");
        call(post("/api/invoices/" + invoiceId + "/payments"), """
                {"customerId": %d, "date": "2026-05-10", "amount": 50, "ticketId": %d}""".formatted(eva, ticketId))
                .andExpect(status().isCreated());
        call(post(base + "/honoraries"), """
                {"date": "2026-05-12", "minutes": 30, "hourlyRate": 200}""").andExpect(status().isCreated());
        long requestId = id(call(post(base + "/advance-requests"), """
                {"date": "2026-05-15", "amount": 500}""").andReturn().getResponse().getContentAsString(), "$.id");
        long advanceId = id(call(post(base + "/advances"), """
                {"customerId": %d, "date": "2026-05-20", "amount": 80, "ticketId": %d}""".formatted(eva, ticketId))
                .andReturn().getResponse().getContentAsString(), "$.id");
        long paymentId = id(call(post(base + "/booking/payments"), """
                {"kind": "EXPENSE", "date": "2026-05-21", "amount": 30}""")
                .andReturn().getResponse().getContentAsString(), "$.id");
        long orderId = id(call(post(base + "/booking/standing-orders"), """
                {"amount": 100, "dayOfMonth": 1, "enabled": true}""")
                .andReturn().getResponse().getContentAsString(), "$.id");

        call(delete(base), null).andExpect(status().isNoContent());

        call(get(base), null).andExpect(status().isNotFound());
        call(get("/api/customers/" + eva), null).andExpect(status().isNotFound());
        call(get("/api/invoices/" + invoiceId), null).andExpect(status().isNotFound());
        call(get("/api/advance-requests/" + requestId), null).andExpect(status().isNotFound());
        call(get("/api/advances/" + advanceId), null).andExpect(status().isNotFound());
        call(get("/api/tickets/" + ticketId), null).andExpect(status().isNotFound());
        call(get("/api/booking/payments/" + paymentId), null).andExpect(status().isNotFound());
        call(get("/api/booking/standing-orders/" + orderId), null).andExpect(status().isNotFound());
        call(get(base + "/honoraries"), null).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void statistics_cover_the_folders_handled_by_the_user() throws Exception {
        long mine = id(call(post("/api/folders"), """
                {"folder": {"type": "INVOICING", "name": "Statistique", "handledBy": "tst"}}""")
                .andReturn().getResponse().getContentAsString(), "$.folder.id");
        call(post("/api/folders"), """
                {"folder": {"type": "INVOICING", "name": "Autre", "handledBy": "xyz"}}""").andExpect(status().isCreated());
        call(post("/api/folders/" + mine + "/honoraries"), """
                {"date": "2024-03-01", "minutes": 60, "hourlyRate": 180}""").andExpect(status().isCreated());

        // tester's shortcut is "tst"
        call(get("/api/reports/user-statistics?username=tester&from=2024-01-01&to=2024-12-31"), null)
                .andExpect(jsonPath("$.folders", hasSize(1)))
                .andExpect(jsonPath("$.folders[0].id").value(mine))
                .andExpect(jsonPath("$.honorariesTotal").value(180.00));
        call(get("/api/reports/user-statistics?username=nobody&from=2024-01-01&to=2024-12-31"), null)
                .andExpect(status().isNotFound());
    }

    @Test
    void a_slip_remembers_the_invoice_it_was_issued_for() throws Exception {
        String opened = call(post("/api/folders"), """
                {"folder": {"type": "INVOICING", "name": "Bulletin"}, "firstCustomer": {"lastName": "Keller"}}""")
                .andReturn().getResponse().getContentAsString();
        long folderId = id(opened, "$.folder.id");
        long keller = id(opened, "$.firstCustomer.id");
        call(post("/api/folders/" + folderId + "/honoraries"), """
                {"date": "2026-06-01", "minutes": 60, "hourlyRate": 200}""").andExpect(status().isCreated());
        long invoiceId = id(call(post("/api/folders/" + folderId + "/invoices"), """
                {"date": "2026-06-02", "delayDays": 30}""").andReturn().getResponse().getContentAsString(), "$.id");

        long ticketId = id(call(post("/api/folders/" + folderId + "/tickets"), """
                {"reference": "000000000000000000000000017", "customerId": %d, "invoiceId": %d}"""
                .formatted(keller, invoiceId))
                .andExpect(jsonPath("$.invoiceId").value(invoiceId))
                .andReturn().getResponse().getContentAsString(), "$.id");
        call(get("/api/debtors/lookup?reference=000000000000000000000000017"), null)
                .andExpect(jsonPath("$.invoiceId").value(invoiceId));

        call(delete("/api/invoices/" + invoiceId), null).andExpect(status().isNoContent());
        call(get("/api/tickets/" + ticketId), null).andExpect(jsonPath("$.invoiceId").doesNotExist());
    }

    @Test
    void access_follows_the_legacy_grants() throws Exception {
        mvc.perform(get("/api/folders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/folders").with(httpBasic("tester", "wrong"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/folders").with(httpBasic("reader", "secret"))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/me").with(httpBasic("reader", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grants", hasSize(0)));
    }

    @Test
    void business_rule_violations_are_reported_as_problems() throws Exception {
        long folderId = id(call(post("/api/folders"), """
                {"folder": {"type": "INVOICING", "name": "Vide"}}""")
                .andReturn().getResponse().getContentAsString(), "$.folder.id");

        call(post("/api/folders/" + folderId + "/invoices"), """
                {"date": "2026-02-01", "delayDays": 30}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Folder " + folderId + " has nothing to invoice"));
        call(get("/api/invoices/999999"), null).andExpect(status().isNotFound());
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String json) throws Exception {
        request.with(httpBasic("tester", "secret"));
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(request);
    }

    private static long id(String json, String path) {
        return ((Number) JsonPath.read(json, path)).longValue();
    }
}
