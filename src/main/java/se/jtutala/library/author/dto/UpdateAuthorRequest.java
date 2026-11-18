package se.jtutala.library.author.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import se.jtutala.library.common.validation.YearNotInFuture;

/// Inkommande DTO för att uppdatera en författare (`PUT /api/authors/{id}`).
///
/// `version` måste skickas med: det är den version klienten senast läste. Matchar den
/// inte databasens version har någon annan ändrat posten under tiden → 409 Conflict.
public record UpdateAuthorRequest(
        @NotBlank @Size(max = 120) String name,
        @Min(1000) @YearNotInFuture Integer birthYear,
        @NotNull Long version) {

    public UpdateAuthorRequest {
        if (name != null) {
            name = name.strip();
        }
    }
}
