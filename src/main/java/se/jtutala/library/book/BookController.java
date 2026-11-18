package se.jtutala.library.book;

import java.net.URI;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import se.jtutala.library.book.dto.BookResponse;
import se.jtutala.library.book.dto.BookResponseV2;
import se.jtutala.library.book.dto.BookSearchCriteria;
import se.jtutala.library.book.dto.CreateBookRequest;
import se.jtutala.library.book.dto.UpdateBookRequest;
import se.jtutala.library.common.dto.PageResponse;
import se.jtutala.library.common.web.SortGuard;
import se.jtutala.library.resilience.RecommendationService;

/// REST-API för böcker.
///
/// Utöver vanlig CRUD visar den:
/// - **API-versionering (Spring Framework 7)**: `GET /api/books/{id}` finns i två versioner.
///   Klienten väljer med headern `API-Version` (konfigurerat i `application.properties`).
///   Mappningar *utan* `version` matchar alla versioner.
/// - **Sökning + sidindelning** med en record som filterobjekt.
/// - **`@Retryable`** via `RecommendationService`.
@RestController
@RequestMapping("/api/books")
public class BookController {

    private static final Set<String> SORTABLE = Set.of("title", "publishedYear", "price", "genre", "createdAt");

    private final BookService bookService;
    private final RecommendationService recommendationService;

    public BookController(BookService bookService, RecommendationService recommendationService) {
        this.bookService = bookService;
        this.recommendationService = recommendationService;
    }

    /// `GET /api/books?genre=FANTASY&title=earth&page=0&size=10&sort=publishedYear,desc`
    @GetMapping
    public PageResponse<BookResponse> search(@Valid BookSearchCriteria criteria,
                                             @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return bookService.search(criteria, SortGuard.requireAllowed(pageable, SORTABLE));
    }

    /// Version 1 (standard när headern saknas).
    @GetMapping(path = "/{id}", version = "1")
    public BookResponse getV1(@PathVariable @Positive long id) {
        return bookService.findById(id);
    }

    /// Version 2: `curl -H "API-Version: 2" localhost:8080/api/books/1`
    @GetMapping(path = "/{id}", version = "2")
    public BookResponseV2 getV2(@PathVariable @Positive long id) {
        return bookService.findByIdV2(id);
    }

    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody CreateBookRequest request) {
        BookResponse created = bookService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public BookResponse update(@PathVariable @Positive long id, @Valid @RequestBody UpdateBookRequest request) {
        return bookService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive long id) {
        bookService.delete(id);
    }

    /// `GET /api/books/recommendations?genre=FANTASY` – anropar ett (simulerat) instabilt
    /// externt system. Titta i loggen: första försöket misslyckas, retry lyckas.
    @GetMapping("/recommendations")
    public List<String> recommendations(@RequestParam Genre genre) {
        return recommendationService.recommendationsFor(genre);
    }
}
