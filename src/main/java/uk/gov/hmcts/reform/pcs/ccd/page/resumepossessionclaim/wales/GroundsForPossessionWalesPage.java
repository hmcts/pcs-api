package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// Grounds for possession Standard Contract and Other Contract
@Component
@Slf4j
public class GroundsForPossessionWalesPage implements CcdPageConfiguration {

    private static final String DISCRETIONARY_GROUNDS = "possessionGroundsWales_DiscretionaryGrounds";

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
                  <p>You may have already given the defendants notice of your intention to begin possession
                  proceedings. If you have, you should have written the grounds you’re making your claim under.
                  You should select these grounds here and select any extra grounds you’d like to add to your claim,
                  if you need to.</p>
                  <p class="govuk-body">
                    <a href="https://www.gov.wales/understanding-possession-action-process-guidance-tenants-contract-holders-html" class="govuk-link" rel="noreferrer noopener" target="_blank">More information about possession grounds (opens in new tab)</a>.
                  </p>
                """
            )
            .complex(PCSCase::getGroundsForPossessionWales)
                .optional(GroundsForPossessionWales::getDiscretionaryGrounds)
                .optional(
                    GroundsForPossessionWales::getEstateManagementGrounds,
                    ShowConditions.fieldContains(DISCRETIONARY_GROUNDS,
                                                 DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160
                    )
                )
                .optional(GroundsForPossessionWales::getMandatoryGrounds)
                .done()
            .label("groundsForPossessionWales-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details,
        CaseDetails<PCSCase, State> before) {

        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();

        GroundsForPossessionWales grounds = data.getGroundsForPossessionWales();
        if (grounds == null) {
            grounds = GroundsForPossessionWales.builder().build();
            data.setGroundsForPossessionWales(grounds);
        }

        Set<DiscretionaryGroundWales> discretionaryGrounds = 
            Objects.requireNonNullElse(grounds.getDiscretionaryGrounds(), Set.of());
        Set<MandatoryGroundWales> mandatoryGrounds = 
            Objects.requireNonNullElse(grounds.getMandatoryGrounds(), Set.of());
        Set<EstateManagementGroundsWales> estateManagementGrounds = 
            Objects.requireNonNullElse(grounds.getEstateManagementGrounds(), Set.of());

        boolean hasDiscretionary = !discretionaryGrounds.isEmpty();
        boolean hasMandatory = !mandatoryGrounds.isEmpty();

        // at least one from Discretionary OR Mandatory
        if (!hasDiscretionary && !hasMandatory) {
            errors.add("Please select at least one ground.");
        }

        // if Estate management parent ticked, require sub-selection
        if (discretionaryGrounds.contains(DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160)) {
            boolean hasEstate = !estateManagementGrounds.isEmpty();

            if (!hasEstate) {
                errors.add("Please select at least one ground in ‘Estate management grounds (section 160)’.");
            }
        }

        if (!errors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", errors))
                .build();
        }

        // ASB/Reasons routing (from master)
        boolean hasRentArrears = discretionaryGrounds.contains(DiscretionaryGroundWales.RENT_ARREARS_S157)
                || mandatoryGrounds.contains(MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181)
                || mandatoryGrounds.contains(MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187);
        boolean hasASB = hasDiscretionary
                && discretionaryGrounds.contains(DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157);
        boolean hasOtherBreach = hasDiscretionary
                && discretionaryGrounds.contains(DiscretionaryGroundWales.OTHER_BREACH_OF_CONTRACT_S157);
        boolean hasEstateManagement = hasDiscretionary
                && discretionaryGrounds.contains(DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160);

        boolean hasMandatoryWithoutArrears = mandatoryGrounds.stream()
                .anyMatch(ground -> ground != MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181
                        && ground != MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187);

        // Determine if there are "other options" (anything that's not rent arrears or ASB)
        boolean hasOtherOptions = hasOtherBreach || hasEstateManagement || hasMandatoryWithoutArrears;

        // Routing rules based on options selected
        if (hasRentArrears && !hasASB && !hasOtherOptions) {
            data.setShowASBQuestionsPageWales(YesOrNo.NO);
            data.setShowReasonsForGroundsPageWales(YesOrNo.NO);
        } else if (hasASB && !hasOtherOptions) {
            data.setShowASBQuestionsPageWales(YesOrNo.YES);
            data.setShowReasonsForGroundsPageWales(YesOrNo.NO);
        } else if (hasOtherOptions) {
            data.setShowASBQuestionsPageWales(YesOrNo.NO);
            data.setShowReasonsForGroundsPageWales(YesOrNo.YES);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .build();
    }
}
