package se.jtutala.library.book.dto;

import java.math.BigDecimal;
import java.time.Instant;

import se.jtutala.library.book.Genre;

/// Utgående DTO, **API-version 1**: platt struktur med författarens id och namn.
public record BookResponse(
        Long id,
        String title,
        String isbn,
        BigDecimal price,
        int publishedYear,
        Genre genre,
        Long authorId,
        String authorName,
        long version,
        Instant createdAt,
        Instant updatedAt) {
}
