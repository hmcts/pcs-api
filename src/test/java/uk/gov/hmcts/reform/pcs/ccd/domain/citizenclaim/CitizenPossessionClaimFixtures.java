package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public final class CitizenPossessionClaimFixtures {

    public static final String COMPLETE_CLAIM_JSON = "/citizenclaim/complete-citizen-possession-claim.json";

    private CitizenPossessionClaimFixtures() {
    }

    public static ObjectMapper draftObjectMapper() {
        return new JacksonConfiguration().draftCaseDataObjectMapper();
    }

    public static CitizenPossessionClaim completeClaim() {
        try (InputStream json = CitizenPossessionClaimFixtures.class.getResourceAsStream(COMPLETE_CLAIM_JSON)) {
            return draftObjectMapper().readValue(json, PCSCase.class).getCitizenPossessionClaim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
