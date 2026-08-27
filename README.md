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
| [`AGENTS.md`](AGENTS.md) | Kontrakt dla agentów AI pracujących w tym repozytorium |

---

## Uruchomienie lokalne

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
