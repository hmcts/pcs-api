package uk.gov.hmcts.reform.pcs.config;

import com.launchdarkly.sdk.server.LDClient;
import com.launchdarkly.sdk.server.LDConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class LaunchDarklyConfiguration {

    @Bean(destroyMethod = "close")
    public LDClient ldClient(@Value("${launchdarkly.sdk-key:}") String sdkKey,
                             @Value("${launchdarkly.offline-mode:false}") Boolean offlineMode) {
        LDConfig.Builder builder = new LDConfig.Builder();

        builder.offline(BooleanUtils.isTrue(offlineMode));

        LDClient client = new LDClient(sdkKey, builder.build());
        if (!client.isInitialized()) {
            log.warn("LaunchDarkly not initialised at startup - serving code defaults until connected");
        }
        return client;
    }

}
