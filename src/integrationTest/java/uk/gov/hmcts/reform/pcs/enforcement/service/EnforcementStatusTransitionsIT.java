package uk.gov.hmcts.reform.pcs.enforcement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.InterimWarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.SuccessfulWarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnOutcome;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.IllegalEnforcementStatusTransitionException;
import uk.gov.hmcts.reform.pcs.enforcement.notification.EnforcementNotificationService;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WarrantRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.strategy.EnforcementTypeStrategyFactory;
import uk.gov.hmcts.reform.pcs.enforcement.testcasesupport.EnforcementFeePaidSimulator;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_PAYMENT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_PROPERTY_VISIT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_REVIEW;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_SCHEDULING;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.BREATHING_SPACE;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.REJECTED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.TRANSFERRED_TO_HIGH_COURT;

/**
 * The enforcement status model of 19-enforcement-status-model.feature against Postgres: every transition in
 * its "Status transitions" table, each one driven by the service that makes it in the real journey wherever
 * one can be driven without CCD, and every transition the model does not allow refused.
 *
 * <p>The transitions a return makes, and the reschedule from "Claimant confirmed", are additions from
 * 11-record-warrant-return.feature and HDPI-7549 rather than rows of that table; they are in
 * {@link #EXPECTED_TRANSITIONS} because the refusal check has to know about them.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Enforcement status transitions (19-enforcement-status-model.feature)")
class EnforcementStatusTransitionsIT extends AbstractPostgresContainerIT {

    /**
     * Every test here puts its enforcement on a case of its own. The Postgres container is shared by the
     * whole integration suite, so the references are counted off a base no other test uses rather than
     * written out per test.
     */
    private static final AtomicLong CASE_REFERENCES = new AtomicLong(7342000000000001L);

    private static final String BAILIFF_USER_ID = "bailiff-uid";
    private static final LocalDateTime EVICTION = LocalDateTime.of(2099, 6, 25, 10, 0);
    private static final Instant SEEDED_AT = Instant.parse("2026-01-01T09:00:00Z");

    /**
     * Every status change the features allow. An independent copy of the table the state machine is built
     * from, so that widening or narrowing it has to be a deliberate change in both places.
     */
    private static final Map<EnforcementStatus, Set<EnforcementStatus>> EXPECTED_TRANSITIONS = expectedTransitions();

    /** The statuses a brand new application can start in. */
    private static final Set<EnforcementStatus> EXPECTED_INITIAL = EnumSet.of(AWAITING_PAYMENT, AWAITING_REVIEW);

    @Autowired
    private EnforcementStatusTransitions enforcementStatusTransitions;

    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;

    @Autowired
    private WarrantRepository warrantRepository;

    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private PcsCaseService pcsCaseService;

    @Autowired
    private CurrentEnforcementService currentEnforcementService;

    @Autowired
    private EnforcementTypeStrategyFactory strategyFactory;

    @Autowired
    private RepeatWarrantFeeWaiver repeatWarrantFeeWaiver;

    @Autowired
    private EnforcementDocumentScheduler enforcementDocumentScheduler;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EnforcementFeePaidSimulator enforcementFeePaidSimulator;

    @Autowired
    private EnforcementDecisionService enforcementDecisionService;

    @Autowired
    private BailiffAllocationService bailiffAllocationService;

    @Autowired
    private EvictionScheduleService evictionScheduleService;

    @Autowired
    private ClaimantEvictionConfirmationService claimantEvictionConfirmationService;

    @Autowired
    private WarrantReturnService warrantReturnService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Allocating a bailiff and recording a return both record who did it, and nothing puts a user on the
     * security context outside a CCD callback.
     */
    @MockitoBean
    private SecurityContextService securityContextService;

    private EnforcementOrderService enforcementOrderService;

    @BeforeEach
    void setUp() {
        when(securityContextService.getCurrentUserId()).thenReturn(BAILIFF_USER_ID);
        // Deleting the draft needs the caller's IDAM user and organisation, and the fee lookup and service
        // request need the Fees Register and Payments; none of them has anything to do with the status, and
        // nor do the work allocation tasks the application produces.
        enforcementOrderService = new EnforcementOrderService(
            enforcementOrderRepository, currentEnforcementService, pcsCaseService, mock(DraftCaseDataService.class),
            strategyFactory, enforcementStatusTransitions, repeatWarrantFeeWaiver,
            mock(EnforcementNotificationService.class), enforcementDocumentScheduler,
            mock(EnforcementFeeService.class, RETURNS_MOCKS), mock(EnforcementWaTaskService.class),
            objectMapper);
    }

    @Test
    @DisplayName("allows exactly the status changes the features describe")
    void allowsExactlyTheTransitionsTheFeaturesDescribe() {
        assertThat(EnforcementStatusTransitions.allowedTargets(null))
            .as("the statuses a new application can start in")
            .isEqualTo(EXPECTED_INITIAL);
        for (EnforcementStatus from : EnforcementStatus.values()) {
            assertThat(EnforcementStatusTransitions.allowedTargets(from))
                .as("the statuses %s can move to", from)
                .isEqualTo(EXPECTED_TRANSITIONS.get(from));
        }
    }

    @ParameterizedTest(name = "{0} -> {2} when {1}")
    @MethodSource("statusTransitionTable")
    @DisplayName("records every transition in the status model table, with the time it changed")
    void recordsEveryTransitionInTheStatusModelTable(EnforcementStatus from, String trigger, EnforcementStatus to) {
        EnforcementOrderEntity enforcement = savedEnforcement(nextCaseReference(), from);

        enforcementStatusTransitions.transition(enforcement, to);
        enforcementOrderRepository.save(enforcement);
        entityManager.flush();
        entityManager.clear();

        assertThat(statusOf(enforcement.getId())).as(trigger).isEqualTo(to.name());
        assertThat(enforcementOrderRepository.findById(enforcement.getId()).orElseThrow().getStatusUpdatedAt())
            .as("when the status changed")
            .isAfter(SEEDED_AT);
    }

    @Test
    @DisplayName("refuses every status change the model does not allow and leaves the stored status alone")
    void refusesEveryTransitionTheModelDoesNotAllow() {
        for (EnforcementStatus from : EnforcementStatus.values()) {
            EnforcementOrderEntity enforcement = savedEnforcement(nextCaseReference(), from);
            List<EnforcementStatus> refused = new ArrayList<>();
            for (EnforcementStatus to : EnforcementStatus.values()) {
                if (EXPECTED_TRANSITIONS.get(from).contains(to)) {
                    continue;
                }
                refused.add(to);
                assertThatThrownBy(() -> enforcementStatusTransitions.transition(enforcement, to))
                    .as("%s -> %s", from, to)
                    .isInstanceOf(IllegalEnforcementStatusTransitionException.class);
            }
            assertThat(refused).as("%s has some refused target", from).isNotEmpty();
            entityManager.flush();
            entityManager.clear();
            assertThat(statusOf(enforcement.getId())).as("%s is unchanged by a refusal", from).isEqualTo(from.name());
        }
    }

    @Test
    @DisplayName("refuses a new enforcement starting anywhere but awaiting payment or awaiting review")
    void refusesANewEnforcementStartingAnywhereElse() {
        for (EnforcementStatus to : EnforcementStatus.values()) {
            if (EXPECTED_INITIAL.contains(to)) {
                continue;
            }
            EnforcementOrderEntity enforcement = savedEnforcement(nextCaseReference(), null);
            assertThatThrownBy(() -> enforcementStatusTransitions.transition(enforcement, to))
                .as("a new enforcement cannot start in %s", to)
                .isInstanceOf(IllegalEnforcementStatusTransitionException.class);
        }
    }

    @Test
    @DisplayName("refuses a change to no status at all")
    void refusesAChangeToNoStatus() {
        EnforcementOrderEntity enforcement = savedEnforcement(nextCaseReference(), AWAITING_SCHEDULING);

        assertThatThrownBy(() -> enforcementStatusTransitions.transition(enforcement, null))
            .isInstanceOf(IllegalEnforcementStatusTransitionException.class);
    }

    // Each transition below is made by the service that makes it in the real journey.

    @Test
    @DisplayName("the claimant submitting an application starts it awaiting payment")
    void theClaimantSubmittingAnApplication() {
        long caseReference = nextCaseReference();
        final ClaimEntity claim = savedClaim(caseReference);

        enforcementOrderService.saveAndClearDraftData(caseReference, submittedWarrant());
        entityManager.flush();
        entityManager.clear();

        EnforcementOrderEntity submitted = enforcementOrderRepository.findByClaimId(claim.getId()).getFirst();
        assertThat(submitted.getStatus()).isEqualTo(AWAITING_PAYMENT);
        assertThat(submitted.getSubmittedAt()).isEqualTo(submitted.getStatusUpdatedAt());
    }

    @Test
    @DisplayName("the claimant paying the fee moves the application to awaiting review")
    void theClaimantPaying() {
        long caseReference = nextCaseReference();
        final UUID id = savedEnforcement(caseReference, AWAITING_PAYMENT).getId();

        assertThat(enforcementFeePaidSimulator.recordFeePaid(caseReference)).isTrue();
        entityManager.flush();
        entityManager.clear();

        assertThat(statusOf(id)).isEqualTo(AWAITING_REVIEW.name());
    }

    @Test
    @DisplayName("the caseworker approving a warrant moves it to bailiff allocation")
    void theCaseworkerApprovesAWarrant() {
        long caseReference = nextCaseReference();
        final UUID id = savedEnforcement(caseReference, AWAITING_REVIEW).getId();

        assertThat(enforcementDecisionService.accept(caseReference).refused()).isFalse();
        entityManager.flush();
        entityManager.clear();

        assertThat(statusOf(id)).isEqualTo(AWAITING_BAILIFF_ALLOCATION.name());
    }

    @Test
    @DisplayName("the caseworker rejecting the application moves it to rejected")
    void theCaseworkerRejectsTheApplication() {
        long caseReference = nextCaseReference();
        final UUID id = savedEnforcement(caseReference, AWAITING_REVIEW).getId();

        assertThat(enforcementDecisionService.reject(caseReference, "Not eligible").refused()).isFalse();
        entityManager.flush();
        entityManager.clear();

        assertThat(statusOf(id)).isEqualTo(REJECTED.name());
    }

    @Test
    @DisplayName("allocating a bailiff moves the warrant to awaiting scheduling")
    void allocatingABailiff() {
        long caseReference = nextCaseReference();
        final UUID id = savedEnforcement(caseReference, AWAITING_BAILIFF_ALLOCATION).getId();

        EnforcementChange<Void> allocated = bailiffAllocationService.allocate(
            caseReference, new BailiffUser(BAILIFF_USER_ID, "Sam Jones", "sam@justice.gov.uk"));
        entityManager.flush();
        entityManager.clear();

        assertThat(allocated.refused()).isFalse();
        assertThat(statusOf(id)).isEqualTo(AWAITING_SCHEDULING.name());
    }

    @Test
    @DisplayName("scheduling the eviction moves the warrant to awaiting the claimant's confirmation")
    void theBailiffSchedulesTheEviction() {
        long caseReference = nextCaseReference();
        final UUID id = savedEnforcement(caseReference, AWAITING_SCHEDULING).getId();

        assertThat(evictionScheduleService.schedule(caseReference, EVICTION, false).refused()).isFalse();
        entityManager.flush();
        entityManager.clear();

        assertThat(statusOf(id)).isEqualTo(AWAITING_CLAIMANT_CONFIRMATION.name());
    }

    @ParameterizedTest(name = "the claimant answers {0}")
    @CsvSource({
        "YES, CLAIMANT_CONFIRMED",
        "NO, CLAIMANT_UNABLE_TO_ATTEND",
    })
    @DisplayName("the claimant's attendance answer moves the warrant on")
    void theClaimantsAttendanceAnswer(YesOrNo canAttend, EnforcementStatus expected) {
        long caseReference = nextCaseReference();
        final UUID id = savedEnforcement(caseReference, AWAITING_CLAIMANT_CONFIRMATION).getId();

        assertThat(claimantEvictionConfirmationService.recordAttendance(caseReference, canAttend).refused()).isFalse();
        entityManager.flush();
        entityManager.clear();

        assertThat(statusOf(id)).isEqualTo(expected.name());
    }

    @Test
    @DisplayName("rescheduling for a claimant who cannot attend moves the warrant back to awaiting confirmation")
    void theBailiffReschedulesTheEviction() {
        long caseReference = nextCaseReference();
        final UUID id = savedEnforcement(caseReference, CLAIMANT_UNABLE_TO_ATTEND).getId();

        assertThat(evictionScheduleService.reschedule(caseReference, EVICTION, false).refused()).isFalse();
        entityManager.flush();
        entityManager.clear();

        assertThat(statusOf(id)).isEqualTo(AWAITING_CLAIMANT_CONFIRMATION.name());
    }

    @ParameterizedTest(name = "a {0} return")
    @CsvSource({
        "closing final, EVICTION_SUCCESSFUL, CLOSED",
        "breathing space, DEFENDANT_STATES_BREATHING_SPACE, BREATHING_SPACE",
        "obstruction, OBSTRUCTION_BY_DEFENDANTS, AWAITING_SCHEDULING",
    })
    @DisplayName("the bailiff's return moves the confirmed warrant on")
    void theBailiffRecordsAReturn(String description, String returnName, EnforcementStatus expected) {
        long caseReference = nextCaseReference();
        final UUID id = savedEnforcement(caseReference, CLAIMANT_CONFIRMED).getId();

        EnforcementChange<RecordedWarrantReturn> recorded =
            warrantReturnService.recordReturn(caseReference, returnOf(returnName));
        entityManager.flush();
        entityManager.clear();

        assertThat(recorded.refused()).as(description).isFalse();
        assertThat(statusOf(id)).as(description).isEqualTo(expected.name());
    }

    @Test
    @DisplayName("'Reopen warrant' moves a closed warrant back to bailiff allocation")
    void theBailiffReopensTheWarrant() {
        long caseReference = nextCaseReference();
        final UUID id = savedEnforcement(caseReference, CLOSED).getId();

        EnforcementChange<RecordedWarrantReturn> recorded =
            warrantReturnService.recordReturn(caseReference, returnOf("REOPEN_WARRANT"));
        entityManager.flush();
        entityManager.clear();

        assertThat(recorded.refused()).isFalse();
        assertThat(statusOf(id)).isEqualTo(AWAITING_BAILIFF_ALLOCATION.name());
    }

    @Test
    @DisplayName("an attendance answer on a warrant that has moved on is refused, not recorded")
    void refusesAnAttendanceAnswerOnAWarrantThatHasMovedOn() {
        long caseReference = nextCaseReference();
        final UUID id = savedEnforcement(caseReference, AWAITING_SCHEDULING).getId();

        EnforcementChange<Void> recorded =
            claimantEvictionConfirmationService.recordAttendance(caseReference, YesOrNo.YES);
        entityManager.flush();
        entityManager.clear();

        assertThat(recorded.refused()).isTrue();
        assertThat(recorded.errors()).containsExactly(ClaimantEvictionConfirmationService.EVICTION_CHANGED_ERROR);
        assertThat(statusOf(id)).isEqualTo(AWAITING_SCHEDULING.name());
    }

    /**
     * The "Status transitions" table of 19-enforcement-status-model.feature, in order.
     */
    private static Stream<Arguments> statusTransitionTable() {
        return Stream.of(
            Arguments.of(null, "the claimant submits an application", AWAITING_PAYMENT),
            Arguments.of(AWAITING_PAYMENT, "the claimant pays", AWAITING_REVIEW),
            Arguments.of(AWAITING_REVIEW, "the caseworker approves a warrant", AWAITING_BAILIFF_ALLOCATION),
            Arguments.of(AWAITING_REVIEW, "the caseworker rejects the application", REJECTED),
            Arguments.of(AWAITING_BAILIFF_ALLOCATION, "a bailiff is allocated", AWAITING_SCHEDULING),
            Arguments.of(AWAITING_SCHEDULING, "the bailiff schedules the eviction", AWAITING_CLAIMANT_CONFIRMATION),
            Arguments.of(AWAITING_CLAIMANT_CONFIRMATION, "the claimant confirms they can attend", CLAIMANT_CONFIRMED),
            Arguments.of(AWAITING_CLAIMANT_CONFIRMATION, "the claimant says they cannot attend",
                         CLAIMANT_UNABLE_TO_ATTEND),
            Arguments.of(CLAIMANT_UNABLE_TO_ATTEND, "the bailiff reschedules the eviction",
                         AWAITING_CLAIMANT_CONFIRMATION),
            Arguments.of(CLAIMANT_CONFIRMED, "the bailiff records a closing final return", CLOSED),
            Arguments.of(CLAIMANT_CONFIRMED, "the bailiff records a breathing space return", BREATHING_SPACE),
            Arguments.of(CLAIMANT_CONFIRMED, "the bailiff records an obstruction return", AWAITING_SCHEDULING),
            Arguments.of(CLOSED, "the bailiff records 'Reopen warrant'", AWAITING_BAILIFF_ALLOCATION));
    }

    private static Map<EnforcementStatus, Set<EnforcementStatus>> expectedTransitions() {
        Map<EnforcementStatus, Set<EnforcementStatus>> expected = new EnumMap<>(EnforcementStatus.class);
        // 19-enforcement-status-model.feature "Status transitions", plus the writ's transfer to the High Court.
        expected.put(AWAITING_PAYMENT, EnumSet.of(AWAITING_REVIEW));
        expected.put(AWAITING_REVIEW, EnumSet.of(AWAITING_BAILIFF_ALLOCATION, REJECTED, TRANSFERRED_TO_HIGH_COURT));
        expected.put(AWAITING_BAILIFF_ALLOCATION, EnumSet.of(AWAITING_SCHEDULING));
        // A return can be recorded at any point after a bailiff is allocated (11-record-warrant-return.feature),
        // so every status from here on can also close, go into breathing space or go back for scheduling.
        expected.put(AWAITING_SCHEDULING, EnumSet.of(AWAITING_CLAIMANT_CONFIRMATION, CLOSED, BREATHING_SPACE));
        expected.put(AWAITING_CLAIMANT_CONFIRMATION, EnumSet.of(CLAIMANT_CONFIRMED, CLAIMANT_UNABLE_TO_ATTEND,
                                                                CLOSED, BREATHING_SPACE, AWAITING_SCHEDULING));
        // "Reschedule eviction" is offered in "Claimant confirmed" as well (HDPI-7549, OPEN-QUESTIONS 7).
        expected.put(CLAIMANT_CONFIRMED, EnumSet.of(AWAITING_CLAIMANT_CONFIRMATION, CLOSED, BREATHING_SPACE,
                                                    AWAITING_SCHEDULING));
        expected.put(CLAIMANT_UNABLE_TO_ATTEND, EnumSet.of(AWAITING_CLAIMANT_CONFIRMATION, CLOSED, BREATHING_SPACE,
                                                           AWAITING_SCHEDULING));
        expected.put(AWAITING_PROPERTY_VISIT, EnumSet.of(CLOSED, BREATHING_SPACE, AWAITING_SCHEDULING));
        // A return is how a warrant leaves breathing space (OPEN-QUESTIONS 4).
        expected.put(BREATHING_SPACE, EnumSet.of(CLOSED, AWAITING_SCHEDULING));
        expected.put(CLOSED, EnumSet.of(AWAITING_BAILIFF_ALLOCATION));
        // A rejected application and a writ transferred to the High Court are finished.
        expected.put(REJECTED, EnumSet.noneOf(EnforcementStatus.class));
        expected.put(TRANSFERRED_TO_HIGH_COURT, EnumSet.noneOf(EnforcementStatus.class));
        return Map.copyOf(expected);
    }

    private static WarrantReturnDetails returnOf(String returnName) {
        if (SuccessfulWarrantReturnName.EVICTION_SUCCESSFUL.name().equals(returnName)) {
            return WarrantReturnDetails.builder()
                .type(WarrantReturnType.FINAL)
                .outcome(WarrantReturnOutcome.EVICTION_SUCCESSFUL)
                .successfulReturnName(SuccessfulWarrantReturnName.EVICTION_SUCCESSFUL)
                .evictionDateTime(LocalDateTime.of(2026, 6, 25, 10, 0))
                .build();
        }
        InterimWarrantReturnName interimReturnName = InterimWarrantReturnName.valueOf(returnName);
        return WarrantReturnDetails.builder()
            .type(WarrantReturnType.INTERIM)
            .interimReturnName(interimReturnName)
            // Mandatory for some return names, and ignored for the rest.
            .additionalDetails("Recorded by the bailiff")
            .build();
    }

    private static EnforcementOrder submittedWarrant() {
        return EnforcementOrder.builder()
            .chooseEnforcementType(DynamicStringList.builder()
                                       .value(DynamicStringListElement.builder()
                                                  .code(SelectEnforcementType.WARRANT.name())
                                                  .build())
                                       .build())
            .warrantDetails(WarrantDetails.builder().build())
            .build();
    }

    private static long nextCaseReference() {
        return CASE_REFERENCES.getAndIncrement();
    }

    private String statusOf(UUID id) {
        return jdbcTemplate.queryForObject("SELECT status FROM enf_case WHERE id = ?", String.class, id);
    }

    /**
     * A warrant application on its own case, at {@code status}. It is a warrant of possession, with its
     * {@code enf_warrant} row, because the bailiff events and the warrant returns only apply to a warrant.
     */
    private EnforcementOrderEntity savedEnforcement(long caseReference, EnforcementStatus status) {
        final ClaimEntity claim = savedClaim(caseReference);

        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setClaim(claim);
        enforcement.setEnforcementOrder(submittedWarrant());
        enforcement.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        enforcement.setStatus(status);
        enforcement.setStatusUpdatedAt(status == null ? null : SEEDED_AT);
        enforcement.setApprovedAt(status != null && status.isGenerated() ? SEEDED_AT : null);
        enforcement.setBailiffDate(EVICTION);
        enforcement.setDispenseWithService(false);
        EnforcementOrderEntity saved = enforcementOrderRepository.save(enforcement);
        warrantRepository.save(WarrantEntity.builder().enforcementOrder(saved).build());
        entityManager.flush();
        entityManager.clear();
        return enforcementOrderRepository.findById(saved.getId()).orElseThrow();
    }

    private ClaimEntity savedClaim(long caseReference) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        pcsCase.addClaim(new ClaimEntity());
        return pcsCaseRepository.save(pcsCase).getClaims().getFirst();
    }
}
