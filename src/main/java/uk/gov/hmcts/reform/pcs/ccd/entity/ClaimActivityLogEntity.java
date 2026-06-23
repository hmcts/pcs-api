package uk.gov.hmcts.reform.pcs.ccd.entity;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "claim_activity_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimActivityLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private PcsCaseEntity pcsCase;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "party_id")
    private PartyEntity party;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "activity_type", nullable = false)
    private ClaimActivityType activityType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private ClaimActivityStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
