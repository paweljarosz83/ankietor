# Plan: concept-dictionary-crud

**Roadmap item:** S-06
**Data:** 2026-08-28
**Format:** wg lekcji M2L2

---

## End state

Co ma być prawdą po tej zmianie:

1. Słownik pojęć żyje w bazie, w dwóch tabelach: `concepts` i `concept_variants`.
2. Zalogowany użytkownik ma ekran, na którym dodaje pojęcie, dodaje i usuwa jego warianty
   językowe oraz usuwa całe pojęcie.
3. `MockSemanticQuestionMatcher` czyta słownik z bazy, a nie ze stałej w kodzie.
4. Migracja przenosi obecne 13 pojęć z kodu do bazy, więc **ekran porównania działa tak
   samo jak przed zmianą** — zero regresji widocznej dla użytkownika.
5. Tryb `lexical` jest nietknięty. Ekran porównania nadal pokazuje różnicę między trybami.
6. Wszystkie 24 istniejące testy przechodzą bez modyfikacji.

Punkt 6 jest kryterium poprawności całej zmiany: jeśli trzeba zmienić istniejący test,
znaczy że zmieniliśmy zachowanie, a nie tylko źródło danych.

## Phases

### Faza 1 — schemat i migracja danych

**Cel:** tabele istnieją i zawierają obecną treść słownika.

- migracja `V4__concept_dictionary.sql`
- `concepts`: `id`, `name` (unikalna), `created_at`
- `concept_variants`: `id`, `concept_id` (FK, `ON DELETE CASCADE`), `variant`, unikalność na parze `(concept_id, variant)`
- `INSERT` przenoszący 13 pojęć i ich warianty z `MockSemanticQuestionMatcher`
- warianty zapisane **znormalizowane** — małe litery, bez diakrytyków, dokładnie jak dziś w kodzie

**Weryfikacja:** `mvn verify` przechodzi; ręczne zapytanie potwierdza liczbę wierszy zgodną z mapą w kodzie.

**Manual gate:** brak.

### Faza 2 — encje i repozytoria

**Cel:** dostęp do słownika przez JPA, bez zmiany zachowania aplikacji.

- `dictionary/models/Concept.java`, `dictionary/models/ConceptVariant.java`
- `dictionary/repos/ConceptRepository.java` z metodą pobierającą pojęcia razem z wariantami
- **bez** interfejsu serwisowego przy jednej implementacji, zgodnie z `AGENTS.md`

**Weryfikacja:** `mvn verify`; `ddl-auto=validate` musi przejść — to bramka na rozjazd encji ze schematem.

**Manual gate:** brak.

### Faza 3 — matcher czyta z bazy

**Cel:** `MockSemanticQuestionMatcher` używa słownika z bazy. Zachowanie identyczne.

- wstrzyknięcie `ConceptRepository`
- usunięcie stałej `SLOWNIK_POJEC`
- **cache w pamięci** wczytywany raz i unieważniany przy zapisie — słownik zmienia się rzadko, a odczyt przy każdym zapytaniu byłby marnotrawstwem
- komentarz klasy zaktualizowany: mockup nadal jest mockupem, zmienia się tylko źródło danych

**Weryfikacja:** `MockSemanticQuestionMatcherTest` przechodzi **bez modyfikacji**. To najważniejszy sygnał w całym planie.

**Manual gate:** **tak.** Otworzyć `/porownanie`, sprawdzić przykład o systemach zarządzania jakością i potwierdzić, że wynik jest identyczny jak przed zmianą.

### Faza 4 — ekran CRUD

**Cel:** użytkownik zarządza słownikiem bez udziału programisty.

- `dictionary/controllers/ConceptController.java` — lista, dodanie, usunięcie pojęcia, dodanie i usunięcie wariantu
- `dictionary/dtos/ConceptForm.java`, `VariantForm.java` — `record` + Bean Validation
- widoki `templates/dictionary/list.html`, `form.html`
- pozycja w menu bocznym
- unieważnienie cache z fazy 3 przy każdym zapisie

**Weryfikacja:** `mvn verify` plus nowy test: dodanie wariantu zmienia wynik dopasowania w mockupie.

**Manual gate:** **tak.** Dodać wariant, sprawdzić na `/porownanie`, że zaczyna działać bez restartu aplikacji.

