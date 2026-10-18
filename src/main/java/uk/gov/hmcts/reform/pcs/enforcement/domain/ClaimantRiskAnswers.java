package uk.gov.hmcts.reform.pcs.enforcement.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.VulnerableCategory;

import java.util.EnumSet;
import java.util.Set;

/**
 * The claimant's answers to the warrant's risk assessment questions: the risks posed to the bailiff, vulnerable
 * people at the property, access to the property and anything else that could help with the eviction. Stored as
 * JSON on each risk assessment update, so a field name change needs a data migration.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClaimantRiskAnswers {

    private YesNoNotSure anyRiskToBailiff;
    private Set<RiskCategory> riskCategories;
    private String violentDetails;
    private String firearmsDetails;
    private String criminalDetails;
    private String verbalThreatsDetails;
    private String protestGroupDetails;
    private String policeSocialServicesDetails;
    private String animalsDetails;
    private YesNoNotSure vulnerablePeoplePresent;
    private VulnerableCategory vulnerableCategory;
    private String vulnerableReasonText;
    private VerticalYesNo isDifficultToAccessProperty;
    private String clarificationOnAccessDifficultyText;
    private VerticalYesNo additionalInformationSelect;
    private String additionalInformationDetails;

    /**
     * A copy without the answers to follow-up questions the claimant was not asked, such as the details of a risk
     * category they did not choose, and with blank text removed. The journey can leave these behind when an answer
     * is changed, and they would otherwise count as changes.
     */
    public ClaimantRiskAnswers normalised() {
        boolean anyRisk = anyRiskToBailiff == YesNoNotSure.YES;
        Set<RiskCategory> categories = anyRisk && riskCategories != null && !riskCategories.isEmpty()
            ? EnumSet.copyOf(riskCategories)
            : null;
        boolean vulnerable = vulnerablePeoplePresent == YesNoNotSure.YES;
        return ClaimantRiskAnswers.builder()
            .anyRiskToBailiff(anyRiskToBailiff)
            .riskCategories(categories)
            .violentDetails(detailsFor(categories, RiskCategory.VIOLENT_OR_AGGRESSIVE, violentDetails))
            .firearmsDetails(detailsFor(categories, RiskCategory.FIREARMS_POSSESSION, firearmsDetails))
            .criminalDetails(detailsFor(categories, RiskCategory.CRIMINAL_OR_ANTISOCIAL, criminalDetails))
            .verbalThreatsDetails(detailsFor(categories, RiskCategory.VERBAL_OR_WRITTEN_THREATS,
                                             verbalThreatsDetails))
            .protestGroupDetails(detailsFor(categories, RiskCategory.PROTEST_GROUP_MEMBER, protestGroupDetails))
            .policeSocialServicesDetails(detailsFor(categories, RiskCategory.AGENCY_VISITS,
                                                    policeSocialServicesDetails))
            .animalsDetails(detailsFor(categories, RiskCategory.AGGRESSIVE_ANIMALS, animalsDetails))
            .vulnerablePeoplePresent(vulnerablePeoplePresent)
            .vulnerableCategory(vulnerable ? vulnerableCategory : null)
            .vulnerableReasonText(vulnerable ? textOrNull(vulnerableReasonText) : null)
            .isDifficultToAccessProperty(isDifficultToAccessProperty)
            .clarificationOnAccessDifficultyText(isDifficultToAccessProperty == VerticalYesNo.YES
                                                     ? textOrNull(clarificationOnAccessDifficultyText) : null)
            .additionalInformationSelect(additionalInformationSelect)
            .additionalInformationDetails(additionalInformationSelect == VerticalYesNo.YES
                                              ? textOrNull(additionalInformationDetails) : null)
            .build();
    }

    private static String detailsFor(Set<RiskCategory> categories, RiskCategory category, String details) {
        return categories != null && categories.contains(category) ? textOrNull(details) : null;
    }

    private static String textOrNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
