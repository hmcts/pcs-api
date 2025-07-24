package uk.gov.hmcts.reform.pcs.postcodecourt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.CourtEligibilityEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.repository.CourtEligibilityRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class PostcodeEligibilityIT extends AbstractPostgresContainerIT {

    @Autowired
    private CourtEligibilityRepository courtEligibilityRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Returns ELIGIBLE status for postcode that maps to eligible ePIMS ID")
    void shouldReturnEligibleStatus() throws Exception {
        String postcode = "W3 7RX";
        int expectedEpimsId = 20262;

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, null);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(eligibilityResult.getEpimsId()).isEqualTo(expectedEpimsId);
        assertThat(eligibilityResult.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
    }

    @Test
    @DisplayName("Returns NOT_ELIGIBLE status for postcode that is not whitelisted")
    void shouldReturnNotEligibleStatusForPostcodeNotWhitelisted() throws Exception {
        String postcode = "M13 9PL";
        int expectedEpimsId = 144641;

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, null);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.NOT_ELIGIBLE);
        assertThat(eligibilityResult.getEpimsId()).isEqualTo(expectedEpimsId);
        assertThat(eligibilityResult.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
    }

    @Test
    @DisplayName("Returns NO_MATCH_FOUND status for postcode that doesn't exist in the table")
    void shouldReturnNoMatchFoundStatusForPostcodeNoEpimsIdMatch() throws Exception {
        String postcode = "NW1 1AB";

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, null);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.NO_MATCH_FOUND);
    }

    @Test
    @DisplayName("Returns NO_MATCH_FOUND status for non-whitelisted postcode that maps to expired ePIMS ID")
    void shouldReturnNoMatchFoundStatusNonWhitelistedExpiredPostcode() throws Exception {
        String postcode = "W3 6RT";

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, null);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.NO_MATCH_FOUND);
    }

    @Test
    @DisplayName("Returns NO_MATCH_FOUND status for whitelisted postcode that maps to expired ePIMS ID")
    void shouldReturnNoMatchFoundStatusWhitelistedNonExpiredPostcode() throws Exception {
        String postcode = "W3 6RT";

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, null);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.NO_MATCH_FOUND);
    }

    @Test
    @DisplayName("Returns NO_MATCH_FOUND status for postcode where the effective_from date is in the future")
    void shouldReturnNoMatchFoundStatusEffectiveFromDateInFuture() throws Exception {
        String postcode = "CF61 1ZH";

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, null);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.NO_MATCH_FOUND);
    }

    @Test
    @DisplayName("Returns MULTIPLE_MATCHES_FOUND status for postcode with multiple ePIMS ID matches")
    void shouldReturnMultipleMatchesFoundStatusForPostcodeMultipleEpimsIdMatch() throws Exception {
        String postcode = "RH13 5JH";

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, null);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.MULTIPLE_MATCHES_FOUND);
    }

    @Test
    @Transactional
    @DisplayName("Returns NOT_ELIGIBLE status for postcode that maps to ePIMS ID eligible from tomorrow")
    void shouldReturnNotEligibleStatus() throws Exception {
        String postcode = "W3 6RS";
        int epimsId = 36791;

        setEligibilityFromDate(epimsId, LocalDate.now(UK_ZONE_ID).plusDays(1));

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, null);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.NOT_ELIGIBLE);
        assertThat(eligibilityResult.getEpimsId()).isEqualTo(epimsId);
        assertThat(eligibilityResult.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
    }

    @Test
    @DisplayName("Returns ELIGIBLE status for partial postcode match that maps to eligible ePIMS ID")
    void shouldReturnEligibleStatusForPartialPostcodeMatch() throws Exception {
        String postcode = "RH14 0AA";
        int expectedEpimsId = 20262;

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, null);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(eligibilityResult.getEpimsId()).isEqualTo(expectedEpimsId);
        assertThat(eligibilityResult.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
    }

    @Test
    @DisplayName("Returns ELIGIBLE status for partial, cross border postcode match that maps to eligible ePIMS ID")
    void shouldReturnEligibleStatusForPartialCrossBorderPostcodeMatch() throws Exception {
        String postcode = "CH14QT";
        String legislativeCountry = "England";
        int expectedEpimsId = 20262;

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, legislativeCountry);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(eligibilityResult.getEpimsId()).isEqualTo(expectedEpimsId);
        assertThat(eligibilityResult.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
    }

    @Test
    @DisplayName("Returns LEGISLATIVE_COUNTRY_REQUIRED for a cross border postcode")
    void shouldReturnLegislativeCountryRequiredStatusForCrossBorderPostcode() throws Exception {
        String postcode = "SY132LH";

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, null);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED);
        assertThat(eligibilityResult.getLegislativeCountries())
            .containsExactly(LegislativeCountry.ENGLAND, LegislativeCountry.WALES);
    }

    @Test
    @DisplayName("Returns ELIGIBLE for a cross border, whitelisted postcode with legislative country value")
    void shouldReturnEligibleStatusForWhitelistedCrossBorderPostcode() throws Exception {
        String postcode = "SY132LH";
        String legislativeCountry = "England";
        int expectedEpimsId = 20262;

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, legislativeCountry);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(eligibilityResult.getEpimsId()).isEqualTo(expectedEpimsId);
        assertThat(eligibilityResult.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
    }

    @Test
    @DisplayName("Returns NOT_ELIGIBLE for a cross border, non-whitelisted postcode with legislative country value")
    void shouldReturnNotEligibleStatusForNonWhitelistedCrossBorderPostcode() throws Exception {
        String postcode = "CH14QJ";
        String legislativeCountry = "Wales";
        int expectedEpimsId = 99999;

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, legislativeCountry);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.NOT_ELIGIBLE);
        assertThat(eligibilityResult.getEpimsId()).isEqualTo(expectedEpimsId);
        assertThat(eligibilityResult.getLegislativeCountry()).isEqualTo(LegislativeCountry.WALES);
    }

    @Test
    @Transactional
    @DisplayName("Returns NOT_ELIGIBLE status for cross border postcode that maps to ePIMS ID eligible from tomorrow")
    void shouldReturnNotEligibleStatusForCrossBorderPostcodeEligibleTomorrow() throws Exception {
        String postcode = "SY132LH";
        String legislativeCountry = "England";
        int epimsId = 20262;

        setEligibilityFromDate(epimsId, LocalDate.now(UK_ZONE_ID).plusDays(1));

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, legislativeCountry);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.NOT_ELIGIBLE);
        assertThat(eligibilityResult.getEpimsId()).isEqualTo(epimsId);
        assertThat(eligibilityResult.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
    }

    @Test
    @DisplayName("Returns NO_MATCH_FOUND for a cross border postcode that maps to expired ePIMS ID")
    void shouldReturnNoMatchFoundStatusForCrossBorderPostcode() throws Exception {
        String postcode = "SY101AB";
        String legislativeCountry = "Wales";

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, legislativeCountry);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.NO_MATCH_FOUND);
    }

    @Test
    @DisplayName("Returns MULTIPLE_MATCHES_FOUND status for cross border postcode with multiple ePIMS ID matches")
    void shouldReturnMultipleMatchesFoundStatusForCrossBorderPostcodeMultipleEpimsIdMatch() throws Exception {
        String postcode = "DN551P";
        String legislativeCountry = "England";

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode, legislativeCountry);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.MULTIPLE_MATCHES_FOUND);
    }

    @Test
    @DisplayName("Returns 400 status code when postcode parameter is empty or missing")
    void shouldReturn400StatusCode() throws Exception {
        String postcode = "";
        String legislativeCountry = "";

        mockMvc.perform(get("/testing-support/claim-eligibility")
                                                  .header("serviceAuthorization", "test")
                                                  .queryParam("postcode", postcode)
                                                  .queryParam("legislativeCountry", legislativeCountry)
                                                  .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    private EligibilityResult getEligibilityForPostcode(String postcode, String legislativeCountry) throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/testing-support/claim-eligibility")
                                                  .header("serviceAuthorization", "test")
                                                  .queryParam("postcode", postcode)
                                                  .queryParam("legislativeCountry", legislativeCountry)
                                                  .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(json, EligibilityResult.class);
    }

    private void setEligibilityFromDate(int epimsId, LocalDate eligibleFrom) {
        CourtEligibilityEntity courtEligibility = courtEligibilityRepository.findById(epimsId)
            .orElseThrow(() -> new AssertionError("Failed to find eligibility for ePIMS ID " + epimsId));

        courtEligibility.setEligibleFrom(eligibleFrom);

        courtEligibilityRepository.save(courtEligibility);
    }
}
