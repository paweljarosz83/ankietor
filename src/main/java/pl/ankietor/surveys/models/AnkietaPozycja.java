package pl.ankietor.surveys.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pl.ankietor.knowledge.models.QuestionAnswer;

import java.time.OffsetDateTime;

/** Pojedyncze pytanie z ankiety klienta wraz z odpowiedzia wybrana przez uzytkownika. */
@Entity
@Table(name = "ankieta_pozycje")
public class AnkietaPozycja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ankieta_id", nullable = false)
    private Ankieta ankieta;

    @Column(name = "pytanie", nullable = false, columnDefinition = "text")
    private String pytanie;

    @Column(name = "odpowiedz", nullable = false, columnDefinition = "text")
    private String odpowiedz;

    /**
     * Para z bazy wiedzy, z ktorej odpowiedz zostala zaproponowana. Moze byc null,
     * gdy uzytkownik napisal odpowiedz od zera albo gdy zrodlowa para zostala usunieta -
     * historia tego, co poszlo do klienta, ma przetrwac zmiany w bazie wiedzy.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private QuestionAnswer source;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected AnkietaPozycja() {
        // wymagane przez JPA
    }

    public AnkietaPozycja(String pytanie, String odpowiedz, QuestionAnswer source) {
        this.pytanie = pytanie;
        this.odpowiedz = odpowiedz;
        this.source = source;
    }

    public Long getId() {
        return id;
    }

    public Ankieta getAnkieta() {
        return ankieta;
    }

    void setAnkieta(Ankieta ankieta) {
        this.ankieta = ankieta;
    }

    public String getPytanie() {
        return pytanie;
    }

    public void setPytanie(String pytanie) {
        this.pytanie = pytanie;
    }

    public String getOdpowiedz() {
        return odpowiedz;
    }

    public void setOdpowiedz(String odpowiedz) {
        this.odpowiedz = odpowiedz;
    }

    public QuestionAnswer getSource() {
        return source;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
