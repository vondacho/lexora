# Database migrations

The schema is versioned with **Liquibase**, run by Spring Boot at startup before Hibernate.

```
lexora-infrastructure/src/main/resources/db/changelog/
├── db.changelog-master.yaml                  includes every change, in order
└── changes/
    ├── 0001-legacy-schema.yaml               baseline: the schema inherited from the PHP application
    ├── 0002-invo-models-single-page.yaml     columns added by the PHP application in 2024
    ├── 0003-tickets-issued-for-documents.yaml slips record the invoice / request they were issued for
    ├── 0004-migrate-legacy-ticket-links.yaml  copies invo_invoices_tickets / invo_requests_tickets into them
    ├── 0005-vat-payments.yaml                 table book_vat_payments; grant booking:vat_payment to tax_summary holders
    ├── 0006-vat-payment-expense.yaml          book_vat_payments.vat_expense_id: the office expense booked for each VAT payment
    ├── 0007-bank-entries.yaml                 table invo_bank_entries: bank transactions already imported from camt.054
    └── sql/
        ├── 0001-legacy-schema-mysql.sql      legacy DDL (2023 dump, MyISAM), for MySQL / MariaDB
        └── 0001-legacy-schema-h2.sql         H2 equivalent, for tests and local development
```

Liquibase records what it applied in two tables it creates in the same database: `DATABASECHANGELOG` and
`DATABASECHANGELOGLOCK`. The PHP application ignores them.

## How the baseline works

| Database | `0001` legacy schema | `0002` single page columns |
|---|---|---|
| Production (tables already there) | marked as applied (`MARK_RAN`), nothing executed | marked as applied, nothing executed |
| Copy of the 2023 dump | marked as applied | executed: adds the columns |
| Empty database | executed: creates the 24 tables | executed |

The checks query `information_schema` directly (`sqlCheck`), so they behave the same on MySQL and H2.
`LiquibaseBaselineTest` proves the first line: on an existing legacy database, both change sets are only recorded,
the data is untouched, and a second run does nothing.

## First start on the production database

1. Back up the database.
2. The database user needs `CREATE`, `ALTER` and `INDEX` privileges for the first start (Liquibase creates its two
   tables) and for any later structural change.
3. Start the application: the log shows the two change sets marked as run. Check with
   `SELECT id, exectype FROM DATABASECHANGELOG;`
4. To start without migrating (e.g. a read-only user), set `LEXORA_LIQUIBASE_ENABLED=false`.

## Local H2 database

The `h2` profile applies `db/changelog-h2.yaml`: the master changelog, then the demo data, H2 only:

| Change set | Content |
|---|---|
| `h2-demo-data` | `db/h2/demo-data.sql`: demo user, defaults, folder Dupont c. Dupont, letter templates |
| `h2-demo-vat-payment-grant` | grant `booking:vat_payment` of the demo user |
| `h2-demo-folders` | `db/h2/demo-folders.sql`: folders for the home page (overdue invoices and requests, folders to invoice, payments, advances, office books), dated relative to the creation day; skipped (marked as run) when one of its folder names already exists |

| `h2-demo-shares` | `db/h2/demo-shares.sql`: shares of the single-customer demo invoices and request, which `h2-demo-folders` created without (statements, receipts and QR-bills work per customer) |
| `h2-demo-recall-templates` | `db/h2/demo-recall-templates.sql`: a second reminder and a formal notice; skipped when templates with these names exist |

Change sets already applied are never edited: their checksum would no longer match on existing local databases. Add a
new change set instead. Tests use `db/changelog-test.yaml` (master changelog + test fixtures) on an
in-memory H2 database. See the README to run locally.

## Adding a change

1. Create `changes/NNNN-short-description.yaml` with one or more change sets (`id: NNNN-...`, `author: <your name>`).
   Prefer Liquibase changes (`addColumn`, `createIndex`, ...) to raw SQL; use `dbms` or changelog properties when MySQL
   and H2 need different types (see `0002`, which maps a flag to `tinyint(1)` on MySQL and `boolean` on H2).
2. Include it at the end of `db.changelog-master.yaml`.
3. Update the JPA entity; `mvn verify` validates the entities against the migrated H2 schema.
4. Never modify a change set once applied somewhere: add a new one. Liquibase rejects edited change sets (checksums).
5. While the PHP application still runs on the same database, keep changes backward compatible for it
   (add columns with defaults, do not rename or drop what it uses).

## Candidate changes

- Drop `invo_invoices_tickets`, `invo_requests_tickets` and `invo_recalls` once the PHP application is retired
  (their data was migrated by 0004).
- Convert the tables from MyISAM to InnoDB, to get transactions (see [data model](data-model.md#recommendations-schema-kept-as-is-for-now)).
- `DECIMAL(10,2)` for amounts, foreign keys once orphans are cleaned up.
