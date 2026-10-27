package ch.conciliations.mabecoju.infrastructure.web;

import ch.conciliations.mabecoju.domain.billing.PaymentReference;
import ch.conciliations.mabecoju.domain.billing.PaymentReference.DocumentKind;
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

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Import of a camt.054 notification: matching by reference or by amount and name, recording, duplicates. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BankStatementApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void matches_the_credits_of_a_notification_records_them_once_and_books_the_surplus_as_an_advance() throws Exception {
        String opened = call(post("/api/folders"), """
                {"folder": {"type": "INVOICING", "name": "Zbinden"},
                 "firstCustomer": {"title": "Madame", "firstName": "Léa", "lastName": "Zbinden"}}""")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long folder = id(opened, "$.folder.id");
        long lea = id(opened, "$.firstCustomer.id");
        long marc = id(call(post("/api/folders/" + folder + "/customers"), """
                {"title": "Monsieur", "firstName": "Marc", "lastName": "Zbinden"}""")
                .andReturn().getResponse().getContentAsString(), "$.id");
        call(post("/api/folders/" + folder + "/honoraries"), """
                {"date": "2026-03-01", "description": "Séance", "minutes": 60, "hourlyRate": 200}""");
        long invoice = id(call(post("/api/folders/" + folder + "/invoices"), """
                {"date": "2026-03-02", "delayDays": 30, "taxRatePercent": 0,
                 "shares": [{"customerId": %d, "amount": 120}, {"customerId": %d, "amount": 80}]}""".formatted(lea, marc))
                .andExpect(jsonPath("$.amount").value(200.00)).andReturn().getResponse().getContentAsString(), "$.id");
        long request = id(call(post("/api/folders/" + folder + "/advance-requests"), """
                {"date": "2026-03-05", "delayDays": 10, "amount": 777.35, "shares": [{"customerId": %d, "amount": 777.35}]}"""
                .formatted(lea)).andReturn().getResponse().getContentAsString(), "$.id");

        String qrLea = PaymentReference.qrReference(PaymentReference.payload(DocumentKind.INVOICE, invoice, lea));
        String rfMarc = PaymentReference.creditorReference(PaymentReference.payload(DocumentKind.INVOICE, invoice, marc));
        String camt = camt(List.of(
                tx("ZB-1", "120.00", qrLea, "ZBINDEN LEA", null),
                tx("ZB-2", "100.00", rfMarc, "Zbinden Marc", null),
                tx("ZB-3", "777.35", null, "ZBINDEN LEA", "provision mars"),
                tx("ZB-4", "50.00", "RF18539007547034", "Inconnu SA", null)));

        String analysis = analyse(camt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].status").value("READY"))
                .andExpect(jsonPath("$.entries[0].target.documentId").value(invoice))
                .andExpect(jsonPath("$.entries[0].target.customerId").value(lea))
                .andExpect(jsonPath("$.entries[0].remaining").value(120.00))
                .andExpect(jsonPath("$.entries[1].status").value("CHECK"))
                .andExpect(jsonPath("$.entries[1].problem").value("OVERPAID"))
                .andExpect(jsonPath("$.entries[1].target.customerName").value("Marc Zbinden"))
                .andExpect(jsonPath("$.entries[1].remaining").value(80.00))
                .andExpect(jsonPath("$.entries[2].status").value("CHECK"))
                .andExpect(jsonPath("$.entries[2].problem").value("SUGGESTED"))
                .andExpect(jsonPath("$.entries[2].target.kind").value("ADVANCE_REQUEST"))
                .andExpect(jsonPath("$.entries[2].target.documentId").value(request))
                .andExpect(jsonPath("$.entries[3].status").value("UNMATCHED"))
                .andExpect(jsonPath("$.entries[3].problem").value("UNKNOWN_REFERENCE"))
                .andReturn().getResponse().getContentAsString();

        String receipts = """
                {"receipts": [
                  {"kind": "INVOICE", "documentId": %1$d, "customerId": %2$d, "date": "2026-03-20", "amount": 120, "reference": "%4$s", "entryReference": "ZB-1"},
                  {"kind": "INVOICE", "documentId": %1$d, "customerId": %3$d, "date": "2026-03-20", "amount": 100, "reference": "%5$s", "entryReference": "ZB-2"},
                  {"kind": "ADVANCE_REQUEST", "documentId": %6$d, "date": "2026-03-20", "amount": 777.35, "entryReference": "ZB-3"}
                ]}""".formatted(invoice, lea, marc, qrLea, rfMarc, request);
        call(post("/api/bank-statements/receipts"), receipts)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].recorded").value(true))
                .andExpect(jsonPath("$.results[0].settled").value(false))
                .andExpect(jsonPath("$.results[1].settled").value(true))
                .andExpect(jsonPath("$.results[1].advance").value(20.00))
                .andExpect(jsonPath("$.results[2].settled").value(true));
        call(get("/api/invoices/" + invoice), null)
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.payments.length()").value(2));
        // Marc paid 20.00 too much: a spontaneous advance of the folder, deducted on the next invoice
        call(get("/api/folders/" + folder + "/advances?invoiced=false"), null)
                .andExpect(jsonPath("$[?(@.amount == 20.0 && @.customerId == %d)]".formatted(marc)).exists());

        // the same file again: nothing new to book
        analyse(camt)
                .andExpect(jsonPath("$.entries[0].status").value("DUPLICATE"))
                .andExpect(jsonPath("$.entries[1].status").value("DUPLICATE"))
                .andExpect(jsonPath("$.entries[2].status").value("DUPLICATE"))
                .andExpect(jsonPath("$.entries[3].status").value("UNMATCHED"));
        call(post("/api/bank-statements/receipts"), receipts)
                .andExpect(jsonPath("$.results[0].recorded").value(false))
                .andExpect(jsonPath("$.results[2].recorded").value(false));
        call(get("/api/invoices/" + invoice), null).andExpect(jsonPath("$.payments.length()").value(2));
    }

    @Test
    void refuses_a_file_that_is_not_a_notification() throws Exception {
        analyse("<html>not a bank file</html>").andExpect(status().isUnprocessableContent());
    }

    private ResultActions analyse(String camt) throws Exception {
        return call(post("/api/bank-statements/analysis"),
                tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(Map.of("fileName", "camt.xml", "content", camt)));
    }

    private static String tx(String bankRef, String amount, String reference, String debtor, String remittance) {
        String rmt = reference != null
                ? "<RmtInf><Strd><CdtrRefInf><Ref>" + reference + "</Ref></CdtrRefInf></Strd></RmtInf>"
                : remittance != null ? "<RmtInf><Ustrd>" + remittance + "</Ustrd></RmtInf>" : "";
        return """
                <TxDtls><Refs><AcctSvcrRef>%s</AcctSvcrRef></Refs><Amt Ccy="CHF">%s</Amt><CdtDbtInd>CRDT</CdtDbtInd>
                <RltdPties><Dbtr><Pty><Nm>%s</Nm></Pty></Dbtr><DbtrAcct><Id><IBAN>CH9300762011623852957</IBAN></Id></DbtrAcct></RltdPties>%s</TxDtls>"""
                .formatted(bankRef, amount, debtor, rmt);
    }

    private static String camt(List<String> transactions) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:camt.054.001.08"><BkToCstmrDbtCdtNtfctn><Ntfctn>
                <Acct><Id><IBAN>CH4431999123000889012</IBAN></Id></Acct>
                <Ntry><Amt Ccy="CHF">1047.35</Amt><CdtDbtInd>CRDT</CdtDbtInd><BookgDt><Dt>2026-03-20</Dt></BookgDt>
                <NtryDtls>%s</NtryDtls></Ntry>
                </Ntfctn></BkToCstmrDbtCdtNtfctn></Document>""".formatted(String.join("", transactions));
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
