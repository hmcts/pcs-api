package uk.gov.hmcts.reform.pcs.ccd.service.rules;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class TenancyDateRules {

    public static final String ENGLAND_TENANCY_DATE_NOT_IN_PAST_ERROR =
        "Date the tenancy or licence began must be in the past";
    public static final String WALES_DATE_NOT_TODAY_ERROR =
        "Occupation contract or licence start date cannot be today";
    public static final String WALES_DATE_NOT_FUTURE_ERROR =
        "Occupation contract or licence start date cannot be in the future";

    private final Clock ukClock;

    public TenancyDateRules(@Qualifier("ukClock") Clock ukClock) {
        this.ukClock = ukClock;
    }

    /**
     * Error when the English tenancy or licence start date is today or in the future; empty if valid or absent.
     */
    public Optional<String> validateEnglandTenancyLicenceDate(LocalDate tenancyLicenceDate) {
        if (tenancyLicenceDate != null && !tenancyLicenceDate.isBefore(LocalDate.now(ukClock))) {
            return Optional.of(ENGLAND_TENANCY_DATE_NOT_IN_PAST_ERROR);
        }
        return Optional.empty();
    }

    /**
     * Errors when the Welsh occupation contract or licence start date is today or in the future.
     */
    public List<String> validateWalesLicenceStartDate(LocalDate licenceStartDate) {
        List<String> errors = new ArrayList<>();
        if (licenceStartDate != null) {
            LocalDate currentDate = LocalDate.now(ukClock);
            if (licenceStartDate.isEqual(currentDate)) {
                errors.add(WALES_DATE_NOT_TODAY_ERROR);
            } else if (licenceStartDate.isAfter(currentDate)) {
                errors.add(WALES_DATE_NOT_FUTURE_ERROR);
            }
        }
        return errors;
    }
}
