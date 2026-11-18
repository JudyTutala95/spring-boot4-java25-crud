package se.jtutala.library.resilience;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import se.jtutala.library.book.Genre;

/// Låtsas-implementation som misslyckas **varannan** gång – deterministiskt, så att
/// det är lätt att se `@Retryable` i aktion i loggen.
@Component
class FakeRemoteCatalog implements RemoteCatalog {

    private final AtomicInteger calls = new AtomicInteger();

    @Override
    public List<String> fetchPopularTitles(Genre genre) {
        if (calls.incrementAndGet() % 2 == 1) {
            throw new RemoteCatalogUnavailableException("Katalogen svarade inte (simulerat fel)");
        }
        return switch (genre) {
            case FANTASY -> List.of("The Hobbit", "Mistborn", "Earthsea");
            case SCIENCE_FICTION -> List.of("Dune", "Foundation", "Hyperion");
            case TECHNOLOGY -> List.of("Effective Java", "Clean Code", "Java Concurrency in Practice");
            case CHILDREN -> List.of("Pippi Långstrump", "Mio min Mio", "Alfons Åberg");
            case CRIME, BIOGRAPHY, OTHER -> List.of();
        };
    }
}
