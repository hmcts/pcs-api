package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;

@Component
@AllArgsConstructor
public class PossessionClaimMerger {

    private final ClaimantOrgNameListCreator claimantOrgNameListCreator;

    public PossessionClaimResponse mergeLatestCaseData(PCSCase latestCase, PossessionClaimResponse savedResponses) {
        return savedResponses.toBuilder()
            .claimantOrganisations(claimantOrgNameListCreator.createClaimantOrgNameList(latestCase))
            .build();
    }
}
