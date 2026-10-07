package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.bulkprint.PackSendStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.bulkprint.PackType;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "bulk_print_queue")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPrintQueueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private PcsCaseEntity pcsCase;

    @Enumerated(EnumType.STRING)
    private PackType packType;

    @ManyToOne
    @JoinColumn(name = "claim_id")
    @JsonBackReference
    private ClaimEntity claim;

    @ManyToOne
    @JoinColumn(name = "counterclaim_id")
    @JsonBackReference
    private CounterClaimEntity counterClaim;

    @ManyToOne
    @JoinColumn(name = "general_application_id")
    @JsonBackReference
    private GenAppEntity genAppEntity;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    private PackSendStatus packSendStatus;

}