## Intent + Contract per file

| Plik | Po co go dotykamy | Kontraktu nie wolno złamać |
|---|---|---|
| `db/migration/V4__concept_dictionary.sql` | nowe tabele plus przeniesienie danych | migracje są **niezmienne po zastosowaniu**; poprawka to `V5`, nigdy edycja `V4` |
| `matching/MockSemanticQuestionMatcher.java` | zmiana źródła słownika | `mode()` **musi** nadal zwracać ciąg zawierający `mockup`; umowny wynik zostaje `0.80` |
| `dictionary/**` | nowa paczka domenowa | układ `controllers / services / models / dtos` wg `AGENTS.md`; bez DAO, bez interfejsów na jedną implementację |
| `templates/layout/main.html` | pozycja w menu | nie ruszać sygnatury fragmentu `html(tytul, podtytul, aktywny, zawartosc)` — używają jej wszystkie widoki |
| `matching/LexicalQuestionMatcher.java` | **NIE DOTYKAMY** | tryb leksykalny musi zostać niezależny od słownika, inaczej ekran porównania traci sens |
| `src/test/**` | tylko **dodawanie** testów | żaden istniejący test nie może wymagać modyfikacji |

## Success Criteria

Sprawdzalne, nie opisowe:

1. `mvn clean verify` — 24 istniejące testy plus nowe, wszystkie zielone, **zero zmian w istniejących**.
2. `/porownanie` dla „Proszę wskazać wdrożone systemy zarządzania jakością" zwraca te same trzy pary co przed zmianą.
3. Dodanie wariantu przez ekran zmienia wynik dopasowania **bez restartu** aplikacji.
4. Usunięcie pojęcia kaskadowo usuwa jego warianty i nie zostawia sierot.
5. `ddl-auto=validate` przechodzi — encje zgadzają się z migracją.
6. CI zielone.
7. Tryb `lexical` daje **identyczne** wyniki jak przed zmianą — weryfikacja przez `QuestionMatcherTest` bez modyfikacji.

## Risks / Open Questions

| Ryzyko | Ocena | Reakcja |
|---|---|---|
| Cache słownika rozjeżdża się z bazą po zapisie | średnie | unieważnienie w tej samej transakcji co zapis; test na to |
| Pusty słownik po nieudanej migracji danych | niskie | mockup zwraca pustą listę, ekran porównania pokazuje brak dopasowań. Degradacja jest widoczna, nie cicha |
| Warianty wpisane z diakrytykami przez UI nie zadziałają | **wysokie** | normalizacja przy zapisie, nie przy odczycie. **To najbardziej prawdopodobny błąd tej zmiany** |
| Rozrost słownika pogarsza wydajność dopasowania | niskie | dopasowanie jest w pamięci; przy setkach wariantów bez znaczenia |

**Open questions:**

1. Czy warianty mają być widoczne dla wszystkich zalogowanych, czy per użytkownik? —
   **zależy od fazy 1 z `test-plan.md`** (ryzyko R-4). Ta zmiana nie powinna startować,
   dopóki tam nie ma decyzji.
2. Czy normalizację robić przy zapisie, czy trzymać oryginał i normalizować przy odczycie?
   Plan zakłada **przy zapisie plus zachowanie oryginału w osobnej kolumnie**, żeby
   użytkownik widział to, co wpisał.

## Warunek wstrzymania

Zatrzymać implementację, jeśli:

- trzeba zmodyfikować którykolwiek istniejący test — znaczy, że zmieniamy zachowanie, nie źródło danych
- trzeba dotknąć `LexicalQuestionMatcher`
- open question nr 1 nie ma odpowiedzi

## Progress

| Faza | Status | Commit | Uwagi |
|---|---|---|---|
| 1 — schemat i migracja | ⬜ nie rozpoczęta | — | — |
| 2 — encje i repozytoria | ⬜ nie rozpoczęta | — | — |
| 3 — matcher z bazy | ⬜ nie rozpoczęta | — | manual gate |
| 4 — ekran CRUD | ⬜ nie rozpoczęta | — | manual gate |

**Ostatnia aktualizacja:** 2026-08-28, plan utworzony, implementacja nierozpoczęta.
