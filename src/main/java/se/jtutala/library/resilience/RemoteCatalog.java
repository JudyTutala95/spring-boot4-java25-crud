package se.jtutala.library.resilience;

import java.util.List;

import se.jtutala.library.book.Genre;

/// Ett externt system (t.ex. en annan tjänst via HTTP) som kan vara instabilt.
///
/// Att ha ett interface gör det enkelt att byta implementation i tester.
/// I ett riktigt projekt skulle detta kunna vara en **HTTP Interface-klient**
/// (`@HttpExchange` + `@ImportHttpServices`, nytt i Spring Framework 7).
public interface RemoteCatalog {

    List<String> fetchPopularTitles(Genre genre);

    /// Kastas när fjärrsystemet inte svarar. Ett tillfälligt fel som är värt att försöka igen.
    final class RemoteCatalogUnavailableException extends RuntimeException {
        public RemoteCatalogUnavailableException(String message) {
            super(message);
        }
    }
}
