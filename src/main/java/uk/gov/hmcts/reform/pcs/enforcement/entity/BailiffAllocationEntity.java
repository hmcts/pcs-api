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

import java.time.Instant;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

/**
 * A bailiff an enforcement is, or was, allocated to (HDPI-8189). The active allocation has no end time.
 */
@Entity
@Table(name = "enf_bailiff_allocation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BailiffAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "enf_case_id", nullable = false, updatable = false)
    private EnforcementOrderEntity enforcementOrder;

    @Column(name = "bailiff_user_id", nullable = false, updatable = false)
    private String bailiffUserId;

    @Column(name = "bailiff_name", updatable = false)
    private String bailiffName;

    @Column(name = "bailiff_email", updatable = false)
    private String bailiffEmail;

    @Column(name = "allocated_by_user_id", updatable = false)
    private String allocatedByUserId;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    public boolean isActive() {
        return endedAt == null;
    }
}
