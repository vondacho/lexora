# Use cases

Use cases discovered in the PHP application (`mabecoju-php`), with where they now live in mabecoju v2.

- **Legacy** — the PHP page or action implementing it (`presentation/<module>/...`).
- **Use case** — the method of the application layer (`mabecoju-application`, package `ch.conciliations.mabecoju.application`).
- **Endpoint** — the REST resource exposing it, as defined in the OpenAPI contract (`static/openapi/mabecoju_openapi_v1.yaml`). All endpoints are under `/api` and need HTTP Basic authentication.
- **Grant** — the legacy page grant (`all_grants`) required, as `module:page`.

The legacy application has three modules: **invoicing** (folders and what customers owe), **booking** (the office books) and **admin** (users).

## Invoicing — folders

| Use case | Legacy | Application | Endpoint | Grant |
|---|---|---|---|---|
| Search folders by name, number or customer name, optionally by type | `page_folder_search`, `page_folder_search_result` | `FolderUseCases.searchFolders` | `GET /folders?name=&number=&customer=&type=` | `invoicing:folder_search` |
| Open a folder with its first customer (the folder number is its id) | `page_folder_create`, `action_create_folder` | `FolderUseCases.openFolder` | `POST /folders` | `invoicing:folder_create` |
| Display a folder | `page_folder_display` | `FolderUseCases.getFolder` | `GET /folders/{id}` | `invoicing:folder_display` |
| Modify a folder | `page_folder_modify`, `action_modify_folder` | `FolderUseCases.updateFolder` | `PUT /folders/{id}` | `invoicing:folder_modify` |
| Delete a folder and everything it holds: invoices, requests, advances, honoraries, payment slips, booking entries, standing orders, customers | `action_delete_folder` | `FolderUseCases.deleteFolder` | `DELETE /folders/{id}` | `invoicing:folder_modify` |
| List / add / remove folder statuses | `page_folder_status_modify`, `action_create_folder_status`, `action_delete_folder_status` | `FolderUseCases.listStatuses`, `addStatus`, `deleteStatus` | `GET/POST /folder-statuses`, `DELETE /folder-statuses/{id}` | `invoicing:folder_status_modify` |

## Invoicing — customers (parties of a folder)

| Use case | Legacy | Application | Endpoint | Grant |
|---|---|---|---|---|
| List the customers of a folder, or those active at a date | `page_folder_display` | `CustomerUseCases.listCustomers`, `listActiveCustomers` | `GET /folders/{id}/customers?activeAt=` | `invoicing:customer_display` |
| Add a customer | `page_customer_create`, `action_create_customer` | `CustomerUseCases.addCustomer` | `POST /folders/{id}/customers` | `invoicing:customer_create` |
| Display / modify / remove a customer | `page_customer_display`, `page_customer_modify`, `action_modify_customer`, `action_delete_customer` | `CustomerUseCases.getCustomer`, `updateCustomer`, `removeCustomer` | `GET/PUT/DELETE /customers/{id}` | `invoicing:customer_display` / `customer_modify` |

## Invoicing — honoraries (billable services)

| Use case | Legacy | Application | Endpoint | Grant |
|---|---|---|---|---|
| List the honoraries of a folder (all, open or invoiced) | `page_folder_content` | `HonoraryUseCases.listHonoraries` | `GET /folders/{id}/honoraries?invoiced=` | `invoicing:folder_honoraries` |
| Record an honorary: minutes × hourly rate + up to 4 expenses | `page_honorary_create`, `action_create_honorary` | `HonoraryUseCases.recordHonorary` | `POST /folders/{id}/honoraries` | `invoicing:honorary_create` |
| Modify / delete an honorary (refused once invoiced) | `page_honorary_modify`, `action_modify_honorary`, `action_delete_honorary` | `HonoraryUseCases.reviseHonorary`, `deleteHonorary` | `PUT/DELETE /honoraries/{id}` | `invoicing:honorary_modify` |

## Invoicing — advance requests and advances

