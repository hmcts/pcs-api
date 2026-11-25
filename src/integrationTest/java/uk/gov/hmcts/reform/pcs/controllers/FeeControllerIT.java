package uk.gov.hmcts.reform.pcs.controllers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.util.IdamHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DisplayName("Claim issue fee")
class FeeControllerIT extends AbstractPostgresContainerIT {

    private static final String PATH = "/fees/claim-issue";
    private static final String LOOKUP_PATH = "/fees-register/fees/lookup";

    private static final WireMockServer FEES_REGISTER = new WireMockServer(0);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private IdamHelper idamHelper;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @DynamicPropertySource
    static void feesRegister(DynamicPropertyRegistry registry) {
        FEES_REGISTER.start();
        registry.add("fees.api.url", FEES_REGISTER::baseUrl);
    }

    @AfterAll
    static void stopFeesRegister() {
        FEES_REGISTER.stop();
    }

    @BeforeEach
    void setUp() {
        FEES_REGISTER.resetAll();
        idamHelper.stubIdamSystemUser(authorizedClientManager, "system-user-id-token");
        when(authTokenGenerator.generate()).thenReturn("ServiceAuthToken");
    }

    @Test
    @DisplayName("Returns the fee the fees register holds for issuing a possession claim")
    void shouldReturnClaimIssueFeeFromFeesRegister() throws Exception {
        FEES_REGISTER.stubFor(WireMock.get(urlPathEqualTo(LOOKUP_PATH))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"code":"FEE0412","description":"Recovery of Land - County Court",
                     "version":4,"fee_amount":404.00}""")));

        mockMvc.perform(MockMvcRequestBuilders.get(PATH).header("ServiceAuthorization", "Bearer service-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value("404.00"))
            .andExpect(jsonPath("$.code").value("FEE0412"))
            .andExpect(jsonPath("$.version").value(4));

        FEES_REGISTER.verify(getRequestedFor(urlPathEqualTo(LOOKUP_PATH))
            .withQueryParam("keyword", equalTo("PossessionCC"))
            .withQueryParam("event", equalTo("issue")));
    }

    @Test
    @DisplayName("Answers 503 when the fees register fails")
    void shouldReturn503WhenFeesRegisterFails() throws Exception {
        FEES_REGISTER.stubFor(WireMock.get(urlPathEqualTo(LOOKUP_PATH)).willReturn(aResponse().withStatus(500)));

        mockMvc.perform(MockMvcRequestBuilders.get(PATH).header("ServiceAuthorization", "Bearer service-token"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.message").value("The claim fee is unavailable"));
    }

    @Test
    @DisplayName("Rejects a request without a service token")
    void shouldRejectRequestWithoutServiceToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(PATH)).andExpect(status().isBadRequest());
    }
}
