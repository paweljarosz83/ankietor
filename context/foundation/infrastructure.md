# Infrastructure — Ankietor

**Data:** 2026-08-28
**Powiązane:** [prd.md](prd.md), [tech-stack.md](tech-stack.md)
**Status:** decyzja podjęta, wdrożenie przed nami

---

## 1. Wsad — odpowiedzi z wywiadu

| Pytanie | Odpowiedź | Konsekwencja |
|---|---|---|
| Region EU? | **bez znaczenia** | odpada wymóg lokalizacji danych, otwiera się pula regionów US |
| Budżet | **0 zł, twardo** | **ograniczenie wiążące** — eliminuje większość platform |
| Publiczny URL | **tak, ma być pokazywany** | dostępność i czas pierwszej odpowiedzi stają się kryterium, nie detalem |
| Procesy w tle, CRON, kolejki | **nie** | odpada wymóg stałego działania procesu; platformy skalujące do zera są w grze |

Budżet 0 zł jest tu jedyną decyzją, która realnie zawęża wybór. Wszystko poniżej
wynika z niej.

## 2. Wybór i uzasadnienie

> **Aplikacja: Render (Free web service). Baza: Neon (Free plan).**

Dwie platformy, nie jedna — bo żadna nie daje jednocześnie darmowej aplikacji
i trwale darmowej bazy.

**Dlaczego nie jedna platforma:** darmowy Postgres na Renderze **wygasa 30 dni po
utworzeniu**, z 14-dniowym okresem karencji na przejście na płatny. To nie jest
ograniczenie techniczne, które da się obejść — to koniec życia zasobu. Aplikacja
przestałaby działać w połowie października.

## 3. Odrzucone opcje i powód

| Platforma | Powód odrzucenia |
|---|---|
| **AWS** | Odrzucone przez właściciela projektu. Niezależnie: 2–3 wieczory na VPC, security groups, subnet group dla RDS i IAM, a po free tier ~25–40 USD/mc. |
| **Fly.io** | Maszyna 2,02 USD/mc, ale **najtańszy Managed Postgres to 38 USD/mc**. Aplikacja za dwa dolary, baza za trzydzieści osiem. Łamie budżet 0 zł. |
| **Railway** | Nie ma już darmowego planu — jednorazowy kredyt próbny, potem płatność. |
| **Render Postgres** | Darmowa instancja wygasa po 30 dniach. Nie nadaje się pod link, który ma żyć. |
| **Supabase** (jako baza) | Kandydat serio rozważany, opisany w punkcie 4. Odrzucony na jednej właściwości. |

### Supabase kontra Neon — decydująca różnica

| | Supabase Free | Neon Free |
|---|---|---|
| Rozmiar bazy | 500 MB | 0,5 GB |
| Bezczynność | **pauzuje projekt po tygodniu**, wymaga ręcznego odpauzowania | compute usypia po 5 min, wraca sam; **brak pauzowania projektu za bezczynność** |
| Limit obliczeń | — | 100 CU-h/mc (≈400 h przy 0,25 CU) |
| `pgvector` | udokumentowany | **do weryfikacji** — patrz punkt 10 |

Dla linku, który ma stać w CV i być klikany rzadko, tygodniowa pauza wymagająca
ręcznej interwencji jest **trybem awarii, nie niedogodnością**. Wygrywa Neon.

Koszt tej decyzji: `pgvector` na Neonie nie jest przez nas potwierdzony, a jest
potrzebny przy włączeniu trybu semantycznego. Ryzyko akceptowalne — tryb semantyczny
jest poza MVP, a schemat podstawowy celowo nie zależy od `pgvector`.

## 4. Dopasowanie do stacku

| Element | Dopasowanie |
|---|---|
| Spring Boot jako proces JVM | ✅ Render uruchamia obraz z `Dockerfile`; brak wsparcia natywnego dla Javy, więc potrzebny będzie `Dockerfile` |
| Brak Dockera lokalnie | ✅ Render buduje obraz po swojej stronie z repozytorium — lokalny Docker zbędny |
| Postgres 15+ z `pg_trgm` i `unaccent` | ✅ oba są w standardowej dystrybucji `contrib`, obecne w Neonie |
| Flyway na starcie | ✅ migracje `V1`–`V3` wykonają się same, więc **45 syntetycznych par pojawi się automatycznie** |
| Brak procesów w tle | ✅ skalowanie do zera nie koliduje z niczym w MVP |

