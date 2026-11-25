package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
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
    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @ParameterizedTest(name = "{0} is {1} {2}")
    @CsvSource({
        "BD10 0JB, ELIGIBLE, England",
        "M13 9PL, ELIGIBLE, England",
        "LL24 0BE, ELIGIBLE, Wales",
        "SY13 2LH, LEGISLATIVE_COUNTRY_REQUIRED,",
        "SW1A 1AA, NO_COURT,"
    })
    void shouldReportEligibilityFromThePostcodeTable(String postcode, String expectedStatus, String country)
        throws Exception {
        mockMvc.perform(check(postcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(expectedStatus))
            .andExpect(country == null
                           ? jsonPath("$.legislativeCountry").doesNotExist()
                           : jsonPath("$.legislativeCountry").value(country));
    }

    @Test
    @Transactional
    @DisplayName("A property in Scotland is not eligible")
    void shouldReportAScottishPropertyAsNotInEnglandOrWales() throws Exception {
        jdbcTemplate.update("INSERT INTO postcode_court_mapping (postcode, epims_id, legislative_country, "
                                + "effective_from, audit) "
                                + "VALUES ('AB10', 99996, 'Scotland', DATE '2025-01-01', '{}'::jsonb)");

        mockMvc.perform(check("AB10 1XG"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NOT_ENGLAND_OR_WALES"));
    }

    @Test
    @DisplayName("A cross-border property is checked against the court of the country chosen")
    void shouldGiveTheChosenCountryOfACrossBorderProperty() throws Exception {
        mockMvc.perform(check("SY13 2LH").param("legislativeCountry", "Wales"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ELIGIBLE"))
            .andExpect(jsonPath("$.legislativeCountry").value("Wales"));
    }

    private static MockHttpServletRequestBuilder check(String postcode) {
        return get(PATH)
            .param("postcode", postcode)
            .header(AUTHORIZATION, "Bearer citizen-token")
            .header("ServiceAuthorization", "ServiceAuthToken");
    }

}
