package se.jtutala.library.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import se.jtutala.library.author.dto.CreateAuthorRequest;

class YearNotInFutureValidatorTest {

    /// Fast klocka: testet ger samma resultat oavsett vilket år det körs.
    YearNotInFutureValidator validator =
            new YearNotInFutureValidator(Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC));

    @ParameterizedTest(name = "år {0} → giltigt={1}")
    @CsvSource({"1900, true", "2026, true", "2027, false"})
    void kontrollerarArtal(int year, boolean expected) {
        assertThat(validator.isValid(year, null)).isEqualTo(expected);
    }

    @Test
    void nullArGiltigt() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    /// Validering av en hel DTO med Bean Validation – utan Spring.
    @Test
    void fungerarTillsammansMedAndraConstraintsPaEnDto() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator beanValidator = factory.getValidator();

            var violations = beanValidator.validate(new CreateAuthorRequest(" ", 99_999));

            assertThat(violations)
                    .extracting(v -> v.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("name", "birthYear");
        }
    }
}