| Use case | Legacy | Application | Endpoint | Grant |
|---|---|---|---|---|
| Prepare a request: open honoraries, available advances, default delay | `page_request_create` | `AdvanceRequestUseCases.prepareRequest` | `GET /folders/{id}/advance-request-draft` | `invoicing:request_create` |
| Request a provision, split between customers | `action_create_request` | `AdvanceRequestUseCases.createRequest` | `POST /folders/{id}/advance-requests` | `invoicing:request_create` |
| List the requests of a folder (filter paid / invoiced) | `page_folder_content` | `AdvanceRequestUseCases.listRequests` | `GET /folders/{id}/advance-requests?paid=&invoiced=` | `invoicing:folder_requests` |
| Display / modify / delete a request (deleting also deletes its advances) | `page_request_display`, `page_request_modify`, `action_modify_request`, `action_delete_request` | `getRequest`, `reviseRequest`, `deleteRequest` | `GET/PUT/DELETE /advance-requests/{id}` | `invoicing:request_display` / `request_modify` |
| Settle a request at a date ("solder") / reopen it | `action_close_request`, `action_reopen_request` | `settleRequest`, `reopenRequest` | `POST/DELETE /advance-requests/{id}/settlement` | `invoicing:request_modify` |
| Send / cancel a reminder | `action_recall_request`, `action_delete_request_recall`, `page_request_recall_print` | `recallRequest`, `cancelRecall` | `POST /advance-requests/{id}/recalls`, `DELETE .../recalls/{recallId}` | `invoicing:request_modify` |
| Receive a spontaneous advance (cash, IBAN or payment slip) | `page_advance_receipt*`, `action_pay_advance` | `AdvanceUseCases.receiveAdvance` | `POST /folders/{id}/advances` | `invoicing:advance_receipt` |
| List the advances of a folder (available / invoiced) | `page_folder_content` | `AdvanceUseCases.listAdvances` | `GET /folders/{id}/advances?invoiced=` | `invoicing:folder_content` |
| Cancel an advance | `action_delete_advance` | `AdvanceUseCases.cancelAdvance` | `DELETE /advances/{id}` | `invoicing:advance_receipt` |

## Invoicing — invoices ("mémoires d'honoraires")

| Use case | Legacy | Application | Endpoint | Grant |
|---|---|---|---|---|
| Prepare an invoice: open honoraries, available advances, active customers, default delay and VAT | `page_invoice_create` | `InvoiceUseCases.prepareInvoice` | `GET /folders/{id}/invoice-draft` | `invoicing:invoice_create` |
| Issue an invoice: bills every open honorary, deducts every available advance | `action_create_invoice` | `InvoiceUseCases.issueInvoice` | `POST /folders/{id}/invoices` | `invoicing:invoice_create` |
| List the invoices of a folder | `page_folder_content` | `InvoiceUseCases.listInvoices` | `GET /folders/{id}/invoices` | `invoicing:folder_invoices` |
| Display an invoice, its honoraries and deducted advances | `page_invoice_display` | `getInvoice`, `invoicedHonoraries`, `deductedAdvances` | `GET /invoices/{id}`, `.../honoraries`, `.../advances` | `invoicing:invoice_display` |
| Modify an invoice (date, delay, discount, VAT, distribution) | `page_invoice_modify`, `action_modify_invoice` | `reviseInvoice` | `PUT /invoices/{id}` | `invoicing:invoice_modify` |
| Delete an invoice; its honoraries and advances become open again | `action_delete_invoice` | `deleteInvoice` | `DELETE /invoices/{id}` | `invoicing:invoice_modify` |
| Settle at a date ("solder") / reopen | `action_close_invoice`, `action_reopen_invoice` | `settleInvoice`, `reopenInvoice` | `POST/DELETE /invoices/{id}/settlement` | `invoicing:invoice_modify` |
| Send / cancel a reminder | `action_recall_invoice`, `action_delete_invoice_recall`, `page_invoice_recall_print` | `recallInvoice`, `cancelRecall` | `POST /invoices/{id}/recalls`, `DELETE .../recalls/{recallId}` | `invoicing:invoice_modify` |
| Record / cancel one payment | `action_delete_invoice_payment` | `recordPayment`, `cancelPayment` | `POST /invoices/{id}/payments`, `DELETE .../payments/{paymentId}` | `invoicing:debtor_receipt` / `invoice_modify` |
| List open / paid invoices | `page_debtors_open`, `page_debtors_payed` | `listOpenInvoices`, `listPaidInvoices` | `GET /invoices?status=open` (or `paid`) | `invoicing:debtors_open` / `debtors_payed` |

