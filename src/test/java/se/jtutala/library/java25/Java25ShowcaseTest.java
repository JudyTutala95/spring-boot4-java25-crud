package se.jtutala.library.java25;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import se.jtutala.library.java25.Java25Showcase.Circle;
import se.jtutala.library.java25.Java25Showcase.Employee;
import se.jtutala.library.java25.Java25Showcase.Rectangle;
import se.jtutala.library.java25.Java25Showcase.Triangle;

/// Kör alla exempel i `Java25Showcase`. Läs dem sida vid sida.
class Java25ShowcaseTest {

    @Nested
    @DisplayName("Records, sealed och pattern matching")
    class PatternMatching {

        @Test
        void areaMedRecordPatterns() {
            assertThat(Java25Showcase.area(new Circle(1))).isCloseTo(Math.PI, within(1e-9));
            assertThat(Java25Showcase.area(new Rectangle(3, 3))).isEqualTo(9);
            assertThat(Java25Showcase.area(new Rectangle(2, 5))).isEqualTo(10);
            assertThat(Java25Showcase.area(new Triangle(4, 3))).isEqualTo(6);
        }

        @Test
        void kompaktKonstruktorValiderar() {
            assertThatIllegalArgumentException().isThrownBy(() -> new Circle(-1));
        }

        @Test
        void recordsHarValueSemantik() {
            assertThat(new Rectangle(1, 2)).isEqualTo(new Rectangle(1, 2)).hasToString("Rectangle[width=1.0, height=2.0]");
        }

        @Test
        void unnamedVariables() {
            assertThat(Java25Showcase.describe(new Rectangle(200, 1))).isEqualTo("en bred rektangel");
            assertThat(Java25Showcase.describe(new Rectangle(2, 1))).isEqualTo("en rektangel");
            assertThat(Java25Showcase.describe(new Circle(1))).isEqualTo("en cirkel");
        }

        @Test
        void switchPaObjectMedTypmonsterOchNull() {
            assertThat(Java25Showcase.format(null)).isEqualTo("null");
            assertThat(Java25Showcase.format(-3)).isEqualTo("negativt heltal -3");
            assertThat(Java25Showcase.format(7)).isEqualTo("heltal 7");
            assertThat(Java25Showcase.format("  ")).isEqualTo("tom sträng");
            assertThat(Java25Showcase.format("hej")).isEqualTo("sträng \"hej\"");
            assertThat(Java25Showcase.format(new BigDecimal("1.50"))).isEqualTo("decimal 1.50");
            assertThat(Java25Showcase.format(List.of(1, 2))).isEqualTo("lista med 2 element");
            assertThat(Java25Showcase.format(3.14)).isEqualTo("okänd typ Double");
        }
    }

    @Test
    @DisplayName("Flexible constructor bodies (JEP 513)")
    void flexibleConstructorBodies() {
        var employee = new Employee("  Judy  ", "E-1");
        assertThat(employee.name()).isEqualTo("Judy");
        assertThat(employee.employeeId()).isEqualTo("E-1");

        assertThatIllegalArgumentException().isThrownBy(() -> new Employee("   ", "E-2"));
    }

    @Nested
    @DisplayName("Sequenced collections")
    class Sequenced {

        @Test
        void getFirstGetLastReversed() {
            assertThat(Java25Showcase.firstLastAndReversed(List.of("a", "b", "c")))
                    .containsExactly("a", "c", "c,b,a");
        }

        @Test
        void sequencedMapPutFirst() {
            var scores = Java25Showcase.orderedScores();
            assertThat(Java25Showcase.firstKey(scores)).isEqualTo("Cecilia");
            assertThat(scores.sequencedKeySet().reversed()).containsExactly("Bo", "Anna", "Cecilia");
        }
    }

    @Nested
    @DisplayName("Stream gatherers")
    class StreamGatherers {

        @Test
        void windowFixed() {
            assertThat(Java25Showcase.batches(List.of(1, 2, 3, 4, 5), 2))
                    .containsExactly(List.of(1, 2), List.of(3, 4), List.of(5));
        }

        @Test
        void windowSliding() {
            assertThat(Java25Showcase.movingAverage(List.of(2, 4, 6, 8), 2)).containsExactly(3.0, 5.0, 7.0);
        }

        @Test
        void scan() {
            assertThat(Java25Showcase.runningTotal(List.of(1, 2, 3, 4))).containsExactly(1, 3, 6, 10);
        }

        @Test
        void egenGatherer() {
            var result = Stream.of("a", "a", "b", "b", "b", "a", "c", "c")
                    .gather(Java25Showcase.distinctConsecutive())
                    .toList();
            assertThat(result).containsExactly("a", "b", "a", "c");
        }
    }

    @Nested
    @DisplayName("Virtuella trådar")
    class VirtualThreads {

        @Test
        void tiotusenBlockerandeUppgifterGarSnabbt() throws Exception {
            long start = System.nanoTime();

            int sum = Java25Showcase.sumOnVirtualThreads(10_000);

            long millis = (System.nanoTime() - start) / 1_000_000;
            assertThat(sum).isEqualTo(10_000 * 10_001 / 2);
            // 10 000 uppgifter × 10 ms sekventiellt = 100 s. Parallellt på virtuella trådar: < 1 s brukar räcka.
            assertThat(millis).isLessThan(10_000);
        }

        @Test
        void traden_ar_virtuell() throws Exception {
            assertThat(Java25Showcase.runsOnVirtualThread()).isTrue();
        }
    }

    @Test
    @DisplayName("Scoped values (JEP 506)")
    void scopedValues() {
        assertThat(Java25Showcase.greetCurrentUser()).isEqualTo("Hej okänd!");
        assertThat(Java25Showcase.runAs("Judy")).isEqualTo("Hej Judy!");
        // Utanför scopet är värdet borta igen – inget att städa upp
        assertThat(Java25Showcase.CURRENT_USER.isBound()).isFalse();
    }

    @Test
    @DisplayName("Text blocks, var, Math.clamp")
    void smasaker() {
        assertThat(Java25Showcase.jsonFor("Dune", 1965)).isEqualTo("{\n  \"title\": \"Dune\",\n  \"year\": 1965\n}\n");
        assertThat(Java25Showcase.squares(4)).containsExactly(1, 4, 9, 16);
        assertThat(Java25Showcase.progressBar(42)).isEqualTo("####......");
        assertThat(Java25Showcase.progressBar(250)).isEqualTo("##########");
    }
}
