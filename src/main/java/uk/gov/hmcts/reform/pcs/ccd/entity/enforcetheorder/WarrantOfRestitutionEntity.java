package uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder;

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
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "enf_warrant_of_restitution")
@Getter
@Setter
public class WarrantOfRestitutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "enf_case_id", nullable = false)
    @JsonBackReference
    private EnforcementOrderEntity enforcementOrder;

    @Enumerated(EnumType.STRING)
    private LanguageUsed languageUsed;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant created;
}
