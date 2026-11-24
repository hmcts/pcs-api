package uk.gov.hmcts.reform.pcs.ccd.service.rules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalOtherGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredNoArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.util.PossessionGroundsValidationUtil;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Validation and routing rules for the possession grounds pages, applicable to both the
 * make a claim journey and non-UI callers.
 */
@Component
public class GroundsRules {

    public static final String AT_LEAST_ONE_GROUND_ERROR = "Please select at least one ground";
    public static final String WALES_AT_LEAST_ONE_GROUND_ERROR = "Please select at least one ground.";
    public static final String ANTISOCIAL_ABSOLUTE_GROUND_ERROR =
        "Please select at least one absolute ground for possession for antisocial behaviour";
    public static final String WALES_ESTATE_MANAGEMENT_ERROR =
        "Please select at least one ground in ‘Estate management grounds (section 160)’.";

    /**
     * Visibility of the Wales ASB questions and grounds reasons pages.
     */
    public record WalesGroundsRouting(YesOrNo showAsbQuestionsPage, YesOrNo showReasonsPage) {
    }

    private final TextAreaValidationService textAreaValidationService;

    @Autowired
    public GroundsRules(TextAreaValidationService textAreaValidationService) {
        this.textAreaValidationService = textAreaValidationService;
    }

    public GroundsRules() {
        this(new TextAreaValidationService());
    }

    /**
     * Errors for the free-text description of an "other" ground; empty when the description is absent or valid.
     */
    public List<String> validateOtherGroundDescription(String description) {
        if (description == null) {
            return new ArrayList<>();
        }
        return textAreaValidationService.validateSingleTextArea(
            description, PCSCase.OTHER_GROUND_DESCRIPTION_LABEL, TextAreaValidationService.MEDIUM_TEXT_LIMIT);
    }

    // ---- England: secure or flexible tenancy ----

    /**
     * At most one error: no ground selected, or antisocial behaviour selected without an absolute ground.
     */
    public List<String> validateSecureOrFlexibleGrounds(SecureOrFlexiblePossessionGrounds grounds) {
        if (grounds.getSecureOrFlexibleDiscretionaryGrounds().isEmpty()
            && grounds.getSecureOrFlexibleDiscretionaryGroundsAlt().isEmpty()
            && grounds.getSecureOrFlexibleMandatoryGrounds().isEmpty()
            && grounds.getSecureOrFlexibleMandatoryGroundsAlt().isEmpty()) {
            return List.of(AT_LEAST_ONE_GROUND_ERROR);
        }
        if (grounds.getSecureOrFlexibleMandatoryGrounds().contains(SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL)
            && grounds.getSecureAntisocialAdditionalGrounds().isEmpty()) {
            return List.of(ANTISOCIAL_ABSOLUTE_GROUND_ERROR);
        }
        return List.of();
    }

    public boolean isRentArrearsOrBreachOfTenancyGroundSelected(SecureOrFlexiblePossessionGrounds grounds) {
        return grounds.getSecureOrFlexibleDiscretionaryGrounds()
            .contains(SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY);
    }

    public YesOrNo showReasonsForSecureOrFlexibleGrounds(SecureOrFlexiblePossessionGrounds grounds) {
        boolean hasOtherDiscretionaryGrounds = grounds.getSecureOrFlexibleDiscretionaryGrounds()
            .stream()
            .anyMatch(ground -> ground != SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY);

        return YesOrNo.from(hasOtherDiscretionaryGrounds
                                || !grounds.getSecureOrFlexibleDiscretionaryGroundsAlt().isEmpty()
                                || !grounds.getSecureOrFlexibleMandatoryGrounds().isEmpty()
                                || !grounds.getSecureOrFlexibleMandatoryGroundsAlt().isEmpty());
    }

    /**
     * YES when the claimant selected the breach of tenancy option, so the breach details text area is shown.
     */
    public YesOrNo showBreachOfTenancyTextarea(Set<RentArrearsOrBreachOfTenancy> rentArrearsOrBreachOfTenancy) {
        return YesOrNo.from(rentArrearsOrBreachOfTenancy.contains(RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY));
    }

    // ---- England: assured tenancy with rent arrears (additional grounds) ----

    public boolean hasAtLeastOneAssuredAdditionalGround(AssuredRentArrearsPossessionGrounds grounds) {
        return PossessionGroundsValidationUtil.hasAtLeastOneGround(
            grounds.getAdditionalMandatoryGrounds(),
            grounds.getAdditionalDiscretionaryGrounds(),
            grounds.getAdditionalOtherGround());
    }

