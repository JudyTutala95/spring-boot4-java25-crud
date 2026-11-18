package se.jtutala.library.author;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static se.jtutala.library.TestData.author;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import se.jtutala.library.TestData;
import se.jtutala.library.author.dto.CreateAuthorRequest;
import se.jtutala.library.author.dto.UpdateAuthorRequest;
import se.jtutala.library.book.BookRepository;
import se.jtutala.library.common.exception.ConflictException;
import se.jtutala.library.common.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    AuthorRepository authorRepository;
    @Mock
    BookRepository bookRepository;

    AuthorService service;

    @BeforeEach
    void setUp() {
        service = new AuthorService(authorRepository, bookRepository, new AuthorMapper());
    }

    @Test
    void findAllMapparSidanTillDto() {
        var pageable = PageRequest.of(0, 10);
        given(authorRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(author(1L, "Astrid Lindgren")), pageable, 1));

        var page = service.findAll(pageable);

        assertThat(page.content()).singleElement().satisfies(a -> {
            assertThat(a.id()).isEqualTo(1L);
            assertThat(a.name()).isEqualTo("Astrid Lindgren");
        });
        assertThat(page.last()).isTrue();
    }

    @Test
    void createSparar() {
        given(authorRepository.existsByNameIgnoreCase("Tove Jansson")).willReturn(false);
        given(authorRepository.save(any(Author.class))).willAnswer(inv -> {
            Author a = inv.getArgument(0);
            TestData.set(a, "id", 7L);
            return a;
        });

        var response = service.create(new CreateAuthorRequest("  Tove Jansson ", 1914));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.name()).isEqualTo("Tove Jansson");
    }

    @Test
    void createAvvisarDubblett() {
        given(authorRepository.existsByNameIgnoreCase("Astrid Lindgren")).willReturn(true);

        assertThatThrownBy(() -> service.create(new CreateAuthorRequest("Astrid Lindgren", 1907)))
                .isInstanceOf(ConflictException.class);
        then(authorRepository).should(never()).save(any());
    }

    @Test
    void updateAndrarEntitetenUtanSave() {
        Author astrid = author(1L, "Astrid");
        given(authorRepository.findById(1L)).willReturn(Optional.of(astrid));

        var response = service.update(1L, new UpdateAuthorRequest("Astrid Lindgren", 1907, 0L));

        assertThat(response.name()).isEqualTo("Astrid Lindgren");
        assertThat(astrid.getBirthYear()).isEqualTo(1907);
        // Dirty checking: ingen save() – flush räcker
        then(authorRepository).should(never()).save(any());
        then(authorRepository).should().flush();
    }

    @Test
    void updateMedFelVersionGerKonflikt() {
        given(authorRepository.findById(1L)).willReturn(Optional.of(author(1L, "Astrid")));

        assertThatThrownBy(() -> service.update(1L, new UpdateAuthorRequest("X", null, 5L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteAvvisasNarForfattarenHarBocker() {
        given(authorRepository.findById(1L)).willReturn(Optional.of(author(1L, "Astrid")));
        given(bookRepository.existsByAuthorId(1L)).willReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("har böcker");
        then(authorRepository).should(never()).delete(any());
    }

    @Test
    void deleteTarBortForfattareUtanBocker() {
        Author lonely = author(5L, "Ensam");
        given(authorRepository.findById(5L)).willReturn(Optional.of(lonely));
        given(bookRepository.existsByAuthorId(5L)).willReturn(false);

        service.delete(5L);

        then(authorRepository).should().delete(lonely);
    }

    @Test
    void getAuthorKastarNotFound() {
        given(authorRepository.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAuthor(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Författare med id 9 hittades inte");
    }
}
