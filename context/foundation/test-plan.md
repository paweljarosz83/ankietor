# Test Plan — Ankietor

**Data:** 2026-08-28
**Wsad:** [prd.md](prd.md), [roadmap.md](roadmap.md), [tech-stack.md](tech-stack.md), [AGENTS.md](../../AGENTS.md)
**Format:** wg lekcji M3L1

---

## 1. Mapa ryzyk

Skala trzystopniowa, zgodnie z lekcją. Ryzyka opisują **scenariusze awarii z perspektywy
użytkownika**, nie miejsca w kodzie — plik i linia to zadanie dla `/10x-research`, nie
dla planu testów.

| # | Scenariusz awarii | Wpływ | Prawd. | Priorytet | Źródło |
|---|---|---|---|---|---|
| R-1 | Użytkownik dostaje propozycję, która nie dotyczy jego pytania, i wysyła ją klientowi | **wysoki** | średnie | **1** | PRD kryterium A2 |
| R-2 | Na pytanie, na które firma już odpowiadała, system nie znajduje niczego — użytkownik pisze od zera i tworzy niespójność | **wysoki** | **wysokie** | **1** | PRD §1, cel produktu |
| R-3 | Osoba niezalogowana widzi treść odpowiedzi firmy o certyfikatach i ubezpieczeniach | **wysoki** | niskie | **2** | nadużycie, oś bezpieczeństwa |
| R-4 | Zalogowany użytkownik modyfikuje albo usuwa parę innego autora przez podmianę identyfikatora w żądaniu | średni | średnie | **2** | nadużycie, IDOR — **ROZSTRZYGNIĘTE 28.08**, patrz §4 faza 1 |
| R-5 | Zapisana odpowiedź nie pojawia się w propozycjach — baza wiedzy nie rośnie, produkt cicho przestaje działać | **wysoki** | niskie | **2** | PRD kryterium A3 |
| R-6 | Polskie znaki rozsypują się między formularzem, bazą i widokiem; odpowiedź wysłana klientowi zawiera „krzaki" | średni | średnie | **2** | doświadczenie własne, kodowanie na Windows |
| R-7 | Migracja odrzucona przy starcie na innym systemie plików — aplikacja nie wstaje po wdrożeniu | średni | średnie | **3** | Flyway liczy sumy kontrolne z treści plików |
| R-8 | Hasło albo adres serwera trafia do repozytorium | **wysoki** | niskie | **2** | AGENTS.md, reguła twarda |
| R-9 | Użytkownik wpisuje bardzo długi tekst i psuje widok albo zapis | niski | średnie | **3** | wejście od użytkownika |
| R-10 | Konto administratora powstaje z hasłem, którego nikt nie zna | średni | średnie | **3** | `infrastructure.md` §7 |

**Świadomie NIE na mapie** — odpowiedź na pytanie „na co nie ma iść budżet testowy":

- awarie dostawcy hostingu i bazy — tańsze do obsłużenia obserwowalnością niż testem
- wydajność przy dużym wolumenie — 45 par, jeden użytkownik, brak wymagań wydajnościowych
- kompatybilność przeglądarek — narzędzie wewnętrzne, jedna przeglądarka
- wygląd na urządzeniach mobilnych — nikt nie wypełnia ankiety klienckiej z telefonu

## 2. Profil istniejących testów

**34 testy, cztery klasy.** Stan uczciwy: brak testów jednostkowych w klasycznym sensie —
wszystkie testy podnoszą kontekst Springa i uderzają w prawdziwą bazę.

| Klasa | Liczba | Poziom | Co chroni |
|---|---|---|---|
| `QuestionMatcherTest` | 10 | integracyjny, prawdziwy Postgres | R-1, R-2 |
| `MainFlowE2ETest` | 7 | przez MockMvc, pełny kontekst | R-3, R-5 |
| `MockSemanticQuestionMatcherTest` | 7 | integracyjny | granica trybu leksykalnego |
| `OwnershipTest` | 10 | przez MockMvc, dwóch użytkowników | R-4 |

**Konsekwencja tego profilu:** suita jest wolna (~90 s lokalnie) i wymaga dostępnej bazy.
Zaakceptowane — uzasadnienie w `tech-stack.md` §3.3. Cena: nie da się uruchomić testów
w podróży ani bez sieci firmowej.

### Ryzyka bez pokrycia

