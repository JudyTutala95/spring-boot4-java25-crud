package se.jtutala.library.author;

import java.util.Objects;

import org.springframework.stereotype.Component;

import se.jtutala.library.author.dto.AuthorResponse;
import se.jtutala.library.author.dto.CreateAuthorRequest;

/// Översätter mellan entitet och DTO:er.
///
/// Handskriven mappning är tydlig och lätt att felsöka. I större projekt används ofta
/// MapStruct, som genererar samma sorts kod vid kompilering.
@Component
public class AuthorMapper {

    public Author toEntity(CreateAuthorRequest request) {
        return new Author(request.name(), request.birthYear());
    }

    public AuthorResponse toResponse(Author author) {
        return new AuthorResponse(
                Objects.requireNonNull(author.getId(), "author har inget id – är den sparad?"),
                author.getName(),
                author.getBirthYear(),
                author.getVersion(),
                author.getCreatedAt(),
                author.getUpdatedAt());
    }
}
