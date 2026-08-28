# Plan brief: concept-dictionary-crud

**Dla kogo:** osoba robiąca review planu albo wracająca do tej zmiany po przerwie.

## W jednym zdaniu

Słownik pojęć przenosimy z zaszytej w kodzie mapy do dwóch tabel i dajemy mu ekran CRUD,
żeby poprawa dopasowania nie wymagała wdrożenia.

## Ocena zmiany

| Wymiar | Ocena |
|---|---|
| Rozmiar | średni — 2 tabele, 2 encje, 1 kontroler, 2 widoki, 1 migracja |
| Ryzyko | **niskie** — nie dotyka trybu domyślnego ani istniejących testów |
| Zależności zewnętrzne | **brak** |
| Odwracalność | pełna — tryb `lexical` działa niezależnie |
| Fazy | 4 |

## Co warto zakwestionować przy review

1. **Czy dwie tabele, czy jedna?** Plan zakłada `concepts` i `concept_variants`.
   Alternatywa: jedna tabela z wariantami w kolumnie tekstowej rozdzielonej przecinkami.
   Prostsze, ale traci możliwość indeksowania i walidacji per wariant.
2. **Czy słownik powinien wpływać na tryb `lexical`, nie tylko na mockup?** Plan mówi:
   nie. Tryb leksykalny ma zostać czysty i mierzalny, żeby ekran porównania nadal
   pokazywał różnicę. Zmieszanie ich zabiłoby wartość diagnostyczną tego ekranu.
3. **Czy 45 par uzasadnia tę pracę?** Przy tej wielkości bazy słownik da się utrzymać
   w kodzie. Argument za bazą jest o przyszłości, nie o teraźniejszości.

## Zalecenie

Wykonać **po** upewnieniu się, że wymaganie nr 4 jest zaliczone. To praca ponad minimum
i nie powinna konkurować z terminem 14.09.2026.
