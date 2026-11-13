package uk.gov.hmcts.reform.pcs.ccd.entity.breathingspace;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "breathing_space")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BreathingSpaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    @JsonBackReference
    private ClaimEntity claim;

    private String referenceNumber;

    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    private BreathingSpaceType type;

    private LocalDate expectedEndDate;

    @Column(name = "created_at",
        nullable = false,
        insertable = false,
        updatable = false)
    private LocalDateTime createdAt;

}
