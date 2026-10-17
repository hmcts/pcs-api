package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;

/**
 * Checks the "Record warrant return" answers (HDPI-7347) against the warrant's current status. XUI already
 * enforces the mandatory drop downs and date, but they are checked again here because hidden fields are not
 * validated by XUI and the status can change while the event is open.
 */
@Component
public class WarrantReturnValidator {

    static final String RETURN_REQUIRED = "Select a return";
    static final String OUTCOME_REQUIRED = "Select an outcome";
    static final String RETURN_NAME_REQUIRED = "Select a return name";
    static final String EVICTION_DATE_TIME_REQUIRED = "Enter the date and time of the eviction";
    static final String EVICTION_DATE_TIME_IN_FUTURE = "The eviction date and time cannot be in the future";
    static final String ADDITIONAL_DETAILS_REQUIRED = "Enter additional details";
    static final String ADDITIONAL_DETAILS_TOO_LONG = "Additional details must be 6,800 characters or fewer";
    static final String CLOSED_WARRANT_CAN_ONLY_BE_REOPENED =
        "The warrant is closed. The only return you can record is \"Reopen warrant\"";
    static final String ONLY_A_CLOSED_WARRANT_CAN_BE_REOPENED = "Only a closed warrant can be reopened";

    static final int ADDITIONAL_DETAILS_LIMIT = RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;

    private final Clock ukClock;

    public WarrantReturnValidator(@Qualifier("ukClock") Clock ukClock) {
        this.ukClock = ukClock;
    }

    /**
     * The errors to show the user, in page order. Empty if the return can be recorded.
     */
    public List<String> validate(WarrantReturnDetails details, EnforcementStatus currentStatus) {
        List<String> errors = new ArrayList<>();
        WarrantReturnType type = details == null ? null : details.getType();
        if (type == null) {
            errors.add(RETURN_REQUIRED);
            return errors;
        }
        if (type == WarrantReturnType.FINAL && details.getOutcome() == null) {
            errors.add(OUTCOME_REQUIRED);
            return errors;
        }

        Optional<WarrantReturnName> returnName = WarrantReturnDetails.selectedReturnName(details);
        if (returnName.isEmpty()) {
            errors.add(RETURN_NAME_REQUIRED);
        }
        if (isSuccessfulEviction(details)) {
            validateEvictionDateTime(details.getEvictionDateTime(), errors);
        }
        returnName.ifPresent(name -> validateAdditionalDetails(name, details.getAdditionalDetails(), errors));
        returnName.ifPresent(name -> validateAgainstStatus(name, currentStatus, errors));
        return errors;
    }

    static boolean isSuccessfulEviction(WarrantReturnDetails details) {
        return details.getType() == WarrantReturnType.FINAL
            && details.getOutcome() == WarrantReturnOutcome.EVICTION_SUCCESSFUL;
    }

    private void validateEvictionDateTime(LocalDateTime evictionDateTime, List<String> errors) {
        if (evictionDateTime == null) {
            errors.add(EVICTION_DATE_TIME_REQUIRED);
        } else if (evictionDateTime.isAfter(LocalDateTime.now(ukClock))) {
            errors.add(EVICTION_DATE_TIME_IN_FUTURE);
        }
    }

    private static void validateAdditionalDetails(WarrantReturnName returnName, String additionalDetails,
                                                  List<String> errors) {
        if (additionalDetails != null && additionalDetails.length() > ADDITIONAL_DETAILS_LIMIT) {
            errors.add(ADDITIONAL_DETAILS_TOO_LONG);
        } else if (returnName.isAdditionalDetailsRequired()
            && (additionalDetails == null || additionalDetails.isBlank())) {
            errors.add(ADDITIONAL_DETAILS_REQUIRED);
        }
    }

    // 19-enforcement-status-model.feature: a closed warrant only takes "Reopen warrant", and only a closed
    // warrant can be reopened.
    private static void validateAgainstStatus(WarrantReturnName returnName, EnforcementStatus currentStatus,
                                              List<String> errors) {
        boolean closed = currentStatus == EnforcementStatus.CLOSED;
        boolean reopen = returnName == WarrantReturnName.REOPEN_WARRANT;
        if (closed && !reopen) {
            errors.add(CLOSED_WARRANT_CAN_ONLY_BE_REOPENED);
        } else if (!closed && reopen) {
            errors.add(ONLY_A_CLOSED_WARRANT_CAN_BE_REOPENED);
        }
    }
}
