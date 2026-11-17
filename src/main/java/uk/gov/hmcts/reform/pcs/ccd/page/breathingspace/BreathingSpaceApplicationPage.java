package uk.gov.hmcts.reform.pcs.ccd.page.breathingspace;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceType;

import java.time.Clock;
import java.time.LocalDate;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Component
public class BreathingSpaceApplicationPage implements CcdPageConfiguration {

    protected static final String FUTURE_START_DATE_ERROR = "Date Breathing Space started must be today or in the past";

    private final Clock ukClock;

    public BreathingSpaceApplicationPage(@Qualifier("ukClock") Clock ukClock) {
        this.ukClock = ukClock;
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("breathingSpaceApplication", this::midEvent)
            .label("breathingSpaceApplication-lineSeparator", "---")
            .complex(PCSCase::getBreathingSpaceDetails)
            .optional(BreathingSpaceDetails::getReferenceNumber)
            .optional(BreathingSpaceDetails::getStartDate)
            .optional(BreathingSpaceDetails::getType)
            .optional(BreathingSpaceDetails::getExpectedEndDate,
                      fieldEquals("breathingSpace_Type", BreathingSpaceType.STANDARD))
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        LocalDate startDate = caseData.getBreathingSpaceDetails() == null
            ? null
            : caseData.getBreathingSpaceDetails().getStartDate();

        AboutToStartOrSubmitResponse<PCSCase, State> response = AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();

        if (startDate != null && startDate.isAfter(LocalDate.now(ukClock))) {
            response.setErrorMessageOverride(FUTURE_START_DATE_ERROR);
        }

        return response;
    }

}
