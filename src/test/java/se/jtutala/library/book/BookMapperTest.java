package se.jtutala.library.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static se.jtutala.library.TestData.author;
import static se.jtutala.library.TestData.book;
import static se.jtutala.library.TestData.createBookRequest;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import se.jtutala.library.author.Author;
import se.jtutala.library.book.dto.BookResponseV2;

/// Mappern är ren Java – testa den med `new`, helt utan Spring.
class BookMapperTest {

    BookMapper mapper = new BookMapper();
    Author bloch = author(2L, "Joshua Bloch");

    @Test
    void toEntityKopierarFaltenFranRequest() {
        Book entity = mapper.toEntity(createBookRequest(2L), bloch);

        assertThat(entity.getId()).as("ny entitet har inget id").isNull();
        assertThat(entity.getTitle()).isEqualTo("Effective Java");
        assertThat(entity.getIsbn()).isEqualTo("9780134685991");
        assertThat(entity.getAuthor()).isSameAs(bloch);
    }

    @Test
    void toResponsePlattarUtForfattaren() {
        var response = mapper.toResponse(book(1L, "Effective Java", bloch));

        assertThat(response.authorId()).isEqualTo(2L);
        assertThat(response.authorName()).isEqualTo("Joshua Bloch");
        assertThat(response.price()).isEqualByComparingTo("499.00");
    }

    @Test
    void toResponseV2NastlarForfattareOchLaggerTillValuta() {
        var response = mapper.toResponseV2(book(1L, "Effective Java", bloch));

        assertThat(response.author()).isEqualTo(new BookResponseV2.AuthorSummary(2L, "Joshua Bloch"));
        assertThat(response.price()).isEqualTo(new BookResponseV2.Price(new BigDecimal("499.00"), "SEK"));
        assertThat(response.genre().label()).isEqualTo("Teknik");
    }

    @Test
    void osparadEntitetKanInteMappasTillResponse() {
        Book unsaved = mapper.toEntity(createBookRequest(2L), bloch);

        assertThatNullPointerException().isThrownBy(() -> mapper.toResponse(unsaved))
                .withMessageContaining("saknar id");
    }
}
