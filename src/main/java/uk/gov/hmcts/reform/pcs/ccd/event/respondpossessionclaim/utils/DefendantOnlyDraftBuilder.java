package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;

@Component
public class DefendantOnlyDraftBuilder {

    public PossessionClaimResponse createDefendantOnlyDraft(PossessionClaimResponse response) {
        return PossessionClaimResponse.builder()
            .defendantContactDetails(response.getDefendantContactDetails())
            .build();
    }
}
