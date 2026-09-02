package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.entercounterclaim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterCounterClaimDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@Component("enterCounterClaimSubmitEventHandler")
@RequiredArgsConstructor
public class SubmitEventHandler implements Submit<PCSCase, State> {

    private static final String PENDING_ISSUE_WHAT_HAPPENS_NEXT = """
        <h3 class="govuk-heading-s">What happens next</h3>
        <p class="govuk-body">The counterclaim will not be issued until the party’s Help With Fees application has
        been reviewed and they’ve paid any outstanding fee.</p>
        <p class="govuk-body govuk-!-margin-bottom-6">Once the party’s application has been approved or their fee
        has been paid, you must issue the counterclaim.</p>
        """;

    private final PartyService partyService;
    private final CounterClaimService counterClaimService;
    private final AddressFormatter addressFormatter;

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();
        EnterCounterClaimDetails counterClaimRequest = caseData.getEnterCounterClaim();

        PartyEntity submittingParty = partyService.getPartyEntityByEntityId(
            caseData.getPartyRadioList().getValueCode(), caseReference);

        String hwfReferenceNumber = counterClaimRequest.getAppliedForHwf() == VerticalYesNo.YES
            ? counterClaimRequest.getHwfReferenceNumber()
            : null;

        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(counterClaimRequest.getClaimTypeOption())
            .courtPermissionGranted(counterClaimRequest.getCourtPermissionGranted())
            .permissionOrderDate(counterClaimRequest.getPermissionOrderDate())
            .claimReceivedDate(counterClaimRequest.getClaimReceivedDate())
            .isClaimAmountKnown(VerticalYesNo.YES)
            .claimAmount(counterClaimRequest.getCounterClaimAmount())
            .appliedForHwf(counterClaimRequest.getAppliedForHwf())
            .hwfReferenceNumber(hwfReferenceNumber)
            .counterClaimAgainst(buildCounterClaimAgainst(caseData.getPartyMultiSelectionList()))
            .build();

        counterClaimService.saveCaseworkerEnteredCounterClaim(caseReference, counterClaim, submittingParty);

        return SubmitResponse.<State>builder()
            .confirmationBody(buildConfirmationBody(caseData, caseReference, hwfReferenceNumber))
            .build();
    }

    private List<ListValue<Party>> buildCounterClaimAgainst(DynamicMultiSelectStringList selectedParties) {
        if (selectedParties == null || selectedParties.getValue() == null) {
            return List.of();
        }

        return selectedParties.getValue().stream()
            .map(element -> ListValue.<Party>builder()
                .id(element.getCode())
                .build())
            .toList();
    }

    private String buildConfirmationBody(PCSCase caseData, long caseReference, String hwfReferenceNumber) {
        boolean appliedForHwf = hwfReferenceNumber != null;
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
}
