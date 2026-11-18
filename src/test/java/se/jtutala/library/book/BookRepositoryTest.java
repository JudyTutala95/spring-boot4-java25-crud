package se.jtutala.library.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import jakarta.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import se.jtutala.library.author.Author;
import se.jtutala.library.author.AuthorRepository;
import se.jtutala.library.config.JpaConfig;

/// **Slice-test för persistenslagret** med `@DataJpaTest`.
///
/// - Startar bara JPA, Flyway och en in-memory-databas – inga controllers eller services.
/// - Varje test körs i en transaktion som **rullas tillbaka** efteråt → testerna påverkar inte varandra.
/// - Flyway kör `V1` och `V2`, så exempeldatan (5 böcker, 3 författare) finns.
/// - `@Import(JpaConfig.class)` behövs för att auditing (`createdAt`) ska fungera.
@DataJpaTest
@Import(JpaConfig.class)
class BookRepositoryTest {

    @Autowired
    BookRepository books;
    @Autowired
    AuthorRepository authors;
    @Autowired
    EntityManager em;

    @Test
    void flywayHarLagtInExempeldata() {
        assertThat(books.count()).isEqualTo(5);
        assertThat(books.existsByIsbn("9780134685991")).isTrue();
    }

    @Test
    void sparadBokFarIdVersionOchTidsstamplar() {
        Author author = authors.save(new Author("Testförfattare", 1980));
        Book saved = books.saveAndFlush(new Book("Testbok", "9789999999991", new BigDecimal("99.50"), 2020, Genre.OTHER, author));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void versionOkarVidUppdatering() {
        Book book = books.findById(1L).orElseThrow();
        long before = book.getVersion();

        book.update("Pippi (ny utgåva)", book.getIsbn(), book.getPrice(), book.getPublishedYear(), book.getGenre(), book.getAuthor());
        books.flush();

        assertThat(book.getVersion()).isEqualTo(before + 1);
    }

    @Test
    void unikIsbnUpprätthallsAvDatabasen() {
        Author author = authors.findById(1L).orElseThrow();

        assertThatThrownBy(() -> books.saveAndFlush(
                new Book("Kopia", "9789129697711", BigDecimal.TEN, 2000, Genre.CHILDREN, author)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sokningFiltrerarPaGenreOchTitel() {
        var page = books.search(Genre.FANTASY, null, "earth", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Book::getTitle).containsExactly("A Wizard of Earthsea");
    }

    @Test
    void sokningUtanFilterReturnerarAllaSorterade() {
        var page = books.search(null, null, "", PageRequest.of(0, 2, Sort.by("publishedYear")));

        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).extracting(Book::getPublishedYear).containsExactly(1945, 1968);
    }

    @Test
    void sokningPaForfattare() {
        var page = books.search(null, 3L, "", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2).allMatch(b -> b.getAuthor().getName().equals("Ursula K. Le Guin"));
    }

    @Test
    void entityGraphLaddarForfattarenDirekt() {
        em.clear(); // töm första nivåns cache så att vi verkligen läser från databasen

        Book book = books.findWithAuthorById(3L).orElseThrow();

        assertThat(Hibernate.isInitialized(book.getAuthor())).as("författaren ska vara hämtad via join").isTrue();
    }

    @Test
    void vanligFindByIdLaddarForfattarenLazy() {
        em.clear();

        Book book = books.findById(3L).orElseThrow();

        assertThat(Hibernate.isInitialized(book.getAuthor())).as("LAZY – bara en proxy").isFalse();
    }

    @Test
    void existsByAuthorId() {
        assertThat(books.existsByAuthorId(1L)).isTrue();
        Author lonely = authors.save(new Author("Utan böcker", null));
        assertThat(books.existsByAuthorId(lonely.getId())).isFalse();
    }
}
