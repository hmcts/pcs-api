package uk.gov.hmcts.reform.pcs.enforcement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;

import java.time.Instant;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

/**
 * A warrant return a bailiff recorded against an enforcement (HDPI-7347). An enforcement keeps every
 * return it has had, so it can be interim, interim, final, then reopened. Returns are never edited.
 */
@Entity
@Table(name = "enf_warrant_return")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantReturnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "enf_case_id", nullable = false, updatable = false)
    private EnforcementOrderEntity enforcementOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_type", nullable = false, updatable = false)
    private WarrantReturnType returnType;

    /**
     * Set for a final return only.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", updatable = false)
    private WarrantReturnOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_name", nullable = false, updatable = false)
    private WarrantReturnName returnName;

    /**
     * When the eviction took place. Set for a successful eviction only.
     */
    @Column(name = "eviction_at", updatable = false)
    private Instant evictionAt;

    @Column(name = "additional_details", updatable = false, length = 6800)
    private String additionalDetails;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    /**
     * The IDAM user id of the bailiff who recorded the return.
     */
    @Column(name = "recorded_by", updatable = false)
    private String recordedBy;
}
