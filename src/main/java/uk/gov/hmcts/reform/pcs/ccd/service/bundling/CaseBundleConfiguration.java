package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.sdk.bundling.managed.BundleSystemUser;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

@Configuration
public class CaseBundleConfiguration {
    @Bean
    BundleSystemUser bundleSystemUser(@Qualifier("systemUpdateUserTokenProvider") IdamTokenProvider user) {
        return user::getAuthToken;
    }
}
