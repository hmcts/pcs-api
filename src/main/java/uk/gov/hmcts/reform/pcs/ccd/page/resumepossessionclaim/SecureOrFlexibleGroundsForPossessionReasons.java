package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ShowCondition;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.NEVER_SHOW;
import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.allOf;
import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.anyOf;
import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.contains;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.FLEXIBLE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.SECURE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.DOMESTIC_VIOLENCE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.FURNITURE_DETERIORATION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.PREMIUM_PAID_MUTUAL_EXCHANGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.PROPERTY_DETERIORATION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.REFUSAL_TO_MOVE_BACK;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.RIOT_OFFENCE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.TENANCY_OBTAINED_BY_FALSE_STATEMENT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.UNREASONABLE_CONDUCT_TIED_ACCOMMODATION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.ADAPTED_ACCOMMODATION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.SPECIAL_NEEDS_ACCOMMODATION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.UNDER_OCCUPYING_AFTER_SUCCESSION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm.CHARITABLE_LANDLORD;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm.LANDLORD_WORKS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm.OVERCROWDING;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm.PROPERTY_SOLD;

@AllArgsConstructor
@Component
public class SecureOrFlexibleGroundsForPossessionReasons implements CcdPageConfiguration {

    private static final ShowCondition.NamedFieldCondition DISCRETIONARY_GROUNDS = when(
        PCSCase::getSecureOrFlexiblePossessionGrounds,
        SecureOrFlexiblePossessionGrounds::getSecureOrFlexibleDiscretionaryGrounds
    );

    private static final String BREACH_OF_TENANCY_GROUND_LABEL = "Breach of the tenancy (ground 1)";
    private static final String SAVE_AND_RETURN_LABEL_ID =
        "secureOrFlexibleGroundsForPossessionReasons-saveAndReturn";
    private static final ShowCondition SHOW_BREACH_OF_TENANCY_TEXTAREA =
        when(PCSCase::getShowBreachOfTenancyTextarea).is(YesOrNo.YES);
    private static final ShowCondition SHOW_REASONS_FOR_GROUNDS_PAGE =
        when(PCSCase::getShowReasonsForGroundsPage).is(YesOrNo.YES);
    private static final ShowCondition IS_ENGLAND =
        when(PCSCase::getLegislativeCountry).is(LegislativeCountry.ENGLAND);
    private static final ShowCondition IS_FLEXIBLE_TENANCY =
        when(PCSCase::getTenancyLicenceDetails, TenancyLicenceDetails::getTypeOfTenancyLicence).is(FLEXIBLE_TENANCY);
    private static final ShowCondition.NamedFieldCondition MANDATORY_GROUNDS = when(
        PCSCase::getSecureOrFlexiblePossessionGrounds,
        SecureOrFlexiblePossessionGrounds::getSecureOrFlexibleMandatoryGrounds
    );
    private static final ShowCondition.NamedFieldCondition MANDATORY_GROUNDS_ALT = when(
        PCSCase::getSecureOrFlexiblePossessionGrounds,
        SecureOrFlexiblePossessionGrounds::getSecureOrFlexibleMandatoryGroundsAlt
    );
    private static final ShowCondition.NamedFieldCondition DISCRETIONARY_GROUNDS_ALT = when(
        PCSCase::getSecureOrFlexiblePossessionGrounds,
        SecureOrFlexiblePossessionGrounds::getSecureOrFlexibleDiscretionaryGroundsAlt
    );

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("secureOrFlexibleGroundsForPossessionReasons", this::midEvent)
            .pageLabel("Reasons for possession")
            .showWhen(anyOf(
                when(PCSCase::getTenancyLicenceDetails, TenancyLicenceDetails::getTypeOfTenancyLicence)
                    .is(SECURE_TENANCY),
                allOf(
                    IS_FLEXIBLE_TENANCY,
                    IS_ENGLAND,
                    anyOf(SHOW_BREACH_OF_TENANCY_TEXTAREA, SHOW_REASONS_FOR_GROUNDS_PAGE))))
            .labelWhen("possessionReasons-lineSeparator","---")
            .complex(PCSCase::getSecureOrFlexibleGroundsReasons)

