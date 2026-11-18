package se.jtutala.library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/// Slår på Spring Data JPA-auditing så att fält märkta med `@CreatedDate` och
/// `@LastModifiedDate` fylls i automatiskt vid insert/update.
///
/// Ligger i en egen klass (inte på `LibraryApplication`) så att `@WebMvcTest`
/// inte försöker starta JPA. I `@DataJpaTest` importeras den med `@Import(JpaConfig.class)`.
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
public class JpaConfig {
}
