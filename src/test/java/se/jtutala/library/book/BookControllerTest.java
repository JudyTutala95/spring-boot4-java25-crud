package se.jtutala.library.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import se.jtutala.library.book.dto.BookResponse;
import se.jtutala.library.book.dto.BookResponseV2;
import se.jtutala.library.book.dto.CreateBookRequest;
import se.jtutala.library.common.exception.ConflictException;
import se.jtutala.library.common.exception.ResourceNotFoundException;
import se.jtutala.library.common.web.CorrelationIdFilter;
import se.jtutala.library.TestData;
import se.jtutala.library.resilience.RecommendationService;

/// **Slice-test för webblagret** med `@WebMvcTest`.
///
/// Startar bara MVC-delen av Spring (controllers, `@RestControllerAdvice`, filter,
/// Jackson, validering) – inte JPA, inte services. Services ersätts med `@MockitoBean`.
///
/// Nyheter jämfört med Spring Boot 3:
/// - `@MockitoBean` (Spring Framework) ersätter Boots borttagna `@MockBean`.
/// - `@WebMvcTest` ligger i paketet `org.springframework.boot.webmvc.test.autoconfigure`
///   (Boot 4 är uppdelat i moduler, test-stöd kommer från `spring-boot-starter-webmvc-test`).
/// - `MockMvcTester` ger AssertJ-stil i stället för `andExpect(...)`-kedjor.
@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    BookService bookService;

    @MockitoBean
    RecommendationService recommendationService;

    BookResponse effectiveJava = new BookResponse(1L, "Effective Java", "9780134685991", new BigDecimal("499.00"),
            2018, Genre.TECHNOLOGY, 2L, "Joshua Bloch", 0L, TestData.NOW, TestData.NOW);

    @Test
    void getUtanVersionHeaderGerVersion1() {
        given(bookService.findById(1L)).willReturn(effectiveJava);

        assertThat(mvc.get().uri("/api/books/1"))
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON)
                .bodyJson()
                .extractingPath("$.authorName").isEqualTo("Joshua Bloch");
    }

    @Test
    void apiVersion2GerNastladForfattareOchValuta() {
        given(bookService.findByIdV2(1L)).willReturn(new BookResponseV2(1L, "Effective Java", "9780134685991",
                new BookResponseV2.Price(new BigDecimal("499.00"), "SEK"), 2018,
                new BookResponseV2.GenreInfo(Genre.TECHNOLOGY, "Teknik"),
                new BookResponseV2.AuthorSummary(2L, "Joshua Bloch"), 0L, TestData.NOW, TestData.NOW));

        var result = mvc.get().uri("/api/books/1").header("API-Version", "2").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.author.name").isEqualTo("Joshua Bloch");
        assertThat(result).bodyJson().extractingPath("$.price.currency").isEqualTo("SEK");
        then(bookService).should(never()).findById(1L);
    }

    @Test
    void okandApiVersionGer400() {
        assertThat(mvc.get().uri("/api/books/1").header("API-Version", "9"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void saknadBokGer404SomProblemDetail() {
        given(bookService.findById(99L)).willThrow(new ResourceNotFoundException("Bok", 99L));

        var result = mvc.get().uri("/api/books/99").exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Bok med id 99 hittades inte");
        assertThat(result).bodyJson().extractingPath("$.type").isEqualTo("urn:problem:not-found");
        assertThat(result).bodyJson().extractingPath("$.correlationId").isNotNull();
    }

    @Test
    void negativtIdGer400() {
        assertThat(mvc.get().uri("/api/books/-1")).hasStatus(HttpStatus.BAD_REQUEST);
        then(bookService).shouldHaveNoInteractions();
    }

    @Test
    void postSkaparBokOchReturnerar201MedLocation() {
        given(bookService.create(any(CreateBookRequest.class))).willReturn(effectiveJava);

        // Text block (Java 15) – läsbar JSON direkt i testet
        var result = mvc.post().uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "  Effective Java  ",
                          "isbn": "978-0-13-468599-1",
                          "price": 499.00,
                          "publishedYear": 2018,
                          "genre": "TECHNOLOGY",
                          "authorId": 2
                        }
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).headers().hasValue("Location", "http://localhost/api/books/1");
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(1);
        // DTO:ns kompakta konstruktor ska ha normaliserat titel och ISBN
        then(bookService).should().create(new CreateBookRequest("Effective Java", "9780134685991",
                new BigDecimal("499.00"), 2018, Genre.TECHNOLOGY, 2L));
    }

    @Test
    void ogiltigBodyGer400MedFaltfel() {
        var result = mvc.post().uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "",
                          "isbn": "123",
                          "price": -5,
                          "publishedYear": 3000,
                          "authorId": 2
                        }
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Valideringsfel");
        assertThat(result).bodyJson().extractingPath("$.errors").asMap()
                .containsOnlyKeys("title", "isbn", "price", "publishedYear", "genre");
        assertThat(result).bodyJson().extractingPath("$.errors.publishedYear")
                .isEqualTo("årtalet får inte ligga i framtiden");
        then(bookService).shouldHaveNoInteractions();
    }

    @Test
    void trasigJsonGer400() {
        assertThat(mvc.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content("{ inte json"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void konfliktGer409() {
        given(bookService.update(eq(1L), any())).willThrow(new ConflictException("Boken har ändrats av någon annan"));

        assertThat(mvc.put().uri("/api/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"X","isbn":"9780134685991","price":1,"publishedYear":2000,
                         "genre":"OTHER","authorId":2,"version":0}
                        """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.detail").isEqualTo("Boken har ändrats av någon annan");
    }

    @Test
    void deleteGer204() {
        assertThat(mvc.delete().uri("/api/books/1")).hasStatus(HttpStatus.NO_CONTENT);
        then(bookService).should().delete(1L);
    }

    @Test
    void sokningBinderQueryParametrarTillRecord() {
        given(bookService.search(any(), any())).willReturn(
                new se.jtutala.library.common.dto.PageResponse<>(List.of(effectiveJava), 0, 20, 1, 1, true));

        assertThat(mvc.get().uri("/api/books?genre=TECHNOLOGY&title=java&sort=publishedYear,desc"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.content[0].title").isEqualTo("Effective Java");

        then(bookService).should().search(
                eq(new se.jtutala.library.book.dto.BookSearchCriteria(Genre.TECHNOLOGY, "java", null)), any());
    }

    @Test
    void sorteringPaOtillatetFaltGer400() {
        assertThat(mvc.get().uri("/api/books?sort=author.name"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").asString().startsWith("Ogiltigt sorteringsfält: author.name");
        then(bookService).shouldHaveNoInteractions();
    }

    @Test
    void korrelationsIdSkickasTillbaka() {
        given(bookService.findById(1L)).willReturn(effectiveJava);

        assertThat(mvc.get().uri("/api/books/1").header(CorrelationIdFilter.HEADER, "abc-123"))
                .hasStatusOk()
                .headers().hasValue(CorrelationIdFilter.HEADER, "abc-123");
    }

    @Test
    void rekommendationer() {
        given(recommendationService.recommendationsFor(Genre.FANTASY)).willReturn(List.of("Earthsea"));

        assertThat(mvc.get().uri("/api/books/recommendations?genre=FANTASY"))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("[\"Earthsea\"]");
    }
}
