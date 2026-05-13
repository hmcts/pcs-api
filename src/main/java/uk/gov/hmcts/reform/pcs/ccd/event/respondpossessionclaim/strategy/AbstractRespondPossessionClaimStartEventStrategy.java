package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractRespondPossessionClaimStartEventStrategy
    implements RespondPossessionClaimStartEventStrategy {

    protected final PossessionClaimResponseMapper responseMapper;
    protected final DraftCaseDataService draftCaseDataService;

    protected PossessionClaimResponse mergeLatestCaseData(PCSCase latestCase, PossessionClaimResponse savedResponses) {
        return savedResponses.toBuilder()
            .claimantOrganisations(createClaimantOrgNameList(latestCase))
            .build();
    }

    protected PCSCase buildCaseWithDraft(PCSCase pcsCase, PossessionClaimResponse response) {
        return pcsCase.toBuilder()
            .possessionClaimResponse(response)
            .hasUnsubmittedCaseData(YesOrNo.YES)
            .build();
    }

    protected PossessionClaimResponse createDefendantOnlyDraft(PossessionClaimResponse response) {
        return PossessionClaimResponse.builder()
            .defendantContactDetails(response.getDefendantContactDetails())
            .build();
    }

    private List<ListValue<String>> createClaimantOrgNameList(PCSCase pcsCase) {
        List<ListValue<Party>> allClaimants = pcsCase.getAllClaimants();

        if (allClaimants == null || allClaimants.isEmpty()) {
            log.warn("No claimant parties found");
            return List.of();
        }

        return allClaimants.stream()
            .map(claimant -> ListValue.<String>builder()
                .id(claimant.getId())
                .value(claimant.getValue().getOrgName())
                .build())
            .toList();
    }

}
