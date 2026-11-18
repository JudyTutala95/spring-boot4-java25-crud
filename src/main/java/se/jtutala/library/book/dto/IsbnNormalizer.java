package se.jtutala.library.book.dto;

/// Delad hjälpkod för request-DTO:erna.
final class IsbnNormalizer {

    private IsbnNormalizer() {
    }

    static String normalize(String isbn) {
        return isbn.replaceAll("[\\s-]", "");
    }
}
