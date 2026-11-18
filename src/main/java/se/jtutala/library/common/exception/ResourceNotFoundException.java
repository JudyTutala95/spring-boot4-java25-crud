package se.jtutala.library.common.exception;

/// Kastas när en efterfrågad resurs inte finns. Översätts till HTTP 404.
public final class ResourceNotFoundException extends LibraryException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s med id %s hittades inte".formatted(resource, id));
    }
}
