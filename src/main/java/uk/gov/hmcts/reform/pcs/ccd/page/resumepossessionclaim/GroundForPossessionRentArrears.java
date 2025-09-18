package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.MandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.HashSet;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * Page for selecting rent arrears grounds for possession.
 */
public class GroundForPossessionRentArrears implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("groundForPossessionRentArrears", this::midEvent)
                .pageLabel("Grounds for possession")
                .showCondition("groundsForPossession=\"Yes\""
                               +  " AND typeOfTenancyLicence=\"ASSURED_TENANCY\"")
                .readonly(PCSCase::getCopyOfRentArrearsGrounds,NEVER_SHOW)
                .label("groundForPossessionRentArrears-info", """
                ---
                <p class="govuk-body">You may have already given the defendants notice of your intention to begin
                    possession proceedings. If you have, you should have written the grounds you're making your
                    claim under. You should select these grounds here.</p>
                <h2 class="govuk-!-font-size-19 govuk-!-margin-bottom-1">Serious rent arrears (ground 8)</h2>
                <p class="govuk-body">Mandatory ground. Can be used if the defendants owe at least:</p>
                <ul class="govuk-list govuk-list--bullet">
                    <li class="govuk-list govuk-!-font-size-19 govuk-!-margin-0">eight weeks' rent if they pay
                        weekly or forthnightly</li>
                    <li class="govuk-list govuk-!-font-size-19 govuk-!-margin-0">two months' rent if they pay
                        monthly</li>
                    <li class="govuk-list govuk-!-font-size-19 govuk-!-margin-0">three months' rent if they pay
                        quarterly or yearly</li>
                </ul>
                <p class="govuk-body">The defendants must owe the rent when the notice is served and the time of the
                    hearing.</p>
                <h2 class="govuk-!-font-size-19 govuk-!-margin-bottom-1">Rent arrears (ground 10)</h2>
                <p class="govuk-body">Discretionary ground. The defendants are in any amount of arrears.</p>
                <h2 class="govuk-!-font-size-19 govuk-!-margin-bottom-1">Persistent delay in paying rent
                    (ground 11)</h2>
                <p class="govuk-body">Discretionary ground. The defendants have persistently delayed paying their
                    rent.</p>
                """)
                .mandatory(PCSCase::getRentArrearsGrounds)
                .mandatory(PCSCase::getHasOtherAdditionalGrounds);
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        // Get the rent arrears grounds that were selected
        Set<RentArrearsGround> rentArrearsGrounds = caseData.getRentArrearsGrounds();

        // Initialize sets if they don't exist
        Set<MandatoryGround> mandatoryGrounds = caseData.getMandatoryGrounds();

        if (mandatoryGrounds == null) {
            mandatoryGrounds = new HashSet<>();
        }

        Set<DiscretionaryGround> discretionaryGrounds = caseData.getDiscretionaryGrounds();

        if (discretionaryGrounds == null) {
            discretionaryGrounds = new HashSet<>();
        }

        if (rentArrearsGrounds != null && !rentArrearsGrounds.isEmpty()) {

            // Check each rent arrears ground and add corresponding grounds to the appropriate sets
            for (RentArrearsGround rentArrearsGround : rentArrearsGrounds) {
                switch (rentArrearsGround) {
                    case SERIOUS_RENT_ARREARS_GROUND8:
                        mandatoryGrounds.add(MandatoryGround.SERIOUS_RENT_ARREARS_GROUND8);
                        break;
                    case RENT_ARREARS_GROUND10:
                        // Ground 10 is discretionary
                        discretionaryGrounds.add(DiscretionaryGround.RENT_ARREARS_GROUND10);
                        break;
                    case PERSISTENT_DELAY_GROUND11:
                        // Ground 11 is discretionary
                        discretionaryGrounds.add(DiscretionaryGround.PERSISTENT_DELAY_GROUND11);
                        break;
                }
            }

        }

        // Update grounds only when the rent arrears options have changed as this will override them
        if (rentArrearsGrounds != null && !rentArrearsGrounds.equals(caseData.getCopyOfRentArrearsGrounds())) {
            caseData.setMandatoryGrounds(mandatoryGrounds);
            caseData.setDiscretionaryGrounds(discretionaryGrounds);
        }
        caseData.setCopyOfRentArrearsGrounds(rentArrearsGrounds);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
