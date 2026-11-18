package se.jtutala.library.author.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import se.jtutala.library.common.validation.YearNotInFuture;

/// Inkommande DTO för att skapa en författare (`POST /api/authors`).
///
/// En **record** (Java 16) är perfekt för DTO:er: oföränderlig, kompakt, med
/// `equals`/`hashCode`/`toString` gratis. Jackson och Bean Validation stöder records fullt ut.
///
/// Varför är dto-paketet inte `@NullMarked`? JSON från klienten kan sakna fält, så
/// komponenterna *kan* vara null tills `@Valid` har körts.
public record CreateAuthorRequest(
        @NotBlank @Size(max = 120) String name,
        @Min(1000) @YearNotInFuture Integer birthYear) {

    /// Kompakt konstruktor: körs innan fälten tilldelas. Bra för normalisering.
    public CreateAuthorRequest {
        if (name != null) {
            name = name.strip();
        }
    }
}
