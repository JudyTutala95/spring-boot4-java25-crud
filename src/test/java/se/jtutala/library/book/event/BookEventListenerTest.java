package se.jtutala.library.book.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;


/// **Parameteriserat test**: samma test körs för flera indata.
class BookEventListenerTest {

    static Stream<Arguments> events() {
        return Stream.of(
                Arguments.of(new BookEvent.BookCreated(1, "Dune", "Frank Herbert"), "Ny bok #1: 'Dune' av Frank Herbert"),
                Arguments.of(new BookEvent.BookUpdated(2, "Dune", 3), "Bok #2 'Dune' uppdaterad"),
                Arguments.of(new BookEvent.BookUpdated(2, "Dune", 11), "Bok #2 'Dune' uppdaterad (ofta ändrad: version 11)"),
                Arguments.of(new BookEvent.BookDeleted(3, "Dune"), "Bok #3 'Dune' borttagen"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("events")
    void beskriverVarjeHandelsetyp(BookEvent event, String expected) {
        assertThat(BookEventListener.describe(event)).isEqualTo(expected);
    }
}
