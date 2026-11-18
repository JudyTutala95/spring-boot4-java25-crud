package se.jtutala.library.book;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/// Repository för böcker. Visar tre sätt att skriva frågor:
///
/// 1. **Ärvda metoder** från `JpaRepository` (`save`, `findById`, `delete` ...).
/// 2. **Derived queries** – Spring Data tolkar metodnamnet (`existsByIsbn`).
/// 3. **`@Query` med JPQL** – för allt som blir för långt som metodnamn.
public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    boolean existsByAuthorId(Long authorId);

    /// `@EntityGraph` hämtar författaren i SAMMA fråga (join fetch).
    /// Utan den skulle `book.getAuthor().getName()` trigga en extra SELECT
    /// – och för en lista med N böcker blir det N extra frågor ("N+1-problemet").
    @EntityGraph(attributePaths = "author")
    Optional<Book> findWithAuthorById(Long id);

    /// Sökning med valfria filter. `:genre is null or ...` gör att ett filter
    /// ignoreras när det inte skickas. Titeln skickas som "" i stället för null
    /// (se `BookService`) – då blir LIKE-uttrycket alltid giltigt.
    ///
    /// Text block (`"""`, Java 15) gör flerradiga frågor läsbara.
    @EntityGraph(attributePaths = "author")
    @Query("""
            select b from Book b
            where (:genre is null or b.genre = :genre)
              and (:authorId is null or b.author.id = :authorId)
              and lower(b.title) like lower(concat('%', :title, '%'))
            """)
    Page<Book> search(@Nullable Genre genre, @Nullable Long authorId, String title, Pageable pageable);
}
