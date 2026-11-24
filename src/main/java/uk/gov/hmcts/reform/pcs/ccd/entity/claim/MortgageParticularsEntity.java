package uk.gov.hmcts.reform.pcs.ccd.entity.claim;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mortgage_particulars")
public class MortgageParticularsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id")
    @JsonBackReference
    private ClaimEntity claim;

    private LocalDate mortgageDate;

    private LocalDate defaultNoticeDate;

    private BigDecimal loanAmount;

    private String currentRepaymentTerms;

    private LocalDate fullRepaymentDate;

    private BigDecimal fullRepaymentAmount;

    private BigDecimal costsAndAdministration;

    private BigDecimal outstandingAmount;

    private BigDecimal startInterestRate;

    private BigDecimal preArrearsInterestRate;

    private BigDecimal claimStartInterestRate;

    private String interestedParty;

    private LocalDate tenancyDate;

    private LocalDate tenantNoticeServedDate;

    private BigDecimal totalArrears;

    private String recoverySteps;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo moneyJudgmentWanted;

    private String originalProviderName;

    private LocalDate originalProviderBoughtDate;

    private LocalDate originalProviderDefendantNotifiedDate;

    @Builder.Default
    @OneToMany(mappedBy = "mortgageParticulars", cascade = ALL, orphanRemoval = true)
    @JsonManagedReference
    @OrderBy("rank ASC")
    private List<MortgageAdditionalPaymentEntity> additionalPayments = new ArrayList<>();

    @UpdateTimestamp
    private LocalDateTime lastModified;

    public void addAdditionalPayment(MortgageAdditionalPaymentEntity additionalPayment) {
        additionalPayments.add(additionalPayment);
        additionalPayment.setMortgageParticulars(this);
    }

}