## Invoicing — payment slips and debtors

| Use case | Legacy | Application | Endpoint | Grant |
|---|---|---|---|---|
| Register a payment slip (typed or scanned reference; known references are reused), optionally for an invoice or a request | `page_ticket_create`, `action_create_ticket` | `TicketUseCases.registerTicket` | `POST /folders/{id}/tickets` | `invoicing:folder_modify` |
| List / display the slips of a folder | `page_folder_display` | `listTickets`, `getTicket` | `GET /folders/{id}/tickets`, `GET /tickets/{id}` | `invoicing:folder_content` |
| Correct a slip number | `action_modify_invoice_ticket` | `renumberTicket` | `PATCH /tickets/{id}` | `invoicing:invoice_modify` |
| Delete an unused slip | `action_delete_ticket` | `deleteTicket` | `DELETE /tickets/{id}` | `invoicing:folder_modify` |
| Find the debtor of a slip and what was paid with it | `page_debtor_search*`, `page_debtor_search_by_bvr_result` | `DebtorUseCases.lookupTicket` | `GET /debtors/lookup?reference=` | `invoicing:debtor_search` |
| Statement of a customer: open invoices and requests with share, paid, remaining | `page_folder_receipt`, `page_debtor_receipt*` | `DebtorUseCases.statement` | `GET /folders/{id}/customers/{customerId}/statement` | `invoicing:debtor_receipt` |
| Receive what a customer paid over several invoices and requests at once | `action_pay_ticket` | `DebtorUseCases.receivePayments` | `POST /folders/{id}/customers/{customerId}/receipts` | `invoicing:debtor_receipt` |
| Debtors overview: folders to invoice, open invoices and requests with days overdue and reminders | `page_debtors_open`, `page_debtors_recall` | `DebtorUseCases.overview` | `GET /debtors/overview` | `invoicing:debtors_open` |

## Invoicing — templates and settings

| Use case | Legacy | Application | Endpoint | Grant |
|---|---|---|---|---|
| Maintain document models (invoice, request, recall letters) | `page_models*`, `page_model_create/modify`, `action_*_model` | `TemplateUseCases.*Model` | `GET/POST /document-models`, `GET/PUT/DELETE /document-models/{id}` | `invoicing:models` |
| Maintain honorary wordings | `page_wordings_modify`, `action_create/delete_wording` | `TemplateUseCases.*Wording` | `GET/POST /wordings`, `DELETE /wordings/{id}` | `invoicing:wordings_modify` |
| Change invoicing defaults (hourly rates, delays, VAT number and rate) | `invoicing/page_parameters_modify`, `action_modify_parameters` | `SettingsUseCases.changeInvoicingSettings` | `GET/PUT /settings/invoicing` | `invoicing:parameters_modify` |

## Booking — office books

