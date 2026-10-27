package io.lexora.infrastructure.web;

import io.lexora.domain.shared.ErrorCode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** The contract lists exactly the error codes of the application, so that clients can translate each of them. */
class ErrorCodeContractTest {

    @Test
    void the_contract_lists_every_error_code() throws IOException {
        String contract;
        try (InputStream in = getClass().getResourceAsStream("/static/openapi/lexora_openapi_v1.yaml")) {
            contract = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher block = Pattern.compile("(?s)\\n    Problem:\\n.*?\\n        code:\\n.*?enum:\\n((?:            - [A-Z_]+\\n)+)").matcher(contract);
        assertThat(block.find()).as("Problem.code enum in the contract").isTrue();
        List<String> inContract = Pattern.compile("- ([A-Z_]+)").matcher(block.group(1)).results().map(m -> m.group(1)).toList();

        assertThat(inContract).containsExactlyElementsOf(Arrays.stream(ErrorCode.values()).map(Enum::name).toList());
    }
}
