package se.jtutala.library.book;

import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.library.author.Author;
import se.jtutala.library.author.AuthorService;
import se.jtutala.library.book.dto.BookResponse;
import se.jtutala.library.book.dto.BookResponseV2;
import se.jtutala.library.book.dto.BookSearchCriteria;
import se.jtutala.library.book.dto.CreateBookRequest;
import se.jtutala.library.book.dto.UpdateBookRequest;
import se.jtutala.library.book.event.BookEvent;
import se.jtutala.library.common.dto.PageResponse;
import se.jtutala.library.common.exception.ConflictException;
import se.jtutala.library.common.exception.ResourceNotFoundException;

/// Affärslogik för böcker. Se `AuthorService` för förklaring av transaktioner och injektion.
///
/// Flödet för en skrivning:
/// ```
/// Controller ──CreateBookRequest──▶ Service ──Book──▶ Repository ──SQL──▶ DB
///     ▲                                │
///     └────────────BookResponse────────┘  (+ BookEvent publiceras)
/// ```
@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorService authorService;
    private final BookMapper mapper;
    private final ApplicationEventPublisher events;

    public BookService(BookRepository bookRepository, AuthorService authorService,
                       BookMapper mapper, ApplicationEventPublisher events) {
        this.bookRepository = bookRepository;
        this.authorService = authorService;
        this.mapper = mapper;
        this.events = events;
    }

    public PageResponse<BookResponse> search(BookSearchCriteria criteria, Pageable pageable) {
        String title = Objects.requireNonNullElse(criteria.title(), "");
        var page = bookRepository.search(criteria.genre(), criteria.authorId(), title, pageable);
        return PageResponse.of(page, mapper::toResponse);
    }

    public BookResponse findById(long id) {
        return mapper.toResponse(getBook(id));
    }

    public BookResponseV2 findByIdV2(long id) {
        return mapper.toResponseV2(getBook(id));
    }

    @Transactional
    public BookResponse create(CreateBookRequest request) {
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new ConflictException("En bok med ISBN %s finns redan".formatted(request.isbn()));
        }
        Author author = authorService.getAuthor(request.authorId());
        Book saved = bookRepository.save(mapper.toEntity(request, author));

        events.publishEvent(new BookEvent.BookCreated(Objects.requireNonNull(saved.getId()), saved.getTitle(), author.getName()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public BookResponse update(long id, UpdateBookRequest request) {
        Book book = getBook(id);
        if (book.getVersion() != request.version()) {
            throw new ConflictException("Boken har ändrats av någon annan (aktuell version %d, du skickade %d). Hämta den igen."
                    .formatted(book.getVersion(), request.version()));
        }
        if (bookRepository.existsByIsbnAndIdNot(request.isbn(), id)) {
            throw new ConflictException("En annan bok har redan ISBN %s".formatted(request.isbn()));
        }
        Author author = authorService.getAuthor(request.authorId());

        book.update(request.title(), request.isbn(), request.price(), request.publishedYear(), request.genre(), author);
        bookRepository.flush(); // så att version/updatedAt i svaret är uppdaterade

        events.publishEvent(new BookEvent.BookUpdated(id, book.getTitle(), book.getVersion()));
        return mapper.toResponse(book);
    }

    @Transactional
    public void delete(long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bok", id));
        bookRepository.delete(book);
        events.publishEvent(new BookEvent.BookDeleted(id, book.getTitle()));
    }

    private Book getBook(long id) {
        return bookRepository.findWithAuthorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bok", id));
    }
}
