package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredAdditionalDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredAdditionalMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredRentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsReasonsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.AssuredDiscretionaryGround.PERSISTENT_DELAY_GROUND11;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AssuredDiscretionaryGround.RENT_ARREARS_GROUND10;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.RENT_ARREARS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherNoGrounds.NO_GROUNDS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY;

@Service
public class ClaimGroundService {

    public List<ClaimGroundEntity> createClaimGroundEntities(PCSCase pcsCase) {
        // Check if Wales first - Wales uses OccupationLicenceTypeWales, not TenancyLicenceType
        if (LegislativeCountry.WALES.equals(pcsCase.getLegislativeCountry())) {
            return getWalesGroundsWithReason(pcsCase);
        }

        TenancyLicenceDetails tenancyDetails = pcsCase.getTenancyLicenceDetails();
        TenancyLicenceType tenancyLicenceType = tenancyDetails != null
            ? tenancyDetails.getTypeOfTenancyLicence() : null;

        if (tenancyLicenceType == null) {
            return Collections.emptyList();
        }

        return switch (tenancyLicenceType) {
            case ASSURED_TENANCY -> getAssuredTenancyGroundsWithReason(pcsCase);
            case INTRODUCTORY_TENANCY, DEMOTED_TENANCY, OTHER ->
                getIntroductoryDemotedOtherTenancyGroundsWithReason(pcsCase);
            case SECURE_TENANCY, FLEXIBLE_TENANCY ->
                getSecureFlexibleTenancyGroundsWithReason(pcsCase);
        };
    }

    private List<ClaimGroundEntity> getAssuredTenancyGroundsWithReason(PCSCase pcsCase) {

        if (pcsCase.getClaimDueToRentArrears() == YesOrNo.YES) {
            return assuredTenancyRentArrearsGroundsWithReason(pcsCase);
        } else {
            return assuredTenancyNoRentArrearsGroundsWithReason(pcsCase);
        }
    }

