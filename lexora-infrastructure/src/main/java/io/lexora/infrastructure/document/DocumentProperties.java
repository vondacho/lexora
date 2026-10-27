package io.lexora.infrastructure.document;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Identity of the office printed on the documents.
 *
 * @param city        place written before the date ("Neuchâtel, le ...")
 * @param signature   name under the letters
 * @param fontFile    TrueType font to embed, or empty for the built-in Helvetica
 * @param fontFamily  CSS name given to that font
 * @param qrBill      creditor of the QR-bills; no QR-bill without IBAN
 */
@ConfigurationProperties(prefix = "lexora.documents")
public record DocumentProperties(
        String city,
        String signature,
        String author,
        Resource headerImage,
        Resource footerImage,
        String fontFile,
        String fontFamily,
        QrBill qrBill) {

    public DocumentProperties {
        qrBill = qrBill == null ? new QrBill(null, null, null, null, null, null, null) : qrBill;
    }

    public record QrBill(String iban, String creditorName, String street, String houseNumber, String postalCode,
                         String town, String country) {

        public boolean isConfigured() {
            return iban != null && !iban.isBlank();
        }
    }
}
