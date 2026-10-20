package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.ConfirmationNumbers;
import uk.gov.hmcts.reform.pcs.enforcement.util.EvictionDateFormat;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EvictionConfirmationFormat {

    public static String format(LocalDateTime dateTime) {
        return EvictionDateFormat.format(dateTime);
    }

    public static String scheduledBody(String formattedCaseReference, long caseReference, String warrantNumber) {
        return """
            %s

            <h2 class="govuk-heading-m">You need to</h2>
            <ul class="govuk-list govuk-list--bullet">
              <li>Print the 'notice of eviction' (form N54)</li>
            </ul>

            <a href="/cases/case-details/%d#Case%%20File%%20View" target="_blank" rel="noopener noreferrer" \
            class="govuk-link">Go to case file view (same service, opens in a new tab)</a>
            """.formatted(ConfirmationNumbers.caseAndWarrant(formattedCaseReference, warrantNumber), caseReference);
    }
}
