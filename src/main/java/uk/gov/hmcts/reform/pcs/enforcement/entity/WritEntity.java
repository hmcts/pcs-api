package uk.gov.hmcts.reform.pcs.enforcement.entity;

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
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;



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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesNoNotSure defendantInBreathingSpace;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo defendantMissedPayments;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo correctNameAndAddress;

    @SuppressWarnings("java:S1133")
    @Deprecated
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo haveLandRegistryFeesBeenPaid;

    @SuppressWarnings("java:S1133")
    @Deprecated
    private BigDecimal amountOfLandRegistryFees;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo hasHiredHighCourtEnforcementOfficer;

    private String hceoDetails;

    @SuppressWarnings("java:S1133")
    @Deprecated
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo areLegalCostsToBeClaimed;

    private BigDecimal amountOfLegalCosts;

    private BigDecimal amountOwed;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo hasClaimTransferredToHighCourt;

    @Enumerated(EnumType.STRING)
    private LanguageUsed languageUsed;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant created;

    @SuppressWarnings("java:S1133")
    @Deprecated
    private String repaymentChoice;

    @SuppressWarnings("java:S1133")
    @Deprecated
    private BigDecimal amountOfRepaymentCosts;

    @SuppressWarnings("java:S1133")
    @Deprecated
    private String repaymentSummaryMarkdown;

}
