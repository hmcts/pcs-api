package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.sdk.doctoolkit.DocToolkitSystemUser;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

/** The doc toolkit fetches the bundle's documents from CDAM, and stores the bundle, as PCS's system user. */
@Configuration
public class CaseBundleConfiguration {
    @Bean
    DocToolkitSystemUser docToolkitSystemUser(@Qualifier("systemUpdateUserTokenProvider") IdamTokenProvider user) {
        return user::getAuthToken;
    }
}
