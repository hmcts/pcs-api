package uk.gov.hmcts.reform.pcs.ccd.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import uk.gov.hmcts.reform.pcs.ccd.domain.bailiff.EvictionOutcome;
import uk.gov.hmcts.reform.pcs.ccd.domain.bailiff.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.ccd.domain.bailiff.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;

import java.time.Instant;

@Entity
@Table(name = "warrant_return")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantReturnEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "enf_warrant_id", nullable = false)
    @JsonBackReference
    private WarrantEntity warrantEntity;

    @Enumerated(EnumType.STRING)
    private WarrantReturnType returnType;

    @Enumerated(EnumType.STRING)
    private EvictionOutcome outcome;

    @Enumerated(EnumType.STRING)
    private WarrantReturnName returnName;

    private String additionalDetails;

    private Instant outcomeDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
