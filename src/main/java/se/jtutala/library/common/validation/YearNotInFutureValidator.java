package se.jtutala.library.common.validation;

import java.time.Clock;
import java.time.Year;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.jspecify.annotations.Nullable;

/// Validerar `@YearNotInFuture`.
///
/// `null` räknas som giltigt – det är `@NotNull`s jobb att stoppa null.
/// Så fungerar alla inbyggda constraints, och det gör dem kombinerbara.
public class YearNotInFutureValidator implements ConstraintValidator<YearNotInFuture, Integer> {

    private final Clock clock;

    /// Används av Hibernate Validator.
    public YearNotInFutureValidator() {
        this(Clock.systemDefaultZone());
    }

    /// Används i tester – en fast klocka gör testet deterministiskt.
    YearNotInFutureValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean isValid(@Nullable Integer year, ConstraintValidatorContext context) {
        return year == null || year <= Year.now(clock).getValue();
    }
}
