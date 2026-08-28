# Plan wdrożenia — Ankietor

**Data:** 2026-08-28
**Wsad:** [infrastructure.md](../foundation/infrastructure.md), [tech-stack.md](../foundation/tech-stack.md)
**Cel:** publiczny URL aplikacji, koszt 0 zł
**Status:** plan zatwierdzony, wykonanie przed nami


---

## 0. Podział odpowiedzialności

| Krok | Wykonawca |
|---|---|
| Konta na Render i Neon, akceptacja regulaminów | **człowiek** |
| Utworzenie bazy w Neonie, skopiowanie connection stringa | **człowiek** |
| `Dockerfile` i konfiguracja builda | agent |
| Wpisanie zmiennych środowiskowych w panelu Render | **człowiek** |
| Pierwszy deployment i weryfikacja | agent, z człowiekiem przy panelu |
| Konfiguracja monitora rozgrzewającego | człowiek |

Zasada z lekcji m1l5: agent nie zakłada kont, nie wpisuje haseł i nie akceptuje
regulaminów. Wszystko, co nieodwracalne albo tożsamościowe, robi człowiek.

---

## 1. Przygotowania ręczne

### 1.1 Neon — baza danych

1. Rejestracja na neon.com, plan **Free**.
2. Nowy projekt, nazwa `ankietor`, region domyślny.
3. Skopiować **connection string** w formacie JDBC.
4. **Sprawdzić, czy w stringu jest `sslmode=require`.** Neon wymaga TLS — bez tego
   pierwsze połączenie padnie z błędem SSL i wygląda to jak problem z hasłem.
