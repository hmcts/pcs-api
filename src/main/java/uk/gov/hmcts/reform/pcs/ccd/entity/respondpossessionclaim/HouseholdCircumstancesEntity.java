package uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;

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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo universalCredit;

    private LocalDate ucApplicationDate;

    @OneToOne(cascade = ALL, mappedBy = "householdCircumstances", orphanRemoval = true)
    @JsonManagedReference
    private RegularIncomeEntity regularIncomeEntity;

    public void setRegularIncomeEntity(RegularIncomeEntity regularIncomeEntity) {
        if (this.regularIncomeEntity != null) {
            this.regularIncomeEntity.setHouseholdCircumstances(null);
        }
        this.regularIncomeEntity = regularIncomeEntity;
        if (this.regularIncomeEntity != null) {
            this.regularIncomeEntity.setHouseholdCircumstances(this);
        }
    }

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo priorityDebts;

    private BigDecimal debtTotal;

    private BigDecimal debtContribution;

    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency debtContributionFrequency;

    private String regularExpenses;

    private BigDecimal expenseAmount;

    @Enumerated(EnumType.STRING)
    private RentPaymentFrequency expenseFrequency;
}
