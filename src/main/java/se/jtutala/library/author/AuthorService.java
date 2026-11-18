package se.jtutala.library.author;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.library.author.dto.AuthorResponse;
import se.jtutala.library.author.dto.CreateAuthorRequest;
import se.jtutala.library.author.dto.UpdateAuthorRequest;
import se.jtutala.library.book.BookRepository;
import se.jtutala.library.common.dto.PageResponse;
import se.jtutala.library.common.exception.ConflictException;
import se.jtutala.library.common.exception.ResourceNotFoundException;

/// Affärslogik för författare.
///
/// - **Konstruktorinjektion** utan `@Autowired`: har klassen en enda konstruktor använder
///   Spring den automatiskt. Fälten kan vara `final` och klassen går att testa med `new`.
/// - **`@Transactional(readOnly = true)` på klassnivå**: alla metoder läser i en transaktion
///   (Hibernate kan då hoppa över dirty checking). Skrivande metoder överskuggar med `@Transactional`.
/// - Service-lagret tar emot och returnerar **DTO:er**. Entiteter lämnar aldrig denna klass.
@Service
@Transactional(readOnly = true)
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final AuthorMapper mapper;

    public AuthorService(AuthorRepository authorRepository, BookRepository bookRepository, AuthorMapper mapper) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
        this.mapper = mapper;
    }

    public PageResponse<AuthorResponse> findAll(Pageable pageable) {
        return PageResponse.of(authorRepository.findAll(pageable), mapper::toResponse);
    }

    public AuthorResponse findById(long id) {
        return mapper.toResponse(getAuthor(id));
    }

    @Transactional
    public AuthorResponse create(CreateAuthorRequest request) {
        if (authorRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Författaren '%s' finns redan".formatted(request.name()));
        }
        Author saved = authorRepository.save(mapper.toEntity(request));
        // saveAndFlush skulle tvinga fram INSERT direkt; save räcker eftersom vi
        // har IDENTITY-id (Hibernate måste göra INSERT för att få id:t).
        return mapper.toResponse(saved);
    }

    @Transactional
    public AuthorResponse update(long id, UpdateAuthorRequest request) {
        Author author = getAuthor(id);
        if (author.getVersion() != request.version()) {
            throw new ConflictException("Författaren har ändrats av någon annan (aktuell version %d, du skickade %d)"
                    .formatted(author.getVersion(), request.version()));
        }
        author.update(request.name(), request.birthYear());
        // Inget save() behövs! Entiteten är "managed" – Hibernate upptäcker ändringen
        // (dirty checking) och gör UPDATE när transaktionen committas.
        // Vi flushar ändå så att version/updatedAt i svaret är de nya värdena.
        authorRepository.flush();
        return mapper.toResponse(author);
    }

    @Transactional
    public void delete(long id) {
        Author author = getAuthor(id);
        if (bookRepository.existsByAuthorId(id)) {
            throw new ConflictException("Författaren har böcker och kan inte tas bort. Ta bort böckerna först.");
        }
        authorRepository.delete(author);
    }

    /// Hjälpmetod för andra services (t.ex. `BookService`) som behöver själva entiteten.
    /// Package-private vore snyggast, men BookService ligger i ett annat paket.
    public Author getAuthor(long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Författare", id));
    }
}
