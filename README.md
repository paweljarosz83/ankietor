# Ankietor

Narzędzie do odpowiadania na powtarzające się pytania z ankiet klienckich. Wklejasz
pytanie z otrzymanej ankiety, aplikacja rozpoznaje, że dotyczy tej samej sprawy co
pytanie odpowiedziane wcześniej, i proponuje jego odpowiedź z liczbową miarą
podobieństwa.

Projekt zaliczeniowy **10xDevs 3.0**.

---

## Problem

Pytania od różnych klientów powtarzają się merytorycznie, ale są inaczej sformułowane.
„Czy posiadacie certyfikat ISO 9001?" i „Proszę wskazać wdrożone systemy zarządzania
jakością" to ta sama sprawa i zero wspólnych słów kluczowych. W efekcie każda ankieta
jest wypełniana od zera, a odpowiedzi bywają niespójne między klientami.

## Stack

Java 21 · Spring Boot 3.5 · PostgreSQL 15 · Flyway · Thymeleaf · AdminLTE 4 · Maven

Uzasadnienie każdego wyboru: [`context/foundation/tech-stack.md`](context/foundation/tech-stack.md)

## Dokumentacja projektowa

| Dokument | Zawartość |
|---|---|
| [`context/foundation/prd.md`](context/foundation/prd.md) | Problem, użytkownik, zakres MVP, kryteria sukcesu, ryzyka |
| [`context/foundation/tech-stack.md`](context/foundation/tech-stack.md) | Decyzje techniczne z uzasadnieniem i pomiarami |
| [`context/foundation/roadmap.md`](context/foundation/roadmap.md) | Sekwencja pracy: foundations, slice’y, zależności, parking |
| [`context/foundation/infrastructure.md`](context/foundation/infrastructure.md) | Wybór platformy, testy anti-bias, rollback, ryzyka |
| [`context/foundation/test-plan.md`](context/foundation/test-plan.md) | Mapa ryzyk, bramki jakości, cookbook testów |
| [`context/deployment/deploy-plan.md`](context/deployment/deploy-plan.md) | Procedura wdrożenia z podziałem odpowiedzialności |
| [`AGENTS.md`](AGENTS.md) | Kontrakt dla agentów AI pracujących w tym repozytorium |

---

## Wymagania zaliczeniowe — gdzie szukać dowodów

Cztery wymagania bloku 10xBuilder i miejsca w kodzie, które je realizują.

### 1. Obsługa akcji CRUD

Dwa niezależne zasoby, oba z pełnym cyklem tworzenie / odczyt / edycja / usunięcie.

| Zasób | Kontroler | Serwis | Encja |
|---|---|---|---|
| Baza wiedzy | [`QuestionAnswerController`](src/main/java/pl/ankietor/knowledge/controllers/QuestionAnswerController.java) | [`QuestionAnswerService`](src/main/java/pl/ankietor/knowledge/services/QuestionAnswerService.java) | `question_answers` ([V1](src/main/resources/db/migration/V1__baseline.sql)) |
| Ankiety | [`AnkietaController`](src/main/java/pl/ankietor/surveys/controllers/AnkietaController.java) | [`AnkietaService`](src/main/java/pl/ankietor/surveys/services/AnkietaService.java) | `ankiety`, `ankieta_pozycje` ([V4](src/main/resources/db/migration/V4__ankiety.sql)) |

### 2. Logika biznesowa

Dopasowanie pytania do wcześniej udzielonych odpowiedzi na podstawie podobieństwa treści,
nie słów kluczowych — [`LexicalQuestionMatcher`](src/main/java/pl/ankietor/matching/LexicalQuestionMatcher.java).
Metryka `word_similarity` i próg `0.52` wybrane pomiarem, nie założeniem; tabela wyników
w [`tech-stack.md` §3.1a](context/foundation/tech-stack.md). Implementacja siedzi za
interfejsem [`QuestionMatcher`](src/main/java/pl/ankietor/matching/QuestionMatcher.java),
więc tryb semantyczny podmienia się konfiguracją, bez zmian w kontrolerach.

### 3. Testy adresujące ryzyko z `test-plan`

Mapa ryzyk R-1…R-10 w [`test-plan.md` §2](context/foundation/test-plan.md).

