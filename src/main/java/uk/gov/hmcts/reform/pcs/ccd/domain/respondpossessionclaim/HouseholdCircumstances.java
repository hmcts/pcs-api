package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.annotation.JacksonMoneyGBP;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdCircumstances {

    @CCD
    private YesOrNo dependantChildren;

    @CCD
    private YesOrNo universalCredit;

    @CCD
    private LocalDate ucApplicationDate;

    @CCD
    private YesOrNo priorityDebts;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal debtTotal;

    @CCD(typeOverride = FieldType.MoneyGBP)
    @JacksonMoneyGBP
    private BigDecimal debtContribution;

    @CCD
    private RecurrenceFrequency debtContributionFrequency;

    @CCD
    private YesOrNo shareAdditionalCircumstances;

    @CCD(max = 500)
    private String additionalCircumstancesDetails;

    @CCD
    private YesOrNo exceptionalHardship;

    @CCD(max = 500)
    private String exceptionalHardshipDetails;
    
    @CCD(max = 500)
    private String dependantChildrenDetails;

    @CCD
    private YesOrNo otherDependants;

    @CCD(max = 500)
    private String otherDependantDetails;

    @CCD
    private YesOrNo otherTenants;

    @CCD(max = 500)
    private String otherTenantsDetails;

    @CCD
    private YesNoNotSure alternativeAccommodation;

    @CCD
    private LocalDate alternativeAccommodationTransferDate;

}
