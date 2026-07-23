package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.addparty;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty.AddLitigationParty;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty.ManagePartyOptionsPage;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.manageParties;

@Component
@AllArgsConstructor
public class ManageParty implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(manageParties.name(), this::submit, this::start)
            .forStates(State.values())
            .name("Manage parties")
            .grant(Permission.CRUD, CASEWORKER_ROLES)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
            .showSummary()
            .endButtonLabel("Submit");

        new PageBuilder(eventBuilder)
            .add(new ManagePartyOptionsPage())
            .add(new AddLitigationParty())
            .add(new AddPartyDetails());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(eventPayload.caseReference());
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();

        caseData.setPartyRadioList(buildApplicantPartyList(mainClaim));

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

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        return SubmitResponse.<State>builder().build();
    }
}
