package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdCircumstances {

    @CCD
    private VerticalYesNo dependantChildren;

    @CCD
    private VerticalYesNo universalCredit;

    @CCD
    private LocalDate ucApplicationDate;

    @CCD
    private VerticalYesNo priorityDebts;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal debtTotal;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal debtContribution;

    @CCD
    private RecurrenceFrequency debtContributionFrequency;

    @CCD
    private VerticalYesNo shareAdditionalCircumstances;

    @CCD(max = 500)
    private String additionalCircumstancesDetails;

    @CCD
    private VerticalYesNo exceptionalHardship;

    @CCD(max = 500)
    private String exceptionalHardshipDetails;

    @CCD(max = 500)
    private String dependantChildrenDetails;

    @CCD
    private VerticalYesNo otherDependants;

    @CCD(max = 500)
    private String otherDependantDetails;

    @CCD
    private VerticalYesNo otherTenants;

    @CCD(max = 500)
    private String otherTenantsDetails;

    @CCD
    private YesNoNotSure alternativeAccommodation;

    @CCD
    private LocalDate alternativeAccommodationTransferDate;

}
