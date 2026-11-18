package se.jtutala.library.java25;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.IntStream;

/// # Java 17 → 25 i ett nötskal
///
/// Varje metod visar en språkfunktion som är **slutgiltig** (inte preview) i Java 25.
/// Läs koden tillsammans med `Java25ShowcaseTest`, som kör alla exempel.
///
/// Den här kommentaren är själv ett exempel: **Markdown-dokumentation med `///`**
/// (JEP 467, Java 23). Inga fler `<p>` och `{@code ...}` – skriv vanlig Markdown.
///
/// | Funktion                          | JEP      | Final i |
/// |-----------------------------------|----------|---------|
/// | Records                           | 395      | 16      |
/// | Sealed classes                    | 409      | 17      |
/// | Pattern matching för switch       | 441      | 21      |
/// | Record patterns                   | 440      | 21      |
/// | Sequenced collections             | 431      | 21      |
/// | Virtuella trådar                  | 444      | 21      |
/// | Unnamed variables `_`             | 456      | 22      |
/// | Markdown-dokumentation            | 467      | 23      |
/// | Stream gatherers                  | 485      | 24      |
/// | Flexible constructor bodies       | 513      | 25      |
/// | Compact source files, `IO`        | 512      | 25      |
/// | Module import declarations        | 511      | 25      |
/// | Scoped values                     | 506      | 25      |
///
/// Compact source files och module imports visas i `examples/HelloJava25.java` i projektroten.
/// Scoped values används på riktigt i `common.web.CorrelationIdFilter`.
public final class Java25Showcase {

    private Java25Showcase() {
    }

    // ------------------------------------------------------------------
    // 1. Records, sealed interfaces och pattern matching (Java 16–22)
    // ------------------------------------------------------------------

    /// En geometrisk form – `sealed` betyder att det bara finns dessa tre.
    public sealed interface Shape permits Circle, Rectangle, Triangle {
    }

    /// Record med **kompakt konstruktor** för validering.
    public record Circle(double radius) implements Shape {
        public Circle {
            if (radius < 0) throw new IllegalArgumentException("radien får inte vara negativ");
        }
    }

    public record Rectangle(double width, double height) implements Shape {
    }

    public record Triangle(double base, double height) implements Shape {
    }

    /// **Switch-uttryck + record patterns + guards (`when`)**.
    /// Ingen `default` behövs: kompilatorn vet att `Shape` bara har tre subtyper.
    public static double area(Shape shape) {
        return switch (shape) {
            case Circle(double r) -> Math.PI * r * r;
            case Rectangle(double w, double h) when w == h -> w * w; // kvadrat
            case Rectangle(double w, double h) -> w * h;
            case Triangle(double b, double h) -> 0.5 * b * h;
        };
    }

    /// **Unnamed variables `_`** (Java 22): visar att en komponent inte används.
    public static String describe(Shape shape) {
        return switch (shape) {
            case Circle _ -> "en cirkel";
            case Rectangle(var w, _) when w > 100 -> "en bred rektangel";
            case Rectangle _ -> "en rektangel";
            case Triangle _ -> "en triangel";
        };
    }

    /// **Pattern matching för `instanceof`** (Java 16) och switch på `Object` med typmönster.
    public static String format(Object value) {
        return switch (value) {
            case null -> "null";
            case Integer i when i < 0 -> "negativt heltal " + i;
            case Integer i -> "heltal " + i;
            case String s when s.isBlank() -> "tom sträng";
            case String s -> "sträng \"" + s + "\"";
            case BigDecimal bd -> "decimal " + bd.toPlainString();
            case List<?> list -> "lista med " + list.size() + " element";
            default -> "okänd typ " + value.getClass().getSimpleName();
        };
    }

    // ------------------------------------------------------------------
    // 2. Flexible constructor bodies (Java 25, JEP 513)
    // ------------------------------------------------------------------

    public static class Person {
        private final String name;

        public Person(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }
    }

    /// Före Java 25 måste `super(...)` vara FÖRSTA satsen. Nu får vi validera och
    /// räkna ut argument innan – utan statiska hjälpmetoder.
    public static class Employee extends Person {
        private final String employeeId;

        public Employee(String rawName, String employeeId) {
            String name = rawName.strip();                        // sats före super() – tillåtet i Java 25
            if (name.isEmpty()) {
                throw new IllegalArgumentException("namn krävs");
            }
            this.employeeId = employeeId;                         // fält får tilldelas före super()
            super(name);
        }

        public String employeeId() {
            return employeeId;
        }
    }

    // ------------------------------------------------------------------
    // 3. Sequenced collections (Java 21)
    // ------------------------------------------------------------------

    /// `getFirst()`, `getLast()`, `reversed()` finns nu på `List`, `Deque`,
    /// `LinkedHashSet`, `LinkedHashMap` m.fl. – inget mer `list.get(list.size() - 1)`.
    public static List<String> firstLastAndReversed(List<String> items) {
        return List.of(items.getFirst(), items.getLast(), String.join(",", items.reversed()));
    }

