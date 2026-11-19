package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkLimits;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.GroundType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.GroundsType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.RentArrearsOrBreachType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalOtherGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredNoArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.NoRentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.RentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureAntisocialAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.GroundsRules;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Routes the flat list of grounds on a bulk claim to the grounds sections and reasons of the make a claim
 * journey that apply to the England tenancy type, then runs the same grounds validation as that journey.
 */
@Component
public class GroundRouter {

    /** Code for a claim made without any ground, valid for introductory, demoted and other tenancies. */
    public static final String NO_GROUNDS = "NO_GROUNDS";

    private final GroundsRules groundsRules;

    public GroundRouter(GroundsRules groundsRules) {
        this.groundsRules = groundsRules;
    }

    /**
     * Routes the claimed grounds for the tenancy type onto the case.
     *
     * @param path path of the Grounds element relative to the claim
     */
    public void route(TenancyLicenceType tenancy,
                      GroundsType grounds,
                      PCSCase target,
                      String path,
                      BulkErrorCollector errors) {

        List<Entry> entries = entries(grounds, path, errors);

        if (!grounds.getRentArrearsOrBreach().isEmpty() && !isSecureOrFlexible(tenancy)) {
            errors.add(path + "/RentArrearsOrBreach", BulkErrorCollector.NOT_ALLOWED,
                       "RentArrearsOrBreach only applies to secure and flexible tenancies");
        }

        switch (tenancy) {
            case ASSURED_TENANCY -> assured(entries, target, path, errors);
            case SECURE_TENANCY, FLEXIBLE_TENANCY -> secureOrFlexible(entries, grounds, target, path, errors);
            case INTRODUCTORY_TENANCY, DEMOTED_TENANCY, OTHER -> introductoryDemotedOrOther(entries, target, errors);
        }
    }

    private static boolean isSecureOrFlexible(TenancyLicenceType tenancy) {
        return tenancy == TenancyLicenceType.SECURE_TENANCY || tenancy == TenancyLicenceType.FLEXIBLE_TENANCY;
    }

    private record Entry(GroundType ground, String path) {
        String code() {
            return ground.getCode();
        }
    }

