package se.jtutala.library.author;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/// JPA-entitet för en författare.
///
/// Entiteten är **intern** – den lämnar aldrig service-lagret. Controllern ser bara
/// DTO:er (`AuthorResponse`, `CreateAuthorRequest` ...). Se README, avsnittet "DTO-mönstret".
///
/// Notera att relationen bara går åt ett håll: `Book` pekar på `Author`, men `Author` har
/// ingen lista med böcker. Enkelriktade `@ManyToOne` är enklast och räcker oftast –
/// böckerna hämtas via `BookRepository` när de behövs.
@Entity
@Table(name = "author")
@EntityListeners(AuditingEntityListener.class)
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Column(nullable = false, length = 120)
    private String name;

    private @Nullable Integer birthYear;

    /// Optimistisk låsning: Hibernate ökar värdet vid varje UPDATE och lägger till
    /// `WHERE version = ?`. Har någon annan hunnit uppdatera raden kastas ett undantag.
    @Version
    private long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private @Nullable Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private @Nullable Instant updatedAt;

    /// JPA kräver en konstruktor utan argument. `protected` så att vår egen kod inte använder den.
    protected Author() {
        this.name = "";
    }

    public Author(String name, @Nullable Integer birthYear) {
        this.name = name;
        this.birthYear = birthYear;
    }

    /// Ändringar sker via metoder med affärsbetydelse i stället för en setter per fält.
    public void update(String name, @Nullable Integer birthYear) {
        this.name = name;
        this.birthYear = birthYear;
    }

    public @Nullable Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public @Nullable Integer getBirthYear() {
        return birthYear;
    }

    public long getVersion() {
        return version;
    }

    public @Nullable Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Instant getUpdatedAt() {
        return updatedAt;
    }

    /// equals/hashCode för entiteter: jämför på id, och använd en konstant hashCode
    /// så att objektet inte "byter hink" i ett HashSet när id tilldelas vid persist.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Author other)) return false; // pattern matching för instanceof
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Author.class.hashCode();
    }

    @Override
    public String toString() {
        return "Author[id=%s, name=%s]".formatted(id, name);
    }
}
