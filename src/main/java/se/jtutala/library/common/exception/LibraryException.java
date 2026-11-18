package se.jtutala.library.common.exception;

/// Basklass för alla domänfel i applikationen.
///
/// **sealed** (Java 17): bara de klasser som räknas upp i `permits` får ärva.
/// Det gör att en `switch` över ett `LibraryException` kan vara *uttömmande*
/// utan `default`-gren – kompilatorn vet exakt vilka subtyper som finns.
/// Se `GlobalExceptionHandler#handleLibraryException`.
public abstract sealed class LibraryException extends RuntimeException
        permits ResourceNotFoundException, ConflictException {

    protected LibraryException(String message) {
        super(message);
    }
}
