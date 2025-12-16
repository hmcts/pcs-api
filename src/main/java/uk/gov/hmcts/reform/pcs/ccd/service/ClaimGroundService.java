package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import static feign.Util.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherNoGrounds.NO_GROUNDS;

@Service
public class ClaimGroundService {

    public List<ClaimGroundEntity> getGroundsWithReason(PCSCase pcsCase) {
        // Check if Wales first - Wales uses OccupationLicenceTypeWales, not TenancyLicenceType
        if (LegislativeCountry.WALES.equals(pcsCase.getLegislativeCountry())) {
            return getWalesGroundsWithReason(pcsCase);
        }

        TenancyLicenceDetails tenancyDetails =
            pcsCase.getTenancyLicenceDetails();
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

        Set<RentArrearsGround> rentArrearsGrounds = pcsCase.getRentArrearsGrounds();
        Set<RentArrearsMandatoryGrounds> rentArrearsMandatoryGrounds = pcsCase
            .getRentArrearsMandatoryGrounds();
        Set<RentArrearsDiscretionaryGrounds> rentArrearsDiscretionaryGrounds = pcsCase
            .getRentArrearsDiscretionaryGrounds();
        RentArrearsGroundsReasons grounds = pcsCase.getRentArrearsGroundsReasons();

        List<ClaimGroundEntity> entities = new ArrayList<>();

        if (rentArrearsMandatoryGrounds == null && rentArrearsDiscretionaryGrounds == null) {
            rentArrearsGrounds.forEach(rentArrearsGround -> {
                entities.add(ClaimGroundEntity.builder()
                                 .groundId(rentArrearsGround.name())
                                 .build());
            });
            return entities;
        }

        if (rentArrearsMandatoryGrounds != null) {
            for (RentArrearsMandatoryGrounds ground : rentArrearsMandatoryGrounds) {
                String reasonText = switch (ground) {
                    case OWNER_OCCUPIER_GROUND1 -> grounds.getOwnerOccupierReason();
                    case REPOSSESSION_GROUND2 -> grounds.getRepossessionByLenderReason();
                    case HOLIDAY_LET_GROUND3 -> grounds.getHolidayLetReason();
                    case STUDENT_LET_GROUND4 -> grounds.getStudentLetReason();
                    case MINISTER_RELIGION_GROUND5 -> grounds.getMinisterOfReligionReason();
                    case REDEVELOPMENT_GROUND6 -> grounds.getRedevelopmentReason();
                    case DEATH_OF_TENANT_GROUND7 -> grounds.getDeathOfTenantReason();
                    case ANTISOCIAL_BEHAVIOUR_GROUND7A -> grounds.getAntisocialBehaviourReason();
                    case NO_RIGHT_TO_RENT_GROUND7B -> grounds.getNoRightToRentReason();
                    case SERIOUS_RENT_ARREARS_GROUND8 -> null;
                };

                entities.add(ClaimGroundEntity.builder()
                                 .groundId(ground.name())
                                 .groundReason(reasonText)
                                 .build());
            }
        }

        if (rentArrearsDiscretionaryGrounds != null) {
            for (RentArrearsDiscretionaryGrounds ground : rentArrearsDiscretionaryGrounds) {
                String reasonText = switch (ground) {
                    case ALTERNATIVE_ACCOMMODATION_GROUND9 -> grounds.getSuitableAltAccommodationReason();
                    case RENT_ARREARS_GROUND10, PERSISTENT_DELAY_GROUND11 -> null;
                    case BREACH_TENANCY_GROUND12 -> grounds.getBreachOfTenancyConditionsReason();
                    case DETERIORATION_PROPERTY_GROUND13 -> grounds.getPropertyDeteriorationReason();
                    case NUISANCE_ANNOYANCE_GROUND14 -> grounds.getNuisanceAnnoyanceReason();
                    case DOMESTIC_VIOLENCE_GROUND14A -> grounds.getDomesticViolenceReason();
                    case OFFENCE_RIOT_GROUND14ZA -> grounds.getOffenceDuringRiotReason();
                    case DETERIORATION_FURNITURE_GROUND15 -> grounds.getFurnitureDeteriorationReason();
                    case EMPLOYEE_LANDLORD_GROUND16 -> grounds.getEmployeeOfLandlordReason();
                    case FALSE_STATEMENT_GROUND17 ->  grounds.getTenancyByFalseStatementReason();
                };

                entities.add(ClaimGroundEntity.builder()
                                 .groundId(ground.name())
                                 .groundReason(reasonText)
                                 .build());
            }
        }
        return entities;
    }

