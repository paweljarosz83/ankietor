package pl.ankietor.matching;

/**
 * Zrodlo wektorow dla trybu semantycznego.
 *
 * Nie ma jeszcze zadnej implementacji i jest to swiadome - MVP dziala w trybie
 * leksykalnym. Wlaczenie trybu semantycznego wymaga dostarczenia beana tego
 * interfejsu oraz zastosowania opcjonalnej migracji dodajacej kolumne wektorowa.
 *
 * Wymaganie wobec modelu: musi byc wielojezyczny, bo pytania sa po polsku.
 */
public interface EmbeddingProvider {

    /** Wektor dla podanego tekstu. Dlugosc musi byc rowna {@link #dimensions()}. */
    float[] embed(String text);

    /** Liczba wymiarow zwracanych wektorow - musi zgadzac sie ze schematem bazy. */
    int dimensions();

    /** Nazwa modelu, do logow i do zapisu w infrastructure.md. */
    String modelName();
}