    public static String firstKey(SequencedMap<String, Integer> map) {
        return map.firstEntry().getKey();
    }

    public static SequencedMap<String, Integer> orderedScores() {
        var map = new LinkedHashMap<String, Integer>();
        map.put("Anna", 90);
        map.put("Bo", 75);
        map.putFirst("Cecilia", 99); // nytt: lägg in först
        return map;
    }

    // ------------------------------------------------------------------
    // 4. Stream gatherers (Java 24, JEP 485)
    // ------------------------------------------------------------------

    /// `Gatherers.windowFixed`: dela upp en ström i grupper – t.ex. för att
    /// skicka poster till databasen i batchar.
    public static List<List<Integer>> batches(List<Integer> values, int batchSize) {
        return values.stream().gather(Gatherers.windowFixed(batchSize)).toList();
    }

    /// `Gatherers.windowSliding`: glidande fönster – t.ex. för glidande medelvärde.
    public static List<Double> movingAverage(List<Integer> values, int window) {
        return values.stream()
                .gather(Gatherers.windowSliding(window))
                .map(w -> w.stream().mapToInt(Integer::intValue).average().orElseThrow())
                .toList();
    }

    /// `Gatherers.scan`: löpande summa (varje steg ser föregående resultat).
    public static List<Integer> runningTotal(List<Integer> values) {
        return values.stream().gather(Gatherers.scan(() -> 0, Integer::sum)).toList();
    }

    /// En **egen gatherer**: släpper bara igenom element som skiljer sig från föregående
    /// (tar bort upprepningar i följd). Något som var krångligt med vanliga streams.
    public static <T> Gatherer<T, ?, T> distinctConsecutive() {
        class State {
            Object previous = State.class; // markör för "inget sett än"
        }
        return Gatherer.ofSequential(
                State::new,
                (state, element, downstream) -> {
                    if (!java.util.Objects.equals(state.previous, element)) {
                        state.previous = element;
                        return downstream.push(element);
                    }
                    return true;
                });
    }

    // ------------------------------------------------------------------
    // 5. Virtuella trådar (Java 21, JEP 444)
    // ------------------------------------------------------------------

    /// Startar `tasks` st blockerande uppgifter, var och en på en egen virtuell tråd.
    /// 10 000 plattformstrådar skulle kosta gigabyte minne – virtuella trådar kostar nästan inget.
    /// Spring Boot slår på dem för Tomcat med `spring.threads.virtual.enabled=true`.
    public static int sumOnVirtualThreads(int tasks) throws InterruptedException, ExecutionException {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) { // AutoCloseable sedan Java 19
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 1; i <= tasks; i++) {
                int n = i;
                Callable<Integer> task = () -> {
                    Thread.sleep(10); // blockerande – den virtuella tråden "parkeras", bärartråden frigörs
                    return n;
                };
                futures.add(executor.submit(task));
            }
            int sum = 0;
            for (Future<Integer> f : futures) {
                sum += f.get();
            }
            return sum;
        }
    }

    public static boolean runsOnVirtualThread() throws InterruptedException {
        var result = new boolean[1];
        Thread thread = Thread.ofVirtual().name("demo").start(() -> result[0] = Thread.currentThread().isVirtual());
        thread.join();
        return result[0];
    }

    // ------------------------------------------------------------------
    // 6. Scoped values (Java 25, JEP 506)
    // ------------------------------------------------------------------

    /// Ett "kontextvärde" som är synligt för all kod som anropas inom ett scope,
    /// utan att skickas som parameter. Ersätter de flesta användningar av `ThreadLocal`.
    public static final ScopedValue<String> CURRENT_USER = ScopedValue.newInstance();

    public static String greetCurrentUser() {
        return CURRENT_USER.isBound() ? "Hej " + CURRENT_USER.get() + "!" : "Hej okänd!";
    }

    public static String runAs(String user) {
        return ScopedValue.where(CURRENT_USER, user).call(Java25Showcase::greetCurrentUser);
    }

    // ------------------------------------------------------------------
    // 7. Småsaker som gör vardagen enklare
    // ------------------------------------------------------------------

    /// Text blocks (Java 15) + `formatted` (Java 15).
    public static String jsonFor(String title, int year) {
        return """
                {
                  "title": "%s",
                  "year": %d
                }
                """.formatted(title, year);
    }

    /// `var` (Java 10) och `Stream.toList()` (Java 16).
    public static List<Integer> squares(int n) {
        var stream = IntStream.rangeClosed(1, n).map(i -> i * i).boxed();
        return stream.toList();
    }

    /// `Math.clamp` (Java 21) och `String.repeat` (Java 11).
    public static String progressBar(int percent) {
        int filled = Math.clamp(percent, 0, 100) / 10;
        return "#".repeat(filled) + ".".repeat(10 - filled);
    }
}
