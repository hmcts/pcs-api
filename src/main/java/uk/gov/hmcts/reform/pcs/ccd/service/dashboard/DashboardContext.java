package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.UUID;

public record DashboardContext(
    long caseReference,
    PcsCaseEntity caseEntity,
    PartyEntity defendant,
    boolean hasDraftResponse,
    boolean hasSubmittedResponse
) {

    // TODO: Use filtered genApps from PCSCase when available; remove this duplicate visibility rule.
    public boolean isVisibleToUser(GenAppEntity genAppEntity, UUID userId) {
        return genAppEntity.getWithoutNotice() != VerticalYesNo.YES
            || (userId != null && userId.equals(
                genAppEntity.getParty() != null ? genAppEntity.getParty().getIdamId() : null));
    }
}
