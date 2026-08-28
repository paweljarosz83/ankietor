# Tech Stack — Ankietor

**Data:** 2026-08-27
**Powiązane:** [prd.md](prd.md), [infrastructure.md](infrastructure.md)

Dokument uzasadnia wybory techniczne. Kryteria oceny wzięte z lekcji 4.1 preworku: **typowany, oparty na konwencjach, popularny w danych treningowych, dobrze udokumentowany**.

---

## 1. Warstwy

| Warstwa | Wybór | Wersja |
|---|---|---|
| Runtime | Java (LTS) | 21.0.7 |
| Framework | Spring Boot | 3.5.x |
| Build | Maven | 3.9.12 |
| Warstwa web | Spring Web MVC | z Boota |
| Widoki | Thymeleaf + `thymeleaf-extras-springsecurity6` | z Boota |
| Bezpieczeństwo | Spring Security, logowanie formularzowe | z Boota |
| Dostęp do danych | Spring Data JPA (Hibernate) | z Boota |
| Baza | PostgreSQL | 15.5 (serwer deweloperski), 15 w CI |
| Migracje | Flyway | z Boota |
| Walidacja | Bean Validation (`spring-boot-starter-validation`) | z Boota |
| UI | AdminLTE 4 (MIT), Bootstrap 5, vanilla JS | dist, bez npm |
| Testy | JUnit 5, Spring Boot Test, MockMvc | z Boota |
| Dev loop | `spring-boot-devtools` (`runtime`, `optional`) | z Boota |
| CI | GitHub Actions | — |
| Hosting | Render (aplikacja) + Neon (baza), plany Free | → [infrastructure.md](infrastructure.md) |

## 2. Ocena stacku wg kryteriów agent-friendly

| Kryterium | Ocena | Uzasadnienie |
|---|---|---|
| Typowany | ✅ | Java 21, jawne typy w encjach, DTO i sygnaturach. Agent nie zgaduje kształtu danych. |
| Konwencje | ✅ | Spring Boot ma silne, powszechnie znane konwencje: autokonfiguracja, struktura startera, adnotacje. |
| Popularność w danych treningowych | ✅ | Spring Boot + Thymeleaf + JPA to jeden z najlepiej reprezentowanych zestawów w publicznym kodzie. Thymeleaf jest rzadszy niż React, ale nadal dobrze pokryty. |
| Dokumentacja | ✅ | Oficjalne docs Springa i Baeldung; obie łatwe do podlinkowania agentowi. |
| **Rozpoznam błąd agenta** | ✅ | Stack produkcyjny autora. To piąte kryterium z lekcji 4.1 i najważniejsze przy 18 dniach. |

Odstępstwo od stacku kursowego (Astro + React + TS + Supabase + Cloudflare) jest świadome i zgodne z lekcją 4.1: *„twój stack musi być wystarczająco czytelny dla agenta i wystarczająco znany dla ciebie"*. Oba warunki spełnione. Koszt odstępstwa: brak gotowego startera kursowego i inna ścieżka deploymentu.

## 3. Decyzje wymagające uzasadnienia

### 3.1 `pg_trgm`, nie full-text search

Postgres nie ma polskiego słownika full-text w standardowej instalacji. `to_tsvector('polish', …)` wymaga słownika ispell/hunspell po stronie serwera, czego na hostingach managed zwykle nie da się wgrać. `'simple'` nie robi stemmingu, więc „certyfikat" i „certyfikatu" byłyby dla niego różnymi słowami.

`pg_trgm` jest niezależny od języka i radzi sobie z polską odmianą, bo formy odmienione dzielą większość trigramów. Dodatkowo `unaccent` normalizuje ą/ć/ę/ł/ń/ó/ś/ź/ż.

Oba rozszerzenia są w standardowej dystrybucji `contrib` i dostępne na praktycznie każdym Postgresie, w tym w obrazie `postgres:15` używanym w CI.

**Ograniczenie przyjęte świadomie:** trigramy nie dopasują fraz bez wspólnego słownictwa („ISO 9001" ↔ „systemy zarządzania jakością"). To kryterium B1 z PRD i domena trybu semantycznego.

### 3.1a `word_similarity`, nie `similarity`

Decyzja podjęta po pomiarze na danych startowych, nie z góry.

`similarity()` normalizuje wynik przez sumę trigramów obu tekstów, więc krótkie zapytanie
wobec długiego pytania w bazie dostaje niską ocenę. Zmierzone przypadki:

| Zapytanie | `similarity` | `word_similarity` |
|---|---|---|
| `ISO 9001` | 18% | **100%** |
| `KRS` | 11% | **100%** |
| `polisa OC` | 17% | **67%** |
| `termin platnosci` | 35% | **84%** |

Przy progu 0.52 `similarity` odrzucałoby wszystkie cztery, mimo że właściwa para
istnieje. `word_similarity` szuka najlepiej pasującego fragmentu celu, więc radzi sobie
z zapytaniem krótszym niż wpis w bazie — a użytkownik realnie wkleja fragmenty pytań.

Cena: `word_similarity` zwraca 100% dla każdego pytania zawierającego dane słowo, więc
krótkie i ogólne zapytania generują remisy. Dlatego `similarity` zostaje jako **drugie
kryterium sortowania** — wśród remisów wyżej trafia para najbardziej podobna jako całość.

