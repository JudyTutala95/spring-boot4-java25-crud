package se.jtutala.library.author;

import java.net.URI;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import se.jtutala.library.author.dto.AuthorResponse;
import se.jtutala.library.author.dto.CreateAuthorRequest;
import se.jtutala.library.author.dto.UpdateAuthorRequest;
import se.jtutala.library.common.dto.PageResponse;
import se.jtutala.library.common.web.SortGuard;

/// REST-API för författare – ett "klassiskt" CRUD-gränssnitt:
///
/// | Metod  | URL                 | Betydelse        | Svar            |
/// |--------|---------------------|------------------|-----------------|
/// | GET    | /api/authors        | lista (sidor)    | 200             |
/// | GET    | /api/authors/{id}   | hämta en         | 200 / 404       |
/// | POST   | /api/authors        | skapa            | 201 + Location  |
/// | PUT    | /api/authors/{id}   | ersätt/uppdatera | 200 / 404 / 409 |
/// | DELETE | /api/authors/{id}   | ta bort          | 204 / 404 / 409 |
///
/// Controllern är tunn: den tar emot HTTP, validerar, delegerar till service och
/// returnerar DTO:er. Ingen affärslogik och inga entiteter här.
@RestController
@RequestMapping("/api/authors")
public class AuthorController {

    private final AuthorService service;

    public AuthorController(AuthorService service) {
        this.service = service;
    }

    /// `Pageable` fylls i från `?page=0&size=20&sort=name,asc`.
    @GetMapping
    public PageResponse<AuthorResponse> list(@PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findAll(SortGuard.requireAllowed(pageable, Set.of("name", "birthYear", "createdAt")));
    }

    /// `@Positive` på en path-variabel: Spring 6.1+ validerar metodparametrar automatiskt
    /// (inget `@Validated` behövs). Fel ger 400 med ProblemDetail.
    @GetMapping("/{id}")
    public AuthorResponse get(@PathVariable @Positive long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<AuthorResponse> create(@Valid @RequestBody CreateAuthorRequest request) {
        AuthorResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public AuthorResponse update(@PathVariable @Positive long id, @Valid @RequestBody UpdateAuthorRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive long id) {
        service.delete(id);
    }
}
