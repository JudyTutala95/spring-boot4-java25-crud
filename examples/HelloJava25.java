// =====================================================================
//  Java 25: Compact source files + module imports
//
//  Kör direkt, utan kompilering, utan Maven:
//      java examples/HelloJava25.java
//
//  - JEP 512 (Compact source files & instance main methods): ingen klass,
//    ingen "public static void main(String[] args)" behövs.
//  - JEP 511 (Module import declarations): "import module java.base;"
//    importerar alla publika paket i modulen (java.util, java.time, ...).
//  - java.lang.IO: enkel in-/utmatning (IO.println, IO.readln).
// =====================================================================

import module java.base;

record Book(String title, int year) {
}

void main() {
    var books = List.of(
            new Book("Effective Java", 2018),
            new Book("Pippi Långstrump", 1945),
            new Book("A Wizard of Earthsea", 1968));

    IO.println("Hej från Java " + Runtime.version().feature() + "!");
    IO.println("Idag är det " + LocalDate.now().getDayOfWeek());

    books.stream()
            .sorted(Comparator.comparingInt(Book::year))
            .forEach(b -> IO.println("  %d  %s".formatted(b.year(), b.title())));

    IO.println("Senaste boken: " + books.stream().max(Comparator.comparingInt(Book::year)).orElseThrow().title());
}
