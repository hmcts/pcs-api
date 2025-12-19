package uk.gov.hmcts.reform.pcs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;

@Slf4j
public class ProfileFeatureActivation implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment env = event.getEnvironment();
        String enableTestingSupport = System.getenv("ENABLE_TESTING_SUPPORT");
        if ("true".equals(enableTestingSupport)) {
            env.addActiveProfile("preview");
            log.info("Preview profile activated due to ENABLE_TESTING_SUPPORT=true");
        }
        log.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
    }

}
