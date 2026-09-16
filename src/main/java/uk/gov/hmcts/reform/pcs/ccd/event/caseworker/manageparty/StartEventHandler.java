package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.manageparty;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.RemovePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.UpdatePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.RemovePartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component("managePartyStartEventHandler")
@RequiredArgsConstructor
public class StartEventHandler implements Start<PCSCase, State> {

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final RemovePartyService removePartyService;

    @Override
    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        initialiseManagePartyDetails(caseData);

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(eventPayload.caseReference());
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();

        caseData.getAddPartyDetails().setPartyRadioList(
            buildPartyList(mainClaim, PartyRole.CLAIMANT, PartyRole.DEFENDANT));
        caseData.getUpdatePartyDetails().setPartyToUpdate(
            buildPartyList(mainClaim, PartyRole.CLAIMANT, PartyRole.DEFENDANT));
        DynamicList removablePartyList = buildRemovablePartyList(mainClaim);
        caseData.getRemovePartyDetails().setPartyToRemove(removablePartyList);
        boolean canSelectParty = !removablePartyList.getListItems().isEmpty();
        caseData.getRemovePartyDetails().setCanSelectParty(YesOrNo.from(canSelectParty));
        caseData.getRemovePartyDetails().setLastPartyMessage(canSelectParty ? null : buildLastPartyMessage(mainClaim));
        caseData.getRemovePartyDetails().setUnremovablePartyList(buildUnremovablePartyList(mainClaim));

        return caseData;
    }

    private void initialiseManagePartyDetails(PCSCase caseData) {
        if (caseData.getAddPartyDetails() == null) {
            caseData.setAddPartyDetails(AddPartyDetails.builder().build());
        }
        if (caseData.getUpdatePartyDetails() == null) {
            caseData.setUpdatePartyDetails(UpdatePartyDetails.builder().build());
        }
        if (caseData.getRemovePartyDetails() == null) {
            caseData.setRemovePartyDetails(RemovePartyDetails.builder().build());
        }
    }

    private DynamicList buildPartyList(ClaimEntity mainClaim, PartyRole... allowedRoles) {
        Set<PartyRole> roles = Set.of(allowedRoles);
        List<DynamicListElement> listItems = mainClaim.getClaimParties().stream()
            .filter(claimPartyEntity -> roles.contains(claimPartyEntity.getRole()))
            .filter(claimPartyEntity -> partyService.isActive(claimPartyEntity.getParty()))
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

    private DynamicList buildRemovablePartyList(ClaimEntity mainClaim) {
        List<DynamicListElement> listItems = mainClaim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.CLAIMANT
                || claimParty.getRole() == PartyRole.DEFENDANT)
            .filter(claimParty -> partyService.isActive(claimParty.getParty()))
            .filter(claimParty -> removePartyService.canSelectForRemoval(claimParty, mainClaim))
            .map(claimParty -> DynamicListElement.builder()
                .code(claimParty.getParty().getId())
                .label(buildPartyListLabel(mainClaim, claimParty.getParty()))
                .build())
            .toList();

        return DynamicList.builder().listItems(listItems).build();
    }

    private String buildUnremovablePartyList(ClaimEntity mainClaim) {
        return mainClaim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.CLAIMANT
                || claimParty.getRole() == PartyRole.DEFENDANT)
            .filter(claimParty -> partyService.isActive(claimParty.getParty()))
            .filter(claimParty -> !removePartyService.canSelectForRemoval(claimParty, mainClaim))
            .map(claimParty -> buildPartyListLabel(mainClaim, claimParty.getParty()))
            .collect(Collectors.joining("\n"));
    }

    private String buildLastPartyMessage(ClaimEntity mainClaim) {
        return """
            You cannot remove a claimant or defendant if only one of these parties exist on the case.

            %s
            """.formatted(buildUnremovablePartyList(mainClaim));
    }

    private String buildPartyListLabel(ClaimEntity mainClaim, PartyEntity partyEntity) {
        return "%s - %s".formatted(
            buildPartyDisplayName(partyEntity),
            partyService.getPartyLabel(mainClaim, partyEntity.getId())
        );
    }

    private String buildPartyDisplayName(PartyEntity partyEntity) {
        if (partyEntity.getNameKnown() == VerticalYesNo.NO) {
            return "Person unknown";
        }
        return partyService.getPartyName(partyEntity);
    }
}
