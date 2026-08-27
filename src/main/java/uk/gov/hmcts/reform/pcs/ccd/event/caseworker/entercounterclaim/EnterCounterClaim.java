package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.entercounterclaim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterCounterClaimDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.CounterClaimAmount;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.CourtPermission;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.HelpWithFees;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.TypeOfCounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimService;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enterCounterClaim;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASEWORKER_EVENTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_3;

@Component
@RequiredArgsConstructor
public class EnterCounterClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final CounterClaimService counterClaimService;
    private final CourtPermission courtPermission;
    private final TypeOfCounterClaim typeOfCounterClaim;
    private final CounterClaimAmount counterClaimAmount;
    private final HelpWithFees helpWithFees;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(enterCounterClaim.name(), this::submit, this::start)
            .forStates(State.CASE_ISSUED)
            .name("Enter a counterclaim")
            .showCondition(ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_3, CASEWORKER_EVENTS))
            .grant(Permission.CRU, CASEWORKER_ROLES)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
            .endButtonLabel("Submit")
            .showSummary();

        new PageBuilder(eventBuilder)
            .add(courtPermission)
            .add(typeOfCounterClaim)
            .add(counterClaimAmount)
            .add(helpWithFees);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(eventPayload.caseReference());
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();
        caseData.setCounterClaimSubmittingPartyList(
            partyService.buildPartyDynamicList(mainClaim, PartyRole.DEFENDANT));
        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();
        EnterCounterClaimDetails counterClaimRequest = caseData.getEnterCounterClaim();

        PartyEntity submittingParty = partyService.getPartyEntityByEntityId(
            caseData.getCounterClaimSubmittingPartyList().getValueCode(), caseReference);

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(counterClaimRequest.getClaimTypeOption())
            .courtPermissionGranted(counterClaimRequest.getCourtPermissionGranted())
            .permissionOrderDate(counterClaimRequest.getPermissionOrderDate())
            .claimReceivedDate(counterClaimRequest.getClaimReceivedDate())
            .build();

        counterClaimService.saveCounterClaim(caseReference, counterClaim, submittingParty);

        return SubmitResponse.<State>builder()
            .build();
    }


}
