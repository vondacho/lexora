# Lexora front end

Angular 22 application of Lexora, built from the clickable mockup (blue style, sidebar that can be hidden, drawer
menu on tablets and phones). It talks to the REST API of `lexora-infrastructure` only through a client generated from
the OpenAPI contract.

Built so far:

| Screen | Route | Content |
|---|---|---|
| Connexion | `/connexion` | HTTP Basic login, checked against `GET /api/me` |
| À traiter | `/a-traiter` | Widgets, each shown only with its grant: overdue invoices and requests to recall (`invoicing:debtors_open`), folders to invoice (same grant), figures of the month with the VAT to put aside (`booking:overview`), folder search (`invoicing:folder_search`) |
| Dossiers | `/dossiers` | Register of the folders: by name (any word) or number, by client (API search), type, status and person in charge, sorted by name, number or opening, 50 per page; opening a folder with its first client (status « En cours » and the user's initials by default) |
| Dossier | `/dossiers/:id` | Folder details changed (« Modifier ») or the folder deleted with everything it holds (its name typed to confirm); clients added, changed, their mandate ended, or removed when entered by mistake (a client with invoices, requests or payments is kept); header and summary (to invoice, available advances, not covered, owed by the clients); timeline by month with filters (sessions, invoices & requests, cash); quick entry of a session; side panel to issue an invoice (shares between two clients with a slider, 5 centimes), request an advance, record a payment received outside the bank file (oldest documents first, surplus as a spontaneous advance), and the detail of each event with its actions: PDF with the chosen letter template, reminder, deletion of an open session, cancellation of an advance or a payment; clients with what each still owes |
| Débiteurs & rappels | `/debiteurs?document=F-12` | Overdue invoices and requests, the longest overdue first; the reminder letter with the template of its number (1st, 2nd…), its text editable for this reminder only with fields inserted at the cursor, one letter per client or a single one, attachments; a preview filled like the PDF (same placeholders, one copy per client with its QR-bill amount); "create and print" records the reminder for what is still due and opens its PDF, then proposes the next unpaid document |
| Dépenses | `/depenses`, `/depenses?onglet=ordres` | Office payments month by month (previous / next month), filters Normal / Frais / Ordres / À payer (unpaid ones of every month); tiles: spending of the month, VAT included (not deductible at the net tax rate), left to pay, monthly standing orders; editor of one-off payments (VAT included computed as the API does, paid today / not yet paid); payments booked by a standing order are read-only and lead to their order; standing orders created, changed, suspended or deleted; the next month's orders booked in one click after a confirmation, for the month the API proposes |
| Décompte TVA | `/tva` | Net tax rate method: first / second semester, year or free period, year by year; money received (invoice payments and advances) × rate = VAT due, with the filing deadline (two months after the period) and a status (running, to pay, late, paid); the rate can be changed (booking settings) while the period is unpaid; recording a payment freezes the money received and the rate, warns when the books changed since, and refuses periods overlapping a paid one; month by month with the amount to put aside; VAT paid history; incomes of the period; period statement PDF; figures copied for the AFC form |
| Synthèses | `/syntheses`, `?onglet=personnes`, `?onglet=releves` | Monthly overview of a year (advances, invoice payments, incomes, paid expenses, balance, bars), totals, average over the months elapsed, comparison with the same months of the year before, VAT due at the net tax rate; per person (folders handled, honoraries, money received and share, for the year, last year or the last 12 months: everyone with `admin:users`, otherwise one's own figures); period statements with their figures and the PDF |
| Modèles de lettres | `/modeles` | Templates grouped by document type; name, printed title, use, intended recipients, pages of each copy (letter, itemised statement for invoices; the legacy layout names are kept); fields inserted at the cursor, unknown fields and missing amounts flagged; formatted text (TipTap: bold, italic, underline, lists, alignment) or plain text — plain-text templates stay plain until converted, as the legacy application would print HTML tags; preview filled with sample data, copy by copy, filled fields highlighted; duplicate, delete; unsaved changes are not lost by picking another template |
| Encaissements | `/encaissements` | Import of the camt.054 notification of the bank (drop or choose the file): each credit matched by its reference, suggested by amount and name, or associated by hand with an open document; overpayments and suggestions to confirm; transactions already imported shown and never booked twice; foreign currencies left aside. `python3 ../tools/sample-camt054.py` writes a sample file paying the open documents of the demo data |

## Run

Node 22.22+ or 24.15+ (`nvm use` picks the version of `.nvmrc`; the `node` 20 on the PATH is too old). The API must run on port 8080, e.g. on H2 from the repository root:
`java -jar lexora-infrastructure/target/lexora.jar --spring.profiles.active=h2`.

```bash
npm install
npm start          # http://localhost:4200, log in as demo / demo
npm test           # unit tests (Vitest)
npm run build      # production build in dist/lexora-frontend
```

`ng serve` forwards `/api` to `http://localhost:8080` (`proxy.conf.json`), so the browser sees one origin. When the
front end is served from another origin, add it to `lexora.security.allowed-origins` of the API.

## API client

`npm run generate:api` (run automatically before `start`, `build` and `test`) generates the Angular services and models
from `../lexora-infrastructure/src/main/resources/static/openapi/lexora_openapi_v1.yaml` into
`src/app/api/generated` with openapi-generator 7.26 (`typescript-angular`, settings in `openapitools.json`). The folder
is not versioned: change the contract, regenerate, and the compiler points at what to adapt. Java is needed to run the
generator.

## Authentication

- The login page sends the credentials to `GET /api/me`; once accepted they are kept in `sessionStorage` (lost when the
  tab is closed) and added to every `/api` call by `authInterceptor`.
- Calls carry `X-Requested-With: XMLHttpRequest`, so the API answers 401 without `WWW-Authenticate` and the browser never
  shows its own login popup.
- A 401 during a session (password changed, user removed) ends the session and returns to the login page, which brings
  the user back to where they were.
- Amounts are computed in centimes with the rounding of the API (`shared/money.ts`): VAT truncated to 10 centimes,
  shares rounded to 5 centimes. Invoices and requests are always issued with shares: what each client owes, the payment
  form and the QR-bills work per client.
- Grants come with the user (`module:page`); `AuthService.hasGrant` decides which widgets to show. The API still checks
  every call.

## Error messages

The API returns a `code` and `params` with each error; `shared/errors.ts` holds the French and English text of every
code (`Record<Problem.CodeEnum, …>`: a code added to the contract does not compile until translated), and
`problemMessage(error, fallback)` gives the text to show. The English `detail` of the API is never shown.

## Languages

The interface is in French or English, switched by the FR | EN button (header, menu drawer and login page); the choice
is kept in the browser (`localStorage`, key `lexora.lang`). French is the default.

- `i18n/i18n.ts`: the `lang` signal and `texts`, the texts of the current language. Components expose
  `protected readonly tx = texts` and templates read `tx().section.key`; texts with values are functions
  (`tx().folder.page.openSessions(n)`). Rules that build texts (timeline, bank matching…) read `texts()` inside
  `computed`, so everything follows a switch without reload.
- Each feature has its `*.i18n.ts` with `xxxFr` and `xxxEn: typeof xxxFr`: a text missing in English does not compile.
  `messages.fr.ts` / `messages.en.ts` gather them; `common.i18n.ts` holds the shared words and the month names.
- Page titles come from the route's `data.titleKey` (`TranslatedTitleStrategy`); error codes have a French and an
  English table in `shared/errors.ts`.
- What is printed stays French: letters, the letter preview and the placeholders' sample values, like the PDFs the
  API produces. Data (folder names, wordings, statuses) is shown as entered.

## Layout

```
src/app/
  api/generated/   client generated from the contract (not versioned)
  core/            AuthService, authInterceptor, authGuard
  shell/           header, sidebar / drawer, navigation entries
  i18n/            language switch, texts of both languages, page titles
  shared/          formatting of amounts (1'230.40) and dates (24.10.2026)
  features/        one folder per screen: login, home (widgets and their rules in home.model.ts), folders, folder, …
                   folder/: FolderStore (data, reloaded after each change), timeline.model.ts, shares.ts and
                   payment.model.ts (the rules, unit-tested), one component per panel
```

Styles: design tokens and shared classes (buttons, cards, widget rows) in `src/styles.css`, the rest in each component.
Below 900 px the sidebar becomes a drawer; below 640 px the header keeps only icons.
