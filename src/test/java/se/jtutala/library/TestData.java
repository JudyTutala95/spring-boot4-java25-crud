package se.jtutala.library;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;

import se.jtutala.library.author.Author;
import se.jtutala.library.book.Book;
import se.jtutala.library.book.Genre;
import se.jtutala.library.book.dto.CreateBookRequest;
import se.jtutala.library.book.dto.UpdateBookRequest;

/// "Test data builder"/object mother: en plats som skapar testobjekt,
/// så att testerna inte fylls av brus och bara visar det som är viktigt.
public final class TestData {

    public static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private TestData() {
    }

    public static Author author(long id, String name) {
        Author author = new Author(name, 1950);
        set(author, "id", id);
        set(author, "createdAt", NOW);
        set(author, "updatedAt", NOW);
        return author;
    }

    public static Book book(long id, String title, Author author) {
        Book book = new Book(title, "9780134685991", new BigDecimal("499.00"), 2018, Genre.TECHNOLOGY, author);
        set(book, "id", id);
        set(book, "createdAt", NOW);
        set(book, "updatedAt", NOW);
        return book;
    }

    public static CreateBookRequest createBookRequest(long authorId) {
        return new CreateBookRequest("Effective Java", "978-0-13-468599-1", new BigDecimal("499.00"),
                2018, Genre.TECHNOLOGY, authorId);
    }

    public static UpdateBookRequest updateBookRequest(long authorId, long version) {
        return new UpdateBookRequest("Effective Java, 3rd ed.", "9780134685991", new BigDecimal("549.00"),
                2018, Genre.TECHNOLOGY, authorId, version);
    }

    /// Entiteter har (med flit) inga setters för id och tidsstämplar – de sätts av
    /// Hibernate. I enhetstester utan databas sätter vi dem med reflection.
    public static void set(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
