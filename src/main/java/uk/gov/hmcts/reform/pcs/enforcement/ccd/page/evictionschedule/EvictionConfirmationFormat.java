package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.ConfirmationNumbers;
import uk.gov.hmcts.reform.pcs.enforcement.util.EvictionDateFormat;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EvictionConfirmationFormat {

    private static final String BODY = """
        %s

        <p class="govuk-body">We've sent an email to the claimant with %s.</p>

        <p class="govuk-body">We will send you a new work task when they either:</p>
        <ul class="govuk-list govuk-list--bullet">
          <li>confirm the eviction date, or</li>
          <li>decline the eviction date, if they cannot attend</li>
        </ul>

        <h2 class="govuk-heading-m">What you need to do next</h2>
        <p class="govuk-body">You need to:</p>
        <ul class="govuk-list govuk-list--bullet">
          <li><a href="/cases/case-details/%d#Case%%20File%%20View" target="_blank" rel="noopener noreferrer" \
        class="govuk-link">%s (same service, opens in a new tab)</a></li>
          <li>find and print the 'notice of eviction' (form N54)</li>
          <li>deliver the completed form to the defendants</li>
          <li>return to this service and mark the 'notice of eviction' as served</li>
        </ul>
        """;

    public static String format(LocalDateTime dateTime) {
        return EvictionDateFormat.format(dateTime);
    }

    public static String scheduledBody(String formattedCaseReference, long caseReference, String warrantNumber) {
        return body(formattedCaseReference, caseReference, warrantNumber,
                    "the eviction date", "go to Case File View");
    }

    public static String rescheduledBody(String formattedCaseReference, long caseReference, String warrantNumber) {
        return body(formattedCaseReference, caseReference, warrantNumber,
                    "the rescheduled eviction date", "return to Case File View");
    }

    private static String body(String formattedCaseReference, long caseReference, String warrantNumber,
                               String emailedDate, String caseFileViewLink) {
        return BODY.formatted(ConfirmationNumbers.caseAndWarrant(formattedCaseReference, warrantNumber),
                              emailedDate, caseReference, caseFileViewLink);
    }
}
