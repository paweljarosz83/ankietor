# AGENTS.md — Ankietor

Standing contract for AI agents working in this repository. Read every session.
Deliberately short: durable project rules only. Task-specific context belongs in the prompt.

Reference documents (read on demand, not preloaded):
- `context/foundation/prd.md` — what is built, for whom, MVP scope, what is out of scope
- `context/foundation/tech-stack.md` — technical choices with justification
- `context/foundation/infrastructure.md` — deployment target (not written yet)

---

## What this application does

Suggests answers to client questionnaire questions by matching a new question against
previously answered ones and ranking candidates by similarity. Polish-language domain.

The core abstraction is `QuestionMatcher` with three implementations selected by configuration.
Do not bypass it. Any matching logic goes behind that interface.

## Stack

Java 21, Spring Boot 3.5.x, Maven, PostgreSQL 16+, Flyway, Spring Data JPA,
Spring Security (form login), Thymeleaf, AdminLTE 4 (`dist`, no npm), JUnit 5 + MockMvc.

## Verification — run before claiming a task is done

```
mvn -B clean verify
```

Never report a task complete without running it. If it cannot run (no database reachable),
say so explicitly instead of assuming success.

## Code conventions

Package root: `pl.ankietor`

```
pl.ankietor
  config/           Spring configuration, SecurityConfig
  security/         User, Role, UserDetails implementation, auth controllers
  knowledge/        QuestionAnswer entity, repository, service, controllers
  matching/         QuestionMatcher interface + implementations
  common/           shared utilities, exception handling
```

Within a domain package: `controllers/`, `services/`, `models/`, `dtos/`.

**Deliberately excluded — do not introduce:**

- DAO classes alongside Spring Data repositories. Use the repository interface directly.
- `Service` interface plus single `ServiceImpl`. Write one concrete class.
  Introduce an interface only when a second implementation genuinely exists.
- Separate `Validator` classes. Use Bean Validation annotations on DTOs.
- Lombok. Use Java records for DTOs and plain classes for entities.

**Required:**

- Constructor injection. No `@Autowired` on fields.
- `record` for DTOs and matcher results.
- Entities: plain classes, explicit getters/setters, no business logic.
- Bean Validation annotations on all DTOs reaching a controller.
- `Optional` for repository lookups that may miss; never return null from a service.

## Database

- Every schema change is a Flyway migration in `src/main/resources/db/migration`.
  Naming: `V<n>__snake_case_description.sql`.
- `spring.jpa.hibernate.ddl-auto=validate`. Never change this.
- The base schema must not depend on `pgvector`. The extension, vector column and
  similarity index live in a separate optional migration applied only when semantic
  mode is enabled. Keep it out of the default migration path.
- Lexical matching relies on `pg_trgm` and `unaccent`. Both are `contrib` extensions.

## Language policy

- Code, identifiers, commits, branches, this file: **English**.
- UI text, validation messages, anything a user reads: **Polish**.
- Seed data (questions and answers): **Polish** — the domain is Polish.
- Documents in `context/`: **Polish**.

## Data — hard rule

All questions and answers in this repository are **synthetic**. The domain mirrors a real
one (certificates, ISO, insurance, NDA, registration data) but the content is invented.

Never add real company data, real client data, or content copied from actual questionnaires.
Never add credentials, internal hostnames, or connection strings to tracked files.
Configuration values come from environment variables; `application.properties` holds
placeholders and defaults only.

## Boundaries — stop and ask

- Do not add a Maven dependency without recording it in `tech-stack.md` with a reason.
- Do not touch `pom.xml` parent version or `java.version`.
- Do not widen MVP scope. `prd.md` section 4 lists what is deliberately excluded;
  if a task seems to require one of those items, stop and say so.
- Do not run destructive database commands. Migrations only.
- Do not commit or push unless asked.

## Working style

- Explore and read before editing. A large diff on a small request is a defect.
- State the verification command you ran and its actual result. Not "should work".
- When a change spans more than three files, propose a plan first.
