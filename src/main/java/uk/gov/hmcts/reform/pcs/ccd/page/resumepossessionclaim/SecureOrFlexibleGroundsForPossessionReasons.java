package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.DOMESTIC_VIOLENCE_GROUND2A;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.FURNITURE_DETERIORATION_GROUND4;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE_GROUND2;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.PREMIUM_PAID_GROUND6;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.PROPERTY_DETERIORATION_GROUND3;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.REFUSAL_TO_MOVE_BACK_GROUND8;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY_GROUND1;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.RIOT_OFFENCE_GROUND2ZA;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.TENANCY_FALSE_STATEMENT_GROUND5;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.UNREASONABLE_CONDUCT_GROUND7;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.ADAPTED_ACCOMMODATION_GROUND13;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.HOUSING_ASSOCIATION_SPECIAL_GROUND14;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.SPECIAL_NEEDS_ACCOMMODATION_GROUND15;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.ACCOMMODATION_NEEDED_FOR_EMPLOYEE_GROUND12;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.UNDER_OCCUPYING_GROUND15A;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm.CHARITABLE_LANDLORD_GROUND11;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm.LANDLORD_WORKS_GROUND10;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm.OVERCROWDING_GROUND9;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm.PROPERTY_SOLD_GROUND10A;

@AllArgsConstructor
@Component
public class SecureOrFlexibleGroundsForPossessionReasons implements CcdPageConfiguration {

    private static final String DISCRETIONARY_GROUNDS = "secureOrFlexibleDiscretionaryGrounds";

    private static final String BREACH_OF_TENANCY_GROUND_LABEL = "Breach of the tenancy (ground 1)";
    private static final String MANDATORY_GROUNDS = "secureOrFlexibleMandatoryGrounds";
    private static final String MANDATORY_GROUNDS_ALT = "secureOrFlexibleMandatoryGroundsAlt";
    private static final String DISCRETIONARY_GROUNDS_ALT = "secureOrFlexibleDiscretionaryGroundsAlt";

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("secureOrFlexibleGroundsForPossessionReasons", this::midEvent)
            .pageLabel("Reasons for possession")
            .showCondition(
                    "tenancy_TypeOfTenancyLicence=\"SECURE_TENANCY\""
                            + " OR tenancy_TypeOfTenancyLicence=\"FLEXIBLE_TENANCY\""
                            + " AND (showBreachOfTenancyTextarea=\"Yes\" OR showReasonsForGroundsPage=\"Yes\")"
                            + " AND legislativeCountry=\"England\""
            )
            .label("possessionReasons-lineSeparator","---")
            .complex(PCSCase::getSecureOrFlexibleGroundsReasons)

