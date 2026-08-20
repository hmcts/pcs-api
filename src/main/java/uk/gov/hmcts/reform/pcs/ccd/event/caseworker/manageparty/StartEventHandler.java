package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.manageparty;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;

@Component("managePartyStartEventHandler")
@RequiredArgsConstructor
public class StartEventHandler implements Start<PCSCase, State> {

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;

    @Override
    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(eventPayload.caseReference());
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();

        caseData.getAddPartyDetails().setPartyRadioList(buildApplicantPartyList(mainClaim));

        return caseData;
    }

    private DynamicList buildApplicantPartyList(ClaimEntity mainClaim) {
        List<DynamicListElement> listItems = mainClaim.getClaimParties().stream()
            .filter(claimPartyEntity -> claimPartyEntity.getRole() == PartyRole.CLAIMANT
                || claimPartyEntity.getRole() == PartyRole.DEFENDANT)
            .map(claimPartyEntity -> DynamicListElement.builder()
                .code(claimPartyEntity.getParty().getId())
                .label("%s - %s".formatted(
                    buildPartyDisplayName(claimPartyEntity.getParty()),
                    partyService.getPartyLabel(mainClaim, claimPartyEntity.getParty().getId())
                ))
                .build())
            .toList();

        return DynamicList.builder().listItems(listItems).build();
    }

    private String buildPartyDisplayName(PartyEntity partyEntity) {
        if (partyEntity.getNameKnown() == VerticalYesNo.NO) {
            return "Person unknown";
        }
        return partyService.getPartyName(partyEntity);
    }

}