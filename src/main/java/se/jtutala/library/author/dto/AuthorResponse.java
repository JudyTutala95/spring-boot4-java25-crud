package se.jtutala.library.author.dto;

import java.time.Instant;

/// Utgående DTO – exakt det klienten får se om en författare.
///
/// Lägger vi till ett internt fält i entiteten (t.ex. "anteckningar") syns det inte i
/// API:t förrän vi medvetet lägger till det här.
public record AuthorResponse(
        Long id,
        String name,
        Integer birthYear,
        long version,
        Instant createdAt,
        Instant updatedAt) {
}
