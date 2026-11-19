package uk.gov.hmcts.reform.pcs.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.healthcheck.CoreCaseDataHealthIndicator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CoreCaseDataHealthConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(CoreCaseDataHealthConfiguration.class)
        .withBean(CoreCaseDataApi.class, () -> mock(CoreCaseDataApi.class));

    @Test
    void shouldRegisterCoreCaseDataHealthIndicatorWhenCcdUrlIsSet() {
        contextRunner
            .withPropertyValues("core_case_data.api.url=http://localhost:4452")
            .run(context -> assertThat(context)
                .hasSingleBean(CoreCaseDataHealthIndicator.class)
                .hasBean("coreCaseData"));
    }

    @Test
    void shouldNotRegisterCoreCaseDataHealthIndicatorWithoutCcdUrl() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(CoreCaseDataHealthIndicator.class));
    }
}
