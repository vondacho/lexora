package ch.conciliations.mabecoju.infrastructure.bank;

import ch.conciliations.mabecoju.application.bank.BankCredit;
import ch.conciliations.mabecoju.application.bank.BankStatement;
import ch.conciliations.mabecoju.application.bank.BankStatementReader;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Money;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Reads ISO 20022 camt.054 notifications (BankToCustomerDebitCreditNotification) of Swiss banks, whatever their
 * version: elements are found by local name. Each credit transaction becomes a {@link BankCredit}; batch entries
 * (several QR payments booked together) give one credit per transaction. Debits and reversals are left out.
 */
@Component
class Camt054Reader implements BankStatementReader {

    @Override
    public BankStatement read(String content) {
        Document document = parse(content);
        List<Element> notifications = descendants(document.getDocumentElement(), "Ntfctn");
        if (!"Document".equals(document.getDocumentElement().getLocalName()) || notifications.isEmpty()) {
            throw new DomainException(ErrorCode.BANK_FILE_INVALID, "The file is not a camt.054 bank notification");
        }
        String iban = null;
        List<BankCredit> credits = new ArrayList<>();
        int debits = 0;
        for (Element notification : notifications) {
            iban = iban != null ? iban : text(path(notification, "Acct", "Id", "IBAN"));
            for (Element entry : children(notification, "Ntry")) {
                if ("true".equalsIgnoreCase(text(child(entry, "RvslInd")))) {
                    continue;
                }
                String entryIndicator = text(child(entry, "CdtDbtInd"));
                LocalDate date = firstDate(child(entry, "BookgDt"), child(entry, "ValDt"));
                Element entryAmount = child(entry, "Amt");
                List<Element> transactions = new ArrayList<>();
                for (Element details : children(entry, "NtryDtls")) {
                    transactions.addAll(children(details, "TxDtls"));
                }
                if (transactions.isEmpty()) {
                    if ("CRDT".equals(entryIndicator)) {
                        credits.add(credit(entry, null, entryAmount, date));
                    } else {
                        debits++;
                    }
                    continue;
                }
                for (Element tx : transactions) {
                    String indicator = either(text(child(tx, "CdtDbtInd")), entryIndicator);
                    if (!"CRDT".equals(indicator)) {
                        debits++;
                        continue;
                    }
                    Element amount = either(child(tx, "Amt"),
                            either(path(tx, "AmtDtls", "TxAmt", "Amt"),
                                    transactions.size() == 1 ? entryAmount : null));
                    credits.add(credit(entry, tx, amount, date));
                }
            }
        }
        LocalDate from = credits.stream().map(BankCredit::date).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        LocalDate to = credits.stream().map(BankCredit::date).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        return new BankStatement(iban, from, to, credits, debits);
    }

    private static BankCredit credit(Element entry, Element tx, Element amount, LocalDate date) {
        if (amount == null || text(amount) == null) {
            throw new DomainException(ErrorCode.BANK_FILE_INVALID, "A credit of the bank notification has no amount");
        }
        String entryReference = tx == null ? null
                : either(text(path(tx, "Refs", "AcctSvcrRef")), text(path(tx, "Refs", "EndToEndId")));
        if (entryReference == null || "NOTPROVIDED".equals(entryReference)) {
            entryReference = text(child(entry, "AcctSvcrRef"));
        }
        Element parties = tx == null ? null : child(tx, "RltdPties");
        String debtor = parties == null ? null
                : either(text(path(parties, "Dbtr", "Nm")), text(path(parties, "Dbtr", "Pty", "Nm")));
        String debtorIban = parties == null ? null : text(path(parties, "DbtrAcct", "Id", "IBAN"));
        Element remittance = tx == null ? null : child(tx, "RmtInf");
        String reference = remittance == null ? null
                : descendants(remittance, "CdtrRefInf").stream().map(e -> text(child(e, "Ref"))).filter(Objects::nonNull)
                .findFirst().orElse(null);
        String free = remittance == null ? null
                : children(remittance, "Ustrd").stream().map(Camt054Reader::text).filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        return new BankCredit(entryReference, date, Money.of(new BigDecimal(text(amount))),
                amount.getAttribute("Ccy"), reference == null ? null : reference.replace(" ", ""), debtor,
                debtorIban, free == null || free.isBlank() ? null : free);
    }

    /** Parser closed to external entities and DOCTYPEs: the file comes from outside. */
    private static Document parse(String content) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null);
            return builder.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new DomainException(ErrorCode.BANK_FILE_INVALID, "The file is not a readable XML bank notification");
        }
    }

    /** The first value when present, else the second (both may be missing). */
    private static <T> T either(T first, T second) {
        return first != null ? first : second;
    }

    private static LocalDate firstDate(Element... dates) {
        for (Element date : dates) {
            if (date == null) {
                continue;
            }
            String day = either(text(child(date, "Dt")), text(child(date, "DtTm")));
            if (day != null && day.length() >= 10) {
                return LocalDate.parse(day.substring(0, 10));
            }
        }
        return null;
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> list = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && name.equals(e.getLocalName())) {
                list.add(e);
            }
        }
        return list;
    }

    private static List<Element> descendants(Element parent, String name) {
        List<Element> list = new ArrayList<>();
        var nodes = parent.getElementsByTagNameNS("*", name);
        for (int i = 0; i < nodes.getLength(); i++) {
            list.add((Element) nodes.item(i));
        }
        return list;
    }

    private static Element child(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        List<Element> list = children(parent, name);
        return list.isEmpty() ? null : list.getFirst();
    }

    private static Element path(Element parent, String... names) {
        Element current = parent;
        for (String name : names) {
            current = child(current, name);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static String text(Element element) {
        if (element == null) {
            return null;
        }
        String value = element.getTextContent().trim();
        return value.isEmpty() ? null : value;
    }
}
