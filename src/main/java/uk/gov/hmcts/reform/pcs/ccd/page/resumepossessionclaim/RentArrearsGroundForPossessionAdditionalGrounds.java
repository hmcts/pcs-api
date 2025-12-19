package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredAdditionalMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredAdditionalDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * Page for selecting additional grounds for possession.
 */
@Slf4j
@Component
public class RentArrearsGroundForPossessionAdditionalGrounds implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("groundForPossessionAdditionalGrounds", this::midEvent)
            .pageLabel("What are your additional grounds for possession?")
            .showCondition("rentArrears_HasOtherAdditionalGrounds=\"Yes\""
                           + " AND tenancy_TypeOfTenancyLicence=\"ASSURED_TENANCY\""
                           + " AND claimDueToRentArrears=\"Yes\""
                           + " AND legislativeCountry=\"England\"")
            .readonly(PCSCase::getShowRentArrearsGroundReasonPage, NEVER_SHOW)
            .label("groundForPossessionAdditionalGrounds-info", """
            ---
            <p class="govuk-body">You may have already given the defendants notice of your intention to begin
                possession proceedings. If you have, you should have written the grounds you’re making your
                claim under. You should select these grounds here and any extra grounds you’d like to add to
                your claim, if you need to.</p>
            <p class="govuk-body">
              <a href="https://england.shelter.org.uk/professional_resources/legal/possession_and_eviction/grounds_for_possession" class="govuk-link" rel="noreferrer noopener" target="_blank">More information about possession grounds (opens in new tab)</a>.
            </p>
            """)
            // Keep canonical sets present in the event for showCondition references
            .readonly(PCSCase::getRentArrearsMandatoryGrounds, NEVER_SHOW)
            .readonly(PCSCase::getRentArrearsDiscretionaryGrounds, NEVER_SHOW)
            .optional(PCSCase::getAssuredAdditionalMandatoryGrounds)
            .optional(PCSCase::getAssuredAdditionalDiscretionaryGrounds)
            .label("groundForPossessionAdditionalGrounds-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                 CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        // Rebuild canonical sets from rent arrears grounds selection
        Set<RentArrearsMandatoryGrounds> mergedMandatory = new HashSet<>();
        Set<RentArrearsDiscretionaryGrounds> mergedDiscretionary = new HashSet<>();
        Set<RentArrearsGround> rentArrearsGrounds = caseData.getGroundsForPossession().getGrounds();

        if (rentArrearsGrounds != null) {
            if (rentArrearsGrounds.contains(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8)) {
                mergedMandatory.add(RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8);
            }
            if (rentArrearsGrounds.contains(RentArrearsGround.RENT_ARREARS_GROUND10)) {
                mergedDiscretionary.add(RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10);
            }
            if (rentArrearsGrounds.contains(RentArrearsGround.PERSISTENT_DELAY_GROUND11)) {
                mergedDiscretionary.add(RentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11);
            }
        }

        // Union additional-only selections (mapped to canonical enums)
        Set<AssuredAdditionalMandatoryGrounds> addMandatory =
            Objects.requireNonNullElse(
                caseData.getAssuredAdditionalMandatoryGrounds(),
                Set.of()
            );

        for (AssuredAdditionalMandatoryGrounds add : addMandatory) {
            mergedMandatory.add(RentArrearsMandatoryGrounds.valueOf(add.name()));
        }

        Set<AssuredAdditionalDiscretionaryGrounds> addDiscretionary =
            Objects.requireNonNullElse(
                caseData.getAssuredAdditionalDiscretionaryGrounds(),
                Set.of()
            );

        for (AssuredAdditionalDiscretionaryGrounds add : addDiscretionary) {
            mergedDiscretionary.add(RentArrearsDiscretionaryGrounds.valueOf(add.name()));
        }

        // Compute selection flags
        boolean hasRentArrearsGrounds = rentArrearsGrounds != null && !rentArrearsGrounds.isEmpty();
        boolean hasAdditionalMandatory = !addMandatory.isEmpty();
        boolean hasAdditionalDiscretionary = !addDiscretionary.isEmpty();

        // Validate according to requirement: if rent arrears grounds were selected previously,
        // then at least one additional ground on THIS page must be selected
        boolean requireAdditionalSelection = hasRentArrearsGrounds;
        boolean hasAnyAdditional = hasAdditionalMandatory || hasAdditionalDiscretionary;

        if (requireAdditionalSelection && !hasAnyAdditional) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(List.of("Please select at least one ground"))
                .build();
        }

        // Backward compatibility: if no rent arrears grounds or additional-only input present,
        // use existing canonical sets
        boolean noRentArrearsGrounds = rentArrearsGrounds == null || rentArrearsGrounds.isEmpty();
        boolean noAdditional = addMandatory.isEmpty() && addDiscretionary.isEmpty();

        Set<RentArrearsMandatoryGrounds> effectiveMandatory = mergedMandatory;
        Set<RentArrearsDiscretionaryGrounds> effectiveDiscretionary = mergedDiscretionary;

        if (noRentArrearsGrounds && noAdditional) {
            effectiveMandatory = Objects.requireNonNullElse(
                caseData.getRentArrearsMandatoryGrounds(), new HashSet<>()
            );
            effectiveDiscretionary = Objects.requireNonNullElse(
                caseData.getRentArrearsDiscretionaryGrounds(), new HashSet<>()
            );
        } else {
            caseData.setRentArrearsMandatoryGrounds(mergedMandatory);
            caseData.setRentArrearsDiscretionaryGrounds(mergedDiscretionary);
        }

        boolean hasOtherMandatoryGrounds = effectiveMandatory.stream()
            .anyMatch(ground -> ground != RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8);

        boolean hasOtherDiscretionaryGrounds = effectiveDiscretionary.stream()
            .anyMatch(ground -> ground != RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10
                && ground != RentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11);

        caseData.setShowRentArrearsGroundReasonPage(
            YesOrNo.from(hasOtherDiscretionaryGrounds || hasOtherMandatoryGrounds)
        );

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
