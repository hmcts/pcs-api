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

    /**
     * The enforcement's columns are written from the case event, the Payments callback and the scheduled tasks,
     * which take no common lock, so they are written under optimistic locking: a write from a copy loaded before
     * someone else's write fails with an ObjectOptimisticLockingFailureException instead of overwriting it.
     *
     * <p>Filing a document is excluded, see {@link #documents}: it writes no column of the enforcement, and
     * several documents are generated at once.</p>
     */
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

    /**
     * Change this only through EnforcementStatusTransitions, which enforces the allowed transitions.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EnforcementStatus status;

    @Column(name = "status_updated_at")
    private Instant statusUpdatedAt;

    /**
     * The warrant or writ number, given when the caseworker approves the application.
     */
    @Column(name = "enforcement_number")
    private String enforcementNumber;

    /**
     * When the caseworker approved the application. For a warrant this is when it was generated, which the free
     * repeat warrant period is measured from (RepeatWarrantFeeWaiver); for a writ it is the date of transfer to
     * the High Court.
     */
    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @OneToOne(mappedBy = "enforcementOrder", cascade = ALL)
    private WarrantOfRestitutionEntity warrantOfRestitutionDetails;

    /**
     * The statement of truth the claimant signed. Null when the application captured none: a draft submitted
     * before reaching the page has none.
     */
    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "sot_id")
    @JsonBackReference
    private StatementOfTruthEntity statementOfTruth;

    @OneToMany(mappedBy = "enfCase", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    @Builder.Default
    @JsonManagedReference
    private List<SelectedDefendantEntity> selectedDefendants = new ArrayList<>();

    /**
     * Filing a document changes no column of the enforcement, only the document's enf_case_id, so it is left
     * out of the optimistic locking: without this, an event that schedules two documents has db-scheduler
     * generate them on two threads, and the second to commit is refused as a stale write.
     *
     * <p>Batched, because the case view and batch print both read the documents of every enforcement on a claim,
     * which without it is a query per enforcement.
     */
    @OneToMany(fetch = LAZY, cascade = ALL, orphanRemoval = true)
    @JoinColumn(name = "enf_case_id")
    @OptimisticLock(excluded = true)
    @BatchSize(size = 20)
    @Builder.Default
    private List<DocumentEntity> documents = new ArrayList<>();

    /**
     * The eviction risk level the bailiff set (HDPI-7343). A high level always has a reason.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Column(name = "risk_level_reason", length = 6800)
    private String riskLevelReason;

    @Column(name = "risk_level_set_at")
    private Instant riskLevelSetAt;

    /**
     * Whether the court has given permission to dispense with service of the notice of eviction. Set when the
     * eviction is scheduled or rescheduled; null before that.
     */
    @Column(name = "dispense_with_service")
    private Boolean dispenseWithService;

    /**
     * When the bailiff served the notice of eviction, as a UK wall-clock time like {@link #bailiffDate}.
     */
    @Column(name = "notice_served_at")
    private LocalDateTime noticeServedAt;

    @Column(name = "notice_served_how")
    private String noticeServedHow;

    /**
     * Derived from the notice of eviction: true once it is served, false when service is dispensed with.
     * Change it only through EvictionScheduleService, which keeps it in step with the fields it derives from.
     */
    @Column(name = "defendants_can_see_enforcement")
    private boolean defendantsCanSeeEnforcement;

    /**
     * When the claimant submitted the application. Null for applications submitted before it was recorded.
     */
    @Column(name = "submitted_at")
    private Instant submittedAt;

    /**
     * The claimant's answer to whether they can attend the eviction on the current eviction date. Null until
     * they answer, and cleared when the eviction is rescheduled.
     */
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