| Ryzyko | Stan |
|---|---|
| R-4 — IDOR | ✅ **pokryte.** `OwnershipTest`, 10 testów: odczyt wspólny, modyfikacja własnościowa, widok „moje pary". Reguła zapisana w `prd.md` §6a |
| R-6 — kodowanie | **brak testu automatycznego.** Sprawdzone ręcznie w przeglądarce |
| R-7 — sumy kontrolne migracji | **brak testu.** Złagodzone przez `.gitattributes` |
| R-9 — długie wejście | **częściowo.** `@Size` waliduje górną granicę, brak testu na zachowanie widoku |
| R-10 — hasło admina | **brak testu** |

## 3. Wyrocznia testów

Asercje muszą brać oczekiwany wynik z wymagań, nie z uruchomienia kodu. Testy A1
sprawdzają, że właściwa para trafia do **top 3** — kryterium pochodzi z PRD, nie z tego,
co zwraca funkcja.

Jedna wartość wymaga uwagi przy utrzymaniu: **próg 0.52** pochodzi z pomiaru na danych
startowych. Przy zmianie metryki podobieństwa albo istotnym powiększeniu bazy wiedzy
trzeba go zmierzyć ponownie, a nie przyjąć, że nadal pasuje.

Reguła przy dopisywaniu testów: przy każdej asercji zapytaj, skąd wzięła się liczba.
Jeśli odpowiedź brzmi „bo tyle zwraca funkcja", to nie jest test.

## 4. Fazy rollout'u

### Faza 1 — zapisać decyzję o widoczności par ✅ WYKONANE 28.08.2026
- **Ryzyko:** R-4
- **Cel:** rozstrzygnąć i **zapisać**, czy baza wiedzy jest wspólna dla wszystkich zalogowanych, czy prywatna per użytkownik
- **Dlaczego pierwsza:** to nie jest zadanie testowe, a produktowe. Nie da się napisać testu autoryzacji, dopóki nie wiadomo, jaka jest reguła. Obecnie baza jest wspólna i **nikt tego nigdzie nie zapisał** — czyli nie da się stwierdzić, czy to decyzja, czy przeoczenie
- **Wynik:** wpis w `prd.md`, a jeśli baza ma być prywatna — nowy slice w roadmapie
- **Wynik faktyczny:** decyzja zapisana w `prd.md` §6a. Odczyt wspólny, bo prywatna baza zniszczyłaby cel produktu. Modyfikacja własnościowa: autor albo `ROLE_ADMIN`. Dodany widok „moje pary" jako zasoby przypisane do użytkownika.
- **Test:** `OwnershipTest` — 10 testów, w tym trzy na próbę modyfikacji cudzej pary przez podmianę identyfikatora

### Faza 2 — kodowanie znaków na całej ścieżce ⬜ do zrobienia
- **Ryzyko:** R-6
- **Cel:** test zapisujący parę z pełnym zestawem polskich znaków i odczytujący ją przez widok
- **Typ:** integracyjny przez MockMvc, bez nowych narzędzi
- **Kryterium:** znaki `ą ć ę ł ń ó ś ź ż` przechodzą formularz, bazę i renderowanie bez zmiany

### Faza 3 — konto administratora ⬜ do zrobienia
- **Ryzyko:** R-10
- **Cel:** dwa testy — konto powstaje z hasłem ze zmiennej środowiskowej; przy jej braku powstaje z hasłem losowym i nie powstaje po raz drugi przy restarcie
- **Typ:** integracyjny

### Faza 4 — długie wejście ⬜ do zrobienia
- **Ryzyko:** R-9
- **Cel:** test na granicy `@Size`, plus sprawdzenie, że tabela nie rozpycha strony
- **Typ:** integracyjny; sprawdzenie widoku ręczne

### Faza 5 — bramka na sekrety w CI ⬜ do zrobienia
- **Ryzyko:** R-8
- **Cel:** krok w `ci.yml`, który zawodzi build, gdy w śledzonych plikach pojawi się wzorzec hasła, klucza albo adresu IP
- **Dlaczego test tego nie złapie:** to nie jest błąd działania aplikacji, a błąd procesu. Właściwym miejscem jest bramka, nie test
- **Uwaga:** dziś sprawdzane ręcznie przed commitem. Ręczna weryfikacja przestaje działać w momencie, gdy commituje ktoś inny

**Faz nie szereguję po ryzyku.** Faza 1 dotyczy ryzyka priorytetu 2, ale jest pierwsza,
bo pozostałe fazy nie zmienią się od jej wyniku, a ona może wygenerować nowy slice.

## 5. Bramki jakości

Stan obecny, weryfikowalny w `ci.yml` i w `AGENTS.md`.

