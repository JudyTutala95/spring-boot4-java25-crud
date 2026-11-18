package se.jtutala.library.common.exception;

/// Kastas när en begäran krockar med nuvarande tillstånd, t.ex. dubblett-ISBN
/// eller att någon annan har uppdaterat posten (optimistisk låsning). Översätts till HTTP 409.
public final class ConflictException extends LibraryException {

    public ConflictException(String message) {
        super(message);
    }
}
