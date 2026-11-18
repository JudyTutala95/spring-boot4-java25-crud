package se.jtutala.library.book;

/// Bokens genre. Lagras som text i databasen (`@Enumerated(EnumType.STRING)`).
///
/// Använd ALDRIG `EnumType.ORDINAL` (standard!) – då sparas positionen (0, 1, 2 ...)
/// och all data blir fel om någon lägger till en konstant i mitten.
public enum Genre {
    CHILDREN("Barn"),
    FANTASY("Fantasy"),
    SCIENCE_FICTION("Science fiction"),
    TECHNOLOGY("Teknik"),
    CRIME("Deckare"),
    BIOGRAPHY("Biografi"),
    OTHER("Övrigt");

    private final String label;

    Genre(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
