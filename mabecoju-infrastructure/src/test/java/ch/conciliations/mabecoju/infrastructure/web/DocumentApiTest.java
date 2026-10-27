package ch.conciliations.mabecoju.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Invoices, requests and reminders printed from the letter templates, with QR-bills. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentApiTest {

    @Autowired
    MockMvc mvc;

    long folderId;
    long anne;
    long paul;

    @BeforeEach
    void folderWithTwoCustomers() throws Exception {
        String opened = body(call(post("/api/folders"), """
                {"folder": {"type": "INVOICING", "name": "Dupont c. Dupont"},
                 "firstCustomer": {"title": "Madame", "firstName": "Anne", "lastName": "Dupont",
                                   "street": "Rue des Alpes 12", "zip": "2000", "city": "Neuchâtel"}}"""));
        folderId = id(opened, "$.folder.id");
        anne = id(opened, "$.firstCustomer.id");
        paul = id(body(call(post("/api/folders/" + folderId + "/customers"), """
                {"title": "Monsieur", "firstName": "Paul", "lastName": "Dupont",
                 "street": "Chemin du Lac 3", "zip": "2001", "city": "Neuchâtel"}""")), "$.id");
    }

    @Test
    void a_pdf_is_declared_as_pdf_whatever_the_client_accepts() throws Exception {
        long invoiceId = invoice();
        long modelId = model("Facture simple", "INVOICE", "Madame, Monsieur,", "layout_invoice.php");
        // generated clients prefer JSON types in Accept; browsers only render a body declared as PDF
        for (String accept : List.of("application/problem+json", "application/json", "*/*")) {
            MvcResult result = call(post("/api/invoices/" + invoiceId + "/document").header("Accept", accept), """
                    {"modelId": %d}""".formatted(modelId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andReturn();
            assertThat(new String(result.getResponse().getContentAsByteArray(), 0, 5)).isEqualTo("%PDF-");
        }
    }

    @Test
    void prints_one_copy_per_customer_with_the_statement_and_a_qr_bill() throws Exception {
        long invoiceId = invoice();
        long modelId = model("Facture détaillée", "INVOICE",
                "Madame, Monsieur,\\n\\nVotre part s'élève à frs. $amount_1 ($title_1) et $amount_2 ($title_2). [TVA $tvaNo]",
                "layout_invoice_double.php", "layout_invoice_double_detail.php");

        MvcResult result = call(post("/api/invoices/" + invoiceId + "/document"), """
                {"modelId": %d, "attachments": 2}""".formatted(modelId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("_f" + invoiceId + "_")))
                .andReturn();

        String text = text(result);
        assertThat(text).contains("Facture détaillée", "Votre part s'élève à frs. 161.55 (Madame) et 161.55 (Monsieur)",
                "[TVA 000 000]",
                "honoraires", "solde dû", "Annexes", "Rue des Alpes 12", "Chemin du Lac 3");
        assertThat(count(text, "Récépissé")).isEqualTo(2);           // one QR-bill per customer
        assertThat(count(text, "161.55")).isGreaterThanOrEqualTo(4);
        // the QR references were registered as slips issued for the invoice
        call(get("/api/folders/" + folderId + "/tickets"), null)
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].invoiceId", everyItem(notNullValue())))
                .andExpect(jsonPath("$[0].number", org.hamcrest.Matchers.matchesPattern("\\d{27}")));

        // a reprint reuses the same references
        call(post("/api/invoices/" + invoiceId + "/document"), """
                {"modelId": %d}""".formatted(modelId)).andExpect(status().isOk());
        call(get("/api/folders/" + folderId + "/tickets"), null).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void merged_addresses_give_a_single_letter_for_the_whole_amount() throws Exception {
        long invoiceId = invoice();
        long modelId = model("Facture", "INVOICE", "Montant total : frs. $amount", "layout_invoice_single.php");

        String text = text(call(post("/api/invoices/" + invoiceId + "/document"), """
                {"modelId": %d, "mergeAddresses": true, "content": "<p>Texte <b>modifié</b> pour cet envoi.</p>"}"""
                .formatted(modelId)).andExpect(status().isOk()).andReturn());

        assertThat(text).contains("Madame et Monsieur", "Anne et Paul Dupont", "Texte modifié pour cet envoi.");
        assertThat(count(text, "Récépissé")).isEqualTo(1);
        assertThat(text).contains("323.10");
    }

    @Test
    void prints_reminders_and_advance_requests() throws Exception {
        long invoiceId = invoice();
        call(post("/api/invoices/" + invoiceId + "/recalls"), """
                {"date": "2026-04-10"}""").andExpect(status().isCreated());
        long recallId = id(body(call(get("/api/invoices/" + invoiceId), null)), "$.recalls[0].id");
        long recallModel = model("Rappel", "RECALL", "Facture du $invoiceDate : reste dû frs. $amount_1.", "layout_recall.php");
        assertThat(text(call(post("/api/invoices/" + invoiceId + "/recalls/" + recallId + "/document"), """
                {"modelId": %d}""".formatted(recallModel)).andExpect(status().isOk()).andReturn()))
                .contains("Facture du 1 mars 2026 : reste dû frs. 161.55.", "10 avril 2026");

        long requestId = id(body(call(post("/api/folders/" + folderId + "/advance-requests"), """
                {"date": "2026-05-01", "amount": 600, "delayDays": 10,
                 "shares": [{"customerId": %d, "amount": 300}, {"customerId": %d, "amount": 300}]}"""
                .formatted(anne, paul))), "$.id");
        long requestModel = model("Provision", "ADVANCE_REQUEST", "Provision de frs. $amount chacun.", "layout_request.php");
        String text = text(call(post("/api/advance-requests/" + requestId + "/document"), """
                {"modelId": %d}""".formatted(requestModel)).andExpect(status().isOk()).andReturn());
        assertThat(text).contains("Provision de frs. 300.00 chacun.");
        assertThat(count(text, "Récépissé")).isEqualTo(2);
    }

    @Test
    void the_template_must_match_the_document() throws Exception {
        long invoiceId = invoice();
        long requestModel = model("Provision", "ADVANCE_REQUEST", "Texte", "layout_request.php");

        call(post("/api/invoices/" + invoiceId + "/document"), """
                {"modelId": %d}""".formatted(requestModel))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void prints_the_period_statements() throws Exception {
        call(post("/api/folders/" + folderId + "/honoraries"), """
                {"date": "2019-02-01", "minutes": 60, "hourlyRate": 100}""").andExpect(status().isCreated());
        long invoiceId = id(body(call(post("/api/folders/" + folderId + "/invoices"), """
                {"date": "2019-02-02", "delayDays": 30}""")), "$.id");
        long ticketId = id(body(call(post("/api/folders/" + folderId + "/tickets"), """
                {"reference": "000000000000000000000019057", "customerId": %d}""".formatted(anne))), "$.id");
        call(post("/api/folders/" + folderId + "/customers/" + anne + "/receipts"), """
                {"ticketId": %d, "invoices": [{"documentId": %d, "date": "2019-03-01", "amount": 100,
                                               "iban": "CH9300762011623852957"}]}""".formatted(ticketId, invoiceId))
                .andExpect(status().isOk());
        call(post("/api/folders/" + folderId + "/advances"), """
                {"customerId": %d, "date": "2019-04-01", "amount": 250, "cash": true}""".formatted(paul))
                .andExpect(status().isCreated());
        call(post("/api/booking/payments"), """
                {"kind": "EXPENSE", "date": "2019-05-01", "creditor": "Papeterie du Lac", "title": "Fournitures",
                 "amount": 107.70, "taxRatePercent": 7.7, "paidOn": "2019-05-02"}""").andExpect(status().isCreated());

        call(get("/api/reports/income?from=2019-01-01&to=2019-12-31"), null)
                .andExpect(jsonPath("$.invoicePayments[0].ticketNumber").value("000000000000000000000019057"))
                .andExpect(jsonPath("$.invoicePayments[0].iban").value("CH9300762011623852957"));

        MvcResult result = call(get("/api/reports/period-statement?from=2019-01-01&to=2019-12-31"), null)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("releves_2019-01-01_2019-12-31.pdf")))
                .andReturn();
        String text = text(result);
        assertThat(text).contains("Relevé des entrées", "période du 01.01.2019 au 31.12.2019",
                "versements (2), encaissé 350.00, TVA due (5.9 %) 20.65",
                "Factures (1), encaissé 100.00", "000000000000000000000019057", "CH9300762011623852957",
                "Avances (1), encaissé 250.00", "Dupont c. Dupont",
                "Relevé des sorties", "paiements (1), total 107.70, dont tva 7.70", "Papeterie du Lac", "frais",
                "page 1 / 2");
    }

    // ----- fixtures ----------------------------------------------------------------------------------------------

    /** 300.- of honoraries, 7.7 % VAT = 23.10, total 323.10, split 161.55 each. */
    private long invoice() throws Exception {
        call(post("/api/folders/" + folderId + "/honoraries"), """
                {"date": "2026-02-20", "description": "Séance de médiation", "minutes": 90, "hourlyRate": 200}""")
                .andExpect(status().isCreated());
        return id(body(call(post("/api/folders/" + folderId + "/invoices"), """
                {"date": "2026-03-01", "delayDays": 30, "taxRatePercent": 7.7,
                 "shares": [{"customerId": %d, "amount": 161.55}, {"customerId": %d, "amount": 161.55}]}"""
                .formatted(anne, paul))), "$.id");
    }

    private long model(String name, String type, String content, String... layouts) throws Exception {
        String slots = String.join(",", List.of(layouts).stream()
                .map(l -> "{\"layout\": \"" + l + "\"}").toList());
        return id(body(call(post("/api/document-models"), """
                {"name": "%s %d", "type": "%s", "title": "%s", "content": "%s", "addressCount": 2, "layouts": [%s]}"""
                .formatted(name, System.nanoTime(), type, name, content, slots))), "$.id");
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String json) throws Exception {
        request.with(httpBasic("tester", "secret"));
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(request);
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    private static long id(String json, String path) {
        return ((Number) JsonPath.read(json, path)).longValue();
    }

    private static String text(MvcResult result) throws Exception {
        try (PDDocument pdf = Loader.loadPDF(result.getResponse().getContentAsByteArray())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(pdf);
        }
    }

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }
}
