package se.jtutala.library.book;

import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import se.jtutala.library.author.Author;
import se.jtutala.library.book.dto.BookResponse;
import se.jtutala.library.book.dto.BookResponseV2;
import se.jtutala.library.book.dto.CreateBookRequest;

/// Mappning entitet ⇄ DTO för böcker. En metod per riktning och API-version.
///
/// Mapparen anropas från service-lagret *inom transaktionen*, så lazy-relationen
/// `book.getAuthor()` kan läsas säkert (Open Session In View är avstängt).
@Component
public class BookMapper {

    static final String CURRENCY = "SEK";

    public Book toEntity(CreateBookRequest request, Author author) {
        return new Book(request.title(), request.isbn(), request.price(),
                request.publishedYear(), request.genre(), author);
    }

    public BookResponse toResponse(Book book) {
        Author author = book.getAuthor();
        return new BookResponse(
                requireId(book.getId()),
                book.getTitle(),
                book.getIsbn(),
                book.getPrice(),
                book.getPublishedYear(),
                book.getGenre(),
                requireId(author.getId()),
                author.getName(),
                book.getVersion(),
                book.getCreatedAt(),
                book.getUpdatedAt());
    }

    public BookResponseV2 toResponseV2(Book book) {
        Author author = book.getAuthor();
        return new BookResponseV2(
                requireId(book.getId()),
                book.getTitle(),
                book.getIsbn(),
                new BookResponseV2.Price(book.getPrice(), CURRENCY),
                book.getPublishedYear(),
                new BookResponseV2.GenreInfo(book.getGenre(), book.getGenre().label()),
                new BookResponseV2.AuthorSummary(requireId(author.getId()), author.getName()),
                book.getVersion(),
                book.getCreatedAt(),
                book.getUpdatedAt());
    }

    private static Long requireId(@Nullable Long id) {
        return Objects.requireNonNull(id, "entiteten saknar id – är den sparad?");
    }
}
