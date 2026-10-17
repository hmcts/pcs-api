package uk.gov.hmcts.reform.pcs.enforcement.ccd;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * The case and warrant numbers a bailiff event's confirmation screen names, in the order
 * {@code DecideEnforcementConfirmation} shows them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfirmationNumbers {

    /**
     * The warrant number is left out when the warrant has none: a warrant is numbered when a caseworker
     * approves the application, so one approved before warrants were numbered has nothing to show.
     *
     * @param warrantNumber the warrant's number, or null for a warrant that has none
     */
    public static String caseAndWarrant(String formattedCaseReference, String warrantNumber) {
        String caseNumber = "Case number: " + formattedCaseReference;
        return StringUtils.isBlank(warrantNumber)
            ? caseNumber
            : caseNumber + "\n\nWarrant number: " + warrantNumber;
    }
}
