package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.manageparty;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.ManagePartyOptions;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.AddPartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@Component("managePartySubmitEventHandler")
@RequiredArgsConstructor
public class SubmitEventHandler implements Submit<PCSCase, State> {

    private final PcsCaseService pcsCaseService;
    private final AddressFormatter addressFormatter;
    private final AddPartyService addPartyService;

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {

        PCSCase caseData = eventPayload.caseData();
        AddPartyDetails partyDetails = caseData.getAddPartyDetails();
        if (partyDetails.getManagePartyOptions() != ManagePartyOptions.ADD_PARTY) {
            return SubmitResponse.<State>builder()
                .build();
        }

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(eventPayload.caseReference());
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();
        DynamicList partyRadioList = partyDetails.getPartyRadioList();
        UUID actingForPartyId = partyRadioList != null ? partyRadioList.getValueCode() : null;
        addPartyService.addParty(partyDetails, pcsCaseEntity, mainClaim, actingForPartyId);

        return SubmitResponse.<State>builder()
            .confirmationBody(buildConfirmationPageForParty(caseData, eventPayload.caseReference()))
            .build();
    }

    private String buildConfirmationPageForParty(PCSCase pcsCase, long caseReference) {
        AddPartyDetails partyDetails = pcsCase.getAddPartyDetails();

        String partyDescription = switch (partyDetails.getAddPartyType()) {
            case CLAIMANT -> "Claimant %s".formatted(resolvePartyName(
                partyDetails.getClaimantOrganisationName(), partyDetails.getClaimantName()));
            case DEFENDANT -> "Defendant %s %s".formatted(
                partyDetails.getFirstName(), partyDetails.getLastName());
            case LITIGATION_FRIEND -> "Litigation friend %s".formatted(resolvePartyName(
                partyDetails.getLitigationFriendOrganisationName(), partyDetails.getLitigationFriendName()));
        };

        return buildConfirmationMarkdown(
            partyDescription, caseReference, pcsCase.getPropertyAddress(), pcsCase.getCaseNameHmctsInternal());
    }

    private String resolvePartyName(String organisationName, String personName) {
        return StringUtils.isNotBlank(organisationName) ? organisationName : personName;
    }

    private String buildConfirmationMarkdown(String partyDescription, long caseReference, AddressUK address,
                                              String caseName) {
        String formatAddress = addressFormatter.formatShortAddress(address, COMMA_DELIMITER);
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">%s added</span><br>
            <span class="govuk-panel__body">Case number: %s</span><br>
            <span class="govuk-panel__body">%s</span>
            <span class="govuk-panel__body">%s</span>
            </div>

            <h3 class="govuk-heading-s">What happens next</h3>
            <p class="govuk-body govuk-!-margin-bottom-6">If the application was made without notice, only the applicant
            will be informed. Otherwise, all parties will be informed.</p>
            """.formatted(partyDescription, caseReference, formatAddress, caseName);
    }

}