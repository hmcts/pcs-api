package uk.gov.hmcts.reform.pcs.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;

import java.time.Duration;

@Configuration
public class IdamUserInfoCacheConfiguration {

    /**
     * Cache of IDAM /o/userinfo responses, keyed by a hash of the bearer token.
     * A token maps to the same user for its whole lifetime, so entries can never be
     * wrong — the TTL only bounds how long a revoked token would still be accepted.
     */
    @Bean
    public Cache<String, UserInfo> idamUserInfoCache(
        @Value("${idam.userinfo-cache.ttl-seconds:120}") long ttlSeconds,
        @Value("${idam.userinfo-cache.max-size:1000}") long maxSize) {

        return Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
            .maximumSize(maxSize)
            .build();
    }

}
