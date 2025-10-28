package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class GroundsForPossessionWales
    implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("groundsForPossessionWales", this::midEvent)
            .pageLabel("What are your grounds for possession?")
            .showCondition("legislativeCountry=\"Wales\" AND "
                + "(occupationLicenceTypeWales=\"STANDARD_CONTRACT\" OR occupationLicenceTypeWales=\"OTHER\")")
            .label(
                "groundsForPossessionWales-info",
                """
                  ---
                  <p class="govuk-body" tabindex="0">
                  You may have already given the defendants notice of your intention to begin possession proceedings.
                  If you have, you should have written the grounds you’re making your claim under. You should select
                  these grounds here and select any extra grounds you’d like to add to your claim, if you need to.
                </p>
                   <p class="govuk-body" tabindex="0">
                      <a class="govuk-link govuk-!-margin-bottom-3" href="https://www.gov.uk/"  rel="noreferrer noopener"\s
                       target="_blank" aria-label="More information about possession grounds (opens in new tab)">
                       More information about possession grounds (opens in new tab)</a>.
                   </p>
                """
            )
            .optional(PCSCase::getDiscretionaryGroundsWales)
            .optional(
                PCSCase::getEstateManagementGroundsWales,
                "discretionaryGroundsWales CONTAINS "
                    + "\"ESTATE_MANAGEMENT_GROUNDS_SECTION_160\""
            )
            .optional(PCSCase::getMandatoryGroundsWales);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details,
        CaseDetails<PCSCase, State> before) {

        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();

        Set<DiscretionaryGroundWales> discretionaryGrounds = data.getDiscretionaryGroundsWales();
        var mandatoryGrounds = data.getMandatoryGroundsWales();
        var estateManagementGrounds = data.getEstateManagementGroundsWales();

        boolean hasDiscretionary = discretionaryGrounds != null && !discretionaryGrounds.isEmpty();
        boolean hasMandatory = mandatoryGrounds != null && !mandatoryGrounds.isEmpty();

        // at least one from Discretionary OR Mandatory
        if (!hasDiscretionary && !hasMandatory) {
            errors.add("Please select at least one ground.");
        }

        // if Estate management parent ticked, require sub-selection
        if (hasDiscretionary
            && discretionaryGrounds.contains(DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_SECTION_160)) {

            boolean hasEstate = estateManagementGrounds != null && !estateManagementGrounds.isEmpty();

            if (!hasEstate) {
                errors.add("Please select at least one ground in 'Estate management grounds (section 160)'.");
            }
        }

        if (!errors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errors(errors)
                .build();
        }

        // Reset all routing flags
        data.setShowReasonsForGroundsPageWales(YesOrNo.NO);
        data.setShowPreActionProtocolPageWales(YesOrNo.NO);
        data.setShowASBQuestionsPageWales(YesOrNo.NO);

        boolean hasRentArrears = discretionaryGrounds != null
                && discretionaryGrounds.contains(DiscretionaryGroundWales.RENT_ARREARS_SECTION_157);
        boolean hasASB = discretionaryGrounds != null
                && discretionaryGrounds.contains(DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_SECTION_157);
        boolean hasOtherBreach = discretionaryGrounds != null
                && discretionaryGrounds.contains(DiscretionaryGroundWales.OTHER_BREACH_SECTION_157);
        boolean hasEstateManagement = discretionaryGrounds != null
                && discretionaryGrounds.contains(DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_SECTION_160);
        boolean hasMandatoryGrounds = hasMandatory;

        // Determine if there are "other options" (anything that's not rent arrears or ASB)
        boolean hasOtherOptions = hasOtherBreach || hasEstateManagement || hasMandatoryGrounds;

        // Routing rules based on options selected
        if (hasRentArrears && !hasASB && !hasOtherOptions) {
            data.setShowPreActionProtocolPageWales(YesOrNo.YES);
        } else if (hasASB && !hasRentArrears && !hasOtherOptions) {
            data.setShowASBQuestionsPageWales(YesOrNo.YES);
        } else if (hasRentArrears && hasASB && !hasOtherOptions) {
            data.setShowASBQuestionsPageWales(YesOrNo.YES);
        } else if (hasOtherOptions) {
            data.setShowReasonsForGroundsPageWales(YesOrNo.YES);
        }


        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .build();
    }
}
