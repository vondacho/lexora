# Differences with the PHP application

mabecoju v2 reproduces the business behaviour of the PHP application. This page lists where it deliberately differs,
the PHP defects found while reading the code, and the questions left open.

## Deliberate changes

| Topic | PHP | v2 | Why |
|---|---|---|---|
| Invoice sums | Honoraries and advances sums posted by the browser | Computed from the stored honoraries and advances | The server should not trust totals from the client. |
| Issuing an empty invoice | Allowed | Rejected when the folder has no open honorary nor available advance | Avoids empty invoices. |
| Invoice shares | Any customer id accepted | Must be customers of the folder | Data integrity. |
| Deleting a customer | Always deleted, even with invoices, payments or advances (left pointing to nobody) | Refused once the customer has a history in the folder; end the mandate (active until) instead | Data integrity. |
| Cancelling an invoice payment | Always reopens the invoice | Reopens unless the remaining payments still cover it | Consistent with automatic settlement. |
| Cancelling an advance | Deleted, request left as is; allowed even when deducted on an invoice | Request reopened if no longer covered; refused when already deducted on an invoice | Otherwise the request stays "paid" and the invoice totals become wrong. |
| Recalling a paid invoice or request | Possible | Rejected | |
| Executing standing orders | All orders, including folder ones, booked in the office books; day 31 in a 30-day month produced an invalid date | Office orders in the office books, folder orders in their folder; day clamped to the month length | |
| Modifying an outgoing payment | The "modifier" link was hidden for standing-order payments, but the action did not check it; the paid date was never saved (unassigned variable) | One-off entries (bank, expense) are editable, paid date included; a standing-order payment can only be deleted (422); `PERMANENT` cannot be entered by hand; the API returns `editable` | |
| Next execution month | Current month returned with a 2-digit year (`date("y")`) | 4-digit year | Bug fix. |
| Empty passwords | A user with an empty password could log in with an empty password | Never accepted | Security. |
| Advance received with a payment slip | The slip was looked up but not linked to the advance (`adva_tick_id` stayed NULL) | The slip id is stored | Bug fix. |
| Spontaneous advance without customer id check | — | Customer must belong to the folder | Data integrity. |
| Deleting a folder | Kept spontaneous advances, payment slips and booking entries (orphans) | Deletes everything the folder holds | Decided 2026-10-24. |
| Modifying or deleting an invoiced honorary | Allowed, leaving the invoice totals stale | Refused | Decided 2026-10-24. |
| Printing a reminder | Created the reminder as a side effect, claimed each customer's full share | Creating and printing are separate; the reminder claims what each customer still owes | |
| Printed documents | Saved under the folder directory and sent to the browser | Generated on demand, never stored | Decided 2026-10-24. |
| Payment slips on documents | None printed (BVR slips handled outside) | QR-bill per copy, reference registered as a slip | Decided 2026-10-24. |
| Copies order | Grouped by page: all letters, then all statements | Grouped by recipient: letter, statement and QR-bill of each customer together | Each recipient gets a contiguous set. |
| Invoice comment | `invo_comment` never written | Written | |

## PHP defects found (not reproduced)

- `ParameterDAO::updateInvoiceDelay` writes into `defa_tariff_other`: changing the invoice delay overwrote the "other" hourly rate.
- `CustomerDAO::update` stores `cust_is_billing = 1` when the flag is unchecked (`$billing ? $billing : 1`).
- `StatisticManager::sumAllAdvancesByIssuerInPeriod` adds `$advance` instead of `$payment` in its second loop, calls
  `findAllAdvancesFromTo` without the folder type, and `Folder::getIssuer()` is never set: the statistics page always shows 0.
  v2 implements the intent with the folders the user handles (`fold_handled_by` = his shortcut), decided 2026-10-24.
- `RequestDAO::selectAllByInvoice` queries a non-existent column `req_invo_id`.
- `CustomerDAO::selectByName` queries a non-existent column `cust_name`.
- Pages call methods that do not exist: `DebtorManager::findRequestByTicket`, `findCustomerByTicket`, `findByName`,
  `findByCustomer`, `UserManager::grantUser`, `ungrantUser`. The "debtor search by folder" page and
  `admin/action_create_mabecoju_user.php` cannot work.
- `DebtorManager::payRequest` uses an undefined `$conn`; `unregisterTicket` passes `l_conn` instead of `$l_conn`;
  `PaymentManager::executeOrders` passes `l_conn` (missing `$`).
- `MiscHelper::month_summary_add` writes July into the `sep` key, losing July in the monthly incomes overview.
- `action_pay_advance.php` uses an undefined `$f_iban`.
- All SQL is built by string concatenation (SQL injection), and passwords are unsalted MD5.

## Open questions

None.

## Decided

| Question | Decision (2026-10-24) |
|---|---|
| Deleting a folder | Cascades to everything the folder holds. |
| Modifying an invoiced honorary | Forbidden, as is deleting it. |
| Statistics "issuer" | The person in charge, `fold_handled_by`, which holds the user's shortcut (`mmb` on all 2023 folders but one). The user name is accepted too. |
| Documents (PDF) | HTML templates rendered with OpenHTMLtoPDF, on demand, with Swiss QR-bills. |
| Legacy ticket link tables | Migrated: each slip now records the invoice or advance request it was issued for (`tick_invo_id`, `tick_req_id`, Liquibase changes 0003 and 0004). The legacy tables are kept read-only until the PHP application is retired. |
