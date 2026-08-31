package pl.ankietor.surveys.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import pl.ankietor.security.models.User;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ankieta klienta wypelniana przez konkretnego uzytkownika.
 *
 * Roznica wobec {@link pl.ankietor.knowledge.models.QuestionAnswer}: baza wiedzy jest
 * wspolna dla zespolu, a ankieta nalezy do osoby. Dlatego owner jest wymagany, a nie
 * opcjonalny - nie istnieje ankieta bez wlasciciela.
 */
@Entity
@Table(name = "ankiety")
public class Ankieta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nazwa", nullable = false, length = 200)
    private String nazwa;

    @Column(name = "klient", length = 200)
    private String klient;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "ankieta", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<AnkietaPozycja> pozycje = new ArrayList<>();

    protected Ankieta() {
        // wymagane przez JPA
    }

    public Ankieta(String nazwa, String klient, User owner) {
        this.nazwa = nazwa;
        this.klient = klient;
        this.owner = owner;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void dodajPozycje(AnkietaPozycja pozycja) {
        pozycja.setAnkieta(this);
        pozycje.add(pozycja);
        this.updatedAt = OffsetDateTime.now();
    }

    public int liczbaPozycji() {
        return pozycje.size();
    }

    public Long getId() {
        return id;
    }

    public String getNazwa() {
        return nazwa;
    }

    public void setNazwa(String nazwa) {
        this.nazwa = nazwa;
    }

    public String getKlient() {
        return klient;
    }

    public void setKlient(String klient) {
        this.klient = klient;
    }

    public User getOwner() {
        return owner;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<AnkietaPozycja> getPozycje() {
        return pozycje;
    }
}
