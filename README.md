# Lexora

Back office of the office: client folders, fees ("honoraires"), provision requests, invoices, payment follow-up and the
office books. Rewrite of the PHP application `mabecoju-php` with Java / Spring Boot, on the **same MySQL database**.
This repository holds the REST API and, in `lexora-frontend`, the Angular front end (in progress).

## Architecture

Hexagonal (ports and adapters), one Maven module per layer so that the compiler enforces the dependency direction:

```
lexora-infrastructure  ──>  lexora-application  ──>  lexora-domain
 Spring Boot, JPA, REST,       use cases                  aggregates, value objects,
 security, configuration       (plain Java +              business rules, repository ports
                               jakarta @Transactional)    (plain Java)
```

| Module | Content |
|---|---|
| `lexora-domain` | Domain model by bounded area: `folder`, `customer`, `honorary`, `invoice`, `advance`, `ticket`, `billing`, `booking`, `template`, `settings`, `user`, `shared`. Repository interfaces (ports) live next to their aggregate. No framework. |
| `lexora-application` | Use cases, one class per area (`InvoiceUseCases`, `DebtorUseCases`, `BookingUseCases`, ...), each public method being one use case. Read models for statements, overviews and reports. |
| `lexora-frontend` | Angular 22 application (npm project, not a Maven module): see [its README](lexora-frontend/README.md). |
| `lexora-infrastructure` | `persistence`: JPA entities on the legacy tables, Spring Data repositories, adapters implementing the domain ports. `web`: REST controllers implementing the API interfaces generated from the contract. `security`: HTTP Basic on `all_users`, grants as authorities. `config`: wiring of the use cases. |

### API contract first

The REST API is defined by the OpenAPI contract
[`lexora-infrastructure/src/main/resources/static/openapi/lexora_openapi_v1.yaml`](lexora-infrastructure/src/main/resources/static/openapi/lexora_openapi_v1.yaml).
At build time, `openapi-generator-maven-plugin` (generator `spring`, Spring Boot 4 / Jackson 3) generates from it, into
`target/generated-sources/openapi`:

- one interface per tag in `io.lexora.infrastructure.web.api` (`FoldersApi`, `InvoicesApi`, ...) carrying
  the Spring MVC mappings, status codes and bean validation of the contract;
- the request and response models in `...web.api.model`, suffixed `Dto`.

Each controller implements one generated interface, adds its `@PreAuthorize` grant and maps between the DTOs and the
domain. To change the API: edit the contract, run `mvn compile`, and implement the new or changed methods (the compiler
points at them). The same file is served at `/openapi/lexora_openapi_v1.yaml` and rendered by Scalar at `/scalar`.

Errors are RFC 9457 problem details. Besides the English `detail` (for logs and developers), each one carries a stable
`code` and its `params`, which clients translate, e.g.
`{"status": 422, "code": "VAT_PERIOD_OVERLAP", "params": {"from": "2026-01-01", "to": "2026-06-30", "paidOn": "2026-08-28"}, "detail": "The VAT from …"}`.
The codes are listed in `ErrorCode` (domain) with the meaning of their parameters, and in the contract (`Problem.code`);
`ErrorCodeContractTest` keeps both lists equal. A business rule throws `new DomainException(ErrorCode.X, "English
message").with("param", value)`; a new code must be added to the contract and translated by the front end, which does
not compile until it is.

ArchUnit tests (`ArchitectureTest`) check that the domain and application layers stay free of Spring and JPA.

Documentation:
- [Use cases](docs/use-cases.md) — every legacy page/action mapped to its use case, endpoint and grant.
- [Domain model](docs/domain-model.md) — aggregates and business rules.
- [Data model](docs/data-model.md) — tables, JPA entities, legacy conventions, schema discrepancies.
- [Differences with the PHP application](docs/legacy-behaviour.md) — deliberate changes, PHP defects, open questions.
- [Documents](docs/documents.md) — PDF invoices, requests and reminders with Swiss QR-bills.
- [Database migrations](docs/database-migrations.md) — Liquibase changelog, starting from the legacy MySQL schema.

## Stack

Java 21 (builds and runs on 25), Spring Boot 4.1, Spring Data JPA / Hibernate 7, Liquibase 5, OpenHTMLtoPDF and the Swiss QR-bill generator (PDF documents), Spring Security 7, MySQL Connector/J,
openapi-generator 7.26 (contract first), Scalar (API reference); tests with JUnit 6, AssertJ, MockMvc, H2, ArchUnit.

## Build and test

```bash
mvn verify
```

The tests run on an in-memory H2 database created from an H2 copy of the legacy schema; Hibernate validates every
entity against it.

## Run locally (H2)

No MySQL needed: the `h2` profile runs on a file H2 database that Liquibase creates with the production changelog,
followed by demo data. QR-bills use the sample QR-IBAN of the Swiss guidelines.

