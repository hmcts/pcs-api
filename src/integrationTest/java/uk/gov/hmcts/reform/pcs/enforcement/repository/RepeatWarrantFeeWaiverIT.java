package uk.gov.hmcts.reform.pcs.enforcement.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementDocumentScheduler;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantReturnEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionService;
import uk.gov.hmcts.reform.pcs.enforcement.notification.EnforcementNotificationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementFeeService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementStatusTransitions;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementWaTaskService;
import uk.gov.hmcts.reform.pcs.enforcement.service.RepeatWarrantFeeWaiver;
import uk.gov.hmcts.reform.pcs.enforcement.service.strategy.EnforcementTypeStrategyFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_PAYMENT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_REVIEW;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName.WARRANT_SUSPENDED_BY_ORDER;

/**
 * A further application submitted against Postgres, with the waiver's clock fixed, so the 12 calendar month
 * boundary is exact (HDPI-8424; 01 and 15 feature files).
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Free repeat warrant (HDPI-8424)")
class RepeatWarrantFeeWaiverIT extends AbstractPostgresContainerIT {

    /**
     * The day the further application is made in most cases, during British Summer Time.
     */
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 8);

    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;
    @Autowired
    private WarrantReturnRepository warrantReturnRepository;
    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private PcsCaseService pcsCaseService;
    @Autowired
    private EnforcementTypeStrategyFactory strategyFactory;
    @Autowired
    private EnforcementStatusTransitions enforcementStatusTransitions;
    @Autowired
    private EnforcementDecisionService enforcementDecisionService;
    @Autowired
    private EntityManager entityManager;

    private final Map<UUID, Long> caseReferences = new HashMap<>();

    @ParameterizedTest(name = "{0}, first warrant generated {1}, applied {2}: {3}")
    @CsvSource({
        "WARRANT_SUSPENDED_BY_ORDER, 2026-01-08, 2026-07-08, AWAITING_REVIEW",
        "WARRANT_SUSPENDED_BY_ORDER, 2025-07-08, 2026-07-08, AWAITING_REVIEW",
        "WARRANT_SUSPENDED_BY_ORDER, 2025-07-07, 2026-07-08, AWAITING_PAYMENT",
        "WARRANT_SUSPENDED_BY_ORDER, 2025-06-08, 2026-07-08, AWAITING_PAYMENT",
        "WARRANT_SUSPENDED_BY_ORDER, 2024-02-29, 2025-02-28, AWAITING_REVIEW",
        "WARRANT_SUSPENDED_BY_ORDER, 2024-02-29, 2025-03-01, AWAITING_PAYMENT",
        "EVICTION_SUCCESSFUL, 2026-01-08, 2026-07-08, AWAITING_PAYMENT",
        "NO_LOCKSMITH_CLAIMANT_FAILED_TO_ARRANGE, 2026-01-08, 2026-07-08, AWAITING_PAYMENT",
    })
    @DisplayName("waives the warrant fee only after a suspension, within 12 calendar months of generation")
    void waivesTheFeeWithin12CalendarMonths(WarrantReturnName returnName, LocalDate generatedOn,
                                            LocalDate appliedOn, EnforcementStatus expected) {
        ClaimEntity claim = savedClaim(1500000000000001L);
        EnforcementOrderEntity earlier = savedClosedWarrant(claim, generatedOn.atTime(10, 0));
        savedFinalReturn(earlier, returnName, appliedOn.minusDays(1));

        Submitted submitted = submitAt(appliedOn, claim, SelectEnforcementType.WARRANT);

        assertThat(submitted.enforcement().getStatus()).isEqualTo(expected);
        assertThat(submitted.paymentOutstanding()).isEqualTo(expected == AWAITING_PAYMENT);
    }

    @Test
    @DisplayName("measures from the claim's first warrant, not the suspended free repeat")
    void measuresFromTheFirstWarrant() {
        ClaimEntity claim = savedClaim(1500000000000002L);
        EnforcementOrderEntity first = savedClosedWarrant(claim, TODAY.minusMonths(13).atTime(10, 0));
        savedFinalReturn(first, WARRANT_SUSPENDED_BY_ORDER, TODAY.minusMonths(4));
        EnforcementOrderEntity repeat = savedClosedWarrant(claim, TODAY.minusMonths(3).atTime(10, 0));
        savedFinalReturn(repeat, WARRANT_SUSPENDED_BY_ORDER, TODAY.minusMonths(1));

        Submitted submitted = submitAt(TODAY, claim, SelectEnforcementType.WARRANT);

        assertThat(submitted.enforcement().getStatus()).isEqualTo(AWAITING_PAYMENT);
    }

    @Test
    @DisplayName("charges for a writ after a suspended warrant")
    void chargesForAWrit() {
        ClaimEntity claim = savedClaim(1500000000000003L);
        EnforcementOrderEntity earlier = savedClosedWarrant(claim, TODAY.minusMonths(6).atTime(10, 0));
        savedFinalReturn(earlier, WARRANT_SUSPENDED_BY_ORDER, TODAY.minusMonths(1));

        Submitted submitted = submitAt(TODAY, claim, SelectEnforcementType.WRIT);

        assertThat(submitted.enforcement().getStatus()).isEqualTo(AWAITING_PAYMENT);
    }

    @Test
    @DisplayName("measures from the generation date that approving the warrant stores")
    void measuresFromTheApprovalStoredByDecideEnforcement() {
        final ClaimEntity claim = savedClaim(1500000000000004L);
        EnforcementOrderEntity first = submitAt(TODAY, claim, SelectEnforcementType.WARRANT).enforcement();
        enforcementStatusTransitions.transition(first, AWAITING_REVIEW);
        enforcementOrderRepository.saveAndFlush(first);

        assertThat(enforcementDecisionService.accept(1500000000000004L)).isPresent();
        entityManager.flush();
        entityManager.clear();
        EnforcementOrderEntity approved = enforcementOrderRepository.findById(first.getId()).orElseThrow();
        assertThat(approved.getApprovedAt()).isNotNull();
        approved.setStatus(CLOSED);
        enforcementOrderRepository.saveAndFlush(approved);
        LocalDate generatedOn = approved.getApprovedAt().atZone(UK_ZONE_ID).toLocalDate();
        savedFinalReturn(approved, WARRANT_SUSPENDED_BY_ORDER, generatedOn);

        EnforcementOrderEntity onTheLastDay =
            submitAt(generatedOn.plusMonths(12), claim, SelectEnforcementType.WARRANT).enforcement();
        assertThat(onTheLastDay.getStatus()).isEqualTo(AWAITING_REVIEW);

        // Out of the way, so the next application is not refused for this one: submit does not check it here.
        onTheLastDay.setStatus(EnforcementStatus.REJECTED);
        enforcementOrderRepository.saveAndFlush(onTheLastDay);
        EnforcementOrderEntity theDayAfter =
            submitAt(generatedOn.plusMonths(12).plusDays(1), claim, SelectEnforcementType.WARRANT).enforcement();
        assertThat(theDayAfter.getStatus()).isEqualTo(AWAITING_PAYMENT);
    }

    private record Submitted(EnforcementOrderEntity enforcement, boolean paymentOutstanding) {
    }

    private Submitted submitAt(LocalDate appliedOn, ClaimEntity claim, SelectEnforcementType enforcementType) {
        UUID claimId = claim.getId();
        Clock ukClock = Clock.fixed(appliedOn.atTime(12, 0).atZone(UK_ZONE_ID).toInstant(), UK_ZONE_ID);
        RepeatWarrantFeeWaiver waiver =
            new RepeatWarrantFeeWaiver(enforcementOrderRepository, warrantReturnRepository, ukClock);
        // Deleting the draft needs the caller's IDAM user and organisation, which this test has no use for, and
        // the fee lookup and service request need the Fees Register and Payments.
        EnforcementOrderService enforcementOrderService = new EnforcementOrderService(
            enforcementOrderRepository, new CurrentEnforcementService(pcsCaseService, enforcementOrderRepository),
            pcsCaseService, mock(DraftCaseDataService.class), strategyFactory,
            enforcementStatusTransitions, waiver, mock(EnforcementNotificationService.class),
            mock(EnforcementDocumentScheduler.class), mock(EnforcementFeeService.class, RETURNS_MOCKS),
            mock(EnforcementWaTaskService.class));
        List<UUID> before = enforcementIds(claimId);
        long caseReference = caseReferences.get(claimId);

        boolean paymentOutstanding =
            enforcementOrderService.saveAndClearDraftData(caseReference, enforcementOrder(enforcementType)).isPresent();
        entityManager.flush();
        entityManager.clear();

        EnforcementOrderEntity submitted = enforcementOrderRepository.findByClaimId(claimId).stream()
            .filter(enforcement -> !before.contains(enforcement.getId()))
            .findFirst()
            .orElseThrow();
        return new Submitted(submitted, paymentOutstanding);
    }

    private List<UUID> enforcementIds(UUID claimId) {
        return enforcementOrderRepository.findByClaimId(claimId).stream()
            .map(EnforcementOrderEntity::getId)
            .toList();
    }

    private EnforcementOrderEntity savedClosedWarrant(ClaimEntity claim, LocalDateTime generatedAt) {
        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity warrant = new EnforcementOrderEntity();
        warrant.setClaim(claim);
        warrant.setEnforcementOrder(enforcementOrder(SelectEnforcementType.WARRANT));
        warrant.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        warrant.setStatus(CLOSED);
        // Before anything submitted in the test, whose status time is now.
        warrant.setStatusUpdatedAt(Instant.parse("2020-01-01T00:00:00Z"));
        warrant.setApprovedAt(generatedAt.atZone(UK_ZONE_ID).toInstant());
        return enforcementOrderRepository.saveAndFlush(warrant);
    }

    private void savedFinalReturn(EnforcementOrderEntity warrant, WarrantReturnName returnName, LocalDate on) {
        warrantReturnRepository.saveAndFlush(WarrantReturnEntity.builder()
            .enforcementOrder(warrant)
            .returnType(WarrantReturnType.FINAL)
            .outcome(returnName == WarrantReturnName.EVICTION_SUCCESSFUL
                         ? WarrantReturnOutcome.EVICTION_SUCCESSFUL
                         : WarrantReturnOutcome.EVICTION_UNSUCCESSFUL)
            .returnName(returnName)
            .recordedAt(on.atTime(15, 0).atZone(UK_ZONE_ID).toInstant())
            .build());
    }

    private static EnforcementOrder enforcementOrder(SelectEnforcementType enforcementType) {
        return EnforcementOrder.builder()
            .chooseEnforcementType(DynamicStringList.builder()
                                       .value(DynamicStringListElement.builder()
                                                  .code(enforcementType.name())
                                                  .build())
                                       .build())
            .warrantDetails(WarrantDetails.builder().build())
            .writDetails(WritDetails.builder().build())
            .warrantOfRestitutionDetails(WarrantOfRestitutionDetails.builder().build())
            .build();
    }

    private ClaimEntity savedClaim(long caseReference) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        ClaimEntity claim = new ClaimEntity();
        pcsCase.addClaim(claim);
        ClaimEntity saved = pcsCaseRepository.save(pcsCase).getClaims().getFirst();
        caseReferences.put(saved.getId(), caseReference);
        return saved;
    }
}
