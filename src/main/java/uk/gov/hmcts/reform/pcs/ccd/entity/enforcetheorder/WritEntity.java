package uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;


import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "enf_writ")
@Getter
@Setter
public class WritEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "enf_case_id", nullable = false)
    @JsonBackReference
    private EnforcementOrderEntity enforcementOrder;

    // NameAndAddressForEviction
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo correctNameAndAddress;

    // Land Registry fees
    /**
     * Not needed in R1.4
     *
     * @deprecated This field is retained only for backwards compatibility.
     */
    @Deprecated(since = "R1.4")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo haveLandRegistryFeesBeenPaid;

    /**
     * Not needed in R1.4
     *
     * @deprecated This field is retained only for backwards compatibility.
     */
    @Deprecated(since = "R1.4")
    private BigDecimal amountOfLandRegistryFees;


    // Direct fields
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo hasHiredHighCourtEnforcementOfficer;

    private String hceoDetails;

    // LegalCosts
    /**
     * Not needed in R1.4
     *
     * @deprecated This field is retained only for backwards compatibility.
     */
    @Deprecated(since = "R1.4")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo areLegalCostsToBeClaimed;

    private BigDecimal amountOfLegalCosts;

    // MoneyOwedByDefendants
    private BigDecimal amountOwed;

    // Direct fields
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo hasClaimTransferredToHighCourt;

    @Enumerated(EnumType.STRING)
    private LanguageUsed languageUsed;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant created;

    //Payment fields
    /**
     * Not needed in R1.4
     *
     * @deprecated These fields are retained only for backwards compatibility.
     */
    @Deprecated(since = "R1.4")
    private String repaymentChoice;
    /**
     * Not needed in R1.4
     *
     * @deprecated This field is retained only for backwards compatibility.
     */
    @Deprecated(since = "R1.4")
    private BigDecimal amountOfRepaymentCosts;
    /**
     * Not needed in R1.4
     *
     * @deprecated This field is retained only for backwards compatibility.
     */
    @Deprecated(since = "R1.4")
    private String repaymentSummaryMarkdown;

}