    private List<ClaimGroundEntity> assuredTenancyNoRentArrearsGroundsWithReason(PCSCase pcsCase) {

        Set<NoRentArrearsMandatoryGrounds> noRentArrearsMandatoryGrounds = pcsCase
                .getNoRentArrearsMandatoryGroundsOptions();
        Set<NoRentArrearsDiscretionaryGrounds> noRentArrearsDiscretionaryGrounds = pcsCase
                .getNoRentArrearsDiscretionaryGroundsOptions();
        NoRentArrearsReasonForGrounds grounds = pcsCase.getNoRentArrearsReasonForGrounds();

        List<ClaimGroundEntity> entities = new ArrayList<>();

        if (noRentArrearsMandatoryGrounds != null) {
            for (NoRentArrearsMandatoryGrounds ground : noRentArrearsMandatoryGrounds) {
                String reasonText = switch (ground) {
                    case OWNER_OCCUPIER -> grounds.getOwnerOccupierTextArea();
                    case REPOSSESSION_BY_LENDER -> grounds.getRepossessionByLenderTextArea();
                    case HOLIDAY_LET -> grounds.getHolidayLetTextArea();
                    case STUDENT_LET -> grounds.getStudentLetTextArea();
                    case MINISTER_OF_RELIGION -> grounds.getMinisterOfReligionTextArea();
                    case REDEVELOPMENT -> grounds.getRedevelopmentTextArea();
                    case DEATH_OF_TENANT -> grounds.getDeathOfTenantTextArea();
                    case ANTISOCIAL_BEHAVIOUR -> grounds.getAntisocialBehaviourTextArea();
                    case NO_RIGHT_TO_RENT -> grounds.getNoRightToRentTextArea();
                    case SERIOUS_RENT_ARREARS -> null;
                };

                entities.add(ClaimGroundEntity.builder()
                                 .groundId(ground.name())
                                 .groundReason(reasonText)
                                 .build());
            }
        }

        if (noRentArrearsDiscretionaryGrounds != null) {
            for (NoRentArrearsDiscretionaryGrounds ground : noRentArrearsDiscretionaryGrounds) {
                String reasonText = switch (ground) {
                    case SUITABLE_ACCOM -> grounds.getSuitableAccomTextArea();
                    case RENT_ARREARS, RENT_PAYMENT_DELAY -> null;
                    case BREACH_OF_TENANCY_CONDITIONS -> grounds.getBreachOfTenancyConditionsTextArea();
                    case PROPERTY_DETERIORATION -> grounds.getPropertyDeteriorationTextArea();
                    case NUISANCE_OR_ILLEGAL_USE -> grounds.getNuisanceOrIllegalUseTextArea();
                    case DOMESTIC_VIOLENCE -> grounds.getDomesticViolenceTextArea();
                    case OFFENCE_DURING_RIOT -> grounds.getOffenceDuringRiotTextArea();
                    case FURNITURE_DETERIORATION -> grounds.getFurnitureDeteriorationTextArea();
                    case LANDLORD_EMPLOYEE -> grounds.getLandlordEmployeeTextArea();
                    case FALSE_STATEMENT -> grounds.getFalseStatementTextArea();
                };

                entities.add(ClaimGroundEntity.builder()
                                 .groundId(ground.name())
                                 .groundReason(reasonText)
                                 .build());
            }
        }

        return entities;
    }

