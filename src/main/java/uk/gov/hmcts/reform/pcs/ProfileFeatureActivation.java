package uk.gov.hmcts.reform.pcs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;

@Slf4j
public class ProfileFeatureActivation implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    public static final String ENABLE_TESTING_SUPPORT = "ENABLE_TESTING_SUPPORT";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment env = event.getEnvironment();
        env.addActiveProfile("preview");
        log.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
    }

}
