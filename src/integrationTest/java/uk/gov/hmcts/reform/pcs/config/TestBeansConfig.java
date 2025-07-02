package uk.gov.hmcts.reform.pcs.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;

@TestConfiguration
public class TestBeansConfig {
    @Bean
    public CoreCaseDataApi coreCaseDataApi() {
        return Mockito.mock(CoreCaseDataApi.class);
    }
}
