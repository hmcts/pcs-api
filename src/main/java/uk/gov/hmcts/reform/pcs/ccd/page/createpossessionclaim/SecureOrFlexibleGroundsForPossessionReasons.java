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
            .page("secureOrFlexibleGroundsForPossessionReasons")
            .pageLabel("Reasons for possession")
            .showCondition(
                    "typeOfTenancyLicence=\"SECURE_TENANCY\""
                            + " OR typeOfTenancyLicence=\"FLEXIBLE_TENANCY\""
                            + " AND (showBreachOfTenancyTextarea=\"Yes\" OR showReasonsForGroundsPage=\"Yes\")"
            )
            .complex(PCSCase::getSecureOrFlexibleGroundsReasons)
            // Discretionary grounds
            .label("breachOfTenancyGround-label","""
                ---
                <h2 class="govuk-heading-l">Breach of the tenancy (ground 1)</h2>
                <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                """, "showBreachOfTenancyTextarea=\"Yes\""
                + " AND secureOrFlexibleDiscretionaryGroundsCONTAINS\"RENT_ARREARS_OR_BREACH_OF_TENANCY\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getBreachOfTenancyGround,
                    "showBreachOfTenancyTextarea=\"Yes\""
                           + " AND secureOrFlexibleDiscretionaryGroundsCONTAINS"
                           + "\"RENT_ARREARS_OR_BREACH_OF_TENANCY\"")

            .label("nuisanceOrImmoralUse-label",
                   """
                   ---
                 <h2 class="govuk-heading-l">Nuisance, annoyance, illegal or immoral use of the property (ground 2)</h2>
                 <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS\"NUISANCE_OR_IMMORAL_USE\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getNuisanceOrImmoralUseGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS\"NUISANCE_OR_IMMORAL_USE\"")

            .label("domesticViolence-label",
                   """
                   ---
                 <h2 class="govuk-heading-l">Domestic violence (ground 2A)</h2>
                 <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS\"DOMESTIC_VIOLENCE\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getDomesticViolenceGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS\"DOMESTIC_VIOLENCE\"")

            .label("riotOffence-label",
                   """
                   ---
                 <h2 class="govuk-heading-l">Offence during a riot (ground 22A)</h2>
                 <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS\"RIOT_OFFENCE\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getRiotOffenceGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS\"RIOT_OFFENCE\"")

            .label("propertyDeterioration-label",
                   """
                   ---
                 <h2 class="govuk-heading-l">Deterioration in the condition of the property (ground 3)</h2>
                 <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS\"PROPERTY_DETERIORATION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getPropertyDeteriorationGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS\"PROPERTY_DETERIORATION\"")

            .label("furnitureDeterioration-label",
                   """
                   ---
                 <h2 class="govuk-heading-l">Deterioration of furniture (ground 4)"</h2>
                 <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS\"FURNITURE_DETERIORATION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getFurnitureDeteriorationGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS\"FURNITURE_DETERIORATION\"")

            .label("tenancyObtainedByFalseStatement-label",
                   """
                   ---
                  <h2 class="govuk-heading-l">Tenancy obtained by false statement (ground 5)</h2>
                  <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                  """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS"
                       + "\"TENANCY_OBTAINED_BY_FALSE_STATEMENT\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getTenancyObtainedByFalseStatementGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS"
                           + "\"TENANCY_OBTAINED_BY_FALSE_STATEMENT\"")

            .label("premiumPaidMutualExchange-label",
                   """
                   ---
                  <h2 class="govuk-heading-l">Premium paid in connection with mutual exchange (ground 6)</h2>
                  <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                  """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS"
                       + "\"PREMIUM_PAID_MUTUAL_EXCHANGE\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getPremiumPaidMutualExchangeGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS"
                           + "\"PREMIUM_PAID_MUTUAL_EXCHANGE\"")

            .label("unreasonableConductTiedAccommodation-label",
                   """
                   ---
                  <h2 class="govuk-heading-l">Unreasonable conduct in tied accommodation (ground 7)</h2>
                  <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                  """,
                   "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                       + "\"UNREASONABLE_CONDUCT_TIED_ACCOMMODATION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getUnreasonableConductTiedAccommodationGround,
                       "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                           + "\"UNREASONABLE_CONDUCT_TIED_ACCOMMODATION\"")

            .label("refusalToMoveBack-label",
                   """
                   ---
                <h2 class="govuk-heading-l">Refusal to move back to main home after works completed (ground 8)</h2>
                <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS\"REFUSAL_TO_MOVE_BACK\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getRefusalToMoveBackGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS\"REFUSAL_TO_MOVE_BACK\"")

            .label("tiedAccommodationNeededForEmployee-label",
                   """
                   ---
                 <h2 class="govuk-heading-l">Tied accommodation needed for another employee (ground 12)</h2>
                 <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                       + "\"TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getTiedAccommodationNeededForEmployeeGround,
                       "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                           + "\"TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE\"")

            .label("adaptedAccommodation-label",
                   """
                   ---
                 <h2 class="govuk-heading-l">Adapted accommodation (ground 13)</h2>
                 <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsAltCONTAINS\"ADAPTED_ACCOMMODATION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getAdaptedAccommodationGround,
                       "secureOrFlexibleDiscretionaryGroundsAltCONTAINS\"ADAPTED_ACCOMMODATION\"")

            .label("housingAssociationSpecialCircumstances-label",
                   """
                   ---
                 <h2 class="govuk-heading-l">Housing association special circumstances accommodation (ground 14)</h2>
                 <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                       + "\"HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getHousingAssociationSpecialCircumstancesGround,
                       "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                           + "\"HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES\"")

            .label("specialNeedsAccommodation-label",
                   """
                   ---
                 <h2 class="govuk-heading-l">Special needs accommodation (ground 15)</h2>
                 <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsAltCONTAINS\"SPECIAL_NEEDS_ACCOMMODATION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getSpecialNeedsAccommodationGround,
                       "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                           + "\"SPECIAL_NEEDS_ACCOMMODATION\"")

            .label("underOccupyingAfterSuccession-label",
                   """
                   ---
                 <h2 class="govuk-heading-l">Under occupying after succession (ground 15A)</h2>
                 <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                       + "\"UNDER_OCCUPYING_AFTER_SUCCESSION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getUnderOccupyingAfterSuccessionGround,
                       "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                           + "\"UNDER_OCCUPYING_AFTER_SUCCESSION\"")

            // Mandatory grounds
            .label("antiSocial-label",
                   """
                   ---
                 <h2 class="govuk-heading-l">Antisocial behaviour</h2>
                 <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                 """,
                   "secureOrFlexibleMandatoryGroundsCONTAINS\"ANTI_SOCIAL\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getAntiSocialGround,
                       "secureOrFlexibleMandatoryGroundsCONTAINS\"ANTI_SOCIAL\"")

            .label("overcrowding-label",
                   """
                   ---
                 <h2 class="govuk-heading-l">Overcrowding (ground 9)</h2>
                 <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                 """,
                   "secureOrFlexibleMandatoryGroundsAltCONTAINS\"OVERCROWDING\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getOvercrowdingGround,
                       "secureOrFlexibleMandatoryGroundsAltCONTAINS\"OVERCROWDING\"")

            .label("landlordWorks-label",
                   """
                   ---
                 <h2 class="govuk-heading-l">Landlord's works (ground 10)</h2>
                 <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                 """,
                   "secureOrFlexibleMandatoryGroundsAltCONTAINS\"LANDLORD_WORKS\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getLandlordWorksGround,
                       "secureOrFlexibleMandatoryGroundsAltCONTAINS\"LANDLORD_WORKS\"")

            .label("propertySold-label",
                   """
                   ---
                 <h2 class="govuk-heading-l">Property sold for redevelopment (ground 10A)</h2>
                 <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                 """,
                   "secureOrFlexibleMandatoryGroundsAltCONTAINS\"PROPERTY_SOLD\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getPropertySoldGround,
                       "secureOrFlexibleMandatoryGroundsAltCONTAINS\"PROPERTY_SOLD\"")

            .label("charitableLandlord-label",
                   """
                   ---
                 <h2 class="govuk-heading-l">Charitable landlords (ground 11)</h2>
                 <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                 """,
                   "secureOrFlexibleMandatoryGroundsAltCONTAINS\"CHARITABLE_LANDLORD\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getCharitableLandlordGround,
                       "secureOrFlexibleMandatoryGroundsAltCONTAINS\"CHARITABLE_LANDLORD\"")
            .done()
                .readonly(PCSCase::getShowBreachOfTenancyTextarea,NEVER_SHOW)
                .readonly(PCSCase::getShowReasonsForGroundsPage,NEVER_SHOW);

    }
}

