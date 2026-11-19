package uk.gov.hmcts.reform.pcs.bulkclaim.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * A claim submitted through the bulk API, tracked from receipt through validation, case creation and payment.
 */
@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bulk_claim_submission")
public class BulkClaimSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Integer version;

    private String organisationId;

    private String customerUniqueIdentifier;

    @Enumerated(EnumType.STRING)
    private BulkClaimKind claimKind;

    private String schemaVersion;

    private String requestHash;

    @Enumerated(EnumType.STRING)
    private BulkClaimSubmissionStatus status;

    private Integer statusCode;

    private Integer failReasonCode;

    private String failReason;

    @JdbcTypeCode(SqlTypes.JSON)
    private String validationErrors;

    @JdbcTypeCode(SqlTypes.JSON)
    private String stagedCaseData;

    @JdbcTypeCode(SqlTypes.JSON)
    private String stagedExtras;

    @JdbcTypeCode(SqlTypes.JSON)
    private String organisationSnapshot;

    private String pbaAccountNumber;

    private String paymentCustomerReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private PcsCaseEntity pcsCase;

    private Long caseReference;

    private String serviceRequestReference;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BulkClaimPaymentStatus paymentStatus = BulkClaimPaymentStatus.NOT_STARTED;

    private String paymentFailureReason;

    @Builder.Default
    private boolean noticeOfActingRequired = false;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant lastModified;

}