    public List<String> validateAssuredAdditionalGrounds(AssuredRentArrearsPossessionGrounds grounds) {
        if (!hasAtLeastOneAssuredAdditionalGround(grounds)) {
            return List.of(AT_LEAST_ONE_GROUND_ERROR);
        }
        return validateOtherGroundDescription(grounds.getAdditionalOtherGroundDescription());
    }

    // ---- England: assured tenancy without rent arrears ----

    public boolean hasAtLeastOneAssuredNoArrearsGround(AssuredNoArrearsPossessionGrounds grounds) {
        return PossessionGroundsValidationUtil.hasAtLeastOneGround(
            grounds.getMandatoryGrounds(), grounds.getDiscretionaryGrounds(), grounds.getOtherGround());
    }

    public List<String> validateAssuredNoArrearsGrounds(AssuredNoArrearsPossessionGrounds grounds) {
        if (!hasAtLeastOneAssuredNoArrearsGround(grounds)) {
            return List.of(AT_LEAST_ONE_GROUND_ERROR);
        }
        return validateOtherGroundDescription(grounds.getOtherGroundDescription());
    }

    /**
     * YES when any ground other than the rent arrears grounds (8, 10 and 11) is selected.
     */
    public YesOrNo showReasonsForAssuredNoArrearsGrounds(AssuredNoArrearsPossessionGrounds grounds) {
        Set<AssuredMandatoryGround> mandatoryGrounds = grounds.getMandatoryGrounds();
        Set<AssuredDiscretionaryGround> discretionaryGrounds = grounds.getDiscretionaryGrounds();
        Set<AssuredAdditionalOtherGround> otherGrounds = grounds.getOtherGround();

        boolean hasOtherMandatoryGrounds = !CollectionUtils.isEmpty(mandatoryGrounds)
            && mandatoryGrounds.stream()
            .anyMatch(ground -> ground != AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8);

        boolean hasOtherDiscretionaryGrounds = !CollectionUtils.isEmpty(discretionaryGrounds)
            && discretionaryGrounds.stream()
            .anyMatch(ground -> ground != AssuredDiscretionaryGround.RENT_ARREARS_GROUND10
                && ground != AssuredDiscretionaryGround.PERSISTENT_DELAY_GROUND11);

        return YesOrNo.from(hasOtherDiscretionaryGrounds
                            || hasOtherMandatoryGrounds
                            || !CollectionUtils.isEmpty(otherGrounds));
    }

    // ---- England: introductory, demoted or other tenancy ----

    public List<String> validateIntroductoryDemotedOrOtherGrounds(
        IntroductoryDemotedOtherGroundsForPossession grounds) {
        return validateOtherGroundDescription(grounds.getOtherGroundDescription());
    }

    /**
     * YES when a ground other than rent arrears is selected, or when the claimant has no grounds to select.
     */
    public YesOrNo showReasonsForIntroductoryDemotedOrOtherGrounds(
        IntroductoryDemotedOtherGroundsForPossession grounds) {

        boolean hasOtherGrounds = Optional.ofNullable(grounds.getIntroductoryDemotedOrOtherGrounds())
            .stream()
            .flatMap(Collection::stream)
            .anyMatch(ground -> ground != IntroductoryDemotedOrOtherGrounds.RENT_ARREARS);

        return YesOrNo.from(hasOtherGrounds
                            || grounds.getHasIntroductoryDemotedOtherGroundsForPossession() == VerticalYesNo.NO);
    }

    // ---- Wales: standard occupation contract ----

    public List<String> validateWalesGrounds(GroundsForPossessionWales grounds) {
        List<String> errors = new ArrayList<>();

        Set<DiscretionaryGroundWales> discretionaryGrounds = discretionaryGrounds(grounds);
        Set<MandatoryGroundWales> mandatoryGrounds = mandatoryGrounds(grounds);
        Set<EstateManagementGroundsWales> estateManagementGrounds =
            grounds == null
                ? Set.of() : Objects.requireNonNullElse(grounds.getEstateManagementGrounds(), Set.of());

        if (discretionaryGrounds.isEmpty() && mandatoryGrounds.isEmpty()) {
            errors.add(WALES_AT_LEAST_ONE_GROUND_ERROR);
        }

        if (discretionaryGrounds.contains(DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160)
            && estateManagementGrounds.isEmpty()) {
            errors.add(WALES_ESTATE_MANAGEMENT_ERROR);
        }

        return errors;
    }

