package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;

import java.util.UUID;

@Component
@AllArgsConstructor
public class PossessionClaimMerger {

    private final ClaimantOrgNameListCreator claimantOrgNameListCreator;

    public PossessionClaimResponse mergeLatestCaseData(PCSCase latestCase, PossessionClaimResponse savedResponses,
                                                       UUID defendantPartyId) {
        String currentDefendantPartyId = defendantPartyId != null ? defendantPartyId.toString() : null;

        return savedResponses.toBuilder()
            .claimantOrganisations(claimantOrgNameListCreator.createClaimantOrgNameList(latestCase))
            .currentDefendantPartyId(currentDefendantPartyId)
            .build();
    }
}
