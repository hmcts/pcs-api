package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotApplicable.YES;

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
            .complex(PCSCase::getWalesHousingAct)
                .mandatory(WalesHousingAct::getRegistered)
                .mandatory(WalesHousingAct::getRegistrationNumber,
                    "walesRegistered=\"YES\"")
                .mandatory(WalesHousingAct::getLicensed)
                .mandatory(WalesHousingAct::getLicenceNumber,
                    "walesLicensed=\"YES\"")
                .mandatory(WalesHousingAct::getLicensedAgentAppointed)
                .label("walesAgentDetails-label", """
                <h3 class="govuk-heading-s">Give details of your licensed agent</h3>
                """,
                    "walesLicensedAgentAppointed=\"YES\"")
                .mandatory(WalesHousingAct::getAgentFirstName,
                    "walesLicensedAgentAppointed=\"YES\"")
                .mandatory(WalesHousingAct::getAgentLastName,
                    "walesLicensedAgentAppointed=\"YES\"")
                .mandatory(WalesHousingAct::getAgentLicenceNumber,
                    "walesLicensedAgentAppointed=\"YES\"")
                .mandatory(WalesHousingAct::getAgentAppointmentDate,
                    "walesLicensedAgentAppointed=\"YES\"")
            .done()
            .label("claimantDetailsWales-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        // Validate agent appointment date is in the past
        WalesHousingAct walesHousingAct = caseData.getWalesHousingAct();
        if (walesHousingAct != null) {
            LocalDate appointmentDate = walesHousingAct.getAgentAppointmentDate();
            if (appointmentDate != null && walesHousingAct.getLicensedAgentAppointed() == YES) {
                LocalDate currentDate = LocalDate.now(ukClock);
                if (appointmentDate.isAfter(currentDate)) {
                    return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                        .errors(List.of("The agent’s date of appointment must be in the past"))
                        .build();
                }
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
