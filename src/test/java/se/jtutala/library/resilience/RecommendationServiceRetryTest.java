package se.jtutala.library.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import se.jtutala.library.book.Genre;
import se.jtutala.library.config.ResilienceConfig;
import se.jtutala.library.resilience.RemoteCatalog.RemoteCatalogUnavailableException;

/// `@Retryable` fungerar bara via en Spring-proxy – därför behövs en (liten) Spring-kontext.
///
/// `@SpringJUnitConfig` startar en minimal kontext med bara de klasser vi anger –
/// mycket snabbare än `@SpringBootTest`, ingen auto-konfiguration alls.
@SpringJUnitConfig(classes = {ResilienceConfig.class, RecommendationService.class})
class RecommendationServiceRetryTest {

    @Autowired
    RecommendationService service;

    @MockitoBean
    RemoteCatalog remoteCatalog;

    @Test
    void forsokerIgenVidTillfalligtFel() {
        given(remoteCatalog.fetchPopularTitles(Genre.FANTASY))
                .willThrow(new RemoteCatalogUnavailableException("nere"))
                .willThrow(new RemoteCatalogUnavailableException("nere igen"))
                .willReturn(List.of("Earthsea"));

        assertThat(service.recommendationsFor(Genre.FANTASY)).containsExactly("Earthsea");

        then(remoteCatalog).should(times(3)).fetchPopularTitles(Genre.FANTASY);
    }

    @Test
    void gerUppEfterMaxAntalForsok() {
        given(remoteCatalog.fetchPopularTitles(Genre.CRIME)).willThrow(new RemoteCatalogUnavailableException("nere"));

        assertThatThrownBy(() -> service.recommendationsFor(Genre.CRIME))
                .isInstanceOf(RemoteCatalogUnavailableException.class);

        then(remoteCatalog).should(times(4)).fetchPopularTitles(Genre.CRIME); // 1 försök + 3 retries
    }

    @Test
    void forsokerInteIgenVidAndraFel() {
        given(remoteCatalog.fetchPopularTitles(Genre.OTHER)).willThrow(new IllegalArgumentException("bugg"));

        assertThatThrownBy(() -> service.recommendationsFor(Genre.OTHER)).isInstanceOf(IllegalArgumentException.class);

        then(remoteCatalog).should(times(1)).fetchPopularTitles(Genre.OTHER);
    }
}
