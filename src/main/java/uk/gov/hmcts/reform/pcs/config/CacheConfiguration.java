package uk.gov.hmcts.reform.pcs.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfiguration {

    private static final int SIX_MONTHS = 6 * 30;

    @Bean
    public CacheManager bankHolidayCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("bank_holiday_cache");
        manager.setCaffeine(Caffeine.newBuilder()
                                .expireAfterWrite(SIX_MONTHS, TimeUnit.DAYS)
                                .maximumSize(1));
        return manager;
    }
}
