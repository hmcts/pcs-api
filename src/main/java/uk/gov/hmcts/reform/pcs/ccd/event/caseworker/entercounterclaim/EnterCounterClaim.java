package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.entercounterclaim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterCounterClaimDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.CounterClaimAmount;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.CourtPermission;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.HelpWithFees;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.PartyCounterClaimAgainst;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.TypeOfCounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.UploadCounterClaimForm;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enterCounterClaim;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASEWORKER_EVENTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_3;

@Component
@RequiredArgsConstructor
public class EnterCounterClaim implements CCDConfig<PCSCase, State, UserRole> {

    private static final String PENDING_ISSUE_WHAT_HAPPENS_NEXT = """
        <h3 class="govuk-heading-s">What happens next</h3>
        <p class="govuk-body">The counterclaim will not be issued until the party’s Help With Fees application has
        been reviewed and they’ve paid any outstanding fee.</p>
        <p class="govuk-body govuk-!-margin-bottom-6">Once the party’s application has been approved or their fee
        has been paid, you must issue the counterclaim.</p>
        """;

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final CounterClaimService counterClaimService;
    private final CourtPermission courtPermission;
    private final TypeOfCounterClaim typeOfCounterClaim;
    private final CounterClaimAmount counterClaimAmount;
    private final HelpWithFees helpWithFees;
    private final PartyCounterClaimAgainst partyCounterClaimAgainst;
    private final UploadCounterClaimForm uploadCounterClaimForm;
    private final AddressFormatter addressFormatter;

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

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();
        EnterCounterClaimDetails counterClaimRequest = caseData.getEnterCounterClaim();

        PartyEntity submittingParty = partyService.getPartyEntityByEntityId(
            caseData.getPartyRadioList().getValueCode(), caseReference);

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(counterClaimRequest.getClaimTypeOption())
            .courtPermissionGranted(counterClaimRequest.getCourtPermissionGranted())
            .permissionOrderDate(counterClaimRequest.getPermissionOrderDate())
            .claimReceivedDate(counterClaimRequest.getClaimReceivedDate())
            .isClaimAmountKnown(VerticalYesNo.YES)
            .claimAmount(counterClaimRequest.getCounterClaimAmount())
            .appliedForHwf(counterClaimRequest.getAppliedForHwf())
            .hwfReferenceNumber(counterClaimRequest.getAppliedForHwf() == VerticalYesNo.YES
                ? counterClaimRequest.getHwfReferenceNumber() : null)
            .counterClaimAgainst(buildCounterClaimAgainst(caseData.getPartyMultiSelectionList()))
            .build();

        counterClaimService.saveCounterClaim(caseReference, counterClaim, submittingParty);

        return SubmitResponse.<State>builder()
            .confirmationBody(buildConfirmationBody(caseData, caseReference))
            .build();
    }

    private String buildConfirmationBody(PCSCase caseData, long caseReference) {
        boolean appliedForHwf = caseData.getEnterCounterClaim().getAppliedForHwf() == VerticalYesNo.YES;

        String title = appliedForHwf ? "Counterclaim pending issue" : "Counterclaim submitted";
        String whatHappensNext = appliedForHwf ? PENDING_ISSUE_WHAT_HAPPENS_NEXT : "";

        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">%s</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">Case number: %s</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">%s</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">%s</span>
            </div>
            %s""".formatted(
                title,
                caseReference,
                addressFormatter.formatShortAddress(caseData.getPropertyAddress(), COMMA_DELIMITER),
                caseData.getCaseNameHmctsInternal(),
                whatHappensNext);
    }

    private List<ListValue<Party>> buildCounterClaimAgainst(DynamicMultiSelectStringList selectedParties) {
        if (selectedParties == null || selectedParties.getValue() == null) {
            return List.of();
        }

        return selectedParties.getValue().stream()
            .map(element -> ListValue.<Party>builder()
                .id(element.getCode())
                .value(Party.builder().build())
                .build())
            .toList();
    }


}
