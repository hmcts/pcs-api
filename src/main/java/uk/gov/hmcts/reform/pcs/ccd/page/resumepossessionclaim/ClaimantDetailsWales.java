package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.WalesLicensingOption.YES;

@Component
public class ClaimantDetailsWales implements CcdPageConfiguration {

    private final Clock ukClock;

    public ClaimantDetailsWales(@Qualifier("ukClock") Clock ukClock) {
        this.ukClock = ukClock;
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("claimantDetailsWales", this::midEvent)
            .pageLabel("Claimant details")
            .showCondition("legislativeCountry=\"Wales\"")
            .label("claimantDetailsWales-info", "---")
            .mandatory(PCSCase::getWalesRegistrationLicensed)
            .mandatory(PCSCase::getWalesRegistrationNumber,
                "walesRegistrationLicensed=\"YES\"")
            .mandatory(PCSCase::getWalesLicenceLicensed)
            .mandatory(PCSCase::getWalesLicenceNumber,
                "walesLicenceLicensed=\"YES\"")
            .mandatory(PCSCase::getWalesLicensedAgentAppointed)
            .label("walesAgentDetails-label", """
            <p class="govuk-body govuk-!-font-size-19">Give details of your licensed agent</p>
            """,
                "walesLicensedAgentAppointed=\"YES\"")
            .mandatory(PCSCase::getWalesAgentFirstName,
                "walesLicensedAgentAppointed=\"YES\"")
            .mandatory(PCSCase::getWalesAgentLastName,
                "walesLicensedAgentAppointed=\"YES\"")
            .mandatory(PCSCase::getWalesAgentLicenceNumber,
                "walesLicensedAgentAppointed=\"YES\"")
            .mandatory(PCSCase::getWalesAgentAppointmentDate,
                "walesLicensedAgentAppointed=\"YES\"");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        List<String> errors = new ArrayList<>();

        // Validate agent appointment date is in the past
        LocalDate appointmentDate = caseData.getWalesAgentAppointmentDate();
        if (appointmentDate != null && caseData.getWalesLicensedAgentAppointed() == YES) {
            LocalDate currentDate = LocalDate.now(ukClock);
            if (!appointmentDate.isBefore(currentDate) && !appointmentDate.isEqual(currentDate)) {
                errors.add("Agent's date of appointment must be today or in the past");
            }
        }

        if (!errors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(errors)
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
