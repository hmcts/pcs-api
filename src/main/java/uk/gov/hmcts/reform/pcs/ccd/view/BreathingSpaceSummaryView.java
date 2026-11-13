package uk.gov.hmcts.reform.pcs.ccd.view;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.BreathingSpaceTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.breathingspace.BreathingSpaceService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class BreathingSpaceSummaryView {

    private static final String NOT_PROVIDED = "Not provided";

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BreathingSpaceService breathingSpaceService;

    public void setCaseFields(PCSCase pcsCase, long caseReference, State state) {
        if (state != State.BREATHING_SPACE) {
            return;
        }

        breathingSpaceService.getLatestBreathingSpace(caseReference)
            .ifPresent(details ->
                           pcsCase.getSummaryTab().setBreathingSpaceDetails(
                               buildTabDetails(details)
                           )
            );
    }

    private BreathingSpaceTabDetails buildTabDetails(BreathingSpaceDetails details) {
        return BreathingSpaceTabDetails.builder()
            .referenceNumber(
                textOrNotProvided(details.getReferenceNumber()))
            .startDate(formatDate(details.getStartDate()))
            .type(details.getType() == null
                      ? NOT_PROVIDED
                      : details.getType().getLabel())
            .expectedEndDate(
                formatDate(details.getExpectedEndDate()))
            .build();
    }

    private String textOrNotProvided(String value) {
        return value == null || value.isBlank()
            ? NOT_PROVIDED
            : value;
    }

    private String formatDate(LocalDate date) {
        return date == null
            ? NOT_PROVIDED
            : date.format(DATE_FORMATTER);
    }
}