            // Discretionary grounds
            .labelWhen("possessionReasons-breachOfTenancyGround-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Breach of the tenancy (ground 1)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                Why are you making a claim for possession under this ground?</h3>
                """, allOf(
                    SHOW_BREACH_OF_TENANCY_TEXTAREA,
                    contains(DISCRETIONARY_GROUNDS, RENT_ARREARS_OR_BREACH_OF_TENANCY)))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getBreachOfTenancyGround,
                allOf(
                    SHOW_BREACH_OF_TENANCY_TEXTAREA,
                    contains(DISCRETIONARY_GROUNDS, RENT_ARREARS_OR_BREACH_OF_TENANCY)))

            .labelWhen("possessionReasons-nuisanceOrImmoralUse-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">
                    Nuisance, annoyance, illegal or immoral use of the property (ground 2)
                 </h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   contains(
                    DISCRETIONARY_GROUNDS,
                    NUISANCE_OR_IMMORAL_USE
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getNuisanceOrImmoralUseGround,
                       contains(
                    DISCRETIONARY_GROUNDS,
                    NUISANCE_OR_IMMORAL_USE
                ))

            .labelWhen("possessionReasons-domesticViolence-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Domestic violence (ground 2A)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   contains(
                    DISCRETIONARY_GROUNDS,
                    DOMESTIC_VIOLENCE
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getDomesticViolenceGround,
                       contains(
                    DISCRETIONARY_GROUNDS,
                    DOMESTIC_VIOLENCE
                ))

            .labelWhen("possessionReasons-riotOffence-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Offence during a riot (ground 2ZA)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   contains(
                    DISCRETIONARY_GROUNDS,
                    RIOT_OFFENCE
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getRiotOffenceGround,
                       contains(
                    DISCRETIONARY_GROUNDS,
                    RIOT_OFFENCE
                ))

            .labelWhen("possessionReasons-propertyDeterioration-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">
                    Deterioration in the condition of the property (ground 3)
                    </h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   contains(
                    DISCRETIONARY_GROUNDS,
                    PROPERTY_DETERIORATION
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getPropertyDeteriorationGround,
                       contains(
                    DISCRETIONARY_GROUNDS,
                    PROPERTY_DETERIORATION
                ))

            .labelWhen("possessionReasons-furnitureDeterioration-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Deterioration of furniture (ground 4)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   contains(
                    DISCRETIONARY_GROUNDS,
                    FURNITURE_DETERIORATION
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getFurnitureDeteriorationGround,
                       contains(
                    DISCRETIONARY_GROUNDS,
                    FURNITURE_DETERIORATION
                ))

            .labelWhen("possessionReasons-tenancyObtainedByFalseStatement-label",
                   """
                  <h2 class="govuk-heading-l" tabindex="0">Tenancy obtained by false statement (ground 5)</h2>
                  <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                  </h3>
                  """,
                   contains(
                    DISCRETIONARY_GROUNDS,
                    TENANCY_OBTAINED_BY_FALSE_STATEMENT
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getTenancyByFalseStatementGround,
                       contains(
                    DISCRETIONARY_GROUNDS,
                    TENANCY_OBTAINED_BY_FALSE_STATEMENT
                ))

            .labelWhen("possessionReasons-premiumPaidMutualExchange-label",
                   """
                  <h2 class="govuk-heading-l" tabindex="0">
                    Premium paid in connection with mutual exchange (ground 6)
                  </h2>
                  <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                  </h3>
                  """,
                   contains(
                    DISCRETIONARY_GROUNDS,
                    PREMIUM_PAID_MUTUAL_EXCHANGE
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getPremiumMutualExchangeGround,
                       contains(
                    DISCRETIONARY_GROUNDS,
                    PREMIUM_PAID_MUTUAL_EXCHANGE
                ))

            .labelWhen("possessionReasons-unreasonableConductTiedAccommodation-label",
                   """
                  <h2 class="govuk-heading-l" tabindex="0">
                    Unreasonable conduct in tied accommodation (ground 7)
                  </h2>
                  <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                  </h3>
                  """,
                   contains(
                    DISCRETIONARY_GROUNDS,
                    UNREASONABLE_CONDUCT_TIED_ACCOMMODATION
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getUnreasonableConductGround,
                       contains(
                    DISCRETIONARY_GROUNDS,
                    UNREASONABLE_CONDUCT_TIED_ACCOMMODATION
                ))

            .labelWhen("possessionReasons-refusalToMoveBack-label",
                   """
                <h2 class="govuk-heading-l" tabindex="0">
                    Refusal to move back to main home after works completed (ground 8)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """,
                   contains(
                    DISCRETIONARY_GROUNDS,
                    REFUSAL_TO_MOVE_BACK
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getRefusalToMoveBackGround,
                       contains(
                    DISCRETIONARY_GROUNDS,
                    REFUSAL_TO_MOVE_BACK
                ))

            // Mandatory grounds
            .labelWhen("possessionReasons-antiSocial-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Antisocial behaviour</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   contains(
                    MANDATORY_GROUNDS,
                    ANTI_SOCIAL
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getAntiSocialGround,
                       contains(
                    MANDATORY_GROUNDS,
                    ANTI_SOCIAL
                ))

            // Mandatory grounds (if alternative accommodation is available)
            .labelWhen("possessionReasons-overcrowding-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Overcrowding (ground 9)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   contains(
                    MANDATORY_GROUNDS_ALT,
                    OVERCROWDING
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getOvercrowdingGround,
                       contains(
                    MANDATORY_GROUNDS_ALT,
                    OVERCROWDING
                ))

            .labelWhen("possessionReasons-landlordWorks-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Landlord’s works (ground 10)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   contains(
                    MANDATORY_GROUNDS_ALT,
                    LANDLORD_WORKS
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getLandlordWorksGround,
                       contains(
                    MANDATORY_GROUNDS_ALT,
                    LANDLORD_WORKS
                ))

            .labelWhen("possessionReasons-propertySold-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Property sold for redevelopment (ground 10A)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   contains(
                    MANDATORY_GROUNDS_ALT,
                    PROPERTY_SOLD
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getPropertySoldGround,
                       contains(
                    MANDATORY_GROUNDS_ALT,
                    PROPERTY_SOLD
                ))

            .labelWhen("possessionReasons-charitableLandlord-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Charitable landlords (ground 11)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   contains(
                    MANDATORY_GROUNDS_ALT,
                    CHARITABLE_LANDLORD
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getCharitableLandlordGround,
                       contains(
                    MANDATORY_GROUNDS_ALT,
                    CHARITABLE_LANDLORD
                ))

            //Discretionary grounds (if alternative accommodation is available)
            .labelWhen("possessionReasons-tiedAccommodationNeededForEmployee-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">
                    Tied accommodation needed for another employee (ground 12)
                 </h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   contains(
                    DISCRETIONARY_GROUNDS_ALT,
                    TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getTiedAccommodationGround,
                       contains(
                    DISCRETIONARY_GROUNDS_ALT,
                    TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE
                ))

            .labelWhen("possessionReasons-adaptedAccommodation-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Adapted accommodation (ground 13)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   contains(
                    DISCRETIONARY_GROUNDS_ALT,
                    ADAPTED_ACCOMMODATION
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getAdaptedAccommodationGround,
                       contains(
                    DISCRETIONARY_GROUNDS_ALT,
                    ADAPTED_ACCOMMODATION
                ))

            .labelWhen("possessionReasons-housingAssociationSpecialCircumstances-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">
                    Housing association special circumstances accommodation (ground 14)
                 </h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   contains(
                    DISCRETIONARY_GROUNDS_ALT,
                    HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getHousingAssocSpecialGround,
                   contains(
                    DISCRETIONARY_GROUNDS_ALT,
                    HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES
                ))

            .labelWhen("possessionReasons-specialNeedsAccommodation-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Special needs accommodation (ground 15)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   contains(
                    DISCRETIONARY_GROUNDS_ALT,
                    SPECIAL_NEEDS_ACCOMMODATION
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getSpecialNeedsAccommodationGround,
                       contains(
                    DISCRETIONARY_GROUNDS_ALT,
                    SPECIAL_NEEDS_ACCOMMODATION
                ))

            .labelWhen("possessionReasons-underOccupyingAfterSuccession-label",
                   """
                 <h2 class="govuk-heading-l" tabindex="0">Under occupying after succession (ground 15A)</h2>
                 <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                 </h3>
                 """,
                   contains(
                    DISCRETIONARY_GROUNDS_ALT,
                    UNDER_OCCUPYING_AFTER_SUCCESSION
                ))
            .mandatoryWhen(SecureOrFlexibleGroundsReasons::getUnderOccupancySuccessionGround,
                       contains(
                    DISCRETIONARY_GROUNDS_ALT,
                    UNDER_OCCUPYING_AFTER_SUCCESSION
                ))
            .done()
                .readonly(PCSCase::getShowBreachOfTenancyTextarea,NEVER_SHOW)
                .readonly(PCSCase::getShowReasonsForGroundsPage,NEVER_SHOW)
                .labelWhen(SAVE_AND_RETURN_LABEL_ID, CommonPageContent.SAVE_AND_RETURN);

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
                SecureOrFlexibleDiscretionaryGrounds.NUISANCE_OR_IMMORAL_USE.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getDomesticViolenceGround(),
                SecureOrFlexibleDiscretionaryGrounds.DOMESTIC_VIOLENCE.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getRiotOffenceGround(),
                SecureOrFlexibleDiscretionaryGrounds.RIOT_OFFENCE.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getPropertyDeteriorationGround(),
                SecureOrFlexibleDiscretionaryGrounds.PROPERTY_DETERIORATION.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getFurnitureDeteriorationGround(),
                SecureOrFlexibleDiscretionaryGrounds.FURNITURE_DETERIORATION.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getTenancyByFalseStatementGround(),
                SecureOrFlexibleDiscretionaryGrounds.TENANCY_OBTAINED_BY_FALSE_STATEMENT.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getPremiumMutualExchangeGround(),
                SecureOrFlexibleDiscretionaryGrounds.PREMIUM_PAID_MUTUAL_EXCHANGE.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getUnreasonableConductGround(),
                SecureOrFlexibleDiscretionaryGrounds.UNREASONABLE_CONDUCT_TIED_ACCOMMODATION.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getRefusalToMoveBackGround(),
                SecureOrFlexibleDiscretionaryGrounds.REFUSAL_TO_MOVE_BACK.getLabel(),
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
                OVERCROWDING.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getLandlordWorksGround(),
                LANDLORD_WORKS.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getPropertySoldGround(),
                PROPERTY_SOLD.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getCharitableLandlordGround(),
                CHARITABLE_LANDLORD.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }

    private TextAreaValidationService.FieldValidation[] buildDiscretionaryGroundsAlternativeAccommValidations(
            SecureOrFlexibleGroundsReasons grounds) {
        return new TextAreaValidationService.FieldValidation[] {
            TextAreaValidationService.FieldValidation.of(
                grounds.getTiedAccommodationGround(),
                TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getAdaptedAccommodationGround(),
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.ADAPTED_ACCOMMODATION.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getHousingAssocSpecialGround(),
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm
                    .HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSpecialNeedsAccommodationGround(),
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.SPECIAL_NEEDS_ACCOMMODATION.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getUnderOccupancySuccessionGround(),
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.UNDER_OCCUPYING_AFTER_SUCCESSION.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }
}
