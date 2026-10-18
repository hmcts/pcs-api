package uk.gov.hmcts.reform.pcs.enforcement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.enforcement.domain.ClaimantRiskAnswers;

import java.time.Instant;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

/**
 * The claimant's risk assessment answers after they changed them in "Confirm the eviction details"
 * (HDPI-7552). Each row holds every answer, not just the changed ones. Rows are never edited.
 */
@Entity
@Table(name = "enf_risk_assessment_update")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessmentUpdateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "enf_case_id", nullable = false, updatable = false)
    private EnforcementOrderEntity enforcementOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers", nullable = false, updatable = false)
    private ClaimantRiskAnswers answers;

    @Column(name = "updated_at", nullable = false, updatable = false)
    private Instant updatedAt;
}