    /**
     * Page visibility derived from the selected grounds; empty when the selection matches none of the routes
     * (in which case the existing flags are left unchanged).
     */
    public Optional<WalesGroundsRouting> deriveWalesGroundsRouting(GroundsForPossessionWales grounds) {
        Set<DiscretionaryGroundWales> discretionaryGrounds = discretionaryGrounds(grounds);
        Set<MandatoryGroundWales> mandatoryGrounds = mandatoryGrounds(grounds);

        boolean hasDiscretionary = !discretionaryGrounds.isEmpty();

        boolean hasRentArrears = discretionaryGrounds.contains(DiscretionaryGroundWales.RENT_ARREARS_S157)
            || mandatoryGrounds.contains(MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181)
            || mandatoryGrounds.contains(MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187);
        boolean hasAsb = hasDiscretionary
            && discretionaryGrounds.contains(DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157);
        boolean hasOtherBreach = hasDiscretionary
            && discretionaryGrounds.contains(DiscretionaryGroundWales.OTHER_BREACH_OF_CONTRACT_S157);
        boolean hasEstateManagement = hasDiscretionary
            && discretionaryGrounds.contains(DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160);

        boolean hasMandatoryWithoutArrears = mandatoryGrounds.stream()
            .anyMatch(ground -> ground != MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181
                && ground != MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187);

        boolean hasOtherOptions = hasOtherBreach || hasEstateManagement || hasMandatoryWithoutArrears;

        return route(hasRentArrears, hasAsb, hasOtherOptions);
    }

    // ---- Wales: secure contract ----

    /**
     * At most one error: estate management parent selected without a sub-selection, or no ground selected.
     */
    public List<String> validateSecureContractGrounds(SecureContractGroundsForPossessionWales grounds) {
        Set<SecureContractDiscretionaryGroundsWales> discretionaryGrounds = grounds.getDiscretionaryGrounds();

        if (discretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS_S160)
            && grounds.getEstateManagementGrounds().isEmpty()) {
            return List.of(WALES_ESTATE_MANAGEMENT_ERROR);
        }

        if (discretionaryGrounds.isEmpty() && grounds.getMandatoryGrounds().isEmpty()) {
            return List.of(AT_LEAST_ONE_GROUND_ERROR);
        }
        return List.of();
    }

    public Optional<WalesGroundsRouting> deriveSecureContractGroundsRouting(
        SecureContractGroundsForPossessionWales grounds) {

        Set<SecureContractDiscretionaryGroundsWales> discretionaryGrounds = grounds.getDiscretionaryGrounds();
        Set<SecureContractMandatoryGroundsWales> mandatoryGrounds = grounds.getMandatoryGrounds();

        boolean hasRentArrears =
            discretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.RENT_ARREARS_S157);
        boolean hasAsb =
            discretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR_S157);
        boolean hasOtherBreach =
            discretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT_S157);
        boolean hasEstateManagement =
            discretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS_S160);

        boolean hasOtherOptions = hasOtherBreach || hasEstateManagement || !mandatoryGrounds.isEmpty();

        return route(hasRentArrears, hasAsb, hasOtherOptions);
    }

    /**
     * Whether antisocial behaviour (section 157) is selected under either the standard or the secure contract.
     */
    public boolean hasWalesAntisocialBehaviourGround(GroundsForPossessionWales standardGrounds,
                                                     SecureContractGroundsForPossessionWales secureGrounds) {
        boolean hasAsbStandard = standardGrounds != null
            && standardGrounds.getDiscretionaryGrounds() != null
            && standardGrounds.getDiscretionaryGrounds().contains(DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_S157);
        boolean hasAsbSecure = secureGrounds != null
            && secureGrounds.getDiscretionaryGrounds() != null
            && secureGrounds.getDiscretionaryGrounds()
                .contains(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR_S157);
        return hasAsbStandard || hasAsbSecure;
    }

    private Optional<WalesGroundsRouting> route(boolean hasRentArrears, boolean hasAsb, boolean hasOtherOptions) {
        if (hasRentArrears && !hasAsb && !hasOtherOptions) {
            return Optional.of(new WalesGroundsRouting(YesOrNo.NO, YesOrNo.NO));
        } else if (hasAsb && !hasOtherOptions) {
            return Optional.of(new WalesGroundsRouting(YesOrNo.YES, YesOrNo.NO));
        } else if (hasOtherOptions) {
            return Optional.of(new WalesGroundsRouting(YesOrNo.NO, YesOrNo.YES));
        }
        return Optional.empty();
    }

    private Set<DiscretionaryGroundWales> discretionaryGrounds(GroundsForPossessionWales grounds) {
        return grounds == null ? Set.of() : Objects.requireNonNullElse(grounds.getDiscretionaryGrounds(), Set.of());
    }

    private Set<MandatoryGroundWales> mandatoryGrounds(GroundsForPossessionWales grounds) {
        return grounds == null ? Set.of() : Objects.requireNonNullElse(grounds.getMandatoryGrounds(), Set.of());
    }
}
