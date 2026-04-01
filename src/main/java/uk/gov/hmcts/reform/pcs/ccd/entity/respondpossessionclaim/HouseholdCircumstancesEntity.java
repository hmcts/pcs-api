package uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "household_circumstances")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdCircumstancesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defendant_response_id")
    @JsonBackReference
    private DefendantResponseEntity defendantResponse;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo dependantChildren;

    private String dependantChildrenDetails;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo otherDependants;

    private String otherDependantDetails;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo otherTenants;

    private String otherTenantsDetails;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesNoNotSure alternativeAccommodation;

    private LocalDate alternativeAccommodationTransferDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo shareAdditionalCircumstances;

    private String additionalCircumstancesDetails;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo exceptionalHardship;

    private String exceptionalHardshipDetails;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo shareIncomeExpenseDetails;

    private String regularIncome;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo universalCredit;

    private LocalDate ucApplicationDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo priorityDebts;

    private BigDecimal debtTotal;

    private BigDecimal debtContribution;

    private String debtContributionFrequency;

    private String regularExpenses;

    //Columns to drop
    private BigDecimal expenseAmount;
    private String expenseFrequency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo householdBills;

    private BigDecimal householdBillsAmount;

    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency householdBillsFrequency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo loanPayments;

    private BigDecimal loanPaymentsAmount;

    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency loanPaymentsFrequency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo childSpousalMaintenance;

    private BigDecimal childSpousalMaintenanceAmount;

    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency childSpousalMaintenanceFrequency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo mobilePhone;

    private BigDecimal mobilePhoneAmount;

    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency mobilePhoneFrequency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo groceryShopping;

    private BigDecimal groceryShoppingAmount;

    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency groceryShoppingFrequency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo fuelParkingTransport;

    private BigDecimal fuelParkingTransportAmount;

    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency fuelParkingTransportFrequency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo schoolCosts;

    private BigDecimal schoolCostsAmount;

    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency schoolCostsFrequency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo clothing;

    private BigDecimal clothingAmount;

    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency clothingFrequency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo otherExpenses;

    private BigDecimal otherExpensesAmount;

    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency otherExpensesFrequency;
}
