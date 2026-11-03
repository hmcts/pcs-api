package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantPartyExtractor;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@RequiredArgsConstructor
public class DraftResponseView {

    private final DefendantPartyExtractor defendantPartyExtractor;
    private final DraftCaseDataService draftCaseDataService;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity, State state, String organisationId) {
        if (state != State.CASE_ISSUED || StringUtils.isBlank(organisationId)) {
            pcsCase.setHasDraftResponse(YesOrNo.NO);
            return;
        }

        long caseReference = pcsCaseEntity.getCaseReference();
        List<PartyEntity> representedDefendants =
            defendantPartyExtractor.extractDefendantsRepresentedBy(pcsCaseEntity, organisationId);

        boolean hasDraftResponse = representedDefendants.stream()
            .anyMatch(defendant -> draftCaseDataService.hasMeaningfulRespondDraft(
                caseReference, respondPossessionClaim, defendant.getId(), organisationId));

        pcsCase.setHasDraftResponse(YesOrNo.from(hasDraftResponse));
    }

}
