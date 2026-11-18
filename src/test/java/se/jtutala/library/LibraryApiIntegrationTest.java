package se.jtutala.library;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import se.jtutala.library.author.dto.AuthorResponse;
import se.jtutala.library.author.dto.CreateAuthorRequest;
import se.jtutala.library.book.Genre;
import se.jtutala.library.book.dto.BookResponse;
import se.jtutala.library.book.dto.BookResponseV2;
import se.jtutala.library.book.dto.CreateBookRequest;
import se.jtutala.library.book.dto.UpdateBookRequest;

/// **Integrationstest** – hela applikationen startas på en slumpad port med riktig
/// Tomcat, riktig databas (H2) och Flyway. Testar hela kedjan HTTP → DB → HTTP.
///
/// `RestTestClient` är nytt i Spring Framework 7: samma flytande API som `WebTestClient`,
/// men för vanliga (blockerande) Spring MVC-appar.
///
/// Integrationstester är långsammare – ha många enhets-/slice-tester och färre av dessa
/// ("testpyramiden").
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class LibraryApiIntegrationTest {

    @Autowired
    RestTestClient client;

    @Test
    void helaCrudFlodetForEnBok() {
        // CREATE författare
        AuthorResponse author = client.post().uri("/api/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateAuthorRequest("Tove Jansson", 1914))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthorResponse.class)
                .returnResult().getResponseBody();
        assertThat(author).isNotNull();

        // CREATE bok
        BookResponse created = client.post().uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateBookRequest("Kometen kommer", "978-91-0-012345-6", new BigDecimal("159.00"),
                        1946, Genre.CHILDREN, author.id()))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(BookResponse.class)
                .returnResult().getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created.isbn()).isEqualTo("9789100123456");
        assertThat(created.version()).isZero();
        assertThat(created.createdAt()).isNotNull();

        // READ v1 och v2
        client.get().uri("/api/books/{id}", created.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.authorName").isEqualTo("Tove Jansson");

        BookResponseV2 v2 = client.get().uri("/api/books/{id}", created.id())
                .header("API-Version", "2")
                .exchange()
                .expectStatus().isOk()
                .expectBody(BookResponseV2.class)
                .returnResult().getResponseBody();
        assertThat(v2).isNotNull();
        assertThat(v2.author().name()).isEqualTo("Tove Jansson");

        // UPDATE
        var update = new UpdateBookRequest("Kometen kommer (rev.)", created.isbn(), new BigDecimal("179.00"),
                1968, Genre.CHILDREN, author.id(), created.version());
        BookResponse updated = client.put().uri("/api/books/{id}", created.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(update)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BookResponse.class)
                .returnResult().getResponseBody();
        assertThat(updated).isNotNull();
        assertThat(updated.title()).isEqualTo("Kometen kommer (rev.)");
        assertThat(updated.version()).isEqualTo(1);

        // UPDATE igen med den GAMLA versionen → 409 (optimistisk låsning)
        client.put().uri("/api/books/{id}", created.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(update)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        // Författaren kan inte tas bort medan den har böcker
        client.delete().uri("/api/authors/{id}", author.id())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        // DELETE bok, sedan författare
        client.delete().uri("/api/books/{id}", created.id()).exchange().expectStatus().isNoContent();
        client.get().uri("/api/books/{id}", created.id()).exchange().expectStatus().isNotFound();
        client.delete().uri("/api/authors/{id}", author.id()).exchange().expectStatus().isNoContent();
    }

    @Test
    void sokningMedFilterOchSidindelning() {
        client.get().uri("/api/books?authorId=1&size=1&sort=publishedYear,desc")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(2)
                .jsonPath("$.totalPages").isEqualTo(2)
                .jsonPath("$.content[0].title").isEqualTo("Bröderna Lejonhjärta");
    }

    @Test
    void ogiltigtSorteringsfaltGer400() {
        client.get().uri("/api/books?sort=finnsInte")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail").value(detail -> assertThat((String) detail).contains("finnsInte"));
    }

    @Test
    void dubblettIsbnGer409() {
        client.post().uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateBookRequest("Kopia", "9780134685991", BigDecimal.ONE, 2018, Genre.TECHNOLOGY, 2L))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void rekommendationerOverleverFlakigtFjarrsystem() {
        // FakeRemoteCatalog misslyckas varannan gång – @Retryable gör att anropet ändå lyckas
        for (int i = 0; i < 3; i++) {
            client.get().uri("/api/books/recommendations?genre=FANTASY")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody().jsonPath("$[0]").isEqualTo("The Hobbit");
        }
    }

    @Test
    void actuatorHealthArUppe() {
        client.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .value(body -> assertThat(body).containsEntry("status", "UP"));
    }
}
