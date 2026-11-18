# Spring Boot 4 + Java 25 – CRUD-lärprojekt 📚

Ett komplett, körbart REST-API för ett bibliotek (författare och böcker) byggt för att
**lära sig** och **hämta inspiration** till moderna Spring-projekt.
Varje klass är kommenterad med *varför* den ser ut som den gör.

| Teknik | Version |
|---|---|
| Java | **25** (LTS) |
| Spring Boot | **4.1** |
| Spring Framework | **7.0** |
| Hibernate ORM | 7.x (Jakarta Persistence 3.2) |
| Jackson | **3.x** (`tools.jackson`) |
| JUnit | **6** |
| Databas | H2 (in-memory) + Flyway |

---

## Innehåll

1. [Kom igång](#kom-igång)
2. [Projektstruktur](#projektstruktur)
3. [Arkitektur och DTO-mönstret](#arkitektur-och-dto-mönstret)
4. [API:t](#apit)
5. [Nytt i Spring Boot 4 / Spring Framework 7](#nytt-i-spring-boot-4--spring-framework-7)
6. [Java 17 → 25: språkfunktionerna](#java-17--25-språkfunktionerna)
7. [JPA och databasen](#jpa-och-databasen)
8. [Felhantering](#felhantering)
9. [Tester](#tester)
10. [Konfiguration](#konfiguration)
11. [Övningar](#övningar)
12. [Fusklapp](#fusklapp)

---

## Kom igång

**Krav:** JDK 25. Maven behövs inte – projektet har Maven Wrapper (`./mvnw`).

```bash
# Med SDKMAN (läser .sdkmanrc i projektet)
sdk env install
```

```bash
./mvnw spring-boot:run
```

```bash
./mvnw test
```

```bash
# Java 25 "compact source file" – körs utan kompilering
java examples/HelloJava25.java
```

När appen kör:

| Vad | URL |
|---|---|
| API | http://localhost:8080/api/books |
| H2-konsol | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:library`, user `sa`) |
| Hälsa | http://localhost:8080/actuator/health |

Färdiga anrop finns i [`requests.http`](requests.http) (öppna i IntelliJ eller VS Code med REST Client).

```bash
curl -s localhost:8080/api/books/3 -H "API-Version: 2"
```

---

## Projektstruktur

Koden är **paketerad per funktion** (feature), inte per lager. Allt som hör till böcker ligger
i `book/`. Det gör det lätt att hitta, och lätt att bryta ut en funktion senare.

```
src/main/java/se/jtutala/library
├── LibraryApplication.java          start + förklaring av @SpringBootApplication
├── config/                          JpaConfig (auditing), ResilienceConfig (@Retryable)
├── common/
│   ├── dto/PageResponse.java        egen, stabil JSON-form för sidor
│   ├── exception/                   sealed LibraryException + GlobalExceptionHandler (ProblemDetail)
│   ├── validation/                  egen Bean Validation-constraint (@YearNotInFuture)
│   └── web/                         CorrelationIdFilter (ScopedValue), SortGuard
├── author/                          Author-entitet, repository, service, controller, mapper
│   └── dto/                         CreateAuthorRequest, UpdateAuthorRequest, AuthorResponse
├── book/                            Book-entitet, Genre, repository, service, controller, mapper
│   ├── dto/                         Create/UpdateBookRequest, BookResponse (v1), BookResponseV2
│   └── event/                       sealed BookEvent + BookEventListener
├── resilience/                      RecommendationService med @Retryable / @ConcurrencyLimit
└── java25/Java25Showcase.java       alla Java 17–25-funktioner med exempel

src/main/resources
├── application.properties           kommenterad konfiguration
└── db/migration/                    Flyway: V1 schema, V2 exempeldata

src/test/java/...                    81 tester: enhets-, slice- och integrationstester
examples/HelloJava25.java            compact source file + module import
```

---

## Arkitektur och DTO-mönstret

```
          JSON                         DTO                        Entitet                SQL
Klient ◀────────▶ Controller ◀──────────────▶ Service ◀──────────────▶ Repository ◀─────────▶ Databas
                  (HTTP, @Valid)   (records)   (affärslogik,           (Spring Data JPA)
                                               @Transactional,
                                               mapper)
```

### Varför DTO:er (Data Transfer Objects)?

En **entitet** (`Book`) beskriver hur data lagras. En **DTO** (`BookResponse`) beskriver vad
API:t visar. Att hålla isär dem ger:

1. **Stabilt API-kontrakt.** Byter du namn på en kolumn eller lägger till ett internt fält
   ändras inte JSON:en av misstag.
2. **Säkerhet.** Klienten kan inte skicka in fält den inte ska få sätta (`id`, `createdAt`,
   `version` ...) – "mass assignment" är omöjligt eftersom request-DTO:n inte har dem.
3. **Ingen lazy-loading-fälla.** Entiteter med `LAZY`-relationer som serialiseras till JSON
   utanför en transaktion ger `LazyInitializationException` eller dolda N+1-frågor.
   DTO:er byggs *inom* transaktionen i service-lagret.
4. **Olika former för olika behov.** `CreateBookRequest` har `authorId`, `BookResponse` har
   `authorName`, `BookResponseV2` har ett nästlat `author`-objekt.
5. **Versionering.** Två API-versioner = två DTO:er, samma entitet.

### Regler som projektet följer

| Regel | Var |
|---|---|
| Controllern tar emot och returnerar **bara DTO:er** | `BookController` |
| Service tar emot DTO, returnerar DTO; entiteter lämnar aldrig service | `BookService` |
| Mappning sker på **ett** ställe | `BookMapper`, `AuthorMapper` |
| DTO:er är **records** (oföränderliga) | `book/dto/*` |
| Request-DTO och response-DTO är **separata** typer | `CreateBookRequest` vs `BookResponse` |
| Validering sitter på request-DTO:n | `@NotBlank`, `@Pattern` ... |
| Normalisering i recordens kompakta konstruktor | `CreateBookRequest` (ISBN utan bindestreck) |
| `spring.jpa.open-in-view=false` | `application.properties` |

> **Mappning för hand eller MapStruct?** Här skrivs mappningen för hand – tydligt och lätt att
> felsöka. I större projekt genererar [MapStruct](https://mapstruct.org) samma kod vid kompilering.

---

## API:t

### Böcker – `/api/books`

| Metod | URL | Beskrivning | Svar |
|---|---|---|---|
| GET | `/api/books?genre=&title=&authorId=&page=&size=&sort=` | Sök + sidindelning | 200 |
| GET | `/api/books/{id}` | Hämta (v1) | 200 / 404 |
| GET | `/api/books/{id}` + `API-Version: 2` | Hämta (v2) | 200 / 404 |
| POST | `/api/books` | Skapa | 201 + `Location` / 400 / 409 |
| PUT | `/api/books/{id}` | Uppdatera (kräver `version`) | 200 / 400 / 404 / 409 |
| DELETE | `/api/books/{id}` | Ta bort | 204 / 404 |
| GET | `/api/books/recommendations?genre=` | Demo av `@Retryable` | 200 |

Tillåtna sorteringsfält: `title`, `publishedYear`, `price`, `genre`, `createdAt`.

### Författare – `/api/authors`

| Metod | URL | Svar |
|---|---|---|
| GET | `/api/authors?page=&size=&sort=` | 200 |
| GET | `/api/authors/{id}` | 200 / 404 |
| POST | `/api/authors` | 201 / 400 / 409 |
| PUT | `/api/authors/{id}` | 200 / 404 / 409 |
| DELETE | `/api/authors/{id}` | 204 / 404 / 409 (har böcker) |

### Exempel

```http
POST /api/books
Content-Type: application/json

{ "title": "Mio, min Mio", "isbn": "978-91-29-65735-0", "price": 139.00,
  "publishedYear": 1954, "genre": "CHILDREN", "authorId": 1 }
```

```json
HTTP/1.1 201 Created
Location: http://localhost:8080/api/books/6

{ "id": 6, "title": "Mio, min Mio", "isbn": "9789129657350", "price": 139.00,
  "publishedYear": 1954, "genre": "CHILDREN", "authorId": 1, "authorName": "Astrid Lindgren",
  "version": 0, "createdAt": "...", "updatedAt": "..." }
```

---

## Nytt i Spring Boot 4 / Spring Framework 7

| Nyhet | Vad det betyder | Var i koden |
|---|---|---|
| **Java 17 baseline, Java 25 stöds fullt** | Records, sealed, virtuella trådar överallt | hela projektet |
| **Jakarta EE 11** | Servlet 6.1, JPA 3.2, Bean Validation 3.1 | `jakarta.*`-importer |
| **Modulariserad Boot** | Mindre jar:ar per teknik. Starters heter t.ex. `spring-boot-starter-webmvc` (inte `-web`), test-stöd per teknik: `spring-boot-starter-webmvc-test`, `-data-jpa-test` | `pom.xml` |
| **Nya paket för test-annotationer** | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` | tester |
| **`@MockBean` borttaget** | Använd `@MockitoBean` / `@MockitoSpyBean` från Spring Framework | `BookControllerTest` |
| **Inbyggd API-versionering** | `@GetMapping(version = "2")` + `spring.mvc.apiversion.*` | `BookController` |
| **Inbyggd resiliens** | `@Retryable`, `@ConcurrencyLimit`, `@EnableResilientMethods` – inget Spring Retry behövs | `RecommendationService` |
| **`RestTestClient`** | Flytande testklient för MVC (som `WebTestClient`) | `LibraryApiIntegrationTest` |
| **`MockMvcTester`** | AssertJ-baserad MockMvc | `BookControllerTest` |
| **JSpecify null-säkerhet** | `@NullMarked` / `@Nullable` från `org.jspecify` – hela Spring är annoterat | `package-info.java` |
| **Jackson 3** | Nytt paket `tools.jackson.*` (annotationerna ligger kvar i `com.fasterxml.jackson.annotation`) | (automatiskt) |
| **HTTP Interface-klienter** | `@HttpExchange` + `@ImportHttpServices` – deklarativa HTTP-klienter | nämns i `RemoteCatalog` |
| **Hibernate 7** | JPA 3.2, bättre typade frågor | `BookRepository` |
| **JUnit 6** | Java 17-baseline, samma API som JUnit 5 | tester |

### API-versionering i detalj

```properties
spring.mvc.apiversion.use.header=API-Version
spring.mvc.apiversion.default=1
```

```java
@GetMapping(path = "/{id}", version = "1") BookResponse   getV1(...)
@GetMapping(path = "/{id}", version = "2") BookResponseV2 getV2(...)
```

- Saknas headern → version 1.
- Okänd version (`API-Version: 9`) → 400 Problem Details.
- Mappningar utan `version` (POST, PUT, ...) matchar alla versioner.
- Alternativ: `use.query-parameter=version`, `use.path-segment=1` (`/api/v2/...`) eller `use.media-type-parameter`.

### Resiliens i detalj

```java
@Retryable(includes = RemoteCatalogUnavailableException.class, maxRetries = 3, delay = 100, multiplier = 2)
@ConcurrencyLimit(10)
public List<String> recommendationsFor(Genre genre) { ... }
```

Fungerar via proxy → anropet måste komma från en *annan* bean. Kör
`GET /api/books/recommendations?genre=FANTASY` och titta i loggen.

---

## Java 17 → 25: språkfunktionerna

Alla exempel finns i [`Java25Showcase.java`](src/main/java/se/jtutala/library/java25/Java25Showcase.java)
och körs av `Java25ShowcaseTest`. Där det passar används de även "på riktigt" i appen:

| Funktion | Java | Används i appen |
|---|---|---|
| **Records** | 16 | Alla DTO:er, `BookEvent`, `PageResponse` |
| **Kompakt konstruktor** | 16 | `CreateBookRequest` normaliserar ISBN |
| **Sealed classes/interfaces** | 17 | `LibraryException`, `BookEvent` |
| **Pattern matching `instanceof`** | 16 | `Book.equals` |
| **Switch-uttryck + typmönster** | 21 | `GlobalExceptionHandler` |
| **Record patterns + `when`-guards** | 21 | `BookEventListener.describe` |
| **Unnamed variables `_`** | 22 | `GlobalExceptionHandler`, `BookEventListener` |
| **Text blocks** | 15 | `@Query` i `BookRepository`, JSON i tester |
| **Virtuella trådar** | 21 | `spring.threads.virtual.enabled=true` |
| **Sequenced collections** | 21 | showcase |
| **Markdown-javadoc `///`** | 23 | överallt i projektet |
| **Stream gatherers** | 24 | showcase (batchning, glidande medel, egen gatherer) |
| **Flexible constructor bodies** | 25 | showcase (`Employee`) |
| **Scoped values** | 25 | `CorrelationIdFilter` |
| **Compact source files + `IO`** | 25 | `examples/HelloJava25.java`, `static void main` i `LibraryApplication` |
| **Module import declarations** | 25 | `examples/HelloJava25.java` |

**Fortfarande preview i Java 25** (kräver `--enable-preview`, används inte här):
structured concurrency, primitiva typer i mönster, stable values.

### Snabbexempel

```java
// Sealed + records + uttömmande switch utan default
sealed interface BookEvent permits BookCreated, BookUpdated, BookDeleted {}

String describe(BookEvent e) {
    return switch (e) {
        case BookCreated(long id, String title, String author) -> "Ny: " + title;
        case BookUpdated(long id, String title, _) -> "Ändrad: " + title;
        case BookDeleted(long id, String title) -> "Borttagen: " + title;
    };
}

// Scoped value i stället för ThreadLocal
ScopedValue.where(CORRELATION_ID, id).call(() -> { chain.doFilter(req, res); return null; });

// Stream gatherers
List<List<Integer>> batches = ids.stream().gather(Gatherers.windowFixed(100)).toList();

// Flexible constructor body
Employee(String rawName) {
    String name = rawName.strip();
    if (name.isEmpty()) throw new IllegalArgumentException();
    super(name);
}
```

---

## JPA och databasen

| Koncept | Förklaring | Var |
|---|---|---|
| **Flyway** äger schemat | `ddl-auto=validate` – Hibernate får bara kontrollera | `db/migration/` |
| **`@Version`** optimistisk låsning | Två samtidiga uppdateringar → den andra får 409 | `Book`, `Author`, `*Service.update` |
| **Auditing** | `@CreatedDate` / `@LastModifiedDate` fylls automatiskt | `JpaConfig`, entiteterna |
| **`FetchType.LAZY`** på `@ManyToOne` | Standard är EAGER – sätt alltid LAZY | `Book.author` |
| **`@EntityGraph`** | Hämtar relationen i samma fråga → inget N+1 | `BookRepository` |
| **Dirty checking** | Ändra en managed entitet → UPDATE vid commit, inget `save()` | `AuthorService.update` |
| **`@Transactional(readOnly = true)`** | På klassnivå, överskuggas på skrivmetoder | services |
| **`EnumType.STRING`** | Aldrig ORDINAL | `Book.genre` |
| **`BigDecimal` för pengar** | Aldrig `double` | `Book.price` |
| **equals/hashCode på id** | Konstant hashCode, säkert i `HashSet` | entiteterna |
| **Enkelriktad relation** | `Book → Author`, ingen `List<Book>` i `Author` | `Author` |
| **Open Session In View av** | Tvingar all DB-åtkomst till service-lagret | `application.properties` |

Tre sätt att skriva frågor (alla i `BookRepository`):

```java
boolean existsByIsbn(String isbn);                       // derived query
@EntityGraph(attributePaths = "author")
Optional<Book> findWithAuthorById(Long id);              // derived + entity graph
@Query("""select b from Book b where ...""")             // JPQL
Page<Book> search(Genre genre, Long authorId, String title, Pageable pageable);
```

---

## Felhantering

Alla fel returneras som **Problem Details (RFC 9457)** med `Content-Type: application/problem+json`:

```json
{
  "type": "urn:problem:not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "Bok med id 999 hittades inte",
  "instance": "/api/books/999",
  "correlationId": "4f1c..."
}
```

Valideringsfel får dessutom en `errors`-karta:

```json
{ "title": "Valideringsfel", "status": 400,
  "errors": { "isbn": "måste vara ett ISBN-13 ...", "publishedYear": "årtalet får inte ligga i framtiden" } }
```

| Undantag | Status |
|---|---|
| `ResourceNotFoundException` | 404 |
| `ConflictException` (dubblett, fel version, författare har böcker) | 409 |
| `OptimisticLockingFailureException`, `DataIntegrityViolationException` | 409 |
| `MethodArgumentNotValidException` (`@Valid`) | 400 + `errors` |
| `HandlerMethodValidationException` (`@Positive` på path-variabel) | 400 |
| Ogiltig JSON, okänd API-version, otillåtet sorteringsfält | 400 |

---

## Tester

**Testpyramiden** – många snabba tester längst ner, få långsamma högst upp:

```
          ▲  LibraryApiIntegrationTest     @SpringBootTest + RestTestClient (hela appen)
         ▲▲▲  BookControllerTest           @WebMvcTest + MockMvcTester (bara webblagret)
        ▲▲▲▲▲ BookRepositoryTest           @DataJpaTest (bara JPA + Flyway + H2)
       ▲▲▲▲▲▲▲ RecommendationServiceRetryTest  @SpringJUnitConfig (minimal kontext)
      ▲▲▲▲▲▲▲▲▲ BookServiceTest, AuthorServiceTest, BookMapperTest,
                 BookEventListenerTest, YearNotInFutureValidatorTest,
                 Java25ShowcaseTest        rena enhetstester (JUnit + Mockito + AssertJ)
```

| Testtyp | Startar | Hastighet | När |
|---|---|---|---|
| Enhetstest (`@ExtendWith(MockitoExtension.class)`) | Ingenting | ms | Affärslogik, mappning |
| `@WebMvcTest` | MVC, filter, advice, Jackson | ~0,5 s | HTTP-status, JSON, validering, felhantering |
| `@DataJpaTest` | JPA, Flyway, H2 | ~1 s | Egna frågor, constraints, mappning till tabeller |
| `@SpringJUnitConfig` | Bara angivna klasser | snabbt | AOP/proxy-beteende (`@Retryable`) |
| `@SpringBootTest` | Allt + Tomcat | sekunder | Helheten, några få flöden |

Tekniker som visas:

- **Given / When / Then** med BDDMockito (`given(...)`, `then(...).should()`)
- `@Nested` + `@DisplayName` för att gruppera tester
- `@ParameterizedTest` med `@CsvSource` och `@MethodSource`
- `ArgumentCaptor` för att kontrollera publicerade händelser
- Fast `Clock` för deterministiska tidstester
- Test data builder (`TestData`)
- Text blocks för JSON i tester

```bash
./mvnw test                                   # alla
```

```bash
./mvnw test -Dtest=BookServiceTest            # en klass
```

```bash
./mvnw test -Dtest='BookServiceTest$Create'   # en @Nested-grupp
```

---

## Konfiguration

Se kommentarerna i [`application.properties`](src/main/resources/application.properties).
De viktigaste:

| Property | Varför |
|---|---|
| `spring.threads.virtual.enabled=true` | Virtuella trådar för Tomcat |
| `spring.jpa.hibernate.ddl-auto=validate` | Flyway äger schemat |
| `spring.jpa.open-in-view=false` | Ingen DB-åtkomst från controllers |
| `spring.mvc.apiversion.use.header=API-Version` | API-versionering via header |
| `management.endpoints.web.exposure.include=health,info,metrics` | Actuator |

`pom.xml` laddar Mockito som Java-agent i tester (JDK 21+ varnar annars för dynamiskt laddade agenter).

---

## Övningar

Bygg vidare för att lära dig mer. Ungefär i svårighetsordning:

1. **PATCH** – lägg till `PATCH /api/books/{id}` med en `PatchBookRequest` där alla fält är valfria.
2. **Ny migration** – lägg till kolumnen `pages` i `V3__add_pages.sql`, uppdatera entitet, DTO:er och mapper.
3. **Ny API-version** – lägg till `version = "3"` som även returnerar antal sidor.
4. **MapStruct** – ersätt `BookMapper` med en MapStruct-mapper och jämför.
5. **Spring Security** – skydda POST/PUT/DELETE med `spring-boot-starter-security` (`@WithMockUser` i tester).
6. **PostgreSQL + Testcontainers** – byt H2 mot PostgreSQL och kör `@DataJpaTest` mot en riktig databas med `@ServiceConnection`.
7. **HTTP Interface-klient** – ersätt `FakeRemoteCatalog` med en `@HttpExchange`-klient mot ett riktigt API, registrerad med `@ImportHttpServices`.
8. **OpenAPI** – lägg till springdoc-openapi och generera Swagger UI.
9. **Specifications** – ersätt `@Query`-sökningen med Spring Data JPA `Specification`.
10. **Native image** – bygg med GraalVM: `./mvnw -Pnative native:compile`.

---

## Fusklapp

```java
// Controller
@RestController @RequestMapping("/api/x")
@GetMapping("/{id}")                      @PathVariable @Positive long id
@PostMapping                              @Valid @RequestBody CreateXRequest req
@ResponseStatus(HttpStatus.NO_CONTENT)    ResponseEntity.created(uri).body(dto)
@GetMapping(path = "/{id}", version = "2")

// Service
@Service @Transactional(readOnly = true)   // + @Transactional på skrivmetoder

// Repository
interface XRepository extends JpaRepository<X, Long>
@Query("""...""")   @EntityGraph(attributePaths = "rel")

// Entitet
@Entity @Id @GeneratedValue(strategy = IDENTITY) @Version
@ManyToOne(fetch = LAZY)  @Enumerated(STRING)  @CreatedDate

// Validering
@NotNull @NotBlank @Size @Min @Max @Positive @Pattern @Email @DecimalMin @Digits @Past

// Test
@WebMvcTest(XController.class) + @MockitoBean + MockMvcTester
@DataJpaTest + @Import(JpaConfig.class)
@SpringBootTest(webEnvironment = RANDOM_PORT) + @AutoConfigureRestTestClient + RestTestClient
@ExtendWith(MockitoExtension.class) + @Mock + given(...).willReturn(...)
```
