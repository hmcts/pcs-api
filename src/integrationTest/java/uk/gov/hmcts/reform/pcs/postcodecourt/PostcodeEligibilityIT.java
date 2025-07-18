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

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(eligibilityResult.getEpimsId()).isEqualTo(expectedEpimsId);
    }

    @Test
    @Transactional
    @DisplayName("Returns NOT_ELIGIBLE status for postcode that maps to ePIMS ID eligible from tomorrow")
    void shouldReturnNotEligibleStatus() throws Exception {
        String postcode = "W3 6RS";
        int epimsId = 36791;

        setEligibilityFromDate(epimsId, LocalDate.now(UK_ZONE_ID).plusDays(1));

        EligibilityResult eligibilityResult = getEligibilityForPostcode(postcode);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.NOT_ELIGIBLE);
    }

    private EligibilityResult getEligibilityForPostcode(String postcode) throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/testing-support/claim-eligibility")
                                                  .queryParam("postcode", postcode)
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
