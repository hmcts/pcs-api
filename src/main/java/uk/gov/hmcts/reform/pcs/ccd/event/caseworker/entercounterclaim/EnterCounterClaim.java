package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.entercounterclaim;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.EventStates;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.CounterClaimAmount;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.CourtPermission;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.HelpWithFees;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.PartyCounterClaimAgainst;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.TypeOfCounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.UploadCounterClaimForm;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enterCounterClaim;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASEWORKER_EVENTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_4;

@Component
public class EnterCounterClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final SubmitEventHandler submitEventHandler;
    private final CourtPermission courtPermission;
    private final TypeOfCounterClaim typeOfCounterClaim;
    private final CounterClaimAmount counterClaimAmount;
    private final HelpWithFees helpWithFees;
    private final PartyCounterClaimAgainst partyCounterClaimAgainst;
    private final UploadCounterClaimForm uploadCounterClaimForm;

    public EnterCounterClaim(PcsCaseService pcsCaseService,
                             PartyService partyService,
                             @Qualifier("enterCounterClaimSubmitEventHandler") SubmitEventHandler submitEventHandler,
                             CourtPermission courtPermission,
                             TypeOfCounterClaim typeOfCounterClaim,
                             CounterClaimAmount counterClaimAmount,
                             HelpWithFees helpWithFees,
                             PartyCounterClaimAgainst partyCounterClaimAgainst,
                             UploadCounterClaimForm uploadCounterClaimForm) {
        this.pcsCaseService = pcsCaseService;
        this.partyService = partyService;
        this.submitEventHandler = submitEventHandler;
        this.courtPermission = courtPermission;
        this.typeOfCounterClaim = typeOfCounterClaim;
        this.counterClaimAmount = counterClaimAmount;
        this.helpWithFees = helpWithFees;
        this.partyCounterClaimAgainst = partyCounterClaimAgainst;
        this.uploadCounterClaimForm = uploadCounterClaimForm;
    }

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(enterCounterClaim.name(), submitEventHandler, this::start)
            .forStates(EventStates.enterCounterClaim())
            .name("Enter a counterclaim")
            .showCondition(ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_4, CASEWORKER_EVENTS))
            .grant(Permission.CRU, CASEWORKER_ROLES)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
            .endButtonLabel("Submit")
            .showSummary();

        new PageBuilder(eventBuilder)
            .add(courtPermission)
            .add(typeOfCounterClaim)
            .add(counterClaimAmount)
            .add(helpWithFees)
            .add(partyCounterClaimAgainst)
            .add(uploadCounterClaimForm);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(eventPayload.caseReference());
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();
        caseData.setPartyRadioList(
            partyService.buildPartyDynamicList(mainClaim, PartyRole.DEFENDANT));
        return caseData;
    }

}
