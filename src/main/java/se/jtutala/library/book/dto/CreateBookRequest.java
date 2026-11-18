package se.jtutala.library.book.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import se.jtutala.library.book.Genre;
import se.jtutala.library.common.validation.YearNotInFuture;

/// Inkommande DTO för `POST /api/books`.
///
/// Notera: klienten skickar `authorId`, inte ett helt författarobjekt.
/// DTO:n beskriver vad *klienten* behöver skicka – inte hur databasen ser ut.
public record CreateBookRequest(
        @NotBlank @Size(max = 200)
        String title,

        @NotBlank
        @Pattern(regexp = "97[89]\\d{10}", message = "måste vara ett ISBN-13 (13 siffror som börjar med 978 eller 979)")
        String isbn,

        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        @NotNull @Min(1450) @YearNotInFuture
        Integer publishedYear,

        @NotNull
        Genre genre,

        @NotNull @Positive
        Long authorId) {

    /// Normalisering innan validering: ta bort blanksteg och bindestreck ur ISBN,
    /// så att både "978-0-13-468599-1" och "9780134685991" accepteras.
    public CreateBookRequest {
        if (title != null) title = title.strip();
        if (isbn != null) isbn = IsbnNormalizer.normalize(isbn);
    }
}
