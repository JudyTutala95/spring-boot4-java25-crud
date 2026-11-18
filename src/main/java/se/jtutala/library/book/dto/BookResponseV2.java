package se.jtutala.library.book.dto;

import java.math.BigDecimal;
import java.time.Instant;

import se.jtutala.library.book.Genre;

/// Utgående DTO, **API-version 2** (`API-Version: 2`).
///
/// Ett *brytande* formatbyte jämfört med v1: priset har valuta och författaren är ett
/// nästlat objekt. Gamla klienter fortsätter få v1 – därför versionerar vi.
///
/// Nästlade records är ett bra sätt att hålla ihop DTO-delar som bara används här.
public record BookResponseV2(
        Long id,
        String title,
        String isbn,
        Price price,
        int publishedYear,
        GenreInfo genre,
        AuthorSummary author,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public record Price(BigDecimal amount, String currency) {
    }

    public record GenreInfo(Genre code, String label) {
    }

    public record AuthorSummary(Long id, String name) {
    }
}
