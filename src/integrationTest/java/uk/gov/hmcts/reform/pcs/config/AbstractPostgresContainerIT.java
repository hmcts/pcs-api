package uk.gov.hmcts.reform.pcs.config;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;

@Slf4j
@Testcontainers
public abstract class AbstractPostgresContainerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    // The Spring context (and so this singleton cache) is shared across test classes,
    // but @MockitoBean mocks are reset per test. Without this, a token validated by an
    // earlier test is served from the cache and never hits the freshly-reset IDAM mock.
    @Autowired(required = false)
    private Cache<String, UserInfo> idamUserInfoCache;

    @BeforeEach
    void invalidateIdamUserInfoCache() {
        if (idamUserInfoCache != null) {
            idamUserInfoCache.invalidateAll();
        }
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

}
