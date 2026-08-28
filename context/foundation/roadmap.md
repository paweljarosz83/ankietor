# Roadmap — Ankietor

**Data:** 2026-08-28
**Wsad:** [prd.md](prd.md), [tech-stack.md](tech-stack.md), [infrastructure.md](infrastructure.md)
**Format:** wg lekcji M2L1, vertical-first z sekcją Foundations

> **Uczciwe zastrzeżenie o kolejności prac.** Ta roadmapa powstała **po** zbudowaniu MVP,
> nie przed. Nie udaje więc planu na przyszłość — większość pozycji trafia do `## Done`
> z prawdziwymi identyfikatorami zmian. Wartość tego dokumentu jest dwojaka: pokazuje
> sekwencję, która faktycznie zadziałała, i porządkuje to, co zostało do zrobienia.
>
> Gdyby powstała pierwsza, sekwencja wyszłaby prawdopodobnie taka sama — bo była
> wymuszona zależnościami, nie wyborem. To widać w polach `Prerequisites`.

---

## Vision recap

Pracownik odpowiadający na ankiety klientów dostaje propozycje odpowiedzi udzielonych
wcześniej na pytania o tym samym znaczeniu, z liczbową miarą podobieństwa. Zamiast pisać
od zera i ryzykować niespójność między klientami — wybiera, poprawia i zapisuje.

## North star

**S-02 — pierwsza propozycja odpowiedzi dla inaczej sformułowanego pytania.**

To jest moment, w którym teza produktu się domyka. S-01 (baza wiedzy z CRUD) jest
konieczny, bo bez zapisanych par nie ma czego proponować — ale sam CRUD to jeszcze nie
produkt, tylko lista. Dopiero gdy wklejone pytanie w innym brzmieniu zwraca właściwą
odpowiedź z wynikiem procentowym, wiadomo, że pomysł działa.

North star nie jest pierwszym slice'em w kolejce i to jest normalne.

## At a glance

| ID | Change ID | Outcome | Prereq | Status |
|---|---|---|---|---|
| F-01 | `database-and-migrations` | Schemat pod kontrolą wersji, rozszerzenia gotowe | — | **done** |
| F-02 | `auth-and-roles` | Produktowe ścieżki chronione logowaniem | F-01 | **done** |
| F-03 | `seed-knowledge-base` | 45 syntetycznych par w bazie | F-01 | **done** |
| F-04 | `ci-quality-gate` | Build i testy w CI przy każdym pushu | F-01 | **done** |
| S-01 | `knowledge-base-crud` | Użytkownik zarządza parami pytanie→odpowiedź | F-02 | **done** |
| S-02 | `lexical-question-matching` | Użytkownik dostaje propozycje dla nowego pytania | F-03, S-01 | **done** |
| S-03 | `mode-comparison-screen` | Użytkownik widzi różnicę między trybami | S-02 | **done** |
| S-04 | `public-deployment` | Aplikacja dostępna pod publicznym adresem | F-04 | **ready** |
| S-05 | `semantic-matching-live` | Dopasowanie działa bez wspólnego słownictwa | S-02 | **blocked** |
| S-06 | `concept-dictionary-crud` | Użytkownik zarządza słownikiem pojęć | S-03 | proposed |

## Baseline

Audyt sześciu warstw, stan na 2026-08-28. Dowody to pliki w repozytorium.

| Warstwa | Stan | Dowód |
|---|---|---|
| Frontend | **present** | `templates/` — layout, 4 widoki, AdminLTE w `static/vendor/` |
| Backend / API | **present** | 4 kontrolery MVC; brak REST API i to jest świadome — aplikacja jest server-rendered |
| Data | **present** | Flyway `V1`–`V3`, JPA, `ddl-auto=validate` |
| Auth | **present** | `config/SecurityConfig.java`, BCrypt, role w bazie |
| Deploy | **partial** | `deploy-plan.md` gotowy, wykonanie przed nami |
| Observability | **absent** | świadomie — patrz `## Parked` |

## Foundations

### F-01: database-and-migrations
- **Unlocks:** S-01, oraz F-02, F-03, F-04
- **Outcome:** Schemat pod kontrolą Flyway, rozszerzenia `pg_trgm` i `unaccent` dostępne, schemat podstawowy niezależny od `pgvector`
- **Status:** done

