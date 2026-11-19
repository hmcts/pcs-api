package uk.gov.hmcts.reform.pcs.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.healthcheck.CoreCaseDataHealthIndicator;

/**
 * Registers the CCD data store health indicator that CoreCaseDataClientAutoConfiguration used to provide.
 * That auto-configuration is excluded in Application because its package-wide @EnableFeignClients would
 * register CoreCaseDataApi and CaseAssignmentApi a second time.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "core_case_data", name = "api.url")
public class CoreCaseDataHealthConfiguration {

    @Bean
    public CoreCaseDataHealthIndicator coreCaseData(CoreCaseDataApi coreCaseDataApi) {
        return new CoreCaseDataHealthIndicator(coreCaseDataApi);
    }
}
