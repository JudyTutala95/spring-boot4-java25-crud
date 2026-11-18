package se.jtutala.library.book.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import se.jtutala.library.book.event.BookEvent.BookCreated;
import se.jtutala.library.book.event.BookEvent.BookDeleted;
import se.jtutala.library.book.event.BookEvent.BookUpdated;

/// Lyssnar på `BookEvent`s.
///
/// `@TransactionalEventListener(phase = AFTER_COMMIT)`: körs först när transaktionen som
/// publicerade händelsen har committats. Rullas transaktionen tillbaka körs lyssnaren inte –
/// vi skickar alltså aldrig ut "boken skapades" för en bok som inte finns.
///
/// Här loggar vi bara, men i verkligheten kan det vara e-post, en Kafka-publicering,
/// cache-invalidering osv. Det frikopplar sidoeffekter från affärslogiken.
@Component
public class BookEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookEvent event) {
        log.info(describe(event));
    }

    /// **Record patterns** (Java 21) plockar isär recorden direkt i `case`.
    /// **Unnamed variables `_`** (Java 22) markerar komponenter vi inte bryr oss om.
    /// Ingen `default` behövs – `BookEvent` är sealed, så switchen är uttömmande.
    static String describe(BookEvent event) {
        return switch (event) {
            case BookCreated(long id, String title, String author) ->
                    "Ny bok #%d: '%s' av %s".formatted(id, title, author);
            case BookUpdated(long id, String title, long version) when version > 10 ->
                    "Bok #%d '%s' uppdaterad (ofta ändrad: version %d)".formatted(id, title, version);
            case BookUpdated(long id, String title, _) ->
                    "Bok #%d '%s' uppdaterad".formatted(id, title);
            case BookDeleted(long id, String title) ->
                    "Bok #%d '%s' borttagen".formatted(id, title);
        };
    }
}