### F-02: auth-and-roles
- **Unlocks:** S-01
- **Outcome:** Wszystkie ścieżki poza `/login` wymagają uwierzytelnienia; hasła hashowane; konto administratora bez hasła w kodzie
- **Status:** done

### F-03: seed-knowledge-base
- **Unlocks:** S-02
- **Outcome:** 45 syntetycznych par w domenie ankiet klienckich
- **Uzasadnienie fundamentu:** bez danych startowych dopasowanie nie ma czego proponować ani czego testować. To fundament z konkretnym slice'em docelowym, nie „przygotujmy dane na wszelki wypadek"
- **Status:** done

### F-04: ci-quality-gate
- **Unlocks:** S-04
- **Outcome:** Build i pełna suita testów przy każdym pushu, przeciw prawdziwemu Postgresowi
- **Uzasadnienie fundamentu:** wdrożenie bez bramki jakości oznaczałoby publikowanie niesprawdzonego kodu. F-04 musi być przed S-04
- **Status:** done

---

## Slices

### S-04: public-deployment
- **Outcome:** User can open the application at a public URL and use it without any local setup.
- **Change ID:** `public-deployment`
- **PRD refs:** wymaganie certyfikacji nr 6 jako warstwa ponad minimum
- **Prerequisites:** F-04 (`ci-quality-gate`)
- **Parallel with:** S-06
- **Blockers:** brak kont Render i Neon — zakłada człowiek
- **Unknowns:**
  - Czy `pgvector` jest dostępny na darmowym planie Neona? — Owner: człowiek. Block: **no** (dotyczy S-05, nie tego slice'a)
  - Czy Render Free wymaga karty przy rejestracji? — Owner: człowiek. Block: **no**
  - Ile trwa pierwsze wejście po godzinie ciszy? — Owner: pomiar po wdrożeniu. Block: **no**
- **Risk:** Wdrożenie **nie jest wymagane do zaliczenia**, więc ten slice ma najniższy priorytet spośród gotowych. Sekwencjonowany tutaj, bo `deploy-plan.md` istnieje i wykonanie jest mechaniczne — ale `deploy-plan.md` §8 zawiera warunek wstrzymania, jeśli zacznie zjadać więcej niż wieczór.
- **Status:** ready

### S-05: semantic-matching-live
- **Outcome:** User can paste a question with no vocabulary overlap with the stored one and still get the right answer proposed.
- **Change ID:** `semantic-matching-live`
- **PRD refs:** kryterium sukcesu B1
- **Prerequisites:** S-02 (`lexical-question-matching`)
- **Parallel with:** —
- **Blockers:** brak dostawcy embeddingów
- **Unknowns:**
  - Czy firma zgadza się na wysyłanie treści pytań do zewnętrznego API? — Owner: człowiek. Block: **yes**
  - Który model wielojęzyczny? Pytania są po polsku, więc model jednojęzyczny angielski nie wystarczy — Owner: człowiek. Block: **yes**
  - Czy `pgvector` jest dostępny na docelowej bazie? — Owner: człowiek. Block: **yes**
- **Risk:** Ten slice jest **wartością dodaną, nie warunkiem zaliczenia** — wymaganie nr 3 jest już spełnione bez AI, zgodnie z lekcją 4.2. Trzy blokujące niewiadome są organizacyjne, nie techniczne: kod po stronie aplikacji istnieje i czeka.
- **Status:** blocked

### S-06: concept-dictionary-crud
- **Outcome:** User can add and edit concept groups, so the lexical mode improves without a code change.
- **Change ID:** `concept-dictionary-crud`
- **PRD refs:** sekcja 8, otwarta decyzja o słowniku pojęć
- **Prerequisites:** S-03 (`mode-comparison-screen`)
- **Parallel with:** S-04
- **Blockers:** —
- **Unknowns:**
  - Czy słownik zarządzany przez użytkownika daje lepszy efekt niż embeddingi za te same pieniądze? — Owner: pomiar na ekranie porównania. Block: **no**
- **Risk:** Sekwencjonowany po S-03, bo ekran porównania dostarcza narzędzia do zmierzenia, czy słownik faktycznie pomaga. Bez tego byłby to CRUD dodany na wiarę. Zaleta wobec S-05: **zero zależności zewnętrznych** i pogłębienie wymagania nr 2.
- **Status:** proposed

---

## Backlog Handoff

Kryteria akceptacji dla S-04, do wpisania w ticket przy podejmowaniu pracy:

- Publiczny URL zwraca stronę logowania z poprawnymi stylami.
- Logowanie kontem `admin` przechodzi.
- Lista bazy wiedzy pokazuje 45 par — potwierdza wykonanie migracji na świeżej bazie.
- Polskie znaki diakrytyczne wyświetlają się poprawnie na całej ścieżce.
- Zapytanie `ISO 9001` zwraca trafienie ze wynikiem 100%.
- Zapytanie `temperatura wrzenia azotu` zwraca komunikat o braku dopasowania.
- Zmierzony i zapisany czas pierwszego wejścia po godzinie ciszy.

Pełna procedura: [deploy-plan.md](../deployment/deploy-plan.md).

## Open Roadmap Questions

| Pytanie | Owner | Wpływ |
|---|---|---|
| Jaka jest formalna procedura składania projektu? Lekcja 4.2 podaje terminy, nie procedurę — informacja jest na Circle | człowiek | **blokuje zaliczenie, nie kod** |
| Czy artefakty z modułów 2–3 w tej formie spełniają wymaganie nr 4? | człowiek, konsultacja na Circle | wysoki |
| Czy commity mają być przypisane do konta GitHub przed upublicznieniem repo? | człowiek | niski, ale jednokierunkowy |
| Czy `pgvector` jest na darmowym Neonie? | człowiek | blokuje S-05 |

## Parked

Świadomie odrzucone, z powodem. Lista pochodzi z `prd.md` §4 i `infrastructure.md`.

| Pozycja | Powód parkowania |
|---|---|
| Import ankiet z Excela, Worda, PDF | Duży koszt parsowania, zero wpływu na tezę produktu. MVP przyjmuje wklejony tekst |
| Eksport odpowiedzi do szablonu klienta | Każdy klient ma inny szablon — to osobny projekt |
| Klient jako encja, przypisywanie ankiet | Nie jest potrzebne, żeby dopasowanie działało |
| Workflow zatwierdzania odpowiedzi | Wymaga drugiej roli i procesu organizacyjnego |
| Wersjonowanie odpowiedzi i historia zmian | Wartość rośnie z czasem życia bazy, nie z MVP |
| Kategorie i tagi pytań | Dopasowanie semantyczne czyni je częściowo zbędnymi |
| **Generowanie** odpowiedzi przez LLM | Ryzykowne merytorycznie bez nadzoru; przy pustej bazie nie ma z czego generować |
| Wielojęzyczność ankiet | Domena jest polska |
| Statystyki i raporty | Nikt jeszcze o nie nie poprosił |
| Observability, alerty | Poza zakresem projektu kursowego — patrz `## Baseline` |
| Preview environments per PR | Funkcja planów płatnych. Bramką jest CI |
| Własna domena | Darmowa subdomena wystarcza |

## Done

| ID | Change ID | Dowód wykonania |
|---|---|---|
| F-01 | `database-and-migrations` | `db/migration/V1`, `V2`; `db/semantic/V100` poza ścieżką domyślną |
| F-02 | `auth-and-roles` | `config/SecurityConfig.java`, `security/`; test `bezLogowaniaBrakDostepu` |
| F-03 | `seed-knowledge-base` | `db/migration/V3__seed_synthetic_knowledge_base.sql`, 45 par |
| F-04 | `ci-quality-gate` | `.github/workflows/ci.yml`; 6 zielonych przebiegów |
| S-01 | `knowledge-base-crud` | `knowledge/`; testy `listaPokazujeDaneStartowe`, `walidacjaOdrzucaKrotkiePytanie` |
| S-02 | `lexical-question-matching` | `matching/LexicalQuestionMatcher.java`; testy A1–A3 |
| S-03 | `mode-comparison-screen` | `matching/controllers/ComparisonController.java`, `MockSemanticQuestionMatcher.java` |

**Sekwencja, która zadziałała:** F-01 → F-02 → S-01 → F-03 → S-02 → F-04 → S-03.

Warto odnotować jedno odstępstwo od czystego vertical-first: F-04 (CI) powstało **po**
S-02, nie przed. Gdyby powstało wcześniej, wyłapałoby użycie wycofanej klasy
`AntPathRequestMatcher` już w F-02, a nie dwa slice'y później. To konkretny koszt
opóźnienia bramki jakości — niewielki tutaj, ale w większym projekcie rósłby liniowo.
