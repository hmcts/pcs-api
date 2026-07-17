package uk.gov.hmcts.reform.pcs.ccd.event.hearing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

@Component
@RequiredArgsConstructor
public class ConfirmationBodyRenderer {

    private final AddressFormatter addressFormatter;

    public String renderHearingAddedConfirmationBody(PCSCase caseData, long caseReference) {
        String address = getFormattedAddress(caseData);
        String caseName = caseData.getCaseNameHmctsInternal();

        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">Hearing added</span><br>
            <span class="govuk-panel__body">Case number #%s</span><br>
            <span class="govuk-panel__body">%s</span><br>
            <span class="govuk-panel__body">%s</span><br>
            </div>

            <h3>What happens next</h3>

            A hearing notice will be issued if you specified one is needed.
            """.formatted(caseReference, address, caseName);
    }

    public String renderHearingCancelledConfirmationBody(PCSCase caseData, long caseReference) {
        String address = getFormattedAddress(caseData);
        String caseName = caseData.getCaseNameHmctsInternal();

        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">Hearing cancelled</span><br>
            <span class="govuk-panel__body">Case number #%s</span><br>
            <span class="govuk-panel__body">%s</span><br>
            <span class="govuk-panel__body">%s</span><br>
            </div>

            <h3>What happens next</h3>

            A cancellation notice will be sent to the parties.
            """.formatted(caseReference, address, caseName);
    }

    private String getFormattedAddress(PCSCase caseData) {
        return addressFormatter
            .formatMediumAddress(caseData.getPropertyAddress(), AddressFormatter.COMMA_DELIMITER);
    }

}
