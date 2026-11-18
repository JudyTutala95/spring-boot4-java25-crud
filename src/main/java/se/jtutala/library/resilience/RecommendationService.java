package se.jtutala.library.resilience;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import se.jtutala.library.book.Genre;
import se.jtutala.library.resilience.RemoteCatalog.RemoteCatalogUnavailableException;

/// **Spring Framework 7 – inbyggd resiliens** (kräver `@EnableResilientMethods`, se `ResilienceConfig`).
///
/// - `@Retryable`: misslyckas anropet med `RemoteCatalogUnavailableException` görs upp till
///   3 nya försök, med väntetid 100 ms → 200 ms → 400 ms (exponentiell backoff).
///   Andra undantag (t.ex. `IllegalArgumentException`) försöks INTE igen.
/// - `@ConcurrencyLimit(10)`: högst 10 samtidiga anrop – skyddar fjärrsystemet.
///   Extra viktigt med virtuella trådar, där det annars kan bli tusentals samtidiga anrop.
///
/// Fungerar via en Spring-proxy: anropet måste komma *utifrån* beanen
/// (ett anrop från en annan metod i samma klass går förbi proxyn och får ingen retry).
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final RemoteCatalog remoteCatalog;

    public RecommendationService(RemoteCatalog remoteCatalog) {
        this.remoteCatalog = remoteCatalog;
    }

    @Retryable(includes = RemoteCatalogUnavailableException.class, maxRetries = 3, delay = 100, multiplier = 2)
    @ConcurrencyLimit(10)
    public List<String> recommendationsFor(Genre genre) {
        log.info("Hämtar rekommendationer för {}", genre);
        return remoteCatalog.fetchPopularTitles(genre);
    }
}