| Bramka | Gdzie | Kiedy | Blokuje? |
|---|---|---|---|
| Kompilacja | `mvn compile` | lokalnie i w CI | tak |
| `ddl-auto=validate` | start aplikacji | każdy start | tak — encje muszą zgadzać się ze schematem |
| Walidacja Flyway | start aplikacji | każdy start | tak |
| Pełna suita testów | `mvn verify` | lokalnie i w CI | tak |
| Dostępność rozszerzeń | krok w `ci.yml` | w CI | tak |
| Przegląd sekretów | ręcznie | przed commitem | **nie — do naprawy, faza 5** |
| Review diffu przez człowieka | — | przed commitem | nie |

**Świadomie bez bramki:** lint i statyczna analiza. Uzasadnienie: przy 20 plikach
i jednym autorze koszt konfiguracji przewyższa zysk. **Sygnał zmiany:** drugi autor
w repozytorium albo przekroczenie 50 plików.

## 6. Cookbook Patterns

### 6.1 Test integracyjny dopasowania
- Lokalizacja: `src/test/java/pl/ankietor/matching/`
- Adnotacje: `@SpringBootTest`, `@ActiveProfiles("test")`
- Baza: prawdziwy Postgres ze zmiennych `TEST_DB_*`. **Bez mockowania repozytoriów** — dopasowanie jest realizowane przez SQL, więc mock testowałby atrapę
- Dane: z migracji `V3`, nigdy tworzone w teście — 45 par jest stałym punktem odniesienia
- Wzorzec: asercja na **obecność w top 3**, nie na konkretną wartość podobieństwa
- Komenda: `mvn -Dtest=QuestionMatcherTest test`

### 6.2 Test przepływu użytkownika
- Lokalizacja: `src/test/java/pl/ankietor/knowledge/`
- Adnotacje: `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`, `@Transactional`
- `@Transactional` na klasie jest **obowiązkowe**, jeśli test zapisuje — wycofuje wiersze i nie zanieczyszcza danych startowych dla pozostałych testów
- Uwierzytelnienie: `@WithUserDetails("admin")` — daje prawdziwy `UserDetailsImpl`, więc `author_id` się wypełnia. `@WithMockUser` **nie wystarczy**
- POST wymaga `.with(csrf())`
- Asercje: `content().string(containsString(...))` — sprawdzają, co widzi użytkownik

### 6.3 Test granicy, czyli że coś NIE działa
- Wzorzec: `assertThat(...).noneSatisfy(...)` z komentarzem **dlaczego** brak wyniku jest poprawny
- Powód istnienia: dokumentuje granicę trybu i wychwyci moment, w którym granica się przesunie
- Referencja: `QuestionMatcherTest.b1GranicaTrybuLeksykalnego`

### 6.4 Czego nie robić
- Nie mockować `JdbcClient` ani repozytoriów w testach dopasowania — logika siedzi w SQL
- Nie tworzyć danych testowych dla dopasowania — używać `V3`
- Nie asertować konkretnych wartości podobieństwa — są wrażliwe na próg i wersję `pg_trgm`
- Nie pisać testu, którego asercja pochodzi z uruchomienia kodu

## 7. Jak agent może podnieść jakość dalej

- **Wygenerować przypadki brzegowe do istniejących ryzyk** — ale wyrocznia musi pochodzić od człowieka, w formie „przy X oczekuję Y", nie „napisz testy do tej klasy"
- **Zaproponować scenariusze nadużycia**, których nie ma na mapie. Agent domyślnie planuje happy path — pytanie „co się stanie, gdy ktoś użyje tego wbrew intencji" trzeba zadać wprost
- **Sprawdzić, czy test potrafi zawieść** — zepsuć kod produkcyjny, uruchomić test, przywrócić. Jeśli test przechodzi po zepsuciu, asercja niczego nie pilnuje
- **Nie prosić o „popraw, aż testy przejdą"** — najkrótszą drogą jest wtedy dopasowanie testu do kodu

## 8. Status

| Faza | Stan |
|---|---|
| Mapa ryzyk | ✅ |
| Audyt istniejących testów | ✅ |
| Cookbook z realnych wzorców | ✅ |
| Faza 1 — widoczność par | ✅ |
| Faza 2 — kodowanie znaków | ⬜ |
| Faza 3 — konto administratora | ⬜ |
| Faza 4 — długie wejście | ⬜ |
| Faza 5 — bramka na sekrety | ⬜ |

**Wymaganie certyfikacji nr 5 jest spełnione** — istnieje test weryfikujący kluczowy
przepływ z perspektywy użytkownika. Pięć faz powyżej to praca ponad minimum, uszeregowana
po ryzyku, nie po łatwości.
