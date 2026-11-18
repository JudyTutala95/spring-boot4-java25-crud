package se.jtutala.library.common.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/// Stabil JSON-form för sidindelade svar.
///
/// Varför inte returnera Spring Datas `Page` direkt? Dess JSON-struktur är en
/// implementationsdetalj som kan ändras mellan versioner (Spring varnar för det).
/// Med en egen record äger *vi* API-kontraktet – det är hela poängen med DTO-mönstret.
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    public PageResponse {
        content = List.copyOf(content); // defensiv kopia – records ska vara oföränderliga
    }

    /// Mappar en sida entiteter till en sida DTO:er.
    public static <E, D> PageResponse<D> of(Page<E> page, Function<? super E, ? extends D> mapper) {
        List<D> content = page.getContent().stream().<D>map(mapper).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
