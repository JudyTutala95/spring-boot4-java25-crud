package se.jtutala.library.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/// Egen Bean Validation-regel: ett årtal får inte ligga i framtiden.
///
/// Så skapar man en egen constraint:
/// 1. En annotation märkt med `@Constraint(validatedBy = ...)`.
/// 2. Tre obligatoriska attribut: `message`, `groups`, `payload`.
/// 3. En `ConstraintValidator` som gör själva kontrollen (se `YearNotInFutureValidator`).
@Documented
@Constraint(validatedBy = YearNotInFutureValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface YearNotInFuture {

    String message() default "årtalet får inte ligga i framtiden";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
