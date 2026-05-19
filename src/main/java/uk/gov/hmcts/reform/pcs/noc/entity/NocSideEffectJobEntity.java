package uk.gov.hmcts.reform.pcs.noc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "noc_side_effect_job")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NocSideEffectJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long caseReference;

    private UUID partyId;

    @Enumerated(EnumType.STRING)
    private NocSideEffectJobType type;

    @Enumerated(EnumType.STRING)
    private NocSideEffectJobStatus status;

    private String userId;

    private String organisationId;

    private String caseRole;

    private String email;

    private String detail;

    private String idempotencyKey;

    private int attempts;

    private String lastError;

    private LocalDateTime createdAt;

    private LocalDateTime availableAt;

    private LocalDateTime completedAt;
}
