package se.jtutala.library.common.web;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/// Ger varje request ett korrelations-id och gör det tillgängligt i hela anropskedjan.
///
/// **Java 25 – Scoped Values (JEP 506, slutgiltig i Java 25).**
/// En `ScopedValue` är ett modernt alternativ till `ThreadLocal`:
///
/// - värdet är *oföränderligt* inom sitt scope,
/// - det försvinner automatiskt när scopet tar slut (inga minnesläckor, inget `remove()`),
/// - det fungerar billigt med virtuella trådar.
///
/// Värdet läses med `CorrelationIdFilter.current()`, t.ex. i `GlobalExceptionHandler`.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";

    private static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();

    /// Korrelations-id för pågående request, om det finns ett.
    public static Optional<String> current() {
        return CORRELATION_ID.isBound() ? Optional.of(CORRELATION_ID.get()) : Optional.empty();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        String id = (header == null || header.isBlank()) ? UUID.randomUUID().toString() : header;
        response.setHeader(HEADER, id);

        try {
            // Allt som körs inuti call(...) ser värdet; efteråt är det borta.
            ScopedValue.where(CORRELATION_ID, id).call(() -> {
                chain.doFilter(request, response);
                return null;
            });
        } catch (IOException | ServletException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
