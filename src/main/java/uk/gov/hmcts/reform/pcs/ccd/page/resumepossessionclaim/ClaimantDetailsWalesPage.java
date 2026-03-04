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
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.time.Clock;
import java.time.LocalDate;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;
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
            .showWhen(when(PCSCase::getLegislativeCountry).is(LegislativeCountry.WALES))
            .label("claimantDetailsWales-info", "---")
            .complex(PCSCase::getWalesHousingAct)
                .mandatory(WalesHousingAct::getRegistered)
                .mandatoryWhen(WalesHousingAct::getRegistrationNumber,
                    when(PCSCase::getWalesHousingAct, WalesHousingAct::getRegistered).is(YES))
                .mandatory(WalesHousingAct::getLicensed)
                .mandatoryWhen(WalesHousingAct::getLicenceNumber,
                    when(PCSCase::getWalesHousingAct, WalesHousingAct::getLicensed).is(YES))
                .mandatory(WalesHousingAct::getLicensedAgentAppointed)
                .labelWhen("walesAgentDetails-label", """
                <h3 class="govuk-heading-s">Give details of your licensed agent</h3>
                """,
                    when(PCSCase::getWalesHousingAct, WalesHousingAct::getLicensedAgentAppointed).is(YES))
                .mandatoryWhen(WalesHousingAct::getAgentFirstName,
                    when(PCSCase::getWalesHousingAct, WalesHousingAct::getLicensedAgentAppointed).is(YES))
                .mandatoryWhen(WalesHousingAct::getAgentLastName,
                    when(PCSCase::getWalesHousingAct, WalesHousingAct::getLicensedAgentAppointed).is(YES))
                .mandatoryWhen(WalesHousingAct::getAgentLicenceNumber,
                    when(PCSCase::getWalesHousingAct, WalesHousingAct::getLicensedAgentAppointed).is(YES))
                .mandatoryWhen(WalesHousingAct::getAgentAppointmentDate,
                    when(PCSCase::getWalesHousingAct, WalesHousingAct::getLicensedAgentAppointed).is(YES))
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
                        .errorMessageOverride("The agent’s date of appointment must be in the past")
                        .build();
                }
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
