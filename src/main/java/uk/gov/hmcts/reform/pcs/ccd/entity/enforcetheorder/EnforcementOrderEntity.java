package uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;

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

    @OneToOne(mappedBy = "enforcementOrder")
    private WarrantEntity warrantDetails;

    @Column(name = "bailiff_date")
    private LocalDateTime bailiffDate;

    @OneToOne(mappedBy = "enforcementOrder")
    private WarrantOfRestitutionEntity warrantOfRestitutionDetails;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "sot_id", nullable = false)
    @JsonBackReference
    private StatementOfTruthEntity statementOfTruth;

    @OneToMany(mappedBy = "enfCase", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    @Builder.Default
    @JsonManagedReference
    private List<DocumentEntity> documents = new ArrayList<>();

    public void addDocuments(List<DocumentEntity> documents) {
        for (DocumentEntity document : documents) {
            document.setEnfCase(this);
            this.documents.add(document);
        }
    }
}
