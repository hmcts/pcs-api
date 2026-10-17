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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
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

    @OneToOne(mappedBy = "enforcementOrder", cascade = ALL)
    private WarrantOfRestitutionEntity warrantOfRestitutionDetails;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "sot_id", nullable = false)
    @JsonBackReference
    private StatementOfTruthEntity statementOfTruth;

    @OneToMany(mappedBy = "enfCase", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    @Builder.Default
    @JsonManagedReference
    private List<SelectedDefendantEntity> selectedDefendants = new ArrayList<>();

    @OneToMany(fetch = LAZY, cascade = ALL, orphanRemoval = true)
    @JoinColumn(name = "enf_case_id")
    @Builder.Default
    private List<DocumentEntity> documents = new ArrayList<>();

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
