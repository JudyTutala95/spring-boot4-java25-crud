package se.jtutala.library.common.exception;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import se.jtutala.library.common.web.CorrelationIdFilter;

/// Central felhantering för alla controllers.
///
/// - Alla fel returneras som **Problem Details (RFC 9457)**, `application/problem+json`.
///   Spring har inbyggt stöd via klassen `ProblemDetail`.
/// - Vi ärver `ResponseEntityExceptionHandler`, som redan hanterar Springs egna fel
///   (400 vid trasig JSON, 405 fel HTTP-metod, 400 ogiltig API-version, osv.)
///   och returnerar dem som `ProblemDetail`.
/// - Varje svar får `correlationId` så att felet kan hittas i loggen.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /// Domänfel. Eftersom `LibraryException` är `sealed` är switchen uttömmande –
    /// lägger någon till en ny subtyp utan att hantera den här blir det kompileringsfel.
    @ExceptionHandler(LibraryException.class)
    ProblemDetail handleLibraryException(LibraryException ex) {
        var status = switch (ex) {
            case ResourceNotFoundException _ -> HttpStatus.NOT_FOUND;
            case ConflictException _ -> HttpStatus.CONFLICT;
        };
        return problem(status, ex.getMessage(), status == HttpStatus.NOT_FOUND ? "not-found" : "conflict");
    }

    /// Två samtidiga uppdateringar av samma rad – Hibernates `@Version` upptäckte det.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Posten ändrades samtidigt av någon annan. Hämta den igen och försök på nytt.", "conflict");
    }

    /// Sista skyddsnätet om en unik-constraint i databasen bryts (t.ex. två samtidiga POST med samma ISBN).
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Brott mot databas-constraint", ex);
        return problem(HttpStatus.CONFLICT, "Begäran bryter mot en databasregel (t.ex. ett unikt värde finns redan).", "conflict");
    }

    /// Klienten skickade `?sort=finnsInte` – Spring Data känner inte till egenskapen.
    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail handleBadSort(PropertyReferenceException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Ogiltigt sorteringsfält: " + ex.getPropertyName(), "bad-request");
    }

    /// `@Valid` på en `@RequestBody` misslyckades. Vi lägger till en karta fält → felmeddelande
    /// så att klienten kan visa felen vid rätt inmatningsfält.
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), Objects.requireNonNullElse(error.getDefaultMessage(), "ogiltigt värde"));
        }

        ProblemDetail body = ex.getBody();
        body.setTitle("Valideringsfel");
        body.setDetail("Ett eller flera fält är ogiltiga.");
        body.setType(URI.create("urn:problem:validation"));
        body.setProperty("errors", errors);
        CorrelationIdFilter.current().ifPresent(id -> body.setProperty("correlationId", id));
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String detail, String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:problem:" + type));
        CorrelationIdFilter.current().ifPresent(id -> problem.setProperty("correlationId", id));
        return problem;
    }
}