    private List<ClaimGroundEntity> assuredTenancyRentArrearsGroundsWithReason(PCSCase pcsCase) {

        AssuredRentArrearsPossessionGrounds groundsForPossession = pcsCase.getAssuredRentArrearsPossessionGrounds();

        Set<AssuredRentArrearsGround> rentArrearsGrounds = groundsForPossession.getRentArrearsGrounds();

        Set<AssuredAdditionalMandatoryGrounds> additionalMandatoryGrounds
            = groundsForPossession.getAdditionalMandatoryGrounds();
        Set<AssuredAdditionalDiscretionaryGrounds> additionalDiscretionaryGrounds
            = groundsForPossession.getAdditionalDiscretionaryGrounds();

        Set<AssuredMandatoryGround> combinedMandatoryGrounds = new HashSet<>();
        Set<AssuredDiscretionaryGround> combinedDiscretionaryGrounds = new HashSet<>();

        if (additionalMandatoryGrounds != null) {
            additionalMandatoryGrounds.forEach(
                additionalMandatoryGround -> combinedMandatoryGrounds.add(
                    AssuredMandatoryGround.valueOf(additionalMandatoryGround.name())
                )
            );
        }

        if (additionalDiscretionaryGrounds != null) {
            additionalDiscretionaryGrounds.forEach(
                additionalDiscretionaryGround -> combinedDiscretionaryGrounds.add(
                    AssuredDiscretionaryGround.valueOf(additionalDiscretionaryGround.name())
                )
            );
        }

        RentArrearsGroundsReasons reasons = pcsCase.getRentArrearsGroundsReasons();

        List<ClaimGroundEntity> claimGroundEntities = new ArrayList<>();

        for (AssuredRentArrearsGround rentArrearsGround : rentArrearsGrounds) {
            switch (rentArrearsGround) {
                case SERIOUS_RENT_ARREARS_GROUND8:
                    // Ground 8 is mandatory
                    combinedMandatoryGrounds.add(SERIOUS_RENT_ARREARS_GROUND8);
                    break;
                case RENT_ARREARS_GROUND10:
                    // Ground 10 is discretionary
                    combinedDiscretionaryGrounds.add(RENT_ARREARS_GROUND10);
                    break;
                case PERSISTENT_DELAY_GROUND11:
                    // Ground 11 is discretionary
                    combinedDiscretionaryGrounds.add(AssuredDiscretionaryGround.PERSISTENT_DELAY_GROUND11);
                    break;
            }
        }

        for (AssuredMandatoryGround ground : combinedMandatoryGrounds) {
            String reasonText = switch (ground) {
                case OWNER_OCCUPIER_GROUND1 -> reasons.getOwnerOccupierReason();
                case REPOSSESSION_GROUND2 -> reasons.getRepossessionByLenderReason();
                case HOLIDAY_LET_GROUND3 -> reasons.getHolidayLetReason();
                case STUDENT_LET_GROUND4 -> reasons.getStudentLetReason();
                case MINISTER_RELIGION_GROUND5 -> reasons.getMinisterOfReligionReason();
                case REDEVELOPMENT_GROUND6 -> reasons.getRedevelopmentReason();
                case DEATH_OF_TENANT_GROUND7 -> reasons.getDeathOfTenantReason();
                case ANTISOCIAL_BEHAVIOUR_GROUND7A -> reasons.getAntisocialBehaviourReason();
                case NO_RIGHT_TO_RENT_GROUND7B -> reasons.getNoRightToRentReason();
                case SERIOUS_RENT_ARREARS_GROUND8 -> null;
            };

            boolean isRentArrearsGround = (ground == SERIOUS_RENT_ARREARS_GROUND8);

            claimGroundEntities.add(ClaimGroundEntity.builder()
                                        .category(ClaimGroundCategory.ASSURED_MANDATORY)
                                        .code(ground.name())
                                        .reason(reasonText)
                                        .isRentArrears(isRentArrearsGround)
                                        .build());
        }

        for (AssuredDiscretionaryGround ground : combinedDiscretionaryGrounds) {
            String reasonText = switch (ground) {
                case ALTERNATIVE_ACCOMMODATION_GROUND9 -> reasons.getSuitableAltAccommodationReason();
                case RENT_ARREARS_GROUND10, PERSISTENT_DELAY_GROUND11 -> null;
                case BREACH_TENANCY_GROUND12 -> reasons.getBreachOfTenancyConditionsReason();
                case DETERIORATION_PROPERTY_GROUND13 -> reasons.getPropertyDeteriorationReason();
                case NUISANCE_ANNOYANCE_GROUND14 -> reasons.getNuisanceAnnoyanceReason();
                case DOMESTIC_VIOLENCE_GROUND14A -> reasons.getDomesticViolenceReason();
                case OFFENCE_RIOT_GROUND14ZA -> reasons.getOffenceDuringRiotReason();
                case DETERIORATION_FURNITURE_GROUND15 -> reasons.getFurnitureDeteriorationReason();
                case EMPLOYEE_LANDLORD_GROUND16 -> reasons.getEmployeeOfLandlordReason();
                case FALSE_STATEMENT_GROUND17 -> reasons.getTenancyByFalseStatementReason();
            };

            boolean isRentArrearsGround = (ground == RENT_ARREARS_GROUND10 || ground == PERSISTENT_DELAY_GROUND11);

            claimGroundEntities.add(ClaimGroundEntity.builder()
                                        .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                                        .code(ground.name())
                                        .reason(reasonText)
                                        .isRentArrears(isRentArrearsGround)
                                        .build());
        }

        return claimGroundEntities;
    }

