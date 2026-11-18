package se.jtutala.library.author;

import org.springframework.data.jpa.repository.JpaRepository;

/// Spring Data JPA skapar implementationen vid uppstart.
///
/// `JpaRepository<Author, Long>` ger gratis: `save`, `findById`, `findAll(Pageable)`,
/// `deleteById`, `existsById`, `count` m.fl.
public interface AuthorRepository extends JpaRepository<Author, Long> {

    /// "Derived query": Spring Data läser metodnamnet och genererar
    /// `select count(a) > 0 from Author a where lower(a.name) = lower(?)`.
    boolean existsByNameIgnoreCase(String name);
}
