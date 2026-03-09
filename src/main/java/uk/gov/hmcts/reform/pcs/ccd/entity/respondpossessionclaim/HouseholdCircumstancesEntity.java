package uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "household_circumstances")
public class HouseholdCircumstancesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    private DefendantResponseEntity defendantResponse;

    @Enumerated(EnumType.STRING)
    private YesOrNo dependantChildren;

    private String dependantChildrenDetails;

    @Enumerated(EnumType.STRING)
    private YesOrNo otherDependants;

    private String otherDependantDetails;

    @Enumerated(EnumType.STRING)
    private YesOrNo otherTenants;

    private String otherTenantsDetails;

    @Enumerated(EnumType.STRING)
    private YesNoNotSure alternativeAccommodation;

    private LocalDate alternativeAccommodationTransferDate;

    @Enumerated(EnumType.STRING)
    private YesOrNo shareAdditionalCircumstances;

    private String additionalCircumstancesDetails;

    @Enumerated(EnumType.STRING)
    private YesOrNo exceptionalHardship;

    private String exceptionalHardshipDetails;

    @Enumerated(EnumType.STRING)
    private YesOrNo shareIncomeExpenseDetails;

    private String regularIncome;

    @Enumerated(EnumType.STRING)
    private YesOrNo universalCredit;

    private LocalDate ucApplicationDate;

    @Enumerated(EnumType.STRING)
    private YesOrNo priorityDebts;

    private BigDecimal debtTotal;

    private String debtContribution;

    private String debtContributionFrequency;

    private String regularExpenses;

    private BigDecimal expenseAmount;

    private String expenseFrequency;
}
