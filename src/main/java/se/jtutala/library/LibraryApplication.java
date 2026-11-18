package se.jtutala.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/// Startpunkten för applikationen.
///
/// `@SpringBootApplication` är en kombination av tre annotationer:
///
/// - `@SpringBootConfiguration` – klassen är en konfigurationsklass.
/// - `@EnableAutoConfiguration` – Boot konfigurerar beans utifrån vad som finns på classpath
///   (t.ex. en `DataSource` eftersom H2 finns, Tomcat eftersom webmvc-startern finns).
/// - `@ComponentScan` – alla `@Component`, `@Service`, `@Repository`, `@RestController` i detta
///   paket och dess underpaket registreras som beans.
///
/// Tips: lägg INTE `@EnableJpaAuditing` eller liknande här – då laddas de även i
/// slice-tester som `@WebMvcTest` och bryter dem. Lägg dem i egna `@Configuration`-klasser
/// (se paketet `config`).
@SpringBootApplication
public class LibraryApplication {

    static void main(String[] args) {
        // Java 25: main behöver inte längre vara public (JEP 512).
        SpringApplication.run(LibraryApplication.class, args);
    }
}
