package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleGroundsReasons;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class SecureOrFlexibleGroundsForPossessionReasons implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("reasonForPossession")
            .pageLabel("Reasons for possession")
            .readonly(PCSCase::getSelectedSecureOrFlexibleDiscretionaryGrounds,NEVER_SHOW)
            .readonly(PCSCase::getSelectedSecureOrFlexibleMandatoryGrounds,NEVER_SHOW)
            .showCondition(
                "(" +
                    "typeOfTenancyLicence=\"SECURE_TENANCY\" OR " +
                    "typeOfTenancyLicence=\"FLEXIBLE_TENANCY\"" +
                    ")" +
                    " AND " +
                    "(" +
                    "rentAreasOrBreachOfTenancy CONTAINS \"BREACH_OF_TENANCY\" OR " +
                    "(" +
                    "selectedSecureOrFlexibleDiscretionaryGrounds!=\"RENT_ARREARS_OR_BREACH_OF_TENANCY\" " +
                    "AND selectedSecureOrFlexibleDiscretionaryGrounds=\"*\"" +
                    ") OR " +
                    "selectedSecureOrFlexibleMandatoryGrounds=\"*\"" +
                    ")"

            )
            .complex(PCSCase::getSecureOrFlexibleGroundsReasons)
            // Discretionary grounds
            .readonly(SecureOrFlexibleGroundsReasons::getBreachOfTenancyLabel,
                      "rentAreasOrBreachOfTenancyCONTAINS" +
                          "\"BREACH_OF_TENANCY\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getBreachOfTenancyGround,
                       "rentAreasOrBreachOfTenancyCONTAINS" +
                           "\"BREACH_OF_TENANCY\"")

            .readonly(SecureOrFlexibleGroundsReasons::getNuisanceOrImmoralUseLabel,
                      "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                          "\"NUISANCE_OR_IMMORAL_USE\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getNuisanceOrImmoralUseGround,
                       "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                           "\"NUISANCE_OR_IMMORAL_USE\"")

            .readonly(SecureOrFlexibleGroundsReasons::getDomesticViolenceLabel,
                      "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                          "\"DOMESTIC_VIOLENCE\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getDomesticViolenceGround,
                       "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                           "\"DOMESTIC_VIOLENCE\"")

            .readonly(SecureOrFlexibleGroundsReasons::getRiotOffenceLabel,
                      "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                          "\"RIOT_OFFENCE\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getRiotOffenceGround,
                       "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                           "\"RIOT_OFFENCE\"")

            .readonly(SecureOrFlexibleGroundsReasons::getPropertyDeteriorationLabel,
                      "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                          "\"PROPERTY_DETERIORATION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getPropertyDeteriorationGround,
                       "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                           "\"PROPERTY_DETERIORATION\"")

            .readonly(SecureOrFlexibleGroundsReasons::getFurnitureDeteriorationLabel,
                      "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                          "\"FURNITURE_DETERIORATION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getFurnitureDeteriorationGround,
                       "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                           "\"FURNITURE_DETERIORATION\"")

            .readonly(SecureOrFlexibleGroundsReasons::getTenancyObtainedByFalseStatementLabel,
                      "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                          "\"TENANCY_OBTAINED_BY_FALSE_STATEMENT\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getTenancyObtainedByFalseStatementGround,
                       "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                           "\"TENANCY_OBTAINED_BY_FALSE_STATEMENT\"")

            .readonly(SecureOrFlexibleGroundsReasons::getPremiumPaidMutualExchangeLabel,
                      "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                          "\"PREMIUM_PAID_MUTUAL_EXCHANGE\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getPremiumPaidMutualExchangeGround,
                       "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                           "\"PREMIUM_PAID_MUTUAL_EXCHANGE\"")

            .readonly(SecureOrFlexibleGroundsReasons::getUnreasonableConductTiedAccommodationLabel,
                      "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                          "\"UNREASONABLE_CONDUCT_TIED_ACCOMMODATION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getUnreasonableConductTiedAccommodationGround,
                       "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                           "\"UNREASONABLE_CONDUCT_TIED_ACCOMMODATION\"")

            .readonly(SecureOrFlexibleGroundsReasons::getRefusalToMoveBackLabel,
                      "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                          "\"REFUSAL_TO_MOVE_BACK\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getRefusalToMoveBackGround,
                       "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                           "\"REFUSAL_TO_MOVE_BACK\"")

            .readonly(SecureOrFlexibleGroundsReasons::getTiedAccommodationNeededForEmployeeLabel,
                      "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                          "\"TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getTiedAccommodationNeededForEmployeeGround,
                       "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                           "\"TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE\"")

            .readonly(SecureOrFlexibleGroundsReasons::getAdaptedAccommodationLabel,
                      "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                          "\"ADAPTED_ACCOMMODATION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getAdaptedAccommodationGround,
                       "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                           "\"ADAPTED_ACCOMMODATION\"")

            .readonly(SecureOrFlexibleGroundsReasons::getHousingAssociationSpecialCircumstancesLabel,
                      "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                          "\"HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getHousingAssociationSpecialCircumstancesGround,
                       "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                           "\"HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES\"")

            .readonly(SecureOrFlexibleGroundsReasons::getSpecialNeedsAccommodationLabel,
                      "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                          "\"SPECIAL_NEEDS_ACCOMMODATION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getSpecialNeedsAccommodationGround,
                       "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                           "\"SPECIAL_NEEDS_ACCOMMODATION\"")

            .readonly(SecureOrFlexibleGroundsReasons::getUnderOccupyingAfterSuccessionLabel,
                      "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                          "\"UNDER_OCCUPYING_AFTER_SUCCESSION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getUnderOccupyingAfterSuccessionGround,
                       "selectedSecureOrFlexibleDiscretionaryGroundsCONTAINS" +
                           "\"UNDER_OCCUPYING_AFTER_SUCCESSION\"")

            // Mandatory grounds
            .readonly(SecureOrFlexibleGroundsReasons::getAntiSocialLabel,
                      "selectedSecureOrFlexibleMandatoryGroundsCONTAINS\"ANTI_SOCIAL\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getAntiSocialGround,
                       "selectedSecureOrFlexibleMandatoryGroundsCONTAINS\"ANTI_SOCIAL\"")

            .readonly(SecureOrFlexibleGroundsReasons::getOvercrowdingLabel,
                      "selectedSecureOrFlexibleMandatoryGroundsCONTAINS\"OVERCROWDING\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getOvercrowdingGround,
                       "selectedSecureOrFlexibleMandatoryGroundsCONTAINS\"OVERCROWDING\"")

            .readonly(SecureOrFlexibleGroundsReasons::getLandlordWorksLabel,
                      "selectedSecureOrFlexibleMandatoryGroundsCONTAINS\"LANDLORD_WORKS\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getLandlordWorksGround,
                       "selectedSecureOrFlexibleMandatoryGroundsCONTAINS\"LANDLORD_WORKS\"")

            .readonly(SecureOrFlexibleGroundsReasons::getPropertySoldLabel,
                      "selectedSecureOrFlexibleMandatoryGroundsCONTAINS\"PROPERTY_SOLD\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getPropertySoldGround,
                       "selectedSecureOrFlexibleMandatoryGroundsCONTAINS\"PROPERTY_SOLD\"")

            .readonly(SecureOrFlexibleGroundsReasons::getCharitableLandlordLabel,
                      "selectedSecureOrFlexibleMandatoryGroundsCONTAINS\"CHARITABLE_LANDLORD\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getCharitableLandlordGround,
                       "selectedSecureOrFlexibleMandatoryGroundsCONTAINS\"CHARITABLE_LANDLORD\"")
            .done();


    }
}
