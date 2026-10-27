# Documents

Invoices, advance requests and their reminders are printed as PDF **on demand** from the letter templates of
`invo_models`; nothing is stored. Each copy ends with a **Swiss QR-bill** for what its recipient still owes.

| Endpoint | Document | Template type |
|---|---|---|
| `POST /api/invoices/{id}/document` | Invoice | `INVOICE` |
| `POST /api/invoices/{id}/recalls/{recallId}/document` | Reminder of an invoice | `RECALL` |
| `POST /api/advance-requests/{id}/document` | Advance request | `ADVANCE_REQUEST` |
| `POST /api/advance-requests/{id}/recalls/{recallId}/document` | Reminder of an advance request | `RECALL` |

Body: `{"modelId": 12, "content": "...", "attachments": 1, "mergeAddresses": false}` — `content` replaces the template
text for this print only (as the legacy print form allowed), `attachments` adds "Annexe(s) : ment." on the first page.
The response is `application/pdf`, with the legacy file name (`{folder}_{f|fr|p|pr}{id}_{yyyymmdd}_{model}.pdf`) in
`Content-Disposition`.

Period statements ("relevés") are printed the same way, by `GET /api/reports/period-statement?from=&to=`
(grant `booking:tax_summary`): incomes on the first page (number of payments, total received, VAT owed at the net
tax rate, then the invoice payments and the advances with folder, slip reference, IBAN and cash flag), expenses of the
office from the next page. No letterhead; page numbers at the bottom. File `releves_{from}_{to}.pdf`.

## How a document is built

- **Recipients**: the customers active at the document date, at most three. Each receives a copy with his address
  first (same rotation as the legacy application), or a single letter "Madame et Monsieur / Anne et Paul Dupont" when
  `mergeAddresses` is set.
- **Pages** of each copy follow the template's layout slots: legacy layouts named `*_detail.php` print the invoice
  statement (honoraries, discount, VAT, totals, advances and balance per customer), the others the letter (addresses,
  reference, title, text, signature). A template without layout prints one letter.
- **Placeholders** use the syntax of the legacy templates (`$amount` or `${amount}`): `issuerName`, `issuerShortcut`,
  `refNo`, `tvaNo` (VAT number of the invoicing settings), `date`, `delay`, `dueDate`, `modelTitle`, `invoiceDate`, `lastRecallDate`, `amount`,
  `amount_1..3`, and per customer `title_N`, `firstName_N`, `lastName_N`, `streetNo_N`, `zip_N`, `city_N`.
  Unknown placeholders print nothing, as in PHP. Templates may be plain text or HTML (sanitised: no images, no scripts).
- **Amounts**: a customer's share of the document; on reminders, what he still owes. A billing customer owes the whole
  amount; a single customer without distribution too.

## QR-bill

| Account configured | Reference printed |
|---|---|
| QR-IBAN (institution id 30000–31999) | QR reference, 27 digits |
| Plain IBAN | Creditor reference `RF..` (ISO 11649) |
| None | No QR-bill |

The reference encodes the document and the customer (`1` invoice / `2` request, document id, customer id), so it is
the same on every reprint and reminder. It is registered as a payment slip issued for the document: a payment scanned
with it is matched to its debtor by `GET /api/debtors/lookup`. The payment part is drawn on its own A4 page at the end
of each copy, in French; copies with nothing left to pay have none.

The office's current account (BCN, `CH96 0076 6000 A148 3790 2`, printed on the footer) is a plain IBAN: QR-bills will
carry `RF` references. Ask the bank for a QR-IBAN to get QR references instead.

## Configuration

```yaml
mabecoju:
  documents:
    city: Neuchâtel                       # "Neuchâtel, le 24 octobre 2026"
    signature: Marna Müller-Bessi
    header-image: classpath:documents/header.png
    footer-image: classpath:documents/footer.png
    font-file: /path/to/GOTHIC.TTF        # MABECOJU_DOCUMENTS_FONT_FILE; Helvetica when empty
    font-family: Century Gothic
    qr-bill:
      iban: ...                           # MABECOJU_QR_BILL_IBAN; no QR-bill when empty
      creditor-name: ...                  # MABECOJU_QR_BILL_CREDITOR_NAME
      street: ...                         # MABECOJU_QR_BILL_STREET, _HOUSE_NUMBER, _POSTAL_CODE, _TOWN
```

Century Gothic (`GOTHIC.TTF` of the PHP server) is a licensed Monotype font: it is not part of the repository.
Point `font-file` to the licensed copy on the server.

## Notes

- The "single page" flags of the templates (`mode_single_page_*`) are kept but have no effect, as in the legacy
  application where their use was commented out.
- The template's `addressCount` is kept for information; the recipients are the active customers.
