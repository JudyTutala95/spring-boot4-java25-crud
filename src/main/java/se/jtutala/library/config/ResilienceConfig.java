package se.jtutala.library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/// Nytt i Spring Framework 7: inbyggd resiliens utan Spring Retry eller Resilience4j.
///
/// `@EnableResilientMethods` aktiverar `@Retryable` och `@ConcurrencyLimit` på
/// metoder i Spring-beans. Se `resilience.RecommendationService`.
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
public class ResilienceConfig {
}