    private List<Entry> entries(GroundsType grounds, String path, BulkErrorCollector errors) {
        List<Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < grounds.getGround().size(); i++) {
            GroundType ground = grounds.getGround().get(i);
            String groundPath = path + "/" + BulkErrorCollector.indexed("Ground", i);
            if (!seen.add(ground.getCode())) {
                errors.add(groundPath + "/Code", BulkErrorCollector.INVALID_VALUE,
                           "Ground " + ground.getCode() + " is listed more than once");
                continue;
            }
            if (!ground.getEstateManagementReason().isEmpty()) {
                errors.add(groundPath + "/EstateManagementReason", BulkErrorCollector.NOT_ALLOWED,
                           "Estate management reasons only apply to Wales");
            }
            entries.add(new Entry(ground, groundPath));
        }
        return entries;
    }

    private void unknown(Entry entry, TenancyLicenceType tenancy, BulkErrorCollector errors) {
        errors.add(entry.path() + "/Code", BulkErrorCollector.INVALID_VALUE,
                   "Ground " + entry.code() + " is not valid for a " + tenancy + " in England");
    }

    /** The reason text for a ground that needs one; null (and ignored) for grounds that do not. */
    private String reason(Entry entry, boolean needed, BulkErrorCollector errors) {
        if (!needed) {
            return null;
        }
        errors.requiredWithMaxLength(entry.path() + "/Reason", entry.ground().getReason(), BulkLimits.MEDIUM_TEXT,
                                     "Reason for ground " + entry.code());
        return entry.ground().getReason();
    }

    private String otherDescription(Entry entry, BulkErrorCollector errors) {
        String description = entry.ground().getOtherDescription();
        if (errors.required(entry.path() + "/OtherDescription", description, "Description of the other ground")) {
            errors.addAll(entry.path() + "/OtherDescription", BulkErrorCollector.TOO_LONG,
                          groundsRules.validateOtherGroundDescription(description));
        }
        return description;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String code) {
        try {
            return Enum.valueOf(type, code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isRentArrearsGround(String code) {
        return parse(AssuredRentArrearsGround.class, code) != null;
    }

    // ---- assured ----

    private void assured(List<Entry> entries, PCSCase target, String path, BulkErrorCollector errors) {
        boolean dueToRentArrears = entries.stream().anyMatch(entry -> isRentArrearsGround(entry.code()));
        target.setClaimDueToRentArrears(YesOrNo.from(dueToRentArrears));
        if (dueToRentArrears) {
            assuredWithRentArrears(entries, target, errors);
        } else {
            assuredWithoutRentArrears(entries, target, path, errors);
        }
    }

    private void assuredWithRentArrears(List<Entry> entries, PCSCase target, BulkErrorCollector errors) {
        Set<AssuredRentArrearsGround> arrears = EnumSet.noneOf(AssuredRentArrearsGround.class);
        Set<AssuredAdditionalMandatoryGrounds> mandatory = EnumSet.noneOf(AssuredAdditionalMandatoryGrounds.class);
        Set<AssuredAdditionalDiscretionaryGrounds> discretionary =
            EnumSet.noneOf(AssuredAdditionalDiscretionaryGrounds.class);
        Set<AssuredAdditionalOtherGround> other = EnumSet.noneOf(AssuredAdditionalOtherGround.class);
        RentArrearsGroundsReasons.RentArrearsGroundsReasonsBuilder reasons = RentArrearsGroundsReasons.builder();
        String description = null;

        for (Entry entry : entries) {
            String code = entry.code();
            AssuredRentArrearsGround arrearsGround = parse(AssuredRentArrearsGround.class, code);
            AssuredAdditionalMandatoryGrounds mandatoryGround = parse(AssuredAdditionalMandatoryGrounds.class, code);
            AssuredAdditionalDiscretionaryGrounds discretionaryGround =
                parse(AssuredAdditionalDiscretionaryGrounds.class, code);
            if (arrearsGround != null) {
                arrears.add(arrearsGround);
            } else if (mandatoryGround != null) {
                mandatory.add(mandatoryGround);
                String text = reason(entry, true, errors);
                switch (mandatoryGround) {
                    case OWNER_OCCUPIER_GROUND1 -> reasons.ownerOccupierReason(text);
                    case REPOSSESSION_GROUND2 -> reasons.repossessionByLenderReason(text);
                    case HOLIDAY_LET_GROUND3 -> reasons.holidayLetReason(text);
                    case STUDENT_LET_GROUND4 -> reasons.studentLetReason(text);
                    case MINISTER_RELIGION_GROUND5 -> reasons.ministerOfReligionReason(text);
                    case REDEVELOPMENT_GROUND6 -> reasons.redevelopmentReason(text);
                    case DEATH_OF_TENANT_GROUND7 -> reasons.deathOfTenantReason(text);
                    case ANTISOCIAL_BEHAVIOUR_GROUND7A -> reasons.antisocialBehaviourReason(text);
                    case NO_RIGHT_TO_RENT_GROUND7B -> reasons.noRightToRentReason(text);
                }
            } else if (discretionaryGround != null) {
                discretionary.add(discretionaryGround);
                String text = reason(entry, true, errors);
                switch (discretionaryGround) {
                    case ALTERNATIVE_ACCOMMODATION_GROUND9 -> reasons.suitableAltAccommodationReason(text);
                    case BREACH_TENANCY_GROUND12 -> reasons.breachOfTenancyConditionsReason(text);
                    case DETERIORATION_PROPERTY_GROUND13 -> reasons.propertyDeteriorationReason(text);
                    case NUISANCE_ANNOYANCE_GROUND14 -> reasons.nuisanceAnnoyanceReason(text);
                    case DOMESTIC_VIOLENCE_GROUND14A -> reasons.domesticViolenceReason(text);
                    case OFFENCE_RIOT_GROUND14ZA -> reasons.offenceDuringRiotReason(text);
                    case DETERIORATION_FURNITURE_GROUND15 -> reasons.furnitureDeteriorationReason(text);
                    case EMPLOYEE_LANDLORD_GROUND16 -> reasons.employeeOfLandlordReason(text);
                    case FALSE_STATEMENT_GROUND17 -> reasons.tenancyByFalseStatementReason(text);
                }
            } else if ("OTHER".equals(code)) {
                other.add(AssuredAdditionalOtherGround.OTHER);
                description = otherDescription(entry, errors);
                reasons.otherGroundReason(reason(entry, true, errors));
            } else {
                unknown(entry, TenancyLicenceType.ASSURED_TENANCY, errors);
            }
        }

        AssuredRentArrearsPossessionGrounds possessionGrounds = AssuredRentArrearsPossessionGrounds.builder()
            .rentArrearsGrounds(arrears)
            .additionalMandatoryGrounds(mandatory)
            .additionalDiscretionaryGrounds(discretionary)
            .additionalOtherGround(other)
            .additionalOtherGroundDescription(description)
            .build();
        boolean hasAdditional = !(mandatory.isEmpty() && discretionary.isEmpty() && other.isEmpty());
        target.setAssuredRentArrearsPossessionGrounds(possessionGrounds);
        target.setHasOtherAdditionalGrounds(YesOrNo.from(hasAdditional));
        target.setShowReasonsForGroundsPage(YesOrNo.from(hasAdditional));
        target.setRentArrearsGroundsReasons(reasons.build());
    }

    private void assuredWithoutRentArrears(List<Entry> entries, PCSCase target, String path,
                                           BulkErrorCollector errors) {
        Set<AssuredMandatoryGround> mandatory = EnumSet.noneOf(AssuredMandatoryGround.class);
        Set<AssuredDiscretionaryGround> discretionary = EnumSet.noneOf(AssuredDiscretionaryGround.class);
        Set<AssuredAdditionalOtherGround> other = EnumSet.noneOf(AssuredAdditionalOtherGround.class);
        NoRentArrearsGroundsReasons.NoRentArrearsGroundsReasonsBuilder reasons = NoRentArrearsGroundsReasons.builder();
        String description = null;

        for (Entry entry : entries) {
            String code = entry.code();
            AssuredMandatoryGround mandatoryGround = parse(AssuredMandatoryGround.class, code);
            AssuredDiscretionaryGround discretionaryGround = parse(AssuredDiscretionaryGround.class, code);
            if (mandatoryGround != null) {
                mandatory.add(mandatoryGround);
                String text = reason(entry, true, errors);
                switch (mandatoryGround) {
                    case OWNER_OCCUPIER_GROUND1 -> reasons.ownerOccupier(text);
                    case REPOSSESSION_GROUND2 -> reasons.repossessionByLender(text);
                    case HOLIDAY_LET_GROUND3 -> reasons.holidayLet(text);
                    case STUDENT_LET_GROUND4 -> reasons.studentLet(text);
                    case MINISTER_RELIGION_GROUND5 -> reasons.ministerOfReligion(text);
                    case REDEVELOPMENT_GROUND6 -> reasons.redevelopment(text);
                    case DEATH_OF_TENANT_GROUND7 -> reasons.deathOfTenant(text);
                    case ANTISOCIAL_BEHAVIOUR_GROUND7A -> reasons.antisocialBehaviour(text);
                    case NO_RIGHT_TO_RENT_GROUND7B -> reasons.noRightToRent(text);
                    case SERIOUS_RENT_ARREARS_GROUND8 -> { }
                }
            } else if (discretionaryGround != null) {
                discretionary.add(discretionaryGround);
                String text = reason(entry, true, errors);
                switch (discretionaryGround) {
                    case ALTERNATIVE_ACCOMMODATION_GROUND9 -> reasons.suitableAlternativeAccomodation(text);
                    case BREACH_TENANCY_GROUND12 -> reasons.breachOfTenancyConditions(text);
                    case DETERIORATION_PROPERTY_GROUND13 -> reasons.propertyDeterioration(text);
                    case NUISANCE_ANNOYANCE_GROUND14 -> reasons.nuisanceOrIllegalUse(text);
                    case DOMESTIC_VIOLENCE_GROUND14A -> reasons.domesticViolence(text);
                    case OFFENCE_RIOT_GROUND14ZA -> reasons.offenceDuringRiot(text);
                    case DETERIORATION_FURNITURE_GROUND15 -> reasons.furnitureDeterioration(text);
                    case EMPLOYEE_LANDLORD_GROUND16 -> reasons.landlordEmployee(text);
                    case FALSE_STATEMENT_GROUND17 -> reasons.falseStatement(text);
                    case RENT_ARREARS_GROUND10, PERSISTENT_DELAY_GROUND11 -> { }
                }
            } else if ("OTHER".equals(code)) {
                other.add(AssuredAdditionalOtherGround.OTHER);
                description = otherDescription(entry, errors);
                reasons.otherGround(reason(entry, true, errors));
            } else {
                unknown(entry, TenancyLicenceType.ASSURED_TENANCY, errors);
            }
        }

        AssuredNoArrearsPossessionGrounds possessionGrounds = AssuredNoArrearsPossessionGrounds.builder()
            .mandatoryGrounds(mandatory)
            .discretionaryGrounds(discretionary)
            .otherGround(other)
            .otherGroundDescription(description)
            .build();
        possessionGrounds.setShowGroundReasonPage(
            groundsRules.showReasonsForAssuredNoArrearsGrounds(possessionGrounds));
        target.setNoRentArrearsGroundsOptions(possessionGrounds);
        target.setNoRentArrearsGroundsReasons(reasons.build());
        target.setShowReasonsForGroundsPage(possessionGrounds.getShowGroundReasonPage());
        if (!groundsRules.hasAtLeastOneAssuredNoArrearsGround(possessionGrounds)) {
            errors.add(path, BulkErrorCollector.REQUIRED, GroundsRules.AT_LEAST_ONE_GROUND_ERROR);
        }
    }

    // ---- secure or flexible ----

    private void secureOrFlexible(List<Entry> entries,
                                  GroundsType grounds,
                                  PCSCase target,
                                  String path,
                                  BulkErrorCollector errors) {
        Set<SecureOrFlexibleDiscretionaryGrounds> discretionary =
            EnumSet.noneOf(SecureOrFlexibleDiscretionaryGrounds.class);
        Set<SecureOrFlexibleMandatoryGrounds> mandatory = EnumSet.noneOf(SecureOrFlexibleMandatoryGrounds.class);
        Set<SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm> discretionaryAlt =
            EnumSet.noneOf(SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.class);
        Set<SecureOrFlexibleMandatoryGroundsAlternativeAccomm> mandatoryAlt =
            EnumSet.noneOf(SecureOrFlexibleMandatoryGroundsAlternativeAccomm.class);
        Set<SecureAntisocialAdditionalGrounds> antisocial = EnumSet.noneOf(SecureAntisocialAdditionalGrounds.class);

        Set<RentArrearsOrBreachOfTenancy> rentOrBreach = EnumSet.noneOf(RentArrearsOrBreachOfTenancy.class);
        for (RentArrearsOrBreachType type : grounds.getRentArrearsOrBreach()) {
            rentOrBreach.add(RentArrearsOrBreachOfTenancy.valueOf(type.name()));
        }
        boolean breach = rentOrBreach.contains(RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY);

        SecureOrFlexibleGroundsReasons.SecureOrFlexibleGroundsReasonsBuilder reasons =
            SecureOrFlexibleGroundsReasons.builder();
        TenancyLicenceType tenancy = TenancyLicenceType.SECURE_TENANCY;

        for (Entry entry : entries) {
            String code = entry.code();
            SecureOrFlexibleDiscretionaryGrounds discretionaryGround =
                parse(SecureOrFlexibleDiscretionaryGrounds.class, code);
            SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm discretionaryAltGround =
                parse(SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.class, code);
            SecureOrFlexibleMandatoryGroundsAlternativeAccomm mandatoryAltGround =
                parse(SecureOrFlexibleMandatoryGroundsAlternativeAccomm.class, code);
            SecureAntisocialAdditionalGrounds antisocialGround = parse(SecureAntisocialAdditionalGrounds.class, code);

            if (discretionaryGround != null) {
                discretionary.add(discretionaryGround);
                boolean needsReason = discretionaryGround != SecureOrFlexibleDiscretionaryGrounds
                    .RENT_ARREARS_OR_BREACH_OF_TENANCY || breach;
                String text = reason(entry, needsReason, errors);
                switch (discretionaryGround) {
                    case RENT_ARREARS_OR_BREACH_OF_TENANCY -> reasons.breachOfTenancyGround(text);
                    case NUISANCE_OR_IMMORAL_USE -> reasons.nuisanceOrImmoralUseGround(text);
                    case DOMESTIC_VIOLENCE -> reasons.domesticViolenceGround(text);
                    case RIOT_OFFENCE -> reasons.riotOffenceGround(text);
                    case PROPERTY_DETERIORATION -> reasons.propertyDeteriorationGround(text);
                    case FURNITURE_DETERIORATION -> reasons.furnitureDeteriorationGround(text);
                    case TENANCY_OBTAINED_BY_FALSE_STATEMENT -> reasons.tenancyByFalseStatementGround(text);
                    case PREMIUM_PAID_MUTUAL_EXCHANGE -> reasons.premiumMutualExchangeGround(text);
                    case UNREASONABLE_CONDUCT_TIED_ACCOMMODATION -> reasons.unreasonableConductGround(text);
                    case REFUSAL_TO_MOVE_BACK -> reasons.refusalToMoveBackGround(text);
                }
            } else if (discretionaryAltGround != null) {
                discretionaryAlt.add(discretionaryAltGround);
                String text = reason(entry, true, errors);
                switch (discretionaryAltGround) {
                    case TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE -> reasons.tiedAccommodationGround(text);
                    case ADAPTED_ACCOMMODATION -> reasons.adaptedAccommodationGround(text);
                    case HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES -> reasons.housingAssocSpecialGround(text);
                    case SPECIAL_NEEDS_ACCOMMODATION -> reasons.specialNeedsAccommodationGround(text);
                    case UNDER_OCCUPYING_AFTER_SUCCESSION -> reasons.underOccupancySuccessionGround(text);
                }
            } else if (mandatoryAltGround != null) {
                mandatoryAlt.add(mandatoryAltGround);
                String text = reason(entry, true, errors);
                switch (mandatoryAltGround) {
                    case OVERCROWDING -> reasons.overcrowdingGround(text);
                    case LANDLORD_WORKS -> reasons.landlordWorksGround(text);
                    case PROPERTY_SOLD -> reasons.propertySoldGround(text);
                    case CHARITABLE_LANDLORD -> reasons.charitableLandlordGround(text);
                }
            } else if (antisocialGround != null) {
                antisocial.add(antisocialGround);
                mandatory.add(SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL);
                String text = reason(entry, true, errors);
                switch (antisocialGround) {
                    case S84A_CONDITION_1 -> reasons.antiSocialCondition1OfS84AGround(text);
                    case S84A_CONDITION_2 -> reasons.antiSocialCondition2OfS84AGround(text);
                    case S84A_CONDITION_3 -> reasons.antiSocialCondition3OfS84AGround(text);
                    case S84A_CONDITION_4 -> reasons.antiSocialCondition4OfS84AGround(text);
                    case S84A_CONDITION_5 -> reasons.antiSocialCondition5OfS84AGround(text);
                }
            } else {
                unknown(entry, tenancy, errors);
            }
        }

        boolean rentGround = discretionary
            .contains(SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY);
        if (rentGround && rentOrBreach.isEmpty()) {
            errors.add(path + "/RentArrearsOrBreach", BulkErrorCollector.REQUIRED,
                       "RentArrearsOrBreach is required when ground RENT_ARREARS_OR_BREACH_OF_TENANCY is claimed");
        } else if (!rentGround && !rentOrBreach.isEmpty()) {
            errors.add(path + "/RentArrearsOrBreach", BulkErrorCollector.NOT_ALLOWED,
                       "RentArrearsOrBreach requires ground RENT_ARREARS_OR_BREACH_OF_TENANCY");
        }

        SecureOrFlexiblePossessionGrounds possessionGrounds = SecureOrFlexiblePossessionGrounds.builder()
            .secureOrFlexibleDiscretionaryGrounds(discretionary)
            .secureOrFlexibleMandatoryGrounds(mandatory)
            .secureOrFlexibleDiscretionaryGroundsAlt(discretionaryAlt)
            .secureOrFlexibleMandatoryGroundsAlt(mandatoryAlt)
            .secureAntisocialAdditionalGrounds(antisocial)
            .build();
        target.setSecureOrFlexiblePossessionGrounds(possessionGrounds);
        target.setRentArrearsOrBreachOfTenancy(rentOrBreach);
        target.setShowBreachOfTenancyTextarea(groundsRules.showBreachOfTenancyTextarea(rentOrBreach));
        target.setShowReasonsForGroundsPage(groundsRules.showReasonsForSecureOrFlexibleGrounds(possessionGrounds));
        target.setSecureOrFlexibleGroundsReasons(reasons.build());
        errors.addAll(path, BulkErrorCollector.REQUIRED,
                      groundsRules.validateSecureOrFlexibleGrounds(possessionGrounds));
    }

    // ---- introductory, demoted or other ----

    private void introductoryDemotedOrOther(List<Entry> entries, PCSCase target, BulkErrorCollector errors) {
        Set<IntroductoryDemotedOrOtherGrounds> selected = EnumSet.noneOf(IntroductoryDemotedOrOtherGrounds.class);
        IntroductoryDemotedOtherGroundReason.IntroductoryDemotedOtherGroundReasonBuilder reasons =
            IntroductoryDemotedOtherGroundReason.builder();
        String description = null;
        boolean noGrounds = false;

        for (Entry entry : entries) {
            String code = entry.code();
            IntroductoryDemotedOrOtherGrounds ground = parse(IntroductoryDemotedOrOtherGrounds.class, code);
            if (NO_GROUNDS.equals(code)) {
                noGrounds = true;
                reasons.noGrounds(reason(entry, true, errors));
            } else if (ground != null) {
                selected.add(ground);
                String text = reason(entry, ground != IntroductoryDemotedOrOtherGrounds.RENT_ARREARS, errors);
                switch (ground) {
                    case ANTI_SOCIAL -> reasons.antiSocialBehaviourGround(text);
                    case BREACH_OF_THE_TENANCY -> reasons.breachOfTheTenancyGround(text);
                    case ABSOLUTE_GROUNDS -> reasons.absoluteGrounds(text);
                    case OTHER -> {
                        description = otherDescription(entry, errors);
                        reasons.otherGround(text);
                    }
                    case RENT_ARREARS -> { }
                }
            } else {
                unknown(entry, TenancyLicenceType.INTRODUCTORY_TENANCY, errors);
            }
        }

        if (noGrounds && !selected.isEmpty()) {
            errors.add("ClaimParticulars/Grounds", BulkErrorCollector.INVALID_VALUE,
                       NO_GROUNDS + " cannot be combined with other grounds");
        } else if (!noGrounds && selected.isEmpty() && !entries.isEmpty()) {
            errors.add("ClaimParticulars/Grounds", BulkErrorCollector.REQUIRED, "Please select at least one ground");
        }

        IntroductoryDemotedOtherGroundsForPossession possessionGrounds =
            IntroductoryDemotedOtherGroundsForPossession.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.from(!noGrounds))
                .introductoryDemotedOrOtherGrounds(noGrounds ? null : selected)
                .otherGroundDescription(description)
                .build();
        target.setIntroductoryDemotedOrOtherGroundsForPossession(possessionGrounds);
        target.setIntroductoryDemotedOtherGroundReason(reasons.build());
        target.setShowIntroductoryDemotedOtherGroundReasonPage(
            groundsRules.showReasonsForIntroductoryDemotedOrOtherGrounds(possessionGrounds));
    }

}
