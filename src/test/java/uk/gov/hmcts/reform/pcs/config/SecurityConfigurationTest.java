package uk.gov.hmcts.reform.pcs.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(SecurityConfigurationTest.TestConfig.class)
@TestPropertySource(properties = "idam.s2s-authorised.ccd-data-store-only-services=ccd_data")
class SecurityConfigurationTest {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String USER_TOKEN = "Bearer user-token";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AuthTokenValidator authTokenValidator;

    @Autowired
    private IdamAuthenticator idamAuthenticator;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(authTokenValidator, idamAuthenticator);
        when(idamAuthenticator.validateAuthToken(anyString())).thenReturn(mock(User.class));
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/ccd-persistence/cases", "/callbacks/about-to-start", "/callbacks/mid-event"})
    void shouldAllowCcdDataStoreOnCcdDataStoreOnlyPaths(String path) throws Exception {
        givenServiceToken("ccd_data");

        mockMvc.perform(post(path)
                            .header(SERVICE_AUTHORIZATION, "ccd_data")
                            .header(AUTHORIZATION, USER_TOKEN))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pcs_api", "pcs_frontend", "xui_webapp", "payment_app"})
    void shouldForbidOtherServicesOnPersistencePath(String serviceName) throws Exception {
        givenServiceToken(serviceName);

        mockMvc.perform(post("/ccd-persistence/cases")
                            .header(SERVICE_AUTHORIZATION, serviceName)
                            .header(AUTHORIZATION, USER_TOKEN))
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pcs_api", "pcs_frontend", "xui_webapp", "payment_app"})
    void shouldForbidOtherServicesOnCallbackPaths(String serviceName) throws Exception {
        givenServiceToken(serviceName);

        mockMvc.perform(post("/callbacks/about-to-start")
                            .header(SERVICE_AUTHORIZATION, serviceName)
                            .header(AUTHORIZATION, USER_TOKEN))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectCcdDataStoreOnlyPathWithoutServiceToken() throws Exception {
        mockMvc.perform(post("/ccd-persistence/cases").header(AUTHORIZATION, USER_TOKEN))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pcs_frontend", "xui_webapp", "payment_app"})
    void shouldStillAllowServiceWideAllowlistOnOtherPaths(String serviceName) throws Exception {
        givenServiceToken(serviceName);

        mockMvc.perform(post("/other")
                            .header(SERVICE_AUTHORIZATION, serviceName))
            .andExpect(status().isOk());
    }

    @Test
    void shouldForbidUnlistedServiceOnOtherPaths() throws Exception {
        givenServiceToken("civil_service");

        mockMvc.perform(post("/other")
                            .header(SERVICE_AUTHORIZATION, "civil_service"))
            .andExpect(status().isForbidden());
    }

    private void givenServiceToken(String serviceName) {
        when(authTokenValidator.getServiceName("Bearer " + serviceName)).thenReturn(serviceName);
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfiguration.class)
    static class TestConfig {

        @Bean
        DefaultConversionService conversionService() {
            return new DefaultConversionService();
        }

        @Bean
        AuthTokenValidator authTokenValidator() {
            return mock(AuthTokenValidator.class);
        }

        @Bean
        IdamAuthenticator idamAuthenticator() {
            return mock(IdamAuthenticator.class);
        }

        @Bean
        IdamAuthenticationFilter idamAuthenticationFilter(IdamAuthenticator idamAuthenticator) {
            return new IdamAuthenticationFilter(idamAuthenticator);
        }

        @Bean
        ServiceAuthFilter serviceAuthFilter(AuthTokenValidator authTokenValidator) {
            return new ServiceAuthFilter(
                authTokenValidator,
                List.of("pcs_api", "pcs_frontend", "xui_webapp", "ccd_data", "payment_app")
            );
        }

        @Bean
        StubController stubController() {
            return new StubController();
        }
    }

    @RestController
    static class StubController {

        @PostMapping({"/ccd-persistence/cases", "/callbacks/about-to-start", "/callbacks/mid-event", "/other"})
        String ok() {
            return "ok";
        }
    }
}
