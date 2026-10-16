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
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.RemovePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.UpdatePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.RemovePartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.RemovePartyService.LAST_PARTY_ERROR;

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
        List<ClaimPartyEntity> activeClaimantsAndDefendants =
            removePartyService.getActiveClaimantsAndDefendants(mainClaim);
        DynamicList removablePartyList = buildRemovablePartyList(mainClaim, activeClaimantsAndDefendants);
        caseData.getRemovePartyDetails().setPartyToRemove(removablePartyList);
        boolean canSelectParty = !removablePartyList.getListItems().isEmpty();
        caseData.getRemovePartyDetails().setCanSelectParty(YesOrNo.from(canSelectParty));
        caseData.getRemovePartyDetails().setLastPartyMessage(canSelectParty ? null
            : buildLastPartyMessage(mainClaim, activeClaimantsAndDefendants));
        caseData.getRemovePartyDetails().setUnremovablePartyList(
            buildUnremovablePartyList(mainClaim, activeClaimantsAndDefendants));

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
                    partyService.getPartyName(claimPartyEntity.getParty()),
                    partyService.getPartyLabel(mainClaim, claimPartyEntity.getParty().getId())
                ))
                .build())
            .toList();

        return DynamicList.builder().listItems(listItems).build();
    }

    private DynamicList buildRemovablePartyList(ClaimEntity mainClaim,
                                                List<ClaimPartyEntity> activeClaimantsAndDefendants) {
        List<DynamicListElement> listItems = activeClaimantsAndDefendants.stream()
            .filter(claimParty -> removePartyService.canSelectForRemoval(claimParty, activeClaimantsAndDefendants))
            .map(claimParty -> DynamicListElement.builder()
                .code(claimParty.getParty().getId())
                .label(buildPartyListLabel(mainClaim, claimParty.getParty()))
                .build())
            .toList();

        return DynamicList.builder().listItems(listItems).build();
    }

    private String buildUnremovablePartyList(ClaimEntity mainClaim,
                                             List<ClaimPartyEntity> activeClaimantsAndDefendants) {
        String unremovablePartyNames = buildUnremovablePartyNames(mainClaim, activeClaimantsAndDefendants);
        return unremovablePartyNames.isBlank() ? null : """
            %s

            %s
            """.formatted(LAST_PARTY_ERROR, unremovablePartyNames);
    }

    private String buildUnremovablePartyNames(ClaimEntity mainClaim,
                                              List<ClaimPartyEntity> activeClaimantsAndDefendants) {
        return activeClaimantsAndDefendants.stream()
            .filter(claimParty -> !removePartyService.canSelectForRemoval(claimParty, activeClaimantsAndDefendants))
            .map(claimParty -> buildPartyListLabel(mainClaim, claimParty.getParty()))
            .collect(Collectors.joining("\n"));
    }

    private String buildLastPartyMessage(ClaimEntity mainClaim, List<ClaimPartyEntity> activeClaimantsAndDefendants) {
        return """
            %s

            %s
            """.formatted(LAST_PARTY_ERROR, buildUnremovablePartyNames(mainClaim, activeClaimantsAndDefendants));
    }

    private String buildPartyListLabel(ClaimEntity mainClaim, PartyEntity partyEntity) {
        return "%s - %s".formatted(
            partyService.getPartyName(partyEntity),
            partyService.getPartyLabel(mainClaim, partyEntity.getId())
        );
    }
}