| Demo data | |
|---|---|
| User | `demo` / `demo`, every grant |
| Settings | tariffs 200 / 150 per hour, VAT 8.1 %, net tax rate 5.9 %, letter templates for invoices and requests, three reminder templates (reminder, second reminder, formal notice) |
| Overdue invoices | Keller (one reminder sent), Meyer, Bernasconi |
| Overdue advance requests | Favre (half paid by one of the two customers), Girard |
| Folders to invoice | Rochat c. Rochat, Martin & Rossi (partly covered by a cash advance), Dupont c. Dupont, Bernasconi |
| Paid invoices | Pittet c. Pittet (bank transfer), Hug (cash, this month) |
| Office books | standing orders for the rent and the insurance with their payments of this month and the last, phone bills, office supplies, an unpaid membership fee |

The dates are relative to the day the database is created: the cases are overdue and the current month has figures.
Delete `~/.lexora/h2` to recreate the data with today's dates.

```bash
mvn install -DskipTests                       # once, and after changes
java -jar lexora-infrastructure/target/lexora.jar --spring.profiles.active=h2
# or: mvn -pl lexora-infrastructure spring-boot:run -Dspring-boot.run.profiles=h2
```

| | |
|---|---|
| API reference (try requests with `demo` / `demo`) | http://localhost:8080/scalar |
| H2 console | http://localhost:8080/h2-console — JDBC URL `jdbc:h2:file:~/.lexora/h2/lexora;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=TRUE`, user `sa`, no password |
| Database files | `~/.lexora/h2` (`LEXORA_H2_DIR`); delete the folder to start again from scratch |

```bash
curl -u demo:demo http://localhost:8080/api/folders
curl -u demo:demo http://localhost:8080/api/folders/1/invoice-draft
```

With the API running, start the front end and log in as `demo` / `demo` at http://localhost:4200
(see [lexora-frontend/README.md](lexora-frontend/README.md)):

```bash
cd lexora-frontend && npm install && npm start
```

To try the bank import ("Encaissements"), write a sample camt.054 file paying the open documents of the running API:
`python3 tools/sample-camt054.py` (writes `camt054-demo.xml`; options `--url`, `--user`, `--password`, `--out`).

## Run on MySQL

```bash
export LEXORA_DB_HOST=127.0.0.1 LEXORA_DB_NAME=lexora LEXORA_DB_USER=... LEXORA_DB_PASSWORD=...
java -jar lexora-infrastructure/target/lexora.jar
```

| Variable / property | Default | |
|---|---|---|
| `LEXORA_DB_HOST`, `LEXORA_DB_PORT`, `LEXORA_DB_NAME` | `127.0.0.1`, `3306`, `lexora` | MySQL database of the PHP application |
| `LEXORA_DB_USER`, `LEXORA_DB_PASSWORD` | `lexora`, empty | |
| `lexora.security.legacy-password-hashing` | `true` | Keep MD5 for new passwords while the PHP application is in use. Set to `false` afterwards: new passwords use bcrypt and MD5 hashes are upgraded at login. |
| `lexora.security.allowed-origins` | `http://localhost:4200` | CORS origins of the Angular front end |
| `LEXORA_LIQUIBASE_ENABLED` | `true` | Run the database migrations at startup |
| `LEXORA_QR_BILL_IBAN`, `..._CREDITOR_NAME`, `..._STREET`, `..._HOUSE_NUMBER`, `..._POSTAL_CODE`, `..._TOWN` | empty | Creditor of the QR-bills; no QR-bill while the IBAN is empty. See [documents](docs/documents.md). |
| `LEXORA_DOCUMENTS_FONT_FILE` | empty | TrueType font embedded in the documents (Helvetica otherwise) |
| `lexora.time-zone` | `Europe/Zurich` | Defines "today" for due dates and default dates |

- API reference: `http://localhost:8080/scalar`, rendered by Scalar from the contract served at `/openapi/lexora_openapi_v1.yaml`
  (usable to generate the Angular client). Both are public.
- Health: `/actuator/health`.
- Every `/api/**` call needs HTTP Basic credentials of an `all_users` account; each endpoint requires the same page grant as
  the legacy page (`GET /api/me` returns the user's grants).

Example:

```bash
curl -u user:password http://localhost:8080/api/folders?name=Dup
curl -u user:password http://localhost:8080/api/debtors/overview
```

## Before production

1. Compare the production schema (`SHOW CREATE TABLE`) with the baseline
   [`0001-legacy-schema-mysql.sql`](lexora-infrastructure/src/main/resources/db/changelog/changes/sql/0001-legacy-schema-mysql.sql), and read
   [what happens at the first start](docs/database-migrations.md#first-start-on-the-production-database).
2. Consider converting the tables from MyISAM to InnoDB: without it, multi-table operations are not atomic.
3. Answer the [open questions](docs/legacy-behaviour.md#open-questions).

## Next steps

- Angular front end; replace HTTP Basic with a token or session-based login at that point.
