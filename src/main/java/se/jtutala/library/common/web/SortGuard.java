package se.jtutala.library.common.web;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/// Släpper bara igenom sortering på uttryckligen tillåtna fält.
///
/// Varför? `?sort=...` kommer direkt från klienten och hamnar i SQL:ens `ORDER BY`.
/// Utan kontroll kan klienten sortera på interna fält (eller relationer som ger tunga joins),
/// och ett okänt fält ger ett 500-fel djupt nere i Hibernate. En allowlist ger tydligt 400.
public final class SortGuard {

    private SortGuard() {
    }

    public static Pageable requireAllowed(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ogiltigt sorteringsfält: %s. Tillåtna: %s".formatted(order.getProperty(), allowed));
            }
        }
        return pageable;
    }
}
