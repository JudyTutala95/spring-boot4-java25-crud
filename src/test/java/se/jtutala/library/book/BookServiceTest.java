package se.jtutala.library.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static se.jtutala.library.TestData.author;
import static se.jtutala.library.TestData.book;
import static se.jtutala.library.TestData.createBookRequest;
import static se.jtutala.library.TestData.updateBookRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import se.jtutala.library.author.Author;
import se.jtutala.library.author.AuthorService;
import se.jtutala.library.book.dto.BookSearchCriteria;
import se.jtutala.library.book.event.BookEvent;
import se.jtutala.library.common.exception.ConflictException;
import se.jtutala.library.common.exception.ResourceNotFoundException;

/// **Rent enhetstest** – ingen Spring, ingen databas. Körs på millisekunder.
///
/// Beroenden ersätts med Mockito-mockar (`@Mock`). Den riktiga `BookMapper` används
/// eftersom den inte har några beroenden – mocka bara det som behövs.
///
/// Struktur: **Given / When / Then** (BDDMockito: `given(...)`, `then(...)`).
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    BookRepository bookRepository;
    @Mock
    AuthorService authorService;
    @Mock
    ApplicationEventPublisher events;
    @Captor
    ArgumentCaptor<BookEvent> eventCaptor;

    BookService service;
    Author bloch = author(2L, "Joshua Bloch");

    @BeforeEach
    void setUp() {
        service = new BookService(bookRepository, authorService, new BookMapper(), events);
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        void returnerarDtoNarBokenFinns() {
            given(bookRepository.findWithAuthorById(1L)).willReturn(Optional.of(book(1L, "Effective Java", bloch)));

            var response = service.findById(1L);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.title()).isEqualTo("Effective Java");
            assertThat(response.authorName()).isEqualTo("Joshua Bloch");
        }

        @Test
        void kastarNotFoundNarBokenSaknas() {
            given(bookRepository.findWithAuthorById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        void spararBokOchPubliceraHandelse() {
            var request = createBookRequest(2L);
            given(bookRepository.existsByIsbn("9780134685991")).willReturn(false);
            given(authorService.getAuthor(2L)).willReturn(bloch);
            // save() returnerar entiteten med id satt – precis som Hibernate gör
            given(bookRepository.save(any(Book.class))).willAnswer(inv -> {
                Book b = inv.getArgument(0);
                se.jtutala.library.TestData.set(b, "id", 10L);
                return b;
            });

            var response = service.create(request);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.isbn()).as("ISBN ska vara normaliserat").isEqualTo("9780134685991");
            then(events).should().publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue()).isEqualTo(new BookEvent.BookCreated(10L, "Effective Java", "Joshua Bloch"));
        }

        @Test
        void avvisarDubblettIsbn() {
            given(bookRepository.existsByIsbn("9780134685991")).willReturn(true);

            assertThatThrownBy(() -> service.create(createBookRequest(2L)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("finns redan");

            then(bookRepository).should(never()).save(any());
            then(events).shouldHaveNoInteractions();
        }

        @Test
        void kastarNotFoundForOkandForfattare() {
            given(authorService.getAuthor(42L)).willThrow(new ResourceNotFoundException("Författare", 42L));

            assertThatThrownBy(() -> service.create(createBookRequest(42L)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        void uppdaterarNarVersionenStammer() {
            Book existing = book(1L, "Effective Java", bloch);
            given(bookRepository.findWithAuthorById(1L)).willReturn(Optional.of(existing));
            given(authorService.getAuthor(2L)).willReturn(bloch);

            var response = service.update(1L, updateBookRequest(2L, 0L));

            assertThat(response.title()).isEqualTo("Effective Java, 3rd ed.");
            assertThat(response.price()).isEqualByComparingTo(new BigDecimal("549.00"));
            then(bookRepository).should().flush();
            then(events).should().publishEvent(any(BookEvent.BookUpdated.class));
        }

        @Test
        void avvisarGammalVersion() {
            Book existing = book(1L, "Effective Java", bloch);
            se.jtutala.library.TestData.set(existing, "version", 3L);
            given(bookRepository.findWithAuthorById(1L)).willReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.update(1L, updateBookRequest(2L, 2L)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("aktuell version 3");

            assertThat(existing.getTitle()).as("entiteten får inte ändras").isEqualTo("Effective Java");
        }

        @Test
        void avvisarIsbnSomEnAnnanBokHar() {
            given(bookRepository.findWithAuthorById(1L)).willReturn(Optional.of(book(1L, "Effective Java", bloch)));
            given(bookRepository.existsByIsbnAndIdNot("9780134685991", 1L)).willReturn(true);

            assertThatThrownBy(() -> service.update(1L, updateBookRequest(2L, 0L)))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        void tarBortOchPubliceraHandelse() {
            Book existing = book(1L, "Effective Java", bloch);
            given(bookRepository.findById(1L)).willReturn(Optional.of(existing));

            service.delete(1L);

            then(bookRepository).should().delete(existing);
            then(events).should().publishEvent(new BookEvent.BookDeleted(1L, "Effective Java"));
        }

        @Test
        void kastarNotFoundNarBokenSaknas() {
            given(bookRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResourceNotFoundException.class);
            then(bookRepository).should(never()).delete(any());
        }
    }

    @Test
    void searchSkickarTomTitelNarFiltretSaknas() {
        var pageable = PageRequest.of(0, 20);
        given(bookRepository.search(Genre.TECHNOLOGY, null, "", pageable))
                .willReturn(new PageImpl<>(List.of(book(1L, "Effective Java", bloch)), pageable, 1));

        var page = service.search(new BookSearchCriteria(Genre.TECHNOLOGY, null, null), pageable);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).singleElement().extracting("title").isEqualTo("Effective Java");
    }
}
