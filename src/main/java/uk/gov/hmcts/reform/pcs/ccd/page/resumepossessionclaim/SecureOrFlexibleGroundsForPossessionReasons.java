package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.SecureOrFlexibleMandatoryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@AllArgsConstructor
@Component
public class SecureOrFlexibleGroundsForPossessionReasons implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("secureOrFlexibleGroundsForPossessionReasons", this::midEvent)
            .pageLabel("Reasons for possession")
            .showCondition(
                    "typeOfTenancyLicence=\"SECURE_TENANCY\""
                            + " OR typeOfTenancyLicence=\"FLEXIBLE_TENANCY\""
                            + " AND (showBreachOfTenancyTextarea=\"Yes\" OR showReasonsForGroundsPage=\"Yes\")"
            )
            .complex(PCSCase::getSecureOrFlexibleGroundsReasons)

            // Discretionary grounds
            .label("possessionReasons-breachOfTenancyGround-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">
                    Breach of the tenancy (ground 1)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                Why are you making a claim for possession under this ground?</h3>
                """, "showBreachOfTenancyTextarea=\"Yes\""
                + " AND secureOrFlexibleDiscretionaryGroundsCONTAINS\"RENT_ARREARS_OR_BREACH_OF_TENANCY\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getBreachOfTenancyGround,
                    "showBreachOfTenancyTextarea=\"Yes\""
                           + " AND secureOrFlexibleDiscretionaryGroundsCONTAINS"
                           + "\"RENT_ARREARS_OR_BREACH_OF_TENANCY\"")

            .label("possessionReasons-nuisanceOrImmoralUse-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">
                    Nuisance, annoyance, illegal or immoral use of the property (ground 2)
                 </h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS\"NUISANCE_OR_IMMORAL_USE\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getNuisanceOrImmoralUseGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS\"NUISANCE_OR_IMMORAL_USE\"")

            .label("possessionReasons-domesticViolence-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Domestic violence (ground 2A)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS\"DOMESTIC_VIOLENCE\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getDomesticViolenceGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS\"DOMESTIC_VIOLENCE\"")

            .label("possessionReasons-riotOffence-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Offence during a riot (ground 2ZA)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS\"RIOT_OFFENCE\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getRiotOffenceGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS\"RIOT_OFFENCE\"")

            .label("possessionReasons-propertyDeterioration-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">
                    Deterioration in the condition of the property (ground 3)
                    </h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS\"PROPERTY_DETERIORATION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getPropertyDeteriorationGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS\"PROPERTY_DETERIORATION\"")

            .label("possessionReasons-furnitureDeterioration-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Deterioration of furniture (ground 4)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS\"FURNITURE_DETERIORATION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getFurnitureDeteriorationGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS\"FURNITURE_DETERIORATION\"")

            .label("possessionReasons-tenancyObtainedByFalseStatement-label",
                   """
                  <h2 class="govuk-heading-l" tabindex="0">Tenancy obtained by false statement (ground 5)</h2>
                  <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                  </h3>
                  """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS"
                       + "\"TENANCY_OBTAINED_BY_FALSE_STATEMENT\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getTenancyByFalseStatementGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS"
                           + "\"TENANCY_OBTAINED_BY_FALSE_STATEMENT\"")

            .label("possessionReasons-premiumPaidMutualExchange-label",
                   """
                  <h2 class="govuk-heading-l" tabindex="0">
                    Premium paid in connection with mutual exchange (ground 6)
                  </h2>
                  <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                  </h3>
                  """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS"
                       + "\"PREMIUM_PAID_MUTUAL_EXCHANGE\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getPremiumMutualExchangeGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS"
                           + "\"PREMIUM_PAID_MUTUAL_EXCHANGE\"")

            .label("possessionReasons-unreasonableConductTiedAccommodation-label",
                   """
                  <h2 class="govuk-heading-l" tabindex="0">
                    Unreasonable conduct in tied accommodation (ground 7)
                  </h2>
                  <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                  </h3>
                  """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS"
                       + "\"UNREASONABLE_CONDUCT_TIED_ACCOMMODATION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getUnreasonableConductGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS"
                           + "\"UNREASONABLE_CONDUCT_TIED_ACCOMMODATION\"")

            .label("possessionReasons-refusalToMoveBack-label",
                   """
                <h2 class="govuk-heading-l" tabindex="0">
                    Refusal to move back to main home after works completed (ground 8)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """,
                   "secureOrFlexibleDiscretionaryGroundsCONTAINS\"REFUSAL_TO_MOVE_BACK\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getRefusalToMoveBackGround,
                       "secureOrFlexibleDiscretionaryGroundsCONTAINS\"REFUSAL_TO_MOVE_BACK\"")

            // Mandatory grounds
            .label("possessionReasons-antiSocial-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Antisocial behaviour</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   "secureOrFlexibleMandatoryGroundsCONTAINS\"ANTI_SOCIAL\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getAntiSocialGround,
                       "secureOrFlexibleMandatoryGroundsCONTAINS\"ANTI_SOCIAL\"")

            // Mandatory grounds (if alternative accommodation is available)
            .label("possessionReasons-overcrowding-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Overcrowding (ground 9)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   "secureOrFlexibleMandatoryGroundsAltCONTAINS\"OVERCROWDING\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getOvercrowdingGround,
                       "secureOrFlexibleMandatoryGroundsAltCONTAINS\"OVERCROWDING\"")

            .label("possessionReasons-landlordWorks-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Landlord's works (ground 10)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   "secureOrFlexibleMandatoryGroundsAltCONTAINS\"LANDLORD_WORKS\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getLandlordWorksGround,
                       "secureOrFlexibleMandatoryGroundsAltCONTAINS\"LANDLORD_WORKS\"")

            .label("possessionReasons-propertySold-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Property sold for redevelopment (ground 10A)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   "secureOrFlexibleMandatoryGroundsAltCONTAINS\"PROPERTY_SOLD\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getPropertySoldGround,
                       "secureOrFlexibleMandatoryGroundsAltCONTAINS\"PROPERTY_SOLD\"")

            .label("possessionReasons-charitableLandlord-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Charitable landlords (ground 11)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   "secureOrFlexibleMandatoryGroundsAltCONTAINS\"CHARITABLE_LANDLORD\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getCharitableLandlordGround,
                       "secureOrFlexibleMandatoryGroundsAltCONTAINS\"CHARITABLE_LANDLORD\"")

            //Discretionary grounds (if alternative accommodation is available)
            .label("possessionReasons-tiedAccommodationNeededForEmployee-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">
                    Tied accommodation needed for another employee (ground 12)
                 </h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                       + "\"TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getTiedAccommodationGround,
                       "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                           + "\"TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE\"")

            .label("possessionReasons-adaptedAccommodation-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Adapted accommodation (ground 13)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsAltCONTAINS\"ADAPTED_ACCOMMODATION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getAdaptedAccommodationGround,
                       "secureOrFlexibleDiscretionaryGroundsAltCONTAINS\"ADAPTED_ACCOMMODATION\"")

            .label("possessionReasons-housingAssociationSpecialCircumstances-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">
                    Housing association special circumstances accommodation (ground 14)
                 </h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                       + "\"HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getHousingAssocSpecialGround,
                       "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                           + "\"HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES\"")

            .label("possessionReasons-specialNeedsAccommodation-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Special needs accommodation (ground 15)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsAltCONTAINS\"SPECIAL_NEEDS_ACCOMMODATION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getSpecialNeedsAccommodationGround,
                       "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                           + "\"SPECIAL_NEEDS_ACCOMMODATION\"")

            .label("possessionReasons-underOccupyingAfterSuccession-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Under occupying after succession (ground 15A)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                       + "\"UNDER_OCCUPYING_AFTER_SUCCESSION\"")
            .mandatory(SecureOrFlexibleGroundsReasons::getUnderOccupancySuccessionGround,
                       "secureOrFlexibleDiscretionaryGroundsAltCONTAINS"
                           + "\"UNDER_OCCUPYING_AFTER_SUCCESSION\"")
            .done()
                .readonly(PCSCase::getShowBreachOfTenancyTextarea,NEVER_SHOW)
                .readonly(PCSCase::getShowReasonsForGroundsPage,NEVER_SHOW);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        
        // Validate all text area fields for character limit - ultra simple approach
        List<String> validationErrors = new ArrayList<>();
        
        SecureOrFlexibleGroundsReasons secureOrFlexibleGrounds = caseData.getSecureOrFlexibleGroundsReasons();
        if (secureOrFlexibleGrounds != null) {
            validationErrors.addAll(textAreaValidationService.validateMultipleTextAreas(
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getBreachOfTenancyGround(),
                    SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getNuisanceOrImmoralUseGround(),
                    SecureOrFlexibleDiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getDomesticViolenceGround(),
                    SecureOrFlexibleDiscretionaryGrounds.DOMESTIC_VIOLENCE.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getRiotOffenceGround(),
                    SecureOrFlexibleDiscretionaryGrounds.RIOT_OFFENCE.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getPropertyDeteriorationGround(),
                    SecureOrFlexibleDiscretionaryGrounds.PROPERTY_DETERIORATION.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getFurnitureDeteriorationGround(),
                    SecureOrFlexibleDiscretionaryGrounds.FURNITURE_DETERIORATION.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getTenancyByFalseStatementGround(),
                    SecureOrFlexibleDiscretionaryGrounds.TENANCY_OBTAINED_BY_FALSE_STATEMENT.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getPremiumMutualExchangeGround(),
                    SecureOrFlexibleDiscretionaryGrounds.PREMIUM_PAID_MUTUAL_EXCHANGE.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getUnreasonableConductGround(),
                    SecureOrFlexibleDiscretionaryGrounds.UNREASONABLE_CONDUCT_TIED_ACCOMMODATION.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getRefusalToMoveBackGround(),
                    SecureOrFlexibleDiscretionaryGrounds.REFUSAL_TO_MOVE_BACK.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getAntiSocialGround(),
                    SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getOvercrowdingGround(),
                    SecureOrFlexibleMandatoryGroundsAlternativeAccomm.OVERCROWDING.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getLandlordWorksGround(),
                    SecureOrFlexibleMandatoryGroundsAlternativeAccomm.LANDLORD_WORKS.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getPropertySoldGround(),
                    SecureOrFlexibleMandatoryGroundsAlternativeAccomm.PROPERTY_SOLD.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getCharitableLandlordGround(),
                    SecureOrFlexibleMandatoryGroundsAlternativeAccomm.CHARITABLE_LANDLORD.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getTiedAccommodationGround(),
                    SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getAdaptedAccommodationGround(),
                    SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.ADAPTED_ACCOMMODATION.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getHousingAssocSpecialGround(),
                    SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getSpecialNeedsAccommodationGround(),
                    SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.SPECIAL_NEEDS_ACCOMMODATION.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    secureOrFlexibleGrounds.getUnderOccupancySuccessionGround(),
                    SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.UNDER_OCCUPYING_AFTER_SUCCESSION.getLabel(),
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                )
            ));
        }
        
        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}