    private List<ClaimGroundEntity> assuredTenancyNoRentArrearsGroundsWithReason(PCSCase pcsCase) {

        Set<AssuredMandatoryGround> mandatoryGrounds = pcsCase.getNoRentArrearsGroundsOptions().getMandatoryGrounds();
        Set<AssuredDiscretionaryGround> discretionaryGrounds = pcsCase
                .getNoRentArrearsGroundsOptions().getDiscretionaryGrounds();
        NoRentArrearsReasonForGrounds reasons = pcsCase.getNoRentArrearsReasonForGrounds();

        List<ClaimGroundEntity> entities = new ArrayList<>();

        if (mandatoryGrounds != null) {
            for (AssuredMandatoryGround ground : mandatoryGrounds) {
                String reasonText = switch (ground) {
                    case OWNER_OCCUPIER_GROUND1 -> reasons.getOwnerOccupier();
                    case REPOSSESSION_GROUND2 -> reasons.getRepossessionByLender();
                    case HOLIDAY_LET_GROUND3 -> reasons.getHolidayLet();
                    case STUDENT_LET_GROUND4 -> reasons.getStudentLet();
                    case MINISTER_RELIGION_GROUND5 -> reasons.getMinisterOfReligion();
                    case REDEVELOPMENT_GROUND6 -> reasons.getRedevelopment();
                    case DEATH_OF_TENANT_GROUND7 -> reasons.getDeathOfTenant();
                    case ANTISOCIAL_BEHAVIOUR_GROUND7A -> reasons.getAntisocialBehaviour();
                    case NO_RIGHT_TO_RENT_GROUND7B -> reasons.getNoRightToRent();
                    case SERIOUS_RENT_ARREARS_GROUND8 -> null;
                };

                boolean isRentArrearsGround = (ground == SERIOUS_RENT_ARREARS_GROUND8);

                entities.add(ClaimGroundEntity.builder()
                                 .category(ClaimGroundCategory.ASSURED_MANDATORY)
                                 .code(ground.name())
                                 .reason(reasonText)
                                 .isRentArrears(isRentArrearsGround)
                                 .build());
            }
        }

        if (discretionaryGrounds != null) {
            for (AssuredDiscretionaryGround ground : discretionaryGrounds) {
                String reasonText = switch (ground) {
                    case ALTERNATIVE_ACCOMMODATION_GROUND9 -> reasons.getSuitableAlternativeAccomodation();
                    case RENT_ARREARS_GROUND10, PERSISTENT_DELAY_GROUND11 -> null;
                    case BREACH_TENANCY_GROUND12 -> reasons.getBreachOfTenancyConditions();
                    case DETERIORATION_PROPERTY_GROUND13 -> reasons.getPropertyDeterioration();
                    case NUISANCE_ANNOYANCE_GROUND14 -> reasons.getNuisanceOrIllegalUse();
                    case DOMESTIC_VIOLENCE_GROUND14A -> reasons.getDomesticViolence();
                    case OFFENCE_RIOT_GROUND14ZA -> reasons.getOffenceDuringRiot();
                    case DETERIORATION_FURNITURE_GROUND15 -> reasons.getFurnitureDeterioration();
                    case EMPLOYEE_LANDLORD_GROUND16 -> reasons.getLandlordEmployee();
                    case FALSE_STATEMENT_GROUND17 -> reasons.getFalseStatement();
                };

                boolean isRentArrearsGround = (ground == RENT_ARREARS_GROUND10 || ground == PERSISTENT_DELAY_GROUND11);

                entities.add(ClaimGroundEntity.builder()
                                 .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                                 .code(ground.name())
                                 .reason(reasonText)
                                 .isRentArrears(isRentArrearsGround)
                                 .build());
            }
        }

        return entities;
    }

    private List<ClaimGroundEntity> getIntroductoryDemotedOtherTenancyGroundsWithReason(PCSCase pcsCase) {

        List<ClaimGroundEntity> entities = new ArrayList<>();

        VerticalYesNo hasGrounds = pcsCase.getIntroductoryDemotedOrOtherGroundsForPossession()
            .getHasIntroductoryDemotedOtherGroundsForPossession();

        if (hasGrounds == VerticalYesNo.YES) {
            Set<IntroductoryDemotedOrOtherGrounds> introductoryDemotedOrOtherGrounds =
                pcsCase.getIntroductoryDemotedOrOtherGroundsForPossession().getIntroductoryDemotedOrOtherGrounds();

            IntroductoryDemotedOtherGroundReason reasons = pcsCase.getIntroductoryDemotedOtherGroundReason();

            if (introductoryDemotedOrOtherGrounds != null) {
                for (IntroductoryDemotedOrOtherGrounds ground : introductoryDemotedOrOtherGrounds) {
                    String reasonText = switch (ground) {
                        case ABSOLUTE_GROUNDS -> reasons.getAbsoluteGrounds();
                        case ANTI_SOCIAL -> reasons.getAntiSocialBehaviourGround();
                        case BREACH_OF_THE_TENANCY -> reasons.getBreachOfTheTenancyGround();
                        case OTHER -> reasons.getOtherGround();
                        case RENT_ARREARS -> null;
                    };

                    String groundDescription = ground.equals(IntroductoryDemotedOrOtherGrounds.OTHER)
                        ? pcsCase.getIntroductoryDemotedOrOtherGroundsForPossession().getOtherGroundDescription()
                        : null;

                    boolean isRentArrearsGround = (ground == RENT_ARREARS);

                    entities.add(
                        ClaimGroundEntity.builder()
                            .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                            .code(ground.name())
                            .reason(reasonText)
                            .description(groundDescription)
                            .isRentArrears(isRentArrearsGround)
                            .build());
                }
            }
        } else {
            entities.add(
                ClaimGroundEntity.builder()
                    .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS)
                    .code(NO_GROUNDS.name())
                    .reason(pcsCase.getIntroductoryDemotedOtherGroundReason().getNoGrounds())
                    .isRentArrears(false)
                    .build());
        }

