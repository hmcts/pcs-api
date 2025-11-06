package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.RequiredArgsConstructor;
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
import uk.gov.hmcts.reform.pcs.ccd.service.routing.RentDetailsRoutingService;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * Page for selecting additional grounds for possession.
 */
@Component
@RequiredArgsConstructor
public class RentArrearsGroundForPossessionAdditionalGrounds implements CcdPageConfiguration {

    private final RentDetailsRoutingService rentDetailsRoutingService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("groundForPossessionAdditionalGrounds", this::midEvent)
            .pageLabel("What are your additional grounds for possession?")
            .showCondition("hasOtherAdditionalGrounds=\"Yes\""
                           + " AND typeOfTenancyLicence=\"ASSURED_TENANCY\""
                           + " AND groundsForPossession=\"Yes\"")
            .readonly(PCSCase::getShowRentArrearsGroundReasonPage, NEVER_SHOW)
            .label("groundForPossessionAdditionalGrounds-info", """
            ---
            <p class="govuk-body">You may have already given the defendants notice of your intention to begin
                possession proceedings. If you have, you should have written the grounds you're making your
                claim under. You should select these grounds here and any extra grounds you'd like to add to
                your claim, if you need to.</p>
            """)
            // Keep canonical sets present in the event for showCondition references
            .readonly(PCSCase::getRentArrearsMandatoryGrounds, NEVER_SHOW)
            .readonly(PCSCase::getRentArrearsDiscretionaryGrounds, NEVER_SHOW)
            .mandatory(PCSCase::getAssuredAdditionalMandatoryGrounds)
            .mandatory(PCSCase::getAssuredAdditionalDiscretionaryGrounds)
            .done();
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                 CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        // Rebuild canonical sets from rent arrears grounds selection
        Set<RentArrearsMandatoryGrounds> mergedMandatory = new HashSet<>();
        Set<RentArrearsDiscretionaryGrounds> mergedDiscretionary = new HashSet<>();
        Set<RentArrearsGround> rentArrearsGrounds = caseData.getRentArrearsGrounds();
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

        // Validate that at least one ground is selected
        if (effectiveMandatory.isEmpty() && effectiveDiscretionary.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(List.of("Please select at least one ground"))
                .build();
        }

        boolean hasOtherMandatoryGrounds = effectiveMandatory.stream()
            .anyMatch(ground -> ground != RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8);

        boolean hasOtherDiscretionaryGrounds = effectiveDiscretionary.stream()
            .anyMatch(ground -> ground != RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10
                && ground != RentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11);

        caseData.setShowRentArrearsGroundReasonPage(
            YesOrNo.from(hasOtherDiscretionaryGrounds || hasOtherMandatoryGrounds)
        );

        // This handles the case when user unchecks rent arrears grounds
        YesOrNo showRentDetails = rentDetailsRoutingService.shouldShowRentDetails(caseData);
        caseData.setShowRentDetailsPage(showRentDetails);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