| Klasa | Testów | Ryzyko | Scenariusz awarii, przed którym chroni |
|---|---|---|---|
| [`QuestionMatcherTest`](src/test/java/pl/ankietor/matching/QuestionMatcherTest.java) | 10 | R-1, R-2 | propozycja nie dotyczy pytania; albo system nie znajduje niczego mimo istniejącej odpowiedzi |
| [`MainFlowE2ETest`](src/test/java/pl/ankietor/knowledge/MainFlowE2ETest.java) | 7 | R-3, R-5 | niezalogowany widzi treść odpowiedzi; zapisana odpowiedź nie wraca w propozycjach |
| [`MockSemanticQuestionMatcherTest`](src/test/java/pl/ankietor/matching/MockSemanticQuestionMatcherTest.java) | 7 | — | granica trybu leksykalnego: klasa pytań, której nie obsłuży |
| [`OwnershipTest`](src/test/java/pl/ankietor/knowledge/OwnershipTest.java) | 10 | R-4 | modyfikacja cudzej pary przez podmianę identyfikatora |
| [`AnkietaPermissionsTest`](src/test/java/pl/ankietor/surveys/AnkietaPermissionsTest.java) | 13 | R-11 | użytkownik widzi cudzą ankietę — listą albo po adresie |
| [`AnkietaRenderingTest`](src/test/java/pl/ankietor/surveys/AnkietaRenderingTest.java) | 4 | R-12 | widok sięga po powiązanie po zamknięciu transakcji i wywala się u użytkownika |

Razem **51 testów**, wszystkie integracyjne przeciw prawdziwemu PostgreSQL.

`AnkietaRenderingTest` celowo **nie** jest `@Transactional` i to jest cała jego racja bytu.
Test transakcyjny trzyma sesję Hibernate otwartą przez całą metodę, więc leniwe powiązanie
zdąży się doczytać i błąd nie wychodzi — a w aplikacji z `open-in-view=false` ten sam widok
wywala się przy renderowaniu. Ta klasa wykryła dwa realne błędy, których nie widziało
pozostałe 47 testów.

### 4. Autentykacja i zasoby przypisane do użytkownika

Logowanie formularzem, hasła BCrypt, role `ROLE_USER` / `ROLE_ADMIN` —
[`SecurityConfig`](src/main/java/pl/ankietor/config/SecurityConfig.java).

Model widoczności rozróżnia dwa rodzaje zasobów i jest opisany w
[`prd.md` §6a i §6b](context/foundation/prd.md):

| | Baza wiedzy | Ankiety |
|---|---|---|
| Właściciel | `author_id` **NULL dopuszczalny** | `owner_id` **NOT NULL** |
| Odczyt | cały zespół | wyłącznie właściciel |
| Zapis | autor albo administrator | **wyłącznie właściciel** |
| Administrator | modyfikuje wszystko | ogląda wszystko, **nie modyfikuje cudzych** |

Rozdział wglądu od zapisu wymusza
[`AnkietaService.mozeModyfikowac`](src/main/java/pl/ankietor/surveys/services/AnkietaService.java),
które sprawdza wyłącznie własność. Lista administratora domyślnie pokazuje jego własne
ankiety; cudze dopiero po jawnym włączeniu przełącznika.

---

## Szybki start (2 komendy)

Ścieżka dla kogoś, kto chce zobaczyć działającą aplikację bez konfigurowania czegokolwiek.
Wymaga PostgreSQL 13+ z rozszerzeniami `pg_trgm` i `unaccent` (oba są w standardowej
dystrybucji `contrib`).

```bash
psql -U postgres -f db/setup/00_baza_i_rola.sql
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Pierwsza komenda zakłada bazę `ankietor`, rolę `ankietor` i rozszerzenia — jest
idempotentna. Druga uruchamia aplikację na `http://localhost:8080`; Flyway wgrywa schemat,
45 syntetycznych par do bazy wiedzy oraz konta i przykładowe ankiety.

| Login | Hasło | Rola | Co widać po zalogowaniu |
|---|---|---|---|
| `anna` | `demo-anna` | USER | 2 własne ankiety |
| `bartek` | `demo-bartek` | USER | 1 własna ankieta, ankiet Anny nie widzi |
| `admin` | `demo-admin` | ADMIN | przełącznik „wszystkie ankiety", bez prawa edycji cudzych |

Zaloguj się jako `anna`, potem jako `bartek` — najszybciej widać wtedy, że ankieta jest
zasobem prywatnym, a baza wiedzy wspólnym. Model uprawnień opisuje `prd.md` §6a i §6b.

Powyższe hasła dotyczą bazy na localhoscie zakładanej tym skryptem i obowiązują wyłącznie
w profilu `demo`. Bez tego profilu wszystkie hasła pochodzą ze zmiennych środowiskowych,
a hasło administratora przy braku zmiennej jest losowane — patrz niżej.

