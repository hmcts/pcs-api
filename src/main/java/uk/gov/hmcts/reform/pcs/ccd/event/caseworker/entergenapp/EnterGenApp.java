package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.entergenapp;

import lombok.RequiredArgsConstructor;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp.ApplicationDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp.ApplicationFee;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp.ConsentAndNotice;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp.HearingDate;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp.ReferApplicationToJudge;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp.UploadGeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp.UploadRelatedEvidence;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enterGenApp;

@Component
@RequiredArgsConstructor
public class EnterGenApp implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final GenAppService genAppService;
    private final ApplicationDetails applicationDetails;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(enterGenApp.name(), this::submit, this::start)
            .forStates(State.CASE_ISSUED)
            .name("Enter a general application")
            .grant(Permission.CRU, CASEWORKER_ROLES)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
            .showSummary();

        new PageBuilder(eventBuilder)
            .add(applicationDetails)
            .add(new HearingDate())
            .add(new ApplicationFee())
            .add(new ConsentAndNotice())
            .add(new UploadGeneralApplication())
            .add(new UploadRelatedEvidence())
            .add(new ReferApplicationToJudge());
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
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        PartyEntity applicantParty = partyService.getPartyEntityByEntityId(
            caseData.getPartyRadioList().getValueCode(), caseReference);

        genAppService.createGenAppEntity(
            caseData.getEnterGenAppRequest(), pcsCaseEntity, applicantParty, GenAppState.GEN_APP_ISSUED);

        return SubmitResponse.<State>builder().build();
    }

}
