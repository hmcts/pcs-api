package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.sdk.SystemCaseEvent;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventOutcome;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventService;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform.CounterClaimFormScheduler;

import java.time.Clock;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Test-support only (bulk print): issues a counterclaim and schedules its form generation, standing
 * in for the HDPI-5806 payment-issue trigger so the counterclaim form (the document posted out via
 * bulk print) can be generated and verified locally/in test environments. Remove once HDPI-5806
 * wires the real trigger.
 */
@RestController
@RequestMapping("/testing-support")
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
public class CounterClaimFormTestingSupportController {

    private final CounterClaimRepository counterClaimRepository;
    private final CounterClaimFormScheduler counterClaimFormScheduler;
    private final SystemCaseEventService systemCaseEventService;
    private final Clock utcClock;

    public CounterClaimFormTestingSupportController(CounterClaimRepository counterClaimRepository,
                                                    CounterClaimFormScheduler counterClaimFormScheduler,
                                                    SystemCaseEventService systemCaseEventService,
                                                    @Qualifier("utcClock") Clock utcClock) {
        this.counterClaimRepository = counterClaimRepository;
        this.counterClaimFormScheduler = counterClaimFormScheduler;
        this.systemCaseEventService = systemCaseEventService;
        this.utcClock = utcClock;
    }

    @PostMapping("/counterclaim/{counterClaimId}/issue")
    public ResponseEntity<UUID> issueAndSchedule(@PathVariable UUID counterClaimId) {
        long caseReference = counterClaimRepository.findCaseReferenceById(counterClaimId)
            .orElseThrow(() -> new IllegalArgumentException("No counter claim found for id: " + counterClaimId));

        systemCaseEventService.submit(
            caseReference,
            new SystemCaseEvent("testCounterClaimIssued", "Test counterclaim issued"),
            UUID.nameUUIDFromBytes(
                ("pcs:test-counterclaim-issued:" + counterClaimId).getBytes(StandardCharsets.UTF_8)
            ),
            context -> {
                CounterClaimEntity counterClaim = counterClaimRepository.findById(counterClaimId)
                    .orElseThrow(() -> new IllegalArgumentException(
                        "No counter claim found for id: " + counterClaimId
                    ));
                counterClaim.setStatus(CounterClaimState.COUNTER_CLAIM_ISSUED);
                if (counterClaim.getClaimIssuedDate() == null) {
                    counterClaim.setClaimIssuedDate(LocalDateTime.now(utcClock));
                }
                counterClaimRepository.save(counterClaim);
                return SystemCaseEventOutcome.noStateChange();
            }
        );

        counterClaimFormScheduler.scheduleCounterClaimFormGeneration(counterClaimId);
        return ResponseEntity.accepted().body(counterClaimId);
    }
}
