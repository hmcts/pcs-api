package uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.hmcts.reform.pcs.ccd.domain.AcceptReject;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RejectionReason;

import java.time.LocalDateTime;

@Entity
@Table(name = "enf_application_decision")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnforcementApplicationDecisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private AcceptReject decision;

    @Enumerated(EnumType.STRING)
    private RejectionReason rejectionReason;

    private String rejectionReasonAdditionalDetails;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime lastModifiedAt;

}
