package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.idam.IdamUserInfoApi;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;
import uk.gov.hmcts.reform.pcs.util.IdamHelper;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DisplayName("Citizen claim property eligibility against the postcode court table")
class CitizenPropertyEligibilityControllerIT extends AbstractPostgresContainerIT {

    private static final String PATH = "/cases/citizen-claims/property-eligibility";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private IdamHelper idamHelper;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private IdamUserInfoApi idamUserInfoApi;
    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;
    @MockitoBean
    private FeatureToggleService featureToggleService;

    @BeforeEach
    void setUp() {
        idamHelper.stubIdamSystemUser(authorizedClientManager, "system-user-id-token");
        when(idamUserInfoApi.getUserInfo(anyString()))
            .thenReturn(UserInfo.builder().uid("citizen-1").roles(List.of("citizen")).build());
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
    }

    @ParameterizedTest
    @CsvSource({
        "BD10 0JB, ELIGIBLE",
        "LL24 0BE, NOT_ENGLAND",
        "SW1A 1AA, NOT_COVERED"
    })
    void shouldReportEligibilityFromThePostcodeTable(String postcode, String expectedStatus) throws Exception {
        mockMvc.perform(get(PATH)
                            .param("postcode", postcode)
                            .header(AUTHORIZATION, "Bearer citizen-token")
                            .header("ServiceAuthorization", "ServiceAuthToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(expectedStatus));
    }

}