    private List<ClaimGroundEntity> getIntroductoryDemotedOtherTenancyGroundsWithReason(
        PCSCase pcsCase) {
        Set<IntroductoryDemotedOrOtherGrounds> introductoryDemotedOrOtherGrounds =
            pcsCase.getIntroductoryDemotedOrOtherGroundsForPossession().getIntroductoryDemotedOrOtherGrounds();

        IntroductoryDemotedOtherGroundReason reasons = pcsCase.getIntroductoryDemotedOtherGroundReason();

        List<ClaimGroundEntity> entities = new ArrayList<>();
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
                    ? pcsCase.getIntroductoryDemotedOrOtherGroundsForPossession().getOtherGroundDescription() : null;

                entities.add(
                    ClaimGroundEntity.builder()
                        .groundId(ground.name())
                        .groundReason(reasonText)
                        .groundDescription(groundDescription)
                        .build());
            }
        }
        if (pcsCase.getIntroductoryDemotedOrOtherGroundsForPossession()
            .getHasIntroductoryDemotedOtherGroundsForPossession() == VerticalYesNo.NO
            && isNotBlank(reasons.getNoGrounds())) {

            entities.add(
                ClaimGroundEntity.builder()
                    .groundId(NO_GROUNDS.name())
                    .groundReason(pcsCase.getIntroductoryDemotedOtherGroundReason().getNoGrounds())
                    .build());
        }

        return entities;
    }

    //TODO - Integrate Secure/Flexible grounds, currently being saved as JSONB
    private List<ClaimGroundEntity> getSecureFlexibleTenancyGroundsWithReason(PCSCase pcsCase) {
        return List.of();
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
        GroundsReasonsWales grounds = pcsCase.getGroundsReasonsWales();

        List<ClaimGroundEntity> entities = new ArrayList<>();

        if (mandatoryGrounds == null && discretionaryGrounds == null && estateGrounds == null
            && secureMandatoryGrounds == null && secureDiscretionaryGrounds == null
            && secureEstateGrounds == null) {
            return entities;
        }

        // Standard/Other Contract - Mandatory grounds
        if (mandatoryGrounds != null) {
            for (MandatoryGroundWales ground : mandatoryGrounds) {
                String reasonText = grounds != null ? switch (ground) {
                    case FAIL_TO_GIVE_UP_S170 -> grounds.getFailToGiveUpS170Reason();
                    case LANDLORD_NOTICE_PERIODIC_S178 -> grounds.getLandlordNoticePeriodicS178Reason();
                    case SERIOUS_ARREARS_PERIODIC_S181 -> grounds.getSeriousArrearsPeriodicS181Reason();
                    case LANDLORD_NOTICE_FT_END_S186 -> grounds.getLandlordNoticeFtEndS186Reason();
                    case SERIOUS_ARREARS_FIXED_TERM_S187 -> grounds.getSeriousArrearsFixedTermS187Reason();
                    case FAIL_TO_GIVE_UP_BREAK_NOTICE_S191 -> grounds.getFailToGiveUpBreakNoticeS191Reason();
                    case LANDLORD_BREAK_CLAUSE_S199 -> grounds.getLandlordBreakClauseS199Reason();
                    case CONVERTED_FIXED_TERM_SCH12_25B2 -> grounds.getConvertedFixedTermSch1225B2Reason();
                } : null;

                entities.add(ClaimGroundEntity.builder()
                    .groundId(ground.name())
                    .groundReason(reasonText)
                    .build());
            }
        }

        // Standard/Other Contract - Discretionary grounds
        if (discretionaryGrounds != null) {
            for (DiscretionaryGroundWales ground : discretionaryGrounds) {
                String reasonText = grounds != null ? switch (ground) {
                    case RENT_ARREARS_SECTION_157 -> null;
                    case ANTISOCIAL_BEHAVIOUR_SECTION_157 -> null;
                    case OTHER_BREACH_SECTION_157 -> grounds.getOtherBreachSection157Reason();
                    case ESTATE_MANAGEMENT_GROUNDS_SECTION_160 -> null;
                } : null;

                entities.add(ClaimGroundEntity.builder()
                    .groundId(ground.name())
                    .groundReason(reasonText)
                    .build());
            }
        }

        // Standard/Other Contract - Estate Management grounds
        if (estateGrounds != null) {
            for (EstateManagementGroundsWales ground : estateGrounds) {
                String reasonText = grounds != null ? switch (ground) {
                    case BUILDING_WORKS -> grounds.getBuildingWorksReason();
                    case REDEVELOPMENT_SCHEMES -> grounds.getRedevelopmentSchemesReason();
                    case CHARITIES -> grounds.getCharitiesReason();
                    case DISABLED_SUITABLE_DWELLING -> grounds.getDisabledSuitableDwellingReason();
                    case HOUSING_ASSOCIATIONS_AND_TRUSTS -> grounds.getHousingAssociationsAndTrustsReason();
                    case SPECIAL_NEEDS_DWELLINGS -> grounds.getSpecialNeedsDwellingsReason();
                    case RESERVE_SUCCESSORS -> grounds.getReserveSuccessorsReason();
                    case JOINT_CONTRACT_HOLDERS -> grounds.getJointContractHoldersReason();
                    case OTHER_ESTATE_MANAGEMENT_REASONS -> grounds.getOtherEstateManagementReasonsReason();
                } : null;

                entities.add(ClaimGroundEntity.builder()
                    .groundId(ground.name())
                    .groundReason(reasonText)
                    .build());
            }
        }

        // Secure Contract - Mandatory grounds
        if (secureMandatoryGrounds != null) {
            for (SecureContractMandatoryGroundsWales ground : secureMandatoryGrounds) {
                String reasonText = grounds != null ? switch (ground) {
                    case FAILURE_TO_GIVE_UP_POSSESSION_SECTION_170 ->
                        grounds.getSecureFailureToGiveUpPossessionSection170Reason();
                    case LANDLORD_NOTICE_SECTION_186 ->
                        grounds.getSecureLandlordNoticeSection186Reason();
                    case FAILURE_TO_GIVE_UP_POSSESSION_SECTION_191 ->
                        grounds.getSecureFailureToGiveUpPossessionSection191Reason();
                    case LANDLORD_NOTICE_SECTION_199 ->
                        grounds.getSecureLandlordNoticeSection199Reason();
                } : null;

                entities.add(ClaimGroundEntity.builder()
                    .groundId(ground.name())
                    .groundReason(reasonText)
                    .build());
            }
        }

        // Secure Contract - Discretionary grounds
        if (secureDiscretionaryGrounds != null) {
            for (SecureContractDiscretionaryGroundsWales ground : secureDiscretionaryGrounds) {
                String reasonText = grounds != null ? switch (ground) {
                    case RENT_ARREARS -> null;
                    case ANTISOCIAL_BEHAVIOUR -> null;
                    case OTHER_BREACH_OF_CONTRACT -> grounds.getSecureOtherBreachOfContractReason();
                    case ESTATE_MANAGEMENT_GROUNDS -> null;
                } : null;

                entities.add(ClaimGroundEntity.builder()
                    .groundId(ground.name())
                    .groundReason(reasonText)
                    .build());
            }
        }

        // Secure Contract - Estate Management grounds
        if (secureEstateGrounds != null) {
            for (EstateManagementGroundsWales ground : secureEstateGrounds) {
                String reasonText = grounds != null ? switch (ground) {
                    case BUILDING_WORKS -> grounds.getSecureBuildingWorksReason();
                    case REDEVELOPMENT_SCHEMES -> grounds.getSecureRedevelopmentSchemesReason();
                    case CHARITIES -> grounds.getSecureCharitiesReason();
                    case DISABLED_SUITABLE_DWELLING -> grounds.getSecureDisabledSuitableDwellingReason();
                    case HOUSING_ASSOCIATIONS_AND_TRUSTS -> grounds.getSecureHousingAssociationsAndTrustsReason();
                    case SPECIAL_NEEDS_DWELLINGS -> grounds.getSecureSpecialNeedsDwellingsReason();
                    case RESERVE_SUCCESSORS -> grounds.getSecureReserveSuccessorsReason();
                    case JOINT_CONTRACT_HOLDERS -> grounds.getSecureJointContractHoldersReason();
                    case OTHER_ESTATE_MANAGEMENT_REASONS -> grounds.getSecureOtherEstateManagementReasonsReason();
                } : null;

                entities.add(ClaimGroundEntity.builder()
                    .groundId(ground.name())
                    .groundReason(reasonText)
                    .build());
            }
        }

        return entities;
    }

}