        return entities;
    }

    private List<ClaimGroundEntity> getSecureFlexibleTenancyGroundsWithReason(PCSCase pcsCase) {
        SecureOrFlexiblePossessionGrounds possessionGrounds = pcsCase.getSecureOrFlexiblePossessionGrounds();

        List<ClaimGroundEntity> claimGroundEntities = new ArrayList<>();

        SecureOrFlexibleGroundsReasons reasons = pcsCase.getSecureOrFlexibleGroundsReasons();

        Set<RentArrearsOrBreachOfTenancy> rentArrearsOrBreachOfTenancy
            = Objects.requireNonNullElse(pcsCase.getRentArrearsOrBreachOfTenancy(), Set.of());

        boolean breachOfTenancy = rentArrearsOrBreachOfTenancy.contains(BREACH_OF_TENANCY);

        possessionGrounds.getSecureOrFlexibleDiscretionaryGrounds().forEach(
            ground -> {
                String reasonText = switch (ground) {
                    case RENT_ARREARS_OR_BREACH_OF_TENANCY
                        -> breachOfTenancy ? reasons.getBreachOfTenancyGround() : null;
                    case NUISANCE_OR_IMMORAL_USE -> reasons.getNuisanceOrImmoralUseGround();
                    case DOMESTIC_VIOLENCE -> reasons.getDomesticViolenceGround();
                    case RIOT_OFFENCE -> reasons.getRiotOffenceGround();
                    case PROPERTY_DETERIORATION -> reasons.getPropertyDeteriorationGround();
                    case FURNITURE_DETERIORATION -> reasons.getFurnitureDeteriorationGround();
                    case TENANCY_OBTAINED_BY_FALSE_STATEMENT -> reasons.getTenancyByFalseStatementGround();
                    case PREMIUM_PAID_MUTUAL_EXCHANGE -> reasons.getPremiumMutualExchangeGround();
                    case UNREASONABLE_CONDUCT_TIED_ACCOMMODATION -> reasons.getUnreasonableConductGround();
                    case REFUSAL_TO_MOVE_BACK -> reasons.getRefusalToMoveBackGround();
                };

                boolean isRentArrearsGround = (ground == RENT_ARREARS_OR_BREACH_OF_TENANCY
                    && rentArrearsOrBreachOfTenancy.contains(RentArrearsOrBreachOfTenancy.RENT_ARREARS));

                claimGroundEntities.add(
                    ClaimGroundEntity.builder()
                        .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY)
                        .code(ground.name())
                        .reason(reasonText)
                        .isRentArrears(isRentArrearsGround)
                        .build());
            }
        );

        possessionGrounds.getSecureOrFlexibleMandatoryGrounds().forEach(
            mandatoryGround -> {
                String reasonText = switch (mandatoryGround) {
                    case ANTI_SOCIAL -> reasons.getAntiSocialGround();
                };

                claimGroundEntities.add(
                    ClaimGroundEntity.builder()
                        .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_MANDATORY)
                        .code(mandatoryGround.name())
                        .reason(reasonText)
                        .isRentArrears(false)
                        .build());
            }
        );

        possessionGrounds.getSecureOrFlexibleDiscretionaryGroundsAlt().forEach(
            discretionaryGroundAlt -> {
                String reasonText = switch (discretionaryGroundAlt) {
                    case TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE -> reasons.getTiedAccommodationGround();
                    case ADAPTED_ACCOMMODATION -> reasons.getAdaptedAccommodationGround();
                    case HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES -> reasons.getHousingAssocSpecialGround();
                    case SPECIAL_NEEDS_ACCOMMODATION -> reasons.getSpecialNeedsAccommodationGround();
                    case UNDER_OCCUPYING_AFTER_SUCCESSION -> reasons.getUnderOccupancySuccessionGround();
                };

                claimGroundEntities.add(
                    ClaimGroundEntity.builder()
                        .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT)
                        .code(discretionaryGroundAlt.name())
                        .reason(reasonText)
                        .isRentArrears(false)
                        .build());
            }
        );

        possessionGrounds.getSecureOrFlexibleMandatoryGroundsAlt().forEach(
            mandatoryGroundAlt -> {
                String reasonText = switch (mandatoryGroundAlt) {
                    case OVERCROWDING -> reasons.getOvercrowdingGround();
                    case LANDLORD_WORKS -> reasons.getLandlordWorksGround();
                    case PROPERTY_SOLD -> reasons.getPropertySoldGround();
                    case CHARITABLE_LANDLORD -> reasons.getCharitableLandlordGround();
                };

                claimGroundEntities.add(
                    ClaimGroundEntity.builder()
                        .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_MANDATORY_ALT)
                        .code(mandatoryGroundAlt.name())
                        .reason(reasonText)
                        .isRentArrears(false)
                        .build());
            }
        );

        return claimGroundEntities;
    }

    private List<ClaimGroundEntity> getWalesGroundsWithReason(PCSCase pcsCase) {

        SecureContractGroundsForPossessionWales secureGrounds =
            Optional.ofNullable(pcsCase.getSecureContractGroundsForPossessionWales())
                .orElse(SecureContractGroundsForPossessionWales.builder()
                            .discretionaryGroundsWales(Set.of())
                            .mandatoryGroundsWales(Set.of())
                            .estateManagementGroundsWales(Set.of())
                            .build());

        GroundsForPossessionWales groundsForPossessionWales =
            Optional.ofNullable(pcsCase.getGroundsForPossessionWales())
                .orElse(GroundsForPossessionWales.builder().build());

        Set<MandatoryGroundWales> mandatoryGrounds = groundsForPossessionWales.getMandatoryGroundsWales();
        Set<DiscretionaryGroundWales> discretionaryGrounds = groundsForPossessionWales.getDiscretionaryGroundsWales();
        Set<EstateManagementGroundsWales> estateGrounds = groundsForPossessionWales.getEstateManagementGroundsWales();
        Set<SecureContractMandatoryGroundsWales> secureMandatoryGrounds =
            secureGrounds.getMandatoryGroundsWales();
        Set<SecureContractDiscretionaryGroundsWales> secureDiscretionaryGrounds =
            secureGrounds.getDiscretionaryGroundsWales();
        Set<EstateManagementGroundsWales> secureEstateGrounds =
            secureGrounds.getEstateManagementGroundsWales();
        GroundsReasonsWales reasons = pcsCase.getGroundsReasonsWales();

        List<ClaimGroundEntity> entities = new ArrayList<>();

        if (mandatoryGrounds == null && discretionaryGrounds == null && estateGrounds == null
            && secureMandatoryGrounds == null && secureDiscretionaryGrounds == null
            && secureEstateGrounds == null) {
            return entities;
        }

        // Standard/Other Contract - Mandatory grounds
        if (mandatoryGrounds != null) {
            for (MandatoryGroundWales ground : mandatoryGrounds) {
                String reasonText = reasons != null ? switch (ground) {
                    case FAIL_TO_GIVE_UP_S170 -> reasons.getFailToGiveUpS170Reason();
                    case LANDLORD_NOTICE_PERIODIC_S178 -> reasons.getLandlordNoticePeriodicS178Reason();
                    case SERIOUS_ARREARS_PERIODIC_S181 -> reasons.getSeriousArrearsPeriodicS181Reason();
                    case LANDLORD_NOTICE_FT_END_S186 -> reasons.getLandlordNoticeFtEndS186Reason();
                    case SERIOUS_ARREARS_FIXED_TERM_S187 -> reasons.getSeriousArrearsFixedTermS187Reason();
                    case FAIL_TO_GIVE_UP_BREAK_NOTICE_S191 -> reasons.getFailToGiveUpBreakNoticeS191Reason();
                    case LANDLORD_BREAK_CLAUSE_S199 -> reasons.getLandlordBreakClauseS199Reason();
                    case CONVERTED_FIXED_TERM_SCH12_25B2 -> reasons.getConvertedFixedTermSch1225B2Reason();
                } : null;

                entities.add(ClaimGroundEntity.builder()
                    .code(ground.name())
                    .reason(reasonText)
                    .build());
            }
        }

        // Standard/Other Contract - Discretionary grounds
        if (discretionaryGrounds != null) {
            for (DiscretionaryGroundWales ground : discretionaryGrounds) {
                String reasonText = reasons != null ? switch (ground) {
                    case RENT_ARREARS_SECTION_157 -> null;
                    case ANTISOCIAL_BEHAVIOUR_SECTION_157 -> null;
                    case OTHER_BREACH_SECTION_157 -> reasons.getOtherBreachSection157Reason();
                    case ESTATE_MANAGEMENT_GROUNDS_SECTION_160 -> null;
                } : null;

                entities.add(ClaimGroundEntity.builder()
                    .code(ground.name())
                    .reason(reasonText)
                    .build());
            }
        }

        // Standard/Other Contract - Estate Management grounds
        if (estateGrounds != null) {
            for (EstateManagementGroundsWales ground : estateGrounds) {
                String reasonText = reasons != null ? switch (ground) {
                    case BUILDING_WORKS -> reasons.getBuildingWorksReason();
                    case REDEVELOPMENT_SCHEMES -> reasons.getRedevelopmentSchemesReason();
                    case CHARITIES -> reasons.getCharitiesReason();
                    case DISABLED_SUITABLE_DWELLING -> reasons.getDisabledSuitableDwellingReason();
                    case HOUSING_ASSOCIATIONS_AND_TRUSTS -> reasons.getHousingAssociationsAndTrustsReason();
                    case SPECIAL_NEEDS_DWELLINGS -> reasons.getSpecialNeedsDwellingsReason();
                    case RESERVE_SUCCESSORS -> reasons.getReserveSuccessorsReason();
                    case JOINT_CONTRACT_HOLDERS -> reasons.getJointContractHoldersReason();
                    case OTHER_ESTATE_MANAGEMENT_REASONS -> reasons.getOtherEstateManagementReasonsReason();
                } : null;

                entities.add(ClaimGroundEntity.builder()
                    .code(ground.name())
                    .reason(reasonText)
                    .build());
            }
        }

        // Secure Contract - Mandatory grounds
        if (secureMandatoryGrounds != null) {
            for (SecureContractMandatoryGroundsWales ground : secureMandatoryGrounds) {
                String reasonText = reasons != null ? switch (ground) {
                    case FAILURE_TO_GIVE_UP_POSSESSION_SECTION_170 ->
                        reasons.getSecureFailureToGiveUpPossessionSection170Reason();
                    case LANDLORD_NOTICE_SECTION_186 ->
                        reasons.getSecureLandlordNoticeSection186Reason();
                    case FAILURE_TO_GIVE_UP_POSSESSION_SECTION_191 ->
                        reasons.getSecureFailureToGiveUpPossessionSection191Reason();
                    case LANDLORD_NOTICE_SECTION_199 ->
                        reasons.getSecureLandlordNoticeSection199Reason();
                } : null;

                entities.add(ClaimGroundEntity.builder()
                    .code(ground.name())
                    .reason(reasonText)
                    .build());
            }
        }

        // Secure Contract - Discretionary grounds
        if (secureDiscretionaryGrounds != null) {
            for (SecureContractDiscretionaryGroundsWales ground : secureDiscretionaryGrounds) {
                String reasonText = reasons != null ? switch (ground) {
                    case RENT_ARREARS -> null;
                    case ANTISOCIAL_BEHAVIOUR -> null;
                    case OTHER_BREACH_OF_CONTRACT -> reasons.getSecureOtherBreachOfContractReason();
                    case ESTATE_MANAGEMENT_GROUNDS -> null;
                } : null;

                entities.add(ClaimGroundEntity.builder()
                    .code(ground.name())
                    .reason(reasonText)
                    .build());
            }
        }

        // Secure Contract - Estate Management grounds
        if (secureEstateGrounds != null) {
            for (EstateManagementGroundsWales ground : secureEstateGrounds) {
                String reasonText = reasons != null ? switch (ground) {
                    case BUILDING_WORKS -> reasons.getSecureBuildingWorksReason();
                    case REDEVELOPMENT_SCHEMES -> reasons.getSecureRedevelopmentSchemesReason();
                    case CHARITIES -> reasons.getSecureCharitiesReason();
                    case DISABLED_SUITABLE_DWELLING -> reasons.getSecureDisabledSuitableDwellingReason();
                    case HOUSING_ASSOCIATIONS_AND_TRUSTS -> reasons.getSecureHousingAssociationsAndTrustsReason();
                    case SPECIAL_NEEDS_DWELLINGS -> reasons.getSecureSpecialNeedsDwellingsReason();
                    case RESERVE_SUCCESSORS -> reasons.getSecureReserveSuccessorsReason();
                    case JOINT_CONTRACT_HOLDERS -> reasons.getSecureJointContractHoldersReason();
                    case OTHER_ESTATE_MANAGEMENT_REASONS -> reasons.getSecureOtherEstateManagementReasonsReason();
                } : null;

                entities.add(ClaimGroundEntity.builder()
                    .code(ground.name())
                    .reason(reasonText)
                    .build());
            }
        }

        return entities;
    }

}
