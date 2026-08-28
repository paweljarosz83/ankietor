# PRD — Ankietor

**Wersja:** 0.4
**Data:** 2026-08-27
**Autor:** Paweł Jarosz
**Kontekst:** projekt zaliczeniowy 10xDevs 3.0, termin złożenia 14.09.2026

**Zmiana względem 0.1:** dwa tryby wyszukiwania za jednym interfejsem. Tryb leksykalny działa w MVP, tryb semantyczny jest przygotowany i wyłączony. Kryteria sukcesu rozbite na dwa poziomy, po jednym na tryb.

---

## 1. Problem

Klienci przysyłają do firmy ankiety i kwestionariusze z pytaniami o firmę — certyfikaty, systemy jakości, ubezpieczenia, zgodność, procedury, dane rejestrowe. Pytania od różnych klientów **powtarzają się merytorycznie, ale są inaczej sformułowane**.

Dziś każda ankieta jest wypełniana od zera. Osoba odpowiadająca:

- nie wie, czy na to samo pytanie już kiedyś odpowiadano,
- nie ma jak tego sprawdzić inaczej niż pamięcią albo przeszukiwaniem starych plików,
- w efekcie odpowiada ponownie, czasem **niespójnie z poprzednią odpowiedzią**.

Koszt: czas i ryzyko dwóch różnych odpowiedzi na to samo pytanie u dwóch klientów.

Wyszukiwanie po słowach kluczowych rozwiązuje to tylko częściowo, bo problemem jest właśnie odmienne sformułowanie. Dwa przykłady z różnych półek trudności:

| Para pytań | Pokrycie słownictwa | Rozwiązywalne leksykalnie? |
|---|---|---|
| „Czy posiadacie certyfikat ISO 9001?" ↔ „Proszę podać numer certyfikatu ISO 9001 wraz z datą ważności" | wysokie | tak |
| „Czy posiadacie certyfikat ISO 9001?" ↔ „Proszę wskazać wdrożone systemy zarządzania jakością" | zerowe | **nie** |

Ten podział jest fundamentem architektury opisanej w punkcie 3.

## 2. Użytkownik

**Główny i jedyny w MVP:** pracownik odpowiadający na ankiety klientów — dział jakości, sprzedaży albo obsługi klienta.

- Zna domenę, nie zna historii wszystkich wcześniejszych odpowiedzi.
- Pracuje pod presją terminu.
- Chce **gotowy punkt startowy**, nie wynik wyszukiwania do przeanalizowania.

**Pierwsza akcja:** wkleja pytanie z otrzymanej ankiety i dostaje propozycje odpowiedzi.

**Poza MVP:** rola przeglądającego bez prawa edycji, właściciel merytoryczny zatwierdzający odpowiedzi.

## 3. Logika biznesowa

> Aplikacja rozpoznaje, że nowe pytanie dotyczy tej samej sprawy co pytanie odpowiedziane wcześniej, i proponuje jego odpowiedź — z liczbową miarą pewności dopasowania.

To decyzja domenowa: system **ocenia i szereguje**, użytkownik dostaje rekomendację z wynikiem, na podstawie którego może jej zaufać albo nie. Zgodnie z wymaganiem certyfikacji nr 3 aplikacja *rekomenduje* i *generuje propozycję* — dwie pozycje z listy dopuszczalnych decyzji domenowych.

### Dwa tryby, jeden kontrakt

Dopasowanie ukryte za jednym interfejsem. Tryb wybierany konfiguracją, bez zmiany kodu i bez rekompilacji.

```
ankietor.matching.mode = lexical | semantic | hybrid
```

| Tryb | Technika | Status w MVP |
|---|---|---|
| `lexical` | `pg_trgm` — podobieństwo trigramowe + `unaccent`, opcjonalne wzmocnienie słownikiem pojęć | **domyślny, działa** |
| `semantic` | embedding pytania + podobieństwo kosinusowe w `pgvector` | przygotowany, wyłączony |
| `hybrid` | wynik leksykalny jako baza, semantyczny jako warstwa podnosząca ranking | przygotowany, wyłączony |

**Kontrakt jest jeden** — implementacje są wymienne i muszą zwracać ten sam kształt wyniku: lista dopasowań z pytaniem, odpowiedzią i znormalizowanym wynikiem podobieństwa 0–1.

„Przygotowany" oznacza konkretnie: interfejs istnieje, implementacja semantyczna istnieje jako klasa, jej aktywacja wymaga wyłącznie ustawienia właściwości i dostarczenia dostawcy embeddingów. Nie oznacza „zostawiliśmy miejsce w architekturze".

