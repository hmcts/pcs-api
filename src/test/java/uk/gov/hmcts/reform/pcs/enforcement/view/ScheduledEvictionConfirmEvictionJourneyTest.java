package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffNoteService;
import uk.gov.hmcts.reform.pcs.enforcement.service.ClaimantEvictionConfirmationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.ClaimantRiskAssessmentService;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementStatusTransitions;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionScheduleService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * The bailiff schedules the eviction, which sets the bailiff date and moves the warrant to "Awaiting claimant
 * confirmation". The claimant's Confirm eviction journey lights up from that date, and the claimant's answer
 * moves the warrant on. Every service is real; only the case and repository are mocked.
 */
@ExtendWith(MockitoExtension.class)
class ScheduledEvictionConfirmEvictionJourneyTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private BailiffNoteService bailiffNoteService;
    @Mock
    private ClaimantRiskAssessmentService claimantRiskAssessmentService;

    private PcsCaseEntity pcsCaseEntity;
    private EnforcementOrderEntity enforcement;
    private EnforcementOrderMediator mediator;
    private EvictionScheduleService evictionScheduleService;
    private ClaimantEvictionConfirmationService claimantEvictionConfirmationService;

    @BeforeEach
    void setUp() {
        pcsCaseEntity = new PcsCaseEntity();
        ClaimEntity claim = new ClaimEntity();
        claim.setId(UUID.randomUUID());
        pcsCaseEntity.setClaims(List.of(claim));
        enforcement = new EnforcementOrderEntity();
        enforcement.setStatus(EnforcementStatus.AWAITING_SCHEDULING);
        lenient().when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        lenient().when(enforcementOrderRepository.findByClaimId(claim.getId())).thenReturn(List.of(enforcement));

        CurrentEnforcementService currentEnforcementService =
            new CurrentEnforcementService(pcsCaseService, enforcementOrderRepository);
        Clock clock = Clock.fixed(Instant.parse("2026-06-20T09:00:00Z"), ZoneOffset.UTC);
        EnforcementStatusTransitions transitions = new EnforcementStatusTransitions(clock);
        mediator = new EnforcementOrderMediator(currentEnforcementService, clock);
        evictionScheduleService = new EvictionScheduleService(currentEnforcementService, enforcementOrderRepository,
                                                              transitions, bailiffNoteService);
        claimantEvictionConfirmationService = new ClaimantEvictionConfirmationService(
            currentEnforcementService, enforcementOrderRepository, transitions, claimantRiskAssessmentService);
    }

    @ParameterizedTest
    @CsvSource({"YES, CLAIMANT_CONFIRMED", "NO, CLAIMANT_UNABLE_TO_ATTEND"})
    void shouldScheduleThenLetTheClaimantConfirmTheEviction(YesOrNo canAttend, EnforcementStatus expected) {
        PCSCase before = PCSCase.builder().build();
        mediator.handleEnforcementRequirements(pcsCaseEntity, before);
        assertThat(before.getEnforcementOrder().getShowConfirmEvictionJourney()).isEqualTo(YesOrNo.NO);

        evictionScheduleService.schedule(CASE_REFERENCE, LocalDateTime.parse("2026-06-25T10:00"), false);

        assertThat(enforcement.getStatus()).isEqualTo(EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION);
        PCSCase after = PCSCase.builder().build();
        mediator.handleEnforcementRequirements(pcsCaseEntity, after);
        assertThat(after.getEnforcementOrder().getShowConfirmEvictionJourney()).isEqualTo(YesOrNo.YES);
        assertThat(after.getEnforcementOrder().getConfirmEvictionSummaryMarkup())
            .contains("The bailiff has given you an eviction date of Thursday, 25 June 2026 at 10:00am")
            .contains("You must confirm the eviction details before 22 June 2026");

        claimantEvictionConfirmationService.recordAttendance(CASE_REFERENCE, canAttend);

        assertThat(enforcement.getStatus()).isEqualTo(expected);
    }
}
