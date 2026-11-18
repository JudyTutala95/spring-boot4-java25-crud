package se.jtutala.library.book;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import se.jtutala.library.author.Author;

/// JPA-entitet för en bok. Se `Author` för förklaring av id, `@Version` och auditing.
@Entity
@Table(name = "book")
@EntityListeners(AuditingEntityListener.class)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 17)
    private String isbn;

    /// Pengar: använd alltid `BigDecimal`, aldrig `double` (0.1 + 0.2 != 0.3).
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int publishedYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Genre genre;

    /// `FetchType.LAZY`: författaren hämtas först när den används.
    /// Standard för `@ManyToOne` är EAGER, vilket lätt ger onödiga joins/frågor –
    /// sätt alltid LAZY och hämta medvetet med `@EntityGraph` när du behöver den.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @Version
    private long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private @Nullable Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private @Nullable Instant updatedAt;

    /// Endast för JPA. Hibernate fyller fälten via reflection direkt efteråt.
    protected Book() {
        this.title = "";
        this.isbn = "";
        this.price = BigDecimal.ZERO;
        this.genre = Genre.OTHER;
    }

    public Book(String title, String isbn, BigDecimal price, int publishedYear, Genre genre, Author author) {
        this.title = title;
        this.isbn = isbn;
        this.price = price;
        this.publishedYear = publishedYear;
        this.genre = genre;
        this.author = author;
    }

    public void update(String title, String isbn, BigDecimal price, int publishedYear, Genre genre, Author author) {
        this.title = title;
        this.isbn = isbn;
        this.price = price;
        this.publishedYear = publishedYear;
        this.genre = genre;
        this.author = author;
    }

    public @Nullable Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getIsbn() {
        return isbn;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getPublishedYear() {
        return publishedYear;
    }

    public Genre getGenre() {
        return genre;
    }

    public Author getAuthor() {
        return author;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Book.class.hashCode();
    }

    @Override
    public String toString() {
        // Inkludera aldrig lazy-relationer i toString – det kan trigga databasfrågor.
        return "Book[id=%s, title=%s, isbn=%s]".formatted(id, title, isbn);
    }
}
