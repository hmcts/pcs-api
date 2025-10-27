package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;

@Component
@Slf4j
public class NoRentArrearsGroundsForPossessionReason implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("noRentArrearsGroundsForPossessionReason")
            .pageLabel("Reasons for possession")
            .showCondition("groundsForPossession=\"No\" "
                               + "AND typeOfTenancyLicence=\"ASSURED_TENANCY\""
                               + " AND showNoRentArrearsGroundReasonPage=\"Yes\"")
            .label("noRentArrearsOptions-lineSeparator", "---")
            .complex(PCSCase::getNoRentArrearsReasonForGrounds)
            // Ground 1
            .label(
                "noRentArrearsOptions-ownerOccupier-label",
                """
                    <h2 class="govuk-heading-l">Owner occupier (ground 1)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"OWNER_OCCUPIER\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getOwnerOccupierTextArea,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"OWNER_OCCUPIER\""
            )
            // Ground 2
            .label(
                "noRentArrearsOptions-repossessionByLender-label",
                """
                    <h2 class="govuk-heading-l">Repossession by the landlord's mortgage lender (ground 2)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"REPOSSESSION_BY_LENDER\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getRepossessionByLenderTextArea,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"REPOSSESSION_BY_LENDER\""
            )
            // Ground 3
            .label(
                "noRentArrearsOptions-holidayLet-label",
                """
                    <h2 class="govuk-heading-l">Holiday let (ground 3)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"HOLIDAY_LET\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getHolidayLetTextArea,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"HOLIDAY_LET\""
            )
            // Ground 4
            .label(
                "noRentArrearsOptions-studentLet-label",
                """
                    <h2 class="govuk-heading-l">Student let (ground 4)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"STUDENT_LET\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getStudentLetTextArea,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"STUDENT_LET\""
            )
            // Ground 5
            .label(
                "noRentArrearsOptions-ministerOfReligion-label",
                """
                    <h2 class="govuk-heading-l">Property required for minister of religion (ground 5)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"MINISTER_OF_RELIGION\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getMinisterOfReligionTextArea,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"MINISTER_OF_RELIGION\""
            )
            // Ground 6
            .label(
                "noRentArrearsOptions-redevelopment-label",
                """
                    <h2 class="govuk-heading-l">Property required for redevelopment (ground 6)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"REDEVELOPMENT\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getRedevelopmentTextArea,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"REDEVELOPMENT\""
            )
            // Ground 7
            .label(
                "noRentArrearsOptions-deathOfTenant-label",
                """
                    <h2 class="govuk-heading-l">Death of the tenant (ground 7)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"DEATH_OF_TENANT\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getDeathOfTenantTextArea,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"DEATH_OF_TENANT\""
            )
            // Ground 7A
            .label(
                "noRentArrearsOptions-antisocialBehaviour-label",
                """
                    <h2 class="govuk-heading-l">Antisocial behaviour (ground 7A)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"ANTISOCIAL_BEHAVIOUR\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getAntisocialBehaviourTextArea,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"ANTISOCIAL_BEHAVIOUR\""
            )
            // Ground 7B
            .label(
                "noRentArrearsOptions-noRightToRent-label",
                """
                    <h2 class="govuk-heading-l">Tenant does not have a right to rent (ground 7B)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"NO_RIGHT_TO_RENT\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getNoRightToRentTextArea,
                "noRentArrearsMandatoryGroundsOptionsCONTAINS\"NO_RIGHT_TO_RENT\""
            )
            // Ground 9
            .label(
                "noRentArrearsOptions-suitableAccom-label",
                """
                    <h2 class="govuk-heading-l">Suitable alternative accommodation (ground 9)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"SUITABLE_ACCOM\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getSuitableAccomTextArea,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"SUITABLE_ACCOM\""
            )
            // Ground 12
            .label(
                "noRentArrearsOptions-breachOfTenancyConditions-label",
                """
                    <h2 class="govuk-heading-l">Breach of tenancy conditions (ground 12)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"BREACH_OF_TENANCY_CONDITIONS\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getBreachOfTenancyConditionsTextArea,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"BREACH_OF_TENANCY_CONDITIONS\""
            )
            // Ground 13
            .label(
                "noRentArrearsOptions-propertyDeterioration-label",
                """
                    <h2 class="govuk-heading-l">Deterioration in the condition of the property (ground 13)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"PROPERTY_DETERIORATION\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getPropertyDeteriorationTextArea,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"PROPERTY_DETERIORATION\""
            )
            // Ground 14
            .label(
                "noRentArrearsOptions-nuisanceOrIllegalUse-label",
                """
                    <h2 class="govuk-heading-l">Nuisance, annoyance, illegal or immoral use of the property
                    (ground 14)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"NUISANCE_OR_ILLEGAL_USE\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getNuisanceOrIllegalUseTextArea,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"NUISANCE_OR_ILLEGAL_USE\""
            )
            // Ground 14A
            .label(
                "noRentArrearsOptions-domesticViolence-label",
                """
                    <h2 class="govuk-heading-l">Domestic violence (ground 14A)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"DOMESTIC_VIOLENCE\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getDomesticViolenceTextArea,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"DOMESTIC_VIOLENCE\""
            )
            // Ground 14ZA
            .label(
                "noRentArrearsOptions-offenceDuringRiot-label",
                """
                    <h2 class="govuk-heading-l">Offence during a riot (ground 14ZA)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"OFFENCE_DURING_RIOT\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getOffenceDuringRiotTextArea,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"OFFENCE_DURING_RIOT\""
            )
            // Ground 15
            .label(
                "noRentArrearsOptions-furnitureDeterioration-label",
                """
                    <h2 class="govuk-heading-l">Deterioration of furniture (ground 15)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"FURNITURE_DETERIORATION\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getFurnitureDeteriorationTextArea,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"FURNITURE_DETERIORATION\""
            )
            // Ground 16
            .label(
                "noRentArrearsOptions-landlordEmployee-label",
                """
                    <h2 class="govuk-heading-l">Employee of the landlord (ground 16)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"LANDLORD_EMPLOYEE\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getLandlordEmployeeTextArea,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"LANDLORD_EMPLOYEE\""
            )
            // Ground 17
            .label(
                "noRentArrearsOptions-falseStatement-label",
                """
                    <h2 class="govuk-heading-l">Tenancy obtained by false statement (ground 17)</h2>
                    <h3 class="govuk-heading-m">Why are you making a claim for possession under this ground?</h3>
                    """,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"FALSE_STATEMENT\""
            )
            .mandatory(
                NoRentArrearsReasonForGrounds::getFalseStatementTextArea,
                "noRentArrearsDiscretionaryGroundsOptionsCONTAINS\"FALSE_STATEMENT\""
            );
    }
}
