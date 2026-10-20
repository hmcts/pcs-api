package uk.gov.hmcts.reform.pcs.enforcement.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "enf_case")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnforcementOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version")
    private Integer version;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    @JsonBackReference
    private ClaimEntity claim;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enforcement_order")
    private EnforcementOrder enforcementOrder;

    @OneToOne(mappedBy = "enforcementOrder", cascade = ALL)
    private RiskProfileEntity riskProfileDetails;

    @OneToOne(mappedBy = "enforcementOrder", cascade = ALL)
    private WarrantEntity warrantDetails;

    @Column(name = "bailiff_date")
    private LocalDateTime bailiffDate;

    // Changed only through EnforcementStatusTransitions.
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EnforcementStatus status;

    @Column(name = "status_updated_at")
    private Instant statusUpdatedAt;

    @Column(name = "enforcement_number")
    private String enforcementNumber;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @OneToOne(mappedBy = "enforcementOrder", cascade = ALL)
    private WarrantOfRestitutionEntity warrantOfRestitutionDetails;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "sot_id")
    @JsonBackReference
    private StatementOfTruthEntity statementOfTruth;

    @OneToMany(mappedBy = "enfCase", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    @Builder.Default
    @JsonManagedReference
    private List<SelectedDefendantEntity> selectedDefendants = new ArrayList<>();

    /**
     * Excluded from optimistic locking so that documents generated concurrently for one enforcement are not
     * refused as stale writes.
     */
    @OneToMany(fetch = LAZY, cascade = ALL, orphanRemoval = true)
    @JoinColumn(name = "enf_case_id")
    @OptimisticLock(excluded = true)
    @BatchSize(size = 20)
    @Builder.Default
    private List<DocumentEntity> documents = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Column(name = "risk_level_reason", length = 6800)
    private String riskLevelReason;

    @Column(name = "risk_level_set_at")
    private Instant riskLevelSetAt;

    @Column(name = "dispense_with_service")
    private Boolean dispenseWithService;

    @Column(name = "notice_served_at")
    private LocalDateTime noticeServedAt;

    @Column(name = "notice_served_how")
    private String noticeServedHow;

    // Set only by EvictionScheduleService.
    @Column(name = "defendants_can_see_enforcement")
    private boolean defendantsCanSeeEnforcement;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "claimant_can_attend")
    private Boolean claimantCanAttend;

    public void addDocuments(List<DocumentEntity> documents) {
        this.documents.addAll(documents);
    }

    public void addSelectedDefendants(List<SelectedDefendantEntity> selectedDefendants) {
        for (SelectedDefendantEntity selectedDefendant : selectedDefendants) {
            selectedDefendant.setEnfCase(this);
            this.selectedDefendants.add(selectedDefendant);
        }
    }
}