---

## Uruchomienie lokalne

Ścieżka pełna, bez danych demonstracyjnych.

### 1. Baza danych

Potrzebny PostgreSQL 13+ z rozszerzeniami `pg_trgm` i `unaccent` (oba są w standardowej
dystrybucji `contrib`). Załóż rolę i dwie bazy — produkcyjną i testową:

```sql
CREATE ROLE ankietor WITH LOGIN PASSWORD 'wlasne_haslo';
CREATE DATABASE ankietor      OWNER ankietor ENCODING 'UTF8';
CREATE DATABASE ankietor_test OWNER ankietor ENCODING 'UTF8';
```

Następnie **w każdej z baz osobno**:

```sql
GRANT ALL ON SCHEMA public TO ankietor;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
```

Rozszerzenia zakłada się kontem o wyższych uprawnieniach — rola `ankietor` może ich nie
mieć. Alternatywnie wszystkie te kroki wykonuje w prawidłowej kolejności
[`tools/DbBootstrap.java`](tools/DbBootstrap.java).

### 2. Zmienne środowiskowe

Żadne hasło nie jest zapisane w repozytorium. `application.properties` zawiera wyłącznie
placeholdery `${ZMIENNA:domyslna}`.

| Zmienna | Rola |
|---|---|
| `DB_URL` | np. `jdbc:postgresql://localhost:5432/ankietor` |
| `DB_USER` | użytkownik bazy |
| `DB_PASSWORD` | hasło |
| `ANKIETOR_ADMIN_PASSWORD` | hasło konta `admin`. Bez niej generowane losowo i wypisane do logu **raz** |
| `TEST_DB_URL` / `TEST_DB_USER` / `TEST_DB_PASSWORD` | baza dla testów |

### 3. Start

```bash
mvn spring-boot:run
```

Aplikacja na `http://localhost:8080`, login `admin`. Migracje Flyway wykonują się
automatycznie i wgrywają 45 syntetycznych par jako dane startowe — bez nich dopasowanie
nie ma czego proponować.

### 4. Testy

```bash
mvn clean verify
```

Testy wymagają dostępnej bazy wskazanej przez `TEST_DB_*`. Nie używamy Testcontainers —
uzasadnienie w `tech-stack.md`, sekcja 3.3.

---

## Jak działa dopasowanie

Trzy tryby za jednym interfejsem `QuestionMatcher`, wybierane konfiguracją:

```properties
ankietor.matching.mode=lexical   # lexical | semantic | hybrid
ankietor.matching.min-similarity=0.52
ankietor.matching.max-results=5
```

| Tryb | Technika | Status |
|---|---|---|
| `lexical` | `pg_trgm` — `word_similarity` na kolumnie znormalizowanej przez `unaccent` | **domyślny, działa** |
| `semantic` | embedding + podobieństwo kosinusowe w `pgvector` | przygotowany, wyłączony |
| `hybrid` | leksykalny jako baza, semantyczny jako warstwa podnosząca ranking | przygotowany, wyłączony |

Tryb leksykalny **nie ma zależności zewnętrznych** — działa bez klucza API i bez
wysyłania treści pytań poza infrastrukturę.

### Włączenie trybu semantycznego

1. dostarczyć bean `EmbeddingProvider` (model musi być wielojęzyczny — pytania są po polsku),
2. dołączyć opcjonalną migrację:
   `spring.flyway.locations=classpath:db/migration,classpath:db/semantic`
3. ustawić `ankietor.matching.mode=semantic`

Schemat podstawowy **nie zależy od `pgvector`**, więc aplikacja stawia się na dowolnym
Postgresie.

### Znane ograniczenie trybu leksykalnego

Trigramy nie dopasują fraz bez wspólnego słownictwa. To kryterium **B1** z PRD i jest
ono pokryte testem, który sprawdza, że w trybie leksykalnym taka para **nie** jest
znajdowana. Ten test dokumentuje, co konkretnie kupuje tryb semantyczny — zamiast
zakładać, że AI pomaga.

---

## Dane

Wszystkie pytania i odpowiedzi w repozytorium są **syntetyczne**. Domena odzwierciedla
realny obszar (certyfikaty, ISO, ubezpieczenia, NDA, dane rejestrowe), ale treść jest
wymyślona. Numery certyfikatów, polis, KRS i NIP nie odnoszą się do żadnego istniejącego
podmiotu.