            // Discretionary grounds
            .label("possessionReasons-breachOfTenancyGround-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Breach of the tenancy (ground 1)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                Why are you making a claim for possession under this ground?</h3>
                """, "showBreachOfTenancyTextarea=\"Yes\" AND "
                + ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, RENT_ARREARS_OR_BREACH_OF_TENANCY_GROUND1))
            .mandatory(SecureOrFlexibleGroundsReasons::getBreachOfTenancyGround,
                    "showBreachOfTenancyTextarea=\"Yes\" AND "
                + ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, RENT_ARREARS_OR_BREACH_OF_TENANCY_GROUND1))

            .label("possessionReasons-nuisanceOrImmoralUse-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">
                    Nuisance, annoyance, illegal or immoral use of the property (ground 2)
                 </h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, NUISANCE_OR_IMMORAL_USE_GROUND2))
            .mandatory(SecureOrFlexibleGroundsReasons::getNuisanceOrImmoralUseGround,
                       ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, NUISANCE_OR_IMMORAL_USE_GROUND2))

            .label("possessionReasons-domesticViolence-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Domestic violence (ground 2A)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, DOMESTIC_VIOLENCE_GROUND2A))
            .mandatory(SecureOrFlexibleGroundsReasons::getDomesticViolenceGround,
                       ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, DOMESTIC_VIOLENCE_GROUND2A))

            .label("possessionReasons-riotOffence-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Offence during a riot (ground 2ZA)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, RIOT_OFFENCE_GROUND2ZA))
            .mandatory(SecureOrFlexibleGroundsReasons::getRiotOffenceGround,
                       ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, RIOT_OFFENCE_GROUND2ZA))

            .label("possessionReasons-propertyDeterioration-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">
                    Deterioration in the condition of the property (ground 3)
                    </h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, PROPERTY_DETERIORATION_GROUND3))
            .mandatory(SecureOrFlexibleGroundsReasons::getPropertyDeteriorationGround,
                       ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, PROPERTY_DETERIORATION_GROUND3))

            .label("possessionReasons-furnitureDeterioration-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Deterioration of furniture (ground 4)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, FURNITURE_DETERIORATION_GROUND4))
            .mandatory(SecureOrFlexibleGroundsReasons::getFurnitureDeteriorationGround,
                       ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, FURNITURE_DETERIORATION_GROUND4))

            .label("possessionReasons-tenancyObtainedByFalseStatement-label",
                   """
                  <h2 class="govuk-heading-l" tabindex="0">Tenancy obtained by false statement (ground 5)</h2>
                  <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                  </h3>
                  """,
                   ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, TENANCY_FALSE_STATEMENT_GROUND5))
            .mandatory(SecureOrFlexibleGroundsReasons::getTenancyByFalseStatementGround,
                       ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, TENANCY_FALSE_STATEMENT_GROUND5))

            .label("possessionReasons-premiumPaidMutualExchange-label",
                   """
                  <h2 class="govuk-heading-l" tabindex="0">
                    Premium paid in connection with mutual exchange (ground 6)
                  </h2>
                  <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                  </h3>
                  """,
                   ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, PREMIUM_PAID_GROUND6))
            .mandatory(SecureOrFlexibleGroundsReasons::getPremiumMutualExchangeGround,
                       ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, PREMIUM_PAID_GROUND6))

            .label("possessionReasons-unreasonableConductTiedAccommodation-label",
                   """
                  <h2 class="govuk-heading-l" tabindex="0">
                    Unreasonable conduct in tied accommodation (ground 7)
                  </h2>
                  <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                  </h3>
                  """,
                   ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, UNREASONABLE_CONDUCT_GROUND7))
            .mandatory(SecureOrFlexibleGroundsReasons::getUnreasonableConductGround,
                       ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, UNREASONABLE_CONDUCT_GROUND7))

            .label("possessionReasons-refusalToMoveBack-label",
                   """
                <h2 class="govuk-heading-l" tabindex="0">
                    Refusal to move back to main home after works completed (ground 8)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """,
                   ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, REFUSAL_TO_MOVE_BACK_GROUND8))
            .mandatory(SecureOrFlexibleGroundsReasons::getRefusalToMoveBackGround,
                       ShowConditions.fieldContains(DISCRETIONARY_GROUNDS, REFUSAL_TO_MOVE_BACK_GROUND8))

            // Mandatory grounds
            .label("possessionReasons-antiSocial-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Antisocial behaviour</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   ShowConditions.fieldContains(MANDATORY_GROUNDS, ANTI_SOCIAL))
            .mandatory(SecureOrFlexibleGroundsReasons::getAntiSocialGround,
                       ShowConditions.fieldContains(MANDATORY_GROUNDS, ANTI_SOCIAL))

            // Mandatory grounds (if alternative accommodation is available)
            .label("possessionReasons-overcrowding-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Overcrowding (ground 9)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   ShowConditions.fieldContains(MANDATORY_GROUNDS_ALT, OVERCROWDING_GROUND9))
            .mandatory(SecureOrFlexibleGroundsReasons::getOvercrowdingGround,
                       ShowConditions.fieldContains(MANDATORY_GROUNDS_ALT, OVERCROWDING_GROUND9))

            .label("possessionReasons-landlordWorks-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Landlord’s works (ground 10)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   ShowConditions.fieldContains(MANDATORY_GROUNDS_ALT, LANDLORD_WORKS_GROUND10))
            .mandatory(SecureOrFlexibleGroundsReasons::getLandlordWorksGround,
                       ShowConditions.fieldContains(MANDATORY_GROUNDS_ALT, LANDLORD_WORKS_GROUND10))

            .label("possessionReasons-propertySold-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Property sold for redevelopment (ground 10A)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   ShowConditions.fieldContains(MANDATORY_GROUNDS_ALT, PROPERTY_SOLD_GROUND10A))
            .mandatory(SecureOrFlexibleGroundsReasons::getPropertySoldGround,
                       ShowConditions.fieldContains(MANDATORY_GROUNDS_ALT, PROPERTY_SOLD_GROUND10A))

            .label("possessionReasons-charitableLandlord-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Charitable landlords (ground 11)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   ShowConditions.fieldContains(MANDATORY_GROUNDS_ALT, CHARITABLE_LANDLORD_GROUND11))
            .mandatory(SecureOrFlexibleGroundsReasons::getCharitableLandlordGround,
                       ShowConditions.fieldContains(MANDATORY_GROUNDS_ALT, CHARITABLE_LANDLORD_GROUND11))

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
                   ShowConditions.fieldContains(DISCRETIONARY_GROUNDS_ALT, ACCOMMODATION_NEEDED_FOR_EMPLOYEE_GROUND12))
            .mandatory(SecureOrFlexibleGroundsReasons::getTiedAccommodationGround,
                       ShowConditions.fieldContains(DISCRETIONARY_GROUNDS_ALT,
                                                    ACCOMMODATION_NEEDED_FOR_EMPLOYEE_GROUND12
                       ))

            .label("possessionReasons-adaptedAccommodation-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Adapted accommodation (ground 13)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   ShowConditions.fieldContains(DISCRETIONARY_GROUNDS_ALT, ADAPTED_ACCOMMODATION_GROUND13))
            .mandatory(SecureOrFlexibleGroundsReasons::getAdaptedAccommodationGround,
                       ShowConditions.fieldContains(DISCRETIONARY_GROUNDS_ALT, ADAPTED_ACCOMMODATION_GROUND13))

            .label("possessionReasons-housingAssociationSpecialCircumstances-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">
                    Housing association special circumstances accommodation (ground 14)
                 </h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   ShowConditions.fieldContains(DISCRETIONARY_GROUNDS_ALT,
                                                HOUSING_ASSOCIATION_SPECIAL_GROUND14
                   ))
            .mandatory(SecureOrFlexibleGroundsReasons::getHousingAssocSpecialGround,
                   ShowConditions.fieldContains(DISCRETIONARY_GROUNDS_ALT,
                                                HOUSING_ASSOCIATION_SPECIAL_GROUND14
                   ))

            .label("possessionReasons-specialNeedsAccommodation-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Special needs accommodation (ground 15)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   ShowConditions.fieldContains(DISCRETIONARY_GROUNDS_ALT, SPECIAL_NEEDS_ACCOMMODATION_GROUND15))
            .mandatory(SecureOrFlexibleGroundsReasons::getSpecialNeedsAccommodationGround,
                       ShowConditions.fieldContains(DISCRETIONARY_GROUNDS_ALT, SPECIAL_NEEDS_ACCOMMODATION_GROUND15))

            .label("possessionReasons-underOccupyingAfterSuccession-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Under occupying after succession (ground 15A)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   ShowConditions.fieldContains(DISCRETIONARY_GROUNDS_ALT, UNDER_OCCUPYING_GROUND15A))
            .mandatory(SecureOrFlexibleGroundsReasons::getUnderOccupancySuccessionGround,
                       ShowConditions.fieldContains(DISCRETIONARY_GROUNDS_ALT, UNDER_OCCUPYING_GROUND15A))
            .done()
                .readonly(PCSCase::getShowBreachOfTenancyTextarea,NEVER_SHOW)
                .readonly(PCSCase::getShowReasonsForGroundsPage,NEVER_SHOW)
                .label("secureOrFlexibleGroundsForPossessionReasons-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = new ArrayList<>();

        SecureOrFlexibleGroundsReasons secureOrFlexibleGrounds = caseData.getSecureOrFlexibleGroundsReasons();
        if (secureOrFlexibleGrounds != null) {
            validationErrors.addAll(validateSecureOrFlexibleGrounds(secureOrFlexibleGrounds));
        }

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }

    private List<String> validateSecureOrFlexibleGrounds(SecureOrFlexibleGroundsReasons grounds) {
        List<TextAreaValidationService.FieldValidation> allValidations = new ArrayList<>();
        allValidations.addAll(List.of(buildDiscretionaryGroundValidations(grounds)));
        allValidations.addAll(List.of(buildMandatoryGroundValidations(grounds)));
        allValidations.addAll(List.of(buildMandatoryGroundsAlternativeAccommValidations(grounds)));
        allValidations.addAll(List.of(buildDiscretionaryGroundsAlternativeAccommValidations(grounds)));

        return textAreaValidationService.validateMultipleTextAreas(
            allValidations.toArray(new TextAreaValidationService.FieldValidation[0])
        );
    }

    private TextAreaValidationService.FieldValidation[] buildDiscretionaryGroundValidations(
            SecureOrFlexibleGroundsReasons grounds) {
        return new TextAreaValidationService.FieldValidation[] {
            TextAreaValidationService.FieldValidation.of(
                grounds.getBreachOfTenancyGround(),
                BREACH_OF_TENANCY_GROUND_LABEL,
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getNuisanceOrImmoralUseGround(),
                SecureOrFlexibleDiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE_GROUND2.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getDomesticViolenceGround(),
                SecureOrFlexibleDiscretionaryGrounds.DOMESTIC_VIOLENCE_GROUND2A.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getRiotOffenceGround(),
                SecureOrFlexibleDiscretionaryGrounds.RIOT_OFFENCE_GROUND2ZA.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getPropertyDeteriorationGround(),
                SecureOrFlexibleDiscretionaryGrounds.PROPERTY_DETERIORATION_GROUND3.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getFurnitureDeteriorationGround(),
                SecureOrFlexibleDiscretionaryGrounds.FURNITURE_DETERIORATION_GROUND4.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getTenancyByFalseStatementGround(),
                SecureOrFlexibleDiscretionaryGrounds.TENANCY_FALSE_STATEMENT_GROUND5.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getPremiumMutualExchangeGround(),
                SecureOrFlexibleDiscretionaryGrounds.PREMIUM_PAID_GROUND6.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getUnreasonableConductGround(),
                SecureOrFlexibleDiscretionaryGrounds.UNREASONABLE_CONDUCT_GROUND7.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getRefusalToMoveBackGround(),
                SecureOrFlexibleDiscretionaryGrounds.REFUSAL_TO_MOVE_BACK_GROUND8.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }

    private TextAreaValidationService.FieldValidation[] buildMandatoryGroundValidations(
            SecureOrFlexibleGroundsReasons grounds) {
        return new TextAreaValidationService.FieldValidation[] {
            TextAreaValidationService.FieldValidation.of(
                grounds.getAntiSocialGround(),
                ANTI_SOCIAL.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }

    private TextAreaValidationService.FieldValidation[] buildMandatoryGroundsAlternativeAccommValidations(
            SecureOrFlexibleGroundsReasons grounds) {
        return new TextAreaValidationService.FieldValidation[] {
            TextAreaValidationService.FieldValidation.of(
                grounds.getOvercrowdingGround(),
                OVERCROWDING_GROUND9.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getLandlordWorksGround(),
                LANDLORD_WORKS_GROUND10.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getPropertySoldGround(),
                PROPERTY_SOLD_GROUND10A.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getCharitableLandlordGround(),
                CHARITABLE_LANDLORD_GROUND11.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }

    private TextAreaValidationService.FieldValidation[] buildDiscretionaryGroundsAlternativeAccommValidations(
            SecureOrFlexibleGroundsReasons grounds) {
        return new TextAreaValidationService.FieldValidation[] {
            TextAreaValidationService.FieldValidation.of(
                grounds.getTiedAccommodationGround(),
                ACCOMMODATION_NEEDED_FOR_EMPLOYEE_GROUND12.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getAdaptedAccommodationGround(),
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.ADAPTED_ACCOMMODATION_GROUND13.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getHousingAssocSpecialGround(),
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm
                    .HOUSING_ASSOCIATION_SPECIAL_GROUND14.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSpecialNeedsAccommodationGround(),
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.SPECIAL_NEEDS_ACCOMMODATION_GROUND15.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getUnderOccupancySuccessionGround(),
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.UNDER_OCCUPYING_GROUND15A.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }
}

