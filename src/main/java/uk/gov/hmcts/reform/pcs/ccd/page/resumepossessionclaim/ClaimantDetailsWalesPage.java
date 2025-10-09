package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantDetailsWales;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.LicensingOption.YES;

@Component
public class ClaimantDetailsWalesPage implements CcdPageConfiguration {

    private final Clock ukClock;

    public ClaimantDetailsWalesPage(@Qualifier("ukClock") Clock ukClock) {
        this.ukClock = ukClock;
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("claimantDetailsWales", this::midEvent)
            .pageLabel("Claimant details")
            .showCondition("legislativeCountry=\"Wales\"")
            .label("claimantDetailsWales-info", "---")
            .complex(PCSCase::getWalesClaimantDetails)
                .mandatory(ClaimantDetailsWales::getWalesRegistrationLicensed)
                .mandatory(ClaimantDetailsWales::getWalesRegistrationNumber,
                    "walesRegistrationLicensed=\"YES\"")
                .mandatory(ClaimantDetailsWales::getWalesLicenseLicensed)
                .mandatory(ClaimantDetailsWales::getWalesLicenseNumber,
                    "walesLicenseLicensed=\"YES\"")
                .mandatory(ClaimantDetailsWales::getWalesLicensedAgentAppointed)
                .label("walesAgentDetails-label", """
                <h3 class="govuk-heading-s">Give details of your licensed agent</h3>
                """,
                    "walesLicensedAgentAppointed=\"YES\"")
                .mandatory(ClaimantDetailsWales::getWalesAgentFirstName,
                    "walesLicensedAgentAppointed=\"YES\"")
                .mandatory(ClaimantDetailsWales::getWalesAgentLastName,
                    "walesLicensedAgentAppointed=\"YES\"")
                .mandatory(ClaimantDetailsWales::getWalesAgentLicenseNumber,
                    "walesLicensedAgentAppointed=\"YES\"")
                .mandatory(ClaimantDetailsWales::getWalesAgentAppointmentDate,
                    "walesLicensedAgentAppointed=\"YES\"")
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        List<String> errors = new ArrayList<>();

        // Validate agent appointment date is in the past
        ClaimantDetailsWales walesDetails = caseData.getWalesClaimantDetails();
        if (walesDetails != null) {
            LocalDate appointmentDate = walesDetails.getWalesAgentAppointmentDate();
            if (appointmentDate != null && walesDetails.getWalesLicensedAgentAppointed() == YES) {
                LocalDate currentDate = LocalDate.now(ukClock);
                if (appointmentDate.isAfter(currentDate)) {
                    errors.add("The agent's date of appointment must be in the past");
                }
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