### Dlaczego tak

- Tryb leksykalny nie ma zależności zewnętrznych — działa w dniu pierwszym, bez klucza API, kosztu i zgody na wysyłanie treści pytań poza firmę.
- Tryb semantyczny rozwiązuje klasę pytań, której leksykalny nie ruszy (druga linia tabeli w punkcie 1).
- Przełącznik pozwala **zmierzyć różnicę** tym samym testem na tych samych danych, zamiast zakładać, że AI pomaga.

## 4. Zakres MVP

Jeden przepływ główny, od początku do końca.

### Przepływ główny

1. Użytkownik loguje się.
2. Wkleja treść pytania z ankiety klienta.
3. System zwraca **do 5 najbardziej podobnych** wcześniej odpowiedzianych pytań, każde z wynikiem podobieństwa i pełną odpowiedzią.
4. Użytkownik wybiera propozycję, edytuje ją albo pisze odpowiedź od zera.
5. Zapisuje nową parę pytanie→odpowiedź, która od tej chwili zasila kolejne propozycje.

### Funkcje w MVP

| Funkcja | Zakres |
|---|---|
| Uwierzytelnianie | Logowanie formularzowe, użytkownicy i role w bazie, hasła hashowane |
| Baza wiedzy | Lista par pytanie→odpowiedź: dodawanie, edycja, usuwanie, podgląd |
| Dopasowanie | Interfejs `QuestionMatcher`, implementacja leksykalna aktywna, semantyczna wyłączona |
| Konfiguracja trybu | Właściwość `ankietor.matching.mode`, widoczna w interfejsie jako informacja o aktywnym trybie |
| Zapis nowej pary | Z ekranu propozycji |
| Dane startowe | 40–60 syntetycznych par, wymagane żeby dopasowanie miało czym proponować |

### Poza MVP — świadomie odrzucone

Zapisane, żeby nie wracały w trakcie:

- import ankiet z plików (Excel, Word, PDF)
- eksport odpowiedzi do szablonu klienta
- klient jako encja, przypisywanie ankiet do klientów
- workflow zatwierdzania odpowiedzi
- wersjonowanie odpowiedzi i historia zmian
- kategorie i tagi pytań
- **generowanie** nowej odpowiedzi przez LLM zamiast wyszukiwania istniejącej
- wielojęzyczność ankiet
- statystyki i raporty

Ekran porównania trybów **został dodany do MVP** — patrz sekcja 4a.

## 4a. Ekran porównania trybów — rozszerzenie zakresu

**Rozszerzenie zakresu MVP**, odnotowane tutaj zgodnie z regułą z `AGENTS.md`.

### Co robi

Jeden ekran, jedno pytanie, dwie kolumny obok siebie: tryb aktywny i warstwa semantyczna.
Ten sam zbiór danych, ten sam próg. Cel to **pokazać, której klasy pytań tryb leksykalny
nie obsługuje** — zamiast twierdzić, że warstwa semantyczna pomoże.

### Prawa kolumna jest mockupem i jest tak oznaczona

Prawdziwy tryb semantyczny wymaga dostawcy embeddingów, klucza API i zgody na wysyłanie
treści pytań poza infrastrukturę. Żadna z tych rzeczy nie jest potrzebna do celu
demonstracyjnego, więc kolumna działa na **ręcznie zapisanym słowniku pojęć**.

Trzy rzeczy, które to czyni uczciwym:

1. **Wyniki są prawdziwe** — to trafienia w istniejące pary, nie zmyślone treści.
2. **Procent jest umowny i tak podpisany** (`~80%`), bo słownik nie mierzy stopnia
   podobieństwa. `mode()` zwraca `mockup slownika pojec`, a ekran ma na górze ostrzeżenie.
3. **Nic nie jest przedstawiane jako AI.**

### Dlaczego słownik, a nie atrapa ze stałą listą wyników

Atrapa kosztowałaby tyle samo i nie dowodziłaby niczego. Słownik pojęć **realnie
rozwiązuje** kryterium B1 na trzech przypadkach, na których tryb leksykalny zwraca zero.
Różnica na ekranie jest więc konsekwencją mechanizmu, nie scenografii.

### Miejsce, w które wejdzie AI

`MockSemanticQuestionMatcher` implementuje ten sam interfejs `QuestionMatcher`, co reszta.
Podmiana na `SemanticQuestionMatcher` z prawdziwym `EmbeddingProvider` **nie dotknie
kontrolera ani widoku** — zmienia się jeden bean.

### Koszt

