package io.lexora.domain.billing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentReferenceTest {

    @Test
    void computes_the_qr_reference_check_digit() {
        // example of the Swiss Implementation Guidelines for the QR-bill
        assertThat(PaymentReference.qrReference("21000000000313947143000901")).isEqualTo("210000000003139471430009017");
        assertThat(PaymentReference.isValidQrReference("21 00000 00003 13947 14300 09017")).isTrue();
        assertThat(PaymentReference.isValidQrReference("210000000003139471430009018")).isFalse();
    }

    @Test
    void computes_the_creditor_reference_check_digits() {
        // example of ISO 11649
        assertThat(PaymentReference.creditorReference("539007547034")).isEqualTo("RF18539007547034");
        assertThat(PaymentReference.isValidCreditorReference("RF18 5390 0754 7034")).isTrue();
    }

    @Test
    void derives_a_stable_reference_per_document_and_customer() {
        String payload = PaymentReference.payload(PaymentReference.DocumentKind.INVOICE, 2232, 1923);

        assertThat(payload).isEqualTo("1000002232000001923");
        assertThat(PaymentReference.isValidQrReference(PaymentReference.qrReference(payload))).isTrue();
        assertThat(PaymentReference.isValidCreditorReference(PaymentReference.creditorReference(payload))).isTrue();
    }

    @Test
    void chooses_the_reference_style_from_the_account() {
        assertThat(PaymentReference.styleFor("CH44 3199 9123 0008 8901 2")).isEqualTo(PaymentReference.Style.QR_REFERENCE);
        assertThat(PaymentReference.styleFor("CH93 0076 2011 6238 5295 7")).isEqualTo(PaymentReference.Style.CREDITOR_REFERENCE);
        assertThat(PaymentReference.styleFor(null)).isEqualTo(PaymentReference.Style.NONE);
    }

    @Test
    void decodes_the_document_and_customer_of_its_own_references() {
        String payload = PaymentReference.payload(PaymentReference.DocumentKind.INVOICE, 2232, 17);
        PaymentReference.Decoded expected = new PaymentReference.Decoded(PaymentReference.DocumentKind.INVOICE, 2232, 17);

        assertThat(PaymentReference.decode(PaymentReference.qrReference(payload))).contains(expected);
        assertThat(PaymentReference.decode(PaymentReference.creditorReference(payload))).contains(expected);
        String spaced = PaymentReference.qrReference(PaymentReference.payload(PaymentReference.DocumentKind.ADVANCE_REQUEST, 374, 6))
                .replaceAll("(.{2})(.{5})(.{5})(.{5})(.{5})(.{5})", "$1 $2 $3 $4 $5 $6");
        assertThat(PaymentReference.decode(spaced))
                .contains(new PaymentReference.Decoded(PaymentReference.DocumentKind.ADVANCE_REQUEST, 374, 6));
    }

    @Test
    void ignores_references_made_elsewhere_or_mistyped() {
        String qr = PaymentReference.qrReference(PaymentReference.payload(PaymentReference.DocumentKind.INVOICE, 2232, 17));
        assertThat(PaymentReference.decode(qr.substring(0, 26) + ((qr.charAt(26) - '0' + 1) % 10))).isEmpty(); // check digit
        assertThat(PaymentReference.decode(PaymentReference.qrReference("12345678901234567890123456"))).isEmpty();
        assertThat(PaymentReference.decode(PaymentReference.creditorReference("ABC123"))).isEmpty();
        assertThat(PaymentReference.decode(null)).isEmpty();
    }
}