## 5. CLI / MCP / API — operability po deploymencie

| Kanał | Stan |
|---|---|
| Render CLI | dostępne, ale wdrożenie i tak jest automatyczne z pusha do `main` |
| Render API | dostępne, tokenem osobistym |
| Neon CLI | dostępne (`neonctl`) |
| Serwer MCP | **brak gotowego dla obu platform** — zostajemy przy CLI |
| Logi | panel Render + API; retencja na darmowym planie ograniczona |

Zgodnie z Deep Dive z lekcji m1l5: start od CLI, MCP dokładamy dopiero, gdy pojawi się
powtarzalny wzorzec zapytań. Przy jednym projekcie i wdrożeniu z pusha taki wzorzec
nie wystąpi.

## 6. Podgląd wdrożenia

Render buduje z gałęzi `main` automatycznie po każdym pushu. Preview environments per
pull request są funkcją planów płatnych — **na darmowym planie ich nie ma**.

Skutek praktyczny: bramką jakości pozostaje **GitHub Actions**, nie preview
deployment. To wystarcza, bo CI uruchamia pełną suitę przeciw prawdziwemu Postgresowi.

## 7. Sekrety

Żaden sekret nie jest i nie będzie trzymany w repozytorium — `application.properties`
zawiera wyłącznie `${ZMIENNA:domyslna}`.

| Zmienna | Gdzie ustawiona | Kto ma dostęp |
|---|---|---|
| `DB_URL` | Render → Environment | właściciel projektu |
| `DB_USER` | Render → Environment | właściciel projektu |
| `DB_PASSWORD` | Render → Environment | właściciel projektu |
| `ANKIETOR_ADMIN_PASSWORD` | Render → Environment | właściciel projektu |
| `TEST_DB_*` | GitHub Actions — wartości kontenera efemerycznego, jawne w `ci.yml` | publiczne, bez znaczenia |

**Pułapka do zapamiętania:** `DB_URL` dla Neona **musi zawierać `sslmode=require`**.
Neon wymaga TLS i bez tego pierwsze połączenie padnie.

**Druga pułapka:** jeśli `ANKIETOR_ADMIN_PASSWORD` nie zostanie ustawione,
`AdminAccountInitializer` wygeneruje hasło losowo i wypisze je do logu **dokładnie raz**.
Na Renderze z krótką retencją logów oznacza to konto administratora bez znanego hasła
i konieczność ręcznej interwencji w bazie.

## 8. Rollback

Nie „da się cofnąć", ale konkretnie:

| Scenariusz | Kroki | Czas |
|---|---|---|
| Wadliwy deployment | Render → Deploys → wybrany poprzedni build → **Rollback** | ~2 min |
| Wadliwy commit | `git revert <sha>` + push → Render przebuduje | ~5 min |
| Wadliwa migracja Flyway | **brak automatycznego rollbacku** — patrz niżej |

**Migracje są jednokierunkowe.** Flyway nie ma migracji „w dół" w tym projekcie.
Wadliwa migracja wymaga ręcznej naprawy w bazie albo migracji naprawczej `V(n+1)`.
Na darmowym planie Neona **nie ma backupów w rozumieniu snapshotów na żądanie** —
odtworzenie oznaczałoby utratę par dodanych po ostatniej migracji.

Łagodząca okoliczność: 45 par danych startowych żyje w `V3__seed_synthetic_knowledge_base.sql`,
czyli w repozytorium. Utrata bazy nie oznacza utraty zawartości demonstracyjnej —
wystarczy pusta baza i ponowny start aplikacji. Tracone są wyłącznie pary dodane na żywo.

## 9. Uprawnienia — co wymaga człowieka

| Akcja | Kto |
|---|---|
| deployment na produkcję | automatycznie z pusha do `main` |
| rollback deploymentu | człowiek, panel Render |
| zmiana zmiennych środowiskowych | człowiek |
| rotacja hasła bazy | człowiek |
| usunięcie bazy albo projektu | człowiek, wyłącznie ręcznie |
| tworzenie kont na Render i Neon | człowiek — agent nie zakłada kont ani nie wpisuje haseł |

## 10. Ryzyka z testów anti-bias

Wynik trzech testów przeprowadzonych **na tym wyborze**, nie na wyborze abstrakcyjnym.

### Test 1 — Devil's advocate

*Najgorsze, co da się powiedzieć o „Render Free + Neon Free":*

1. **Zimny start zabija jedyny cel tego wdrożenia.** Aplikacja usypia po 15 minutach
   bez ruchu, a powrót zajmuje **około minuty**. Rekruter albo mentor klika link z CV,
   widzi stronę ładowania, po piętnastu sekundach zamyka kartę i wyciąga wniosek, że
   projekt nie działa. Link staje się **obciążeniem, nie atutem**. To najpoważniejszy
   zarzut i dotyczy dokładnie tego, po co robimy deployment.
2. **750 godzin instancji to limit na cały workspace, nie na usługę.** Drugi darmowy
   projekt w tym samym koncie zabiera z tej samej puli i wyłącza oba po wyczerpaniu.
3. **Darmowe plany się kończą.** Railway skasował swój darmowy plan. Render może
   zrobić to samo. Link w CV, który umiera po pół roku, jest gorszy niż brak linku.
4. **Dwóch dostawców to dwa konta, dwa panele i dwa tryby awarii**, a połączenie do bazy
   idzie przez publiczny internet — dochodzi opóźnienie i wymóg TLS.
5. **100 CU-godzin Neona da się przepalić.** Cokolwiek, co pinguje bazę bez przerwy,
   wyczerpie limit w połowie miesiąca i operacje zaczną padać.

### Test 2 — Pre-mortem

*Jest listopad 2026. Żałuję tego wyboru. Co się stało:*

Wdrożenie poszło we wrześniu bez problemu. Demo Day wypadł dobrze — link został
otwarty piętnaście minut przed prezentacją, więc instancja była rozgrzana i wszystko
odpowiadało natychmiast. Wniosek: działa.

Link trafił do CV. W listopadzie ktoś z procesu rekrutacyjnego kliknął go po pięciu
tygodniach ciszy. Render dawno uśpił instancję. Strona ładowania trzymała
siedemdziesiąt sekund — do czasu spin-upu doszło jeszcze kilka sekund na wybudzenie
compute w Neonie i start JVM. Osoba zamknęła kartę po kilkunastu sekundach.

**Nic się nie zepsuło.** Wszystko działało zgodnie ze specyfikacją darmowego planu.
A link nie wykonał swojego jedynego zadania.

Błędne założenie, które do tego doprowadziło: **testowałem wdrożenie zawsze na
rozgrzanej instancji.** Ani razu nie sprawdziłem, jak wygląda pierwsze wejście po
tygodniu ciszy — czyli dokładnie ten scenariusz, który jest normą dla linku w CV,
a nie wyjątkiem.

### Test 3 — Unknown unknowns

*Czego scoring nie objął, bo nie było takiego pytania:*

- **Backupy.** Darmowy plan Render Postgres wprost nie ma backupów; darmowy Neon ma
  ograniczone okno przywracania. Nie pytaliśmy o to, bo dane są syntetyczne — ale to
  przestaje być prawdą w dniu, w którym ktoś wpisze pierwszą prawdziwą odpowiedź.
- **Ścieżka wyjścia.** Oba to zwykły Postgres, więc `pg_dump` wystarcza. Vendor lock-in
  praktycznie zerowy — to rzadka i niedoceniana zaleta tego zestawu.
- **Status produkcyjny funkcji.** Nie sprawdzaliśmy, co na tych planach jest GA, a co
  w becie. Przy darmowych tierach zmiany regulaminu bywają szybsze niż komunikacja.
- **Compliance.** Poza zakresem, dopóki dane są syntetyczne. Pierwsza prawdziwa
  odpowiedź klienta w tej bazie zmienia to natychmiast — i to jest **sygnał wyjścia
  z darmowego planu**, nie temat do przemyślenia później.
- **Region.** Uznany za nieistotny, ale darmowe plany zwykle przypisują region US.
  Opóźnienie z Polski ~120 ms. Dla tej aplikacji bez znaczenia; dla realnego narzędzia
  wewnętrznego już nie.

### Decyzje po testach

| Ustalenie | Decyzja |
|---|---|
| Zimny start ~1 min | **zmieniam plan, nie wybór** — patrz mitygacja poniżej |
| Limit 750 h na workspace | akceptuję; w tym koncie nie będzie drugiej darmowej usługi |
| Darmowe plany mogą zniknąć | zapisane jako znane ograniczenie, sygnał zmiany w punkcie 12 |
| Brak backupów | akceptuję dla danych syntetycznych; sygnał zmiany przy pierwszych realnych |
| Dwóch dostawców | akceptuję — alternatywa łamie budżet |
| Limit CU Neona | akceptuję pod warunkiem mitygacji o ograniczonym harmonogramie |

**Mitygacja zimnego startu.** Zewnętrzny darmowy monitor (np. cron-job.org albo
UptimeRobot) odpytuje aplikację co 10 minut **wyłącznie w godzinach 7:00–21:00**.
Efekt: w godzinach, w których ktokolwiek realnie kliknie link, instancja jest
rozgrzana, a compute Neona wybudzone. Zużycie: ~14 h/dzień × 31 = **~434 h/mc**,
czyli z zapasem poniżej 750 h. Ping całodobowy dałby 744 h i stałby na granicy limitu —
dlatego harmonogram jest ograniczony celowo, a nie z oszczędności.

To jest realny efekt testu devil's advocate. Bez niego wdrożenie spełniałoby
specyfikację i nie spełniałoby swojego celu.

## 11. Decyzje techniczne

| Parametr | Wartość |
|---|---|
| Region | domyślny dla darmowego planu (prawdopodobnie US) |
| Runtime aplikacji | obraz z `Dockerfile`, build po stronie Render |
| Wersja Postgresa | zgodna z domyślną Neona, minimum 15 |
| Plan | Free na obu platformach |
| Wdrożenie | automatyczne z pusha do `main` |
| Tryb dopasowania na produkcji | `lexical` — bez zależności zewnętrznych |
| Monitor rozgrzewający | zewnętrzny, co 10 min, 7:00–21:00 |

## 12. Sygnał zmiany decyzji

Ten wybór wraca do rozmowy, gdy zajdzie **którakolwiek** z poniższych sytuacji:

1. **W bazie pojawia się pierwsza prawdziwa odpowiedź klienta.** Wtedy zaczynają
   obowiązywać backupy, retencja, region i compliance — a darmowy plan żadnego z tych
   wymagań nie spełnia. To najważniejszy z sygnałów.
2. **Projekt przestaje być projektem kursowym**, a zaczyna być narzędziem, z którego
   ktoś korzysta w pracy. Wtedy właściwym kierunkiem jest on-prem albo Azure, obok
   reszty infrastruktury firmy — **nie** kolejny darmowy plan i nie AWS.
3. **Włączamy tryb semantyczny.** Wymaga potwierdzenia `pgvector` na Neonie i miejsca
   na wektory w 0,5 GB.
4. **Render albo Neon zmienia warunki darmowego planu.**
5. **Baza przekracza 0,5 GB** albo egress zaczyna zbliżać się do limitu.

## 13. Do zweryfikowania przy wdrożeniu

- [ ] `pgvector` dostępny na darmowym planie Neona
- [ ] `pg_trgm` i `unaccent` możliwe do założenia własnymi uprawnieniami w Neonie
- [ ] `sslmode=require` w `DB_URL`
- [ ] czy Render Free wymaga karty przy rejestracji
- [ ] **czas pierwszego wejścia po tygodniu ciszy** — zmierzyć, nie założyć