5. W konsoli SQL Neona wykonać:

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
```

6. **Zweryfikować `pgvector`** — punkt otwarty z `infrastructure.md`:

```sql
SELECT name FROM pg_available_extensions WHERE name = 'vector';
```

Brak wyniku nie blokuje wdrożenia (tryb semantyczny jest poza MVP), ale trzeba to
zapisać jako znane ograniczenie.

### 1.2 Render — aplikacja

1. Rejestracja na render.com, plan **Free**.
2. Połączenie konta z GitHubem, autoryzacja dostępu do repozytorium `ankietor`.
3. **Repozytorium jest prywatne** — Render musi dostać do niego dostęp jawnie.

---

## 2. Kroki automatyczne — agent

### 2.1 `Dockerfile`

Render nie ma natywnego runtime'u dla Javy, więc build idzie z obrazu. Budowanie
odbywa się **po stronie Render**, więc lokalny Docker nie jest potrzebny.

Założenia obrazu:

- build wielostopniowy: `maven:3.9-eclipse-temurin-21` do budowy, `eclipse-temurin:21-jre-alpine` do uruchomienia,
- **testy pomijane w obrazie** (`-DskipTests`) — testy są bramką w GitHub Actions, nie w buildzie obrazu, i nie mają dostępu do bazy testowej z kontekstu Render,
- warstwa zależności cache'owana osobno od kodu, żeby przebudowa po zmianie klasy nie ściągała Mavena od nowa,
- port z `$PORT` — Render wstrzykuje go dynamicznie i **nie da się założyć 8080**,
- uruchomienie jako użytkownik bez uprawnień roota.

### 2.2 Profil produkcyjny

- `spring.thymeleaf.cache=true` — na produkcji szablony nie mogą być przeładowywane,
- `spring.jpa.open-in-view=false` — już ustawione,
- `ankietor.matching.mode=lexical` — bez zależności zewnętrznych,
- poziom logowania podniesiony do `INFO`, bez SQL-i.

### 2.3 Health check

Render potrzebuje ścieżki do sprawdzania, czy instancja żyje. `/login` nadaje się
bez dokładania zależności, bo jest jedyną ścieżką dostępną bez uwierzytelnienia
i zwraca `200`.

---

## 3. Sekrety do skonfigurowania

Wpisuje **człowiek**, w panelu Render → Environment. Żadna z tych wartości nie jest
i nie będzie w repozytorium.

| Zmienna | Źródło | Uwaga |
|---|---|---|
| `DB_URL` | connection string z Neona | **musi zawierać `sslmode=require`** |
| `DB_USER` | z Neona | |
| `DB_PASSWORD` | z Neona | |
| `ANKIETOR_ADMIN_PASSWORD` | wybrane przez człowieka | **jeśli pominięte, hasło admina zostanie wygenerowane losowo i wypisane do logu tylko raz** — na darmowym planie z krótką retencją logów oznacza to konto bez znanego hasła |
| `SPRING_PROFILES_ACTIVE` | `prod` | |

---

## 4. Kolejność wykonania

1. Człowiek: konta Neon i Render, baza, rozszerzenia, connection string.
2. Agent: `Dockerfile`, profil `prod`, commit, push. **CI musi przejść na zielono** — to bramka przed wdrożeniem.
3. Człowiek: nowy Web Service na Render, wskazanie repozytorium, wybór builda z `Dockerfile`, plan Free.
4. Człowiek: wpisanie pięciu zmiennych środowiskowych.
5. Render: pierwszy build i deployment automatycznie.
6. Wspólnie: weryfikacja z listy w punkcie 5.
7. Człowiek: monitor rozgrzewający, co 10 min, w godzinach 7:00–21:00.

---

## 5. Weryfikacja po wdrożeniu

Nie „sprawdzić, czy działa", ale konkretnie:

- [ ] publiczny URL zwraca stronę logowania z poprawnym stylem — jeśli CSS nie ładuje się, problem jest w ścieżkach statycznych, nie w aplikacji
- [ ] logowanie kontem `admin` przechodzi
- [ ] **lista bazy wiedzy pokazuje 45 par** — potwierdza, że Flyway wykonał `V1`–`V3` na świeżej bazie
- [ ] ekran „Dopasuj pytanie" dla frazy `ISO 9001` zwraca trafienie z wynikiem 100%
- [ ] dla frazy `temperatura wrzenia azotu` zwraca komunikat o braku dopasowania
- [ ] polskie znaki diakrytyczne wyświetlają się poprawnie — weryfikuje kodowanie na całej ścieżce
- [ ] zapis nowej pary działa i pojawia się w propozycjach
- [ ] **czas pierwszego wejścia po godzinie ciszy** — zmierzyć stoperem, wpisać do `infrastructure.md`

Ostatni punkt jest tu z konkretnego powodu: w pre-mortem z `infrastructure.md` błędnym
założeniem, które doprowadziło do porażki, było **testowanie wyłącznie na rozgrzanej
instancji**. Ten pomiar zamyka tę lukę.

---

## 6. Rollback

Procedura w [infrastructure.md, punkt 8](../foundation/infrastructure.md). Skrót:

| Co | Jak | Czas |
|---|---|---|
| wadliwy deployment | Render → Deploys → poprzedni build → Rollback | ~2 min |
| wadliwy commit | `git revert` + push | ~5 min |
| wadliwa migracja | **brak automatycznego cofnięcia** — migracja naprawcza `V(n+1)` |

Dane startowe żyją w `V3__seed_synthetic_knowledge_base.sql`, więc utrata bazy nie
oznacza utraty zawartości demonstracyjnej. Tracone są wyłącznie pary dodane na żywo.

---

## 7. Czego ten plan świadomie nie zawiera

- **preview deploymentów per pull request** — funkcja planów płatnych; bramką pozostaje GitHub Actions,
- **własnej domeny** — darmowa subdomena Render wystarcza,
- **monitoringu i alertów** — poza zakresem projektu kursowego,
- **skalowania** — jedna instancja, brak wymagań wydajnościowych,
- **automatyzacji tworzenia zasobów przez API Render/Neon** — przy jednorazowym wdrożeniu koszt skryptowania przewyższa zysk.

---

## 8. Warunek wstrzymania

Wdrożenie **nie jest wymagane do zaliczenia** — wymaganie certyfikacji nr 6 dotyczy
CI/CD i jest już spełnione i zweryfikowane trzema zielonymi przebiegami.

Jeśli którykolwiek krok z tego planu zacznie zjadać więcej niż jeden wieczór,
**właściwą decyzją jest wstrzymanie wdrożenia**, a nie brnięcie dalej. Termin złożenia
projektu to 14.09.2026 i pozostałe wymagania mają pierwszeństwo.