Konsekwencja implementacyjna, warta zapamiętania: próg operatora `<%` ustawia się przez
GUC `pg_trgm.word_similarity_threshold`, a **nie** przez `set_limit()`, który dotyczy
wyłącznie operatora `%`. Ustawienie przez `set_config(..., is_local = true)` obowiązuje
tylko w bieżącej transakcji, dlatego `@Transactional(readOnly = true)` na metodzie
matchera **jest warunkiem poprawności, nie kosmetyką** — bez niej próg wracałby do
wartości domyślnej 0.6 przed wykonaniem zapytania.

### 3.2 Flyway, nie `ddl-auto`

Schemat pod kontrolą wersji, bo:

- migracja włączająca tryb semantyczny (`pgvector`, kolumna wektorowa, indeks) musi być **osobna i opcjonalna**,
- CI stawia bazę od zera przy każdym uruchomieniu,
- `ddl-auto=update` nie da się przełączyć w tryb „zastosuj tę migrację, ale nie tę".

`ddl-auto` ustawione na `validate`.

### 3.3 Brak Testcontainers

Standardowe podejście do testów Spring Boot + Postgres to Testcontainers, ale wymagają Dockera, którego nie ma na maszynie deweloperskiej.

Rozwiązanie: testy integracyjne działają przeciw bazie wskazanej konfiguracją.

- **Lokalnie** — osobna baza `ankietor_test` na serwerze deweloperskim.
- **W CI** — service container `postgres:15`, adres przez zmienne środowiskowe.

Wada: konieczność posiadania dostępnej bazy do uruchomienia pełnej suity lokalnie. Zaakceptowana — koszt instalacji Docker Desktop na maszynie służbowej jest wyższy.

### 3.4 MockMvc jako test „z perspektywy użytkownika"

Wymaganie certyfikacji nr 5 mówi o teście weryfikującym działanie z perspektywy użytkownika. Realizacja przez MockMvc: żądanie HTTP przez pełny kontekst aplikacji, z uwierzytelnieniem, przez kontroler, do bazy i z powrotem.

Selenium albo Playwright dałyby test przez przeglądarkę, ale koszt konfiguracji przy 18 dniach jest nieproporcjonalny. MockMvc weryfikuje **zachowanie**, nie implementację — a to jest sedno wymagania.

### 3.4a `spring-boot-devtools`

Dodane po napotkaniu realnego kosztu: `spring-boot:run` czyta szablony i pliki statyczne
z `target/classes`, więc bez devtools każda zmiana w `.html` albo `.css` wymaga pełnego
restartu Mavena (~8 s plus utrata sesji logowania). Przy kilkudziesięciu iteracjach
na widokach to wymierna strata.

Zakres `runtime` i `optional` — zależność **nie wchodzi** do artefaktu produkcyjnego
i jest automatycznie wyłączana, gdy aplikacja działa z w pełni spakowanego JAR-a.

### 3.5 AdminLTE 4 w wersji `dist`

MIT, Bootstrap 5, bardzo dobra reprezentacja w danych treningowych — agent zna te klasy CSS i nie będzie ich wymyślał.

Wersja `dist` (gotowe CSS/JS wrzucone do `src/main/resources/static`) zamiast instalacji przez npm. Powód: zero pipeline'u frontendowego, zero `node_modules` w projekcie backendowym, zero kroku budowania assetów w CI.

**Odrzucone:** Bracket (ThemePixels) — komercyjny, nie może trafić do projektu, który idzie do oceny i potencjalnie na Demo Day.

### 3.6 Bez DAO, bez interfejsów serwisowych na jedną implementację

Odstępstwo od konwencji z istniejących projektów autora, uzasadnione objętością pracy przy 18 dniach:

| Wzorzec | Decyzja |
|---|---|
| `Dao` + `DaoImpl` obok `Repository` | **usunięte** — Spring Data JPA załatwia to interfejsem repozytorium |
| `Service` + `ServiceImpl` przy jednej implementacji | **usunięte** — klasa serwisowa bez interfejsu |
| Walidatory jako osobne klasy `Validator` | **usunięte** — Bean Validation na DTO |
| Układ paczek `controllers / services / models / dtos` + paczki domenowe | **zachowane** — znajoma nawigacja |
| Interfejs przy wielu implementacjach | **zachowane** — `QuestionMatcher` |

Szacunkowy efekt: 15–20 plików Java na MVP zamiast 40+.

## 4. Języki

Zgodnie z polityką z lekcji 3.4 preworku — język przypisany do typu zadania, nie wybrany raz na zawsze:

| Element | Język |
|---|---|
| Kod, nazwy klas, pól, metod | angielski |
| Commity, branche | angielski |
| `AGENTS.md` — stały kontrakt z agentem | angielski |
| `prd.md`, `tech-stack.md`, `infrastructure.md` | polski — kontekst domenowy i decyzje, czytane też przez mentorów |
| UI, komunikaty walidacji, treści dla użytkownika | polski — język produktu |
| Dane w bazie (pytania, odpowiedzi) | polski — domena jest polska |

## 5. Wersje i aktualizacja

Wersja Spring Boota podnoszona tylko w obrębie 3.5.x. Żadnych aktualizacji majora w trakcie 18 dni.

`pom.xml` bez zależności poza wypisanymi w punkcie 1 — każda dodatkowa wymaga wpisu w tym dokumencie z uzasadnieniem.
