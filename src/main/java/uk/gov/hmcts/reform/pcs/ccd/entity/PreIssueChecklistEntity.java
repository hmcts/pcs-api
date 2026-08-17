package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.PreIssueChecklistCode;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "pre_issue_checklist")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreIssueChecklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private PreIssueChecklistCode code;

    private boolean allowManualCompletion;

    private LocalDateTime scheduledCompletionTime;

    private boolean completed;

    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id")
    private ClaimEntity claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "response_id")
    private DefendantResponseEntity response;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterclaim_id")
    private CounterClaimEntity counterClaim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gen_app_id")
    private GenAppEntity genApp;
}