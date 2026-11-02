package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sdp_claim_snapshot")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SdpClaimSnapshotEntity {

    @Id
    @Column(name = "claim_id")
    private UUID claimId;

    @Column(name = "snapshot_version", nullable = false)
    private Integer snapshotVersion;

    @Column(name = "snapshot_hash", nullable = false)
    private String snapshotHash;

    @Column(name = "exported_at", nullable = false)
    private LocalDateTime exportedAt;
}
