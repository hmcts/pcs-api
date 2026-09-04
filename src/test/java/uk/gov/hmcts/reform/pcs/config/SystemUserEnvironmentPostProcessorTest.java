package uk.gov.hmcts.reform.pcs.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.mock.env.MockEnvironment;
import uk.gov.hmcts.reform.pcs.config.SystemUserIdentityResolver.SystemUserIdentity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemUserEnvironmentPostProcessorTest {

    @Mock
    private SystemUserIdentityResolver resolver;

    private SystemUserEnvironmentPostProcessor underTest;
    private MockEnvironment environment;

    @BeforeEach
    void setUp() {
        underTest = new SystemUserEnvironmentPostProcessor(new DeferredLogs(), resolver);
        environment = new MockEnvironment();
    }

    @Test
    void resolvesTheSystemUserIdentityFromIdamAndPublishesIt() throws Exception {
        when(resolver.resolve(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(new SystemUserIdentity("resolved-uid", "Service", "Account"));

        underTest.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("ccd.decentralised-runtime.system-user.id"))
            .isEqualTo("resolved-uid");
        assertThat(environment.getProperty("ccd.decentralised-runtime.system-user.first-name"))
            .isEqualTo("Service");
        assertThat(environment.getProperty("ccd.decentralised-runtime.system-user.last-name"))
            .isEqualTo("Account");
    }

    @Test
    void skipsResolutionWhenTheIdIsSuppliedExplicitly() {
        environment.setProperty("PCS_IDAM_SYSTEM_USER_ID", "configured-uid");

        underTest.postProcessEnvironment(environment, null);

        verifyNoInteractions(resolver);
        assertThat(environment.getProperty("ccd.decentralised-runtime.system-user.id")).isNull();
    }

    @Test
    void fallsBackToConfiguredDefaultsWhenResolutionFails() throws Exception {
        when(resolver.resolve(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new IllegalStateException("idam down"));

        underTest.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("ccd.decentralised-runtime.system-user.id")).isNull();
    }

    @Test
    void defaultsBlankResolvedNames() throws Exception {
        when(resolver.resolve(any(), any(), any(), any(), any()))
            .thenReturn(new SystemUserIdentity("resolved-uid", null, " "));

        underTest.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("ccd.decentralised-runtime.system-user.first-name"))
            .isEqualTo("Service");
        assertThat(environment.getProperty("ccd.decentralised-runtime.system-user.last-name"))
            .isEqualTo("Account");
    }
}