Jedna klasa, jeden kontroler, jeden widok, siedem testów. Bez migracji, bez nowej encji,
bez zależności w `pom.xml`. Słownik jest zaszyty w kodzie, bo mockup nie potrzebuje CRUD-a —
gdyby miał zostać na stałe, trafiłby do tabeli.

### Test, który pilnuje sensu tego ekranu

`MockSemanticQuestionMatcherTest` sprawdza nie to, że mockup jest dobry, ale że
**domyka lukę, którą tryb leksykalny zostawia**: dla tego samego zapytania mockup
znajduje parę o ISO 9001, a tryb leksykalny jej nie znajduje. Gdyby kiedyś zaczął
znajdować, ekran porównania przestałby mieć sens — i test to wychwyci.

## 5. Kryteria sukcesu

Rozbite na dwa poziomy, bo tryby rozwiązują różne klasy problemu. Oba są treścią testów.

### Poziom A — obowiązkowy, tryb leksykalny

**A1. Odmiana i parafraza z pokryciem słownictwa.** Dla pytania sformułowanego inaczej, ale zawierającego wspólne pojęcia kluczowe (odmienione, w innej kolejności, z inną interpunkcją), właściwa para trafia do **top 3**.

**A2. Brak fałszywych propozycji.** Dla pytania bez odpowiednika w bazie system nie pokazuje nic poniżej progu podobieństwa i komunikuje, że nie znalazł dopasowania.

**A3. Domknięcie pętli.** Para zapisana przez użytkownika jest natychmiast widoczna jako propozycja dla kolejnego, podobnego pytania.

### Poziom B — dokumentujący wartość trybu semantycznego

**B1. Brak pokrycia leksykalnego.** Dla pary pytań o zerowym pokryciu słownictwa, ale tożsamych merytorycznie (ISO 9001 ↔ systemy zarządzania jakością), właściwa para trafia do **top 3**.

Test na B1 jest uruchamiany warunkowo — tylko gdy tryb semantyczny jest aktywny. **W trybie leksykalnym oczekujemy, że B1 nie przejdzie, i to jest wynik, nie porażka.** Ta różnica jest udokumentowanym efektem przełączenia trybu.

## 6. Dane

| Encja | Pola kluczowe |
|---|---|
| `User` | login, hasło (hash), role, aktywny |
| `Role` | nazwa |
| `QuestionAnswer` | treść pytania, treść odpowiedzi, data utworzenia, autor |

**Schemat podstawowy nie zależy od `pgvector`.** Kolumna wektorowa, rozszerzenie i indeks podobieństwa są dostarczane **osobną, opcjonalną migracją**, stosowaną dopiero przy włączeniu trybu semantycznego. Powód: aplikacja musi dać się postawić na dowolnym Postgresie, także takim bez `pgvector`.

Migracje: Flyway. Migracja włączająca tryb semantyczny jest napisana od początku, ale trzymana osobno.

**Dane w projekcie zaliczeniowym są syntetyczne.** Domena odzwierciedla realny obszar (certyfikaty, ISO, ubezpieczenia, NDA, dane rejestrowe), treść pytań i odpowiedzi jest wymyślona. Żadnych rzeczywistych danych firmowych ani danych klientów.

## 6a. Model widoczności i własności

**Decyzja podjęta 2026-08-28.** Rozstrzyga ryzyko R-4 z [test-plan.md](test-plan.md):
zakres uprawnień zalogowanego użytkownika do par utworzonych przez kogoś innego.

| Operacja | Reguła |
|---|---|
| **Odczyt** | wspólny dla wszystkich zalogowanych |
| **Dopasowanie** | działa na całej bazie, niezależnie od autorstwa |
| **Widok „moje pary"** | filtr pokazujący wyłącznie pary utworzone przez zalogowanego użytkownika |
| **Edycja i usuwanie** | wyłącznie autor pary albo `ROLE_ADMIN` |
| **Pary z danych startowych** (bez autora) | wyłącznie `ROLE_ADMIN` — zawartość systemowa |

### Dlaczego odczyt jest wspólny, a nie prywatny

Prywatna baza per użytkownik **zniszczyłaby cel produktu**. Problem opisany w §1 to
niespójność odpowiedzi między klientami. Jeśli dwie osoby w firmie mają rozłączne bazy,
dokładnie ta niespójność wraca — tylko trudniej ją zauważyć, bo każdy widzi u siebie
porządek.

Baza wiedzy jest zasobem zespołu. To jest cecha produktu, nie luka w autoryzacji.

### Dlaczego modyfikacja jest własnościowa

