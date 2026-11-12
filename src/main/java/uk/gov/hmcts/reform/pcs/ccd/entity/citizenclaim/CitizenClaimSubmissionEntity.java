package uk.gov.hmcts.reform.pcs.ccd.entity.citizenclaim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

/**
 * The citizen's answers exactly as submitted, kept alongside the mapped claim so that answers
 * with no domain column yet are not lost.
 */
@Entity
@Table(name = "citizen_claim_submission")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenClaimSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private ClaimEntity claim;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "submitted_claim", nullable = false)
    private String submittedClaim;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
