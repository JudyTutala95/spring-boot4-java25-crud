package se.jtutala.library.book.event;

/// Domänhändelser som `BookService` publicerar.
///
/// **Sealed interface + records** = en *algebraisk datatyp*: en händelse är EXAKT en av
/// de tre varianterna. Tillsammans med pattern matching i `switch` (Java 21) får vi
/// typsäker, uttömmande hantering utan `instanceof`-kedjor. Se `BookEventListener`.
public sealed interface BookEvent {

    long bookId();

    record BookCreated(long bookId, String title, String authorName) implements BookEvent {
    }

    record BookUpdated(long bookId, String title, long newVersion) implements BookEvent {
    }

    record BookDeleted(long bookId, String title) implements BookEvent {
    }
}
