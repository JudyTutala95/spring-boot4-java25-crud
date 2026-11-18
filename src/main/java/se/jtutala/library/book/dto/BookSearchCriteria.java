package se.jtutala.library.book.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import se.jtutala.library.book.Genre;

/// Sökfilter från query-parametrar: `GET /api/books?genre=FANTASY&title=wiz&authorId=3`.
///
/// Spring MVC kan binda query-parametrar direkt till en record via dess konstruktor.
/// Alla fält är valfria (null = filtrera inte).
public record BookSearchCriteria(
        Genre genre,
        @Size(max = 200) String title,
        @Positive Long authorId) {
}