| Use case | Legacy | Application | Endpoint | Grant |
|---|---|---|---|---|
| List / record / modify / delete outgoing payments of the office | `booking/page_payments*`, `action_*_payment` | `BookingUseCases.listPayments`, `recordPayment`, `updatePayment`, `deletePayment` | `GET/POST /booking/payments`, `GET/PUT/DELETE /booking/payments/{id}` | `booking:payments_list`, `payment_create`, `payment_modify`, `payment_display` |
| Same for the books of a tutelage folder | `invoicing/page_payment_*`, `invoicing/action_*_payment` | same, with a folder | `GET/POST /folders/{id}/booking/payments` | idem |
| Maintain standing orders | `page_orders*`, `action_*_order` | `BookingUseCases.*Order` | `GET/POST /booking/standing-orders`, `/folders/{id}/booking/standing-orders`, `GET/PUT/DELETE /booking/standing-orders/{id}` | `booking:orders_list`, `order_create`, `order_modify` |
| Next month to execute | `page_orders` | `nextExecutionMonth` | `GET /booking/standing-orders/next-execution?folderId=` | `booking:orders` |
| Execute the standing orders of a month | `action_execute_orders` | `executeOrders` | `POST /booking/standing-orders/executions` | `booking:order_modify` |
| Tax summary / incomes of a period (invoice payments + advances, VAT owed at the net tax rate) | `page_tax_summary*`, `layout_inputs_from_to` | `ReportingUseCases.incomeReport` | `GET /reports/income?from=&to=` | `booking:tax_summary` |
| Expenses of a period | `layout_outputs_from_to` | `ReportingUseCases.expenseReport` | `GET /reports/expenses?from=&to=` | `booking:tax_summary` |
| Period statements as PDF: incomes with the VAT owed, then expenses ("relevés") | `action_generate_reports` | `ReportingUseCases.printPeriodStatement` | `GET /reports/period-statement?from=&to=` | `booking:tax_summary` |
| Monthly overview of incomes and expenses | `page_overview` | `ReportingUseCases.monthlyOverview` | `GET /reports/monthly-overview` | `booking:overview` |
| Statistics of the folders a user handles (`fold_handled_by` = his shortcut) | `page_statistics_search*` | `ReportingUseCases.userStatistics` | `GET /reports/user-statistics?username=&from=&to=` | `booking:statistics_search` |
| Change booking defaults (net VAT rate) | `booking/page_parameters_modify` | `SettingsUseCases.changeBookingSettings` | `GET/PUT /settings/booking` | `booking:parameters_modify` |
| Record / list / delete the VAT paid for a period (new in v2); recording also books the amount as a paid office expense to the AFC, deleting removes it | — | `VatUseCases.recordPayment`, `listPayments`, `deletePayment` | `GET/POST /booking/vat-payments`, `DELETE /booking/vat-payments/{id}` | `booking:tax_summary` (list), `booking:vat_payment` |
| Import a bank notification (camt.054): match each credit by its QR or creditor reference (the slip registered when printing, or the reference itself), or suggest a document by amount and payer name; record the confirmed ones (surplus as a spontaneous advance, transactions already imported skipped) (new in v2) | — | `BankImportUseCases.analyse`, `record` | `POST /bank-statements/analysis`, `POST /bank-statements/receipts` | `invoicing:debtor_receipt` |

## Admin

| Use case | Legacy | Application | Endpoint | Grant |
|---|---|---|---|---|
| Log in | `login/action_login` | Spring Security (`LegacyUserDetailsService`) | HTTP Basic on every call | — |
| Who am I, with my grants | session bean | `UserUseCases.getUser` | `GET /me` | authenticated |
| List / create / modify / delete users (deleting removes the grants) | `admin/page_users*`, `action_*_user` | `UserUseCases.*User` | `GET/POST /users`, `GET/PUT/DELETE /users/{username}` | `admin:users` |
| Grant / revoke one page, all pages | `admin/action_*_grant*`, `cmd_*_grant*` | `grant`, `revoke`, `grantAll`, `revokeAll` | `PUT/DELETE /users/{username}/grants[/{module}/{page}]` | `admin:users` |
| Catalogue of grants | `business/grants.php` | `grantCatalog` | `GET /grants` | `admin:users` |

## Invoicing — documents

| Use case | Legacy | Application | Endpoint | Grant |
|---|---|---|---|---|
| Print an invoice (letter and/or statement, QR-bill per customer) | `page_invoice_print`, `action_print_invoice` | `DocumentUseCases.printInvoice` | `POST /invoices/{id}/document` | `invoicing:invoice_display` |
| Print a reminder of an invoice | `page_invoice_recall_print`, `action_print_invoice_recall` | `printInvoiceRecall` | `POST /invoices/{id}/recalls/{recallId}/document` | `invoicing:invoice_display` |
| Print an advance request | `page_request_print`, `action_print_request` | `printAdvanceRequest` | `POST /advance-requests/{id}/document` | `invoicing:request_display` |
| Print a reminder of an advance request | `page_request_recall_print`, `action_print_request_recall` | `printAdvanceRequestRecall` | `POST /advance-requests/{id}/recalls/{recallId}/document` | `invoicing:request_display` |

See [documents](documents.md).

## Not ported yet

| Legacy feature | Notes |
|---|---|
| Database backup (`backup/dump.php`) and file manager (`utils/tinyfilemanager`) | Operational tooling rather than use cases; better handled outside the application. |