Wspólny odczyt nie wymaga wspólnego zapisu. Autor odpowiedzi wie, w jakim kontekście ją
napisał, więc on ją poprawia. Administrator ma dostęp do wszystkiego, żeby dało się
poprawić pary po osobie, która odeszła z firmy.

Konsekwencja praktyczna: przypadkowe usunięcie cudzej pracy jest niemożliwe bez
uprawnień administratora.

### Zabezpieczenie jest w serwisie, nie w widoku

Widok ukrywa przyciski edycji dla par, których użytkownik nie jest autorem — ale to
**wygoda, nie zabezpieczenie**. Regułę wymusza `QuestionAnswerService`, w każdej operacji
zapisu, więc podmiana identyfikatora w żądaniu POST nic nie da. Pokryte testem
`OwnershipTest.obcyNieNadpisujePrzezPost`.

## 7. Mapowanie na wymagania certyfikacji 10xDevs

| # | Wymaganie | Jak spełnione |
|---|---|---|
| 1 | Kontrola dostępu | Logowanie formularzowe, użytkownicy i role w bazie |
| 2 | Zarządzanie danymi | Pełny CRUD na bazie wiedzy, wynikający z domeny |
| 3 | Logika biznesowa | Ocena i ranking dopasowania pytań — bez zależności od AI |
| 4 | Artefakty projektowe | `prd.md`, `tech-stack.md`, `infrastructure.md`, `deploy-plan.md`, plany implementacji, `AGENTS.md` |
| 5 | Test | Testy na kryteria A1–A3, plus warunkowy test na B1 |
| 6 | CI/CD | GitHub Actions: build Maven + testy, Postgres jako service container |

Wymaganie nr 3 jest spełnione **bez AI** — zgodnie z lekcją 4.2, w której logika biznesowa „może być AI, ale nie musi". Tryb semantyczny jest wartością dodaną, nie warunkiem zaliczenia.

## 8. Otwarte decyzje

- **Próg podobieństwa — ROZSTRZYGNIĘTE: `0.52`.** Wyznaczone empirycznie na 45 parach
  danych startowych i 15 przypadkach testowych: najniższe prawdziwe trafienie 55%,
  najwyższe fałszywe 50%. Próg 0.52 daje 3 punkty zapasu w obie strony. Zastrzeżenie:
  15 przypadków to nie zbiór walidacyjny — próg jest uzasadniony, nie udowodniony,
  i wraca do rozmowy przy istotnym powiększeniu bazy wiedzy.
- **Metryka podobieństwa — ROZSTRZYGNIĘTE: `word_similarity` z `similarity` jako
  rozstrzygnięciem remisów.** Powód i pomiary w `tech-stack.md`, sekcja 3.1a.
- **Słownik pojęć jako wzmocnienie trybu leksykalnego** — tabela odwzorowań typu „ISO 9001 ↔ system zarządzania jakością" podniosłaby tryb leksykalny w stronę kryterium B1 i pogłębiła CRUD. Decyzja po zmierzeniu, jak radzą sobie same trigramy. Domyślnie: **nie w MVP**.
- **Dostawca embeddingów** — nieblokujący, dopóki tryb semantyczny jest wyłączony. Wymóg: model wielojęzyczny, bo pytania są po polsku.
- **Platforma hostingowa** — Spring Boot nie działa na Cloudflare Workers ani Pages. Wymaga własnego researchu; wynik trafia do `infrastructure.md`.
- **Nazwa projektu** — roboczo „Ankietor".

## 9. Ryzyka i sygnały zmiany decyzji

| Ryzyko | Plan |
|---|---|
| Trigramy nie osiągają kryterium A1 na danych startowych | Dodać słownik pojęć jako wzmocnienie. Zakres MVP bez zmian. |
| Brak polskiego słownika full-text na hostingu managed | Już uwzględnione — dlatego `pg_trgm`, nie `to_tsvector('polish')`. |
| Brak dostępu do embeddingów w ogóle | Bez wpływu na zaliczenie. Tryb semantyczny zostaje wyłączony, kryterium B1 jest udokumentowane jako nieosiągnięte w trybie leksykalnym. |
| Hosting bez `pgvector` | Bez wpływu — migracja wektorowa jest opcjonalna i nieaplikowana. |
| Pierwszy przepływ end-to-end nie działa do **31.08.2026** | Zakres schodzi niżej: rezygnacja z edycji propozycji, sam wybór i zapis. |

Zakres MVP wraca do rozmowy tylko przy ostatniej pozycji tej tabeli.
