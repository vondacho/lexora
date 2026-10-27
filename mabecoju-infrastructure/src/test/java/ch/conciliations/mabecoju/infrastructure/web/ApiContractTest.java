package ch.conciliations.mabecoju.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The OpenAPI contract and its Scalar rendering are public; the contract's constraints are enforced. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiContractTest {

    @Autowired
    MockMvc mvc;

    @Test
    void publishes_the_contract() throws Exception {
        mvc.perform(get("/openapi/mabecoju_openapi_v1.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("openapi: 3.0.3")))
                .andExpect(content().string(containsString("/api/invoices/{id}/payments:")));
    }

    @Test
    void renders_the_contract_with_scalar() throws Exception {
        mvc.perform(get("/scalar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/openapi/mabecoju_openapi_v1.yaml")));
    }

    @Test
    void rejects_requests_breaking_the_contract() throws Exception {
        mvc.perform(post("/api/folders").with(httpBasic("tester", "secret"))
                        .contentType("application/json")
                        .content("""
                                {"folder": {"type": "INVOICING", "name": ""}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/reports/income?from=2026-01-01").with(httpBasic("tester", "secret")))
                .andExpect(status().isBadRequest());
    }
}
