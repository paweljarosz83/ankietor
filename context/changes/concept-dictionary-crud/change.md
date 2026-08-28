# Change: concept-dictionary-crud

**Roadmap item:** S-06
**Status:** proposed
**Utworzono:** 2026-08-28

## Intencja

Przenieść słownik pojęć z kodu do bazy i dać użytkownikowi ekran do zarządzania nim.
Dziś słownik jest zaszyty w `MockSemanticQuestionMatcher` jako stała mapa — działa,
ale zmiana wymaga rekompilacji i wdrożenia.

## Dlaczego to jest warte zrobienia

Trzy powody, w kolejności wagi:

1. **Domena wie lepiej niż algorytm.** Osoba odpowiadająca na ankiety wie, że
   „systemy zarządzania jakością" i „ISO 9001" to ta sama sprawa. Embeddingi zgadują
   to statystycznie; człowiek z tej firmy wie to na pewno.
2. **Zero zależności zewnętrznych.** Alternatywa (S-05, tryb semantyczny) wymaga klucza
   API, kosztu i zgody na wysyłanie treści pytań poza infrastrukturę. Ten slice nie
   wymaga niczego.
3. **Pogłębia wymaganie certyfikacji nr 2.** Druga encja z pełnym CRUD i relacją,
   zamiast jednej tabeli.

## Czego ten change NIE robi

- nie usuwa `MockSemanticQuestionMatcher` — zmienia mu źródło danych z mapy w kodzie na bazę
- nie zmienia trybu domyślnego; `lexical` zostaje aktywny
- nie dotyka `LexicalQuestionMatcher`
- nie wprowadza embeddingów — to S-05

## Powiązane

- [roadmap.md](../../foundation/roadmap.md) — S-06
- [prd.md](../../foundation/prd.md) — §8, otwarta decyzja o słowniku pojęć
- [test-plan.md](../../foundation/test-plan.md) — §6.1, wzorzec testu dopasowania
