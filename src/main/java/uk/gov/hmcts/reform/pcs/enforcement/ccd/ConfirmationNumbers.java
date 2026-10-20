package uk.gov.hmcts.reform.pcs.enforcement.ccd;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfirmationNumbers {

    public static String caseAndWarrant(String formattedCaseReference, String warrantNumber) {
        String caseNumber = "Case number: " + formattedCaseReference;
        return StringUtils.isBlank(warrantNumber)
            ? caseNumber
            : caseNumber + "\n\nWarrant number: " + warrantNumber;
    }
}
