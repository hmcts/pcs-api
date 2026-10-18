package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementEventAvailability;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffAllocationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffNoteService;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionScheduleService;
import uk.gov.hmcts.reform.pcs.enforcement.service.RiskLevelService;
import uk.gov.hmcts.reform.pcs.enforcement.service.WarrantReturnService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_REVIEW;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;
import static uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDataUtil.buildEnforcementOrderWithSpecifiedType;

/**
 * The flags must offer an event exactly when its start callback would let it start, so each case below runs the
 * view and the real start checks of the services against the same current enforcement.
 */
@ExtendWith(MockitoExtension.class)
class EnforcementEventAvailabilityViewTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private PcsCaseService pcsCaseService;

    @InjectMocks
    private EnforcementEventAvailabilityView underTest;
    @InjectMocks
    private EnforcementOrderService enforcementOrderService;
    @InjectMocks
    private EnforcementDecisionService enforcementDecisionService;
    @InjectMocks
    private BailiffAllocationService bailiffAllocationService;
    @InjectMocks
    private RiskLevelService riskLevelService;
    @InjectMocks
    private BailiffNoteService bailiffNoteService;
    @InjectMocks
    private WarrantReturnService warrantReturnService;
    @InjectMocks
    private EvictionScheduleService evictionScheduleService;

    private final PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

    @ParameterizedTest(name = "{0}")
    @MethodSource("currentEnforcements")
    void shouldOfferEachEventExactlyWhenItsStartCallbackWouldStart(String description,
                                                                    EnforcementOrderEntity current) {
        // Given
        givenTheCurrentEnforcementIs(current);
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setEventAvailability(pcsCaseEntity, pcsCase, State.CASE_ISSUED);
        new EvictionScheduleView(evictionScheduleService).setEvictionScheduleFields(pcsCaseEntity, pcsCase);

        // Then
        EnforcementEventAvailability flags = pcsCase.getEnforcementOrder().getEventAvailability();
        assertThat(flags.getCanApplyForEnforcement())
            .isEqualTo(YesOrNo.from(!enforcementOrderService.hasLiveEnforcement(CASE_REFERENCE)));
        assertThat(flags.getCanDecideEnforcement())
            .isEqualTo(YesOrNo.from(enforcementDecisionService.hasEnforcementAwaitingReview(CASE_REFERENCE)));
        assertThat(flags.getCanAllocateBailiff())
            .isEqualTo(YesOrNo.from(starts(() -> bailiffAllocationService.getAllocatableEnforcement(CASE_REFERENCE))));
        assertThat(flags.getCanSetRiskLevel())
            .isEqualTo(YesOrNo.from(starts(() -> riskLevelService.getEnforcementForRiskLevel(CASE_REFERENCE))));
        assertThat(flags.getCanAddBailiffNote())
            .isEqualTo(YesOrNo.from(bailiffNoteService.hasEnforcement(CASE_REFERENCE)));
        // Offered unless the start would refuse for want of a bailiff: "no warrant" is refused with an error.
        assertThat(flags.getCanRecordWarrantReturn())
            .isEqualTo(YesOrNo.from(!refusedAsNotAvailable(() -> warrantReturnService.findWarrant(CASE_REFERENCE))));

        EvictionSchedule schedule = Optional.ofNullable(pcsCase.getEnforcementOrder().getEvictionSchedule())
            .orElseGet(EvictionSchedule::new);
        assertThat(schedule.getCanScheduleEviction() == YesOrNo.YES)
            .isEqualTo(starts(() -> evictionScheduleService.checkCanSchedule(CASE_REFERENCE)));
        assertThat(schedule.getCanRescheduleEviction() == YesOrNo.YES)
            .isEqualTo(starts(() -> evictionScheduleService.checkCanReschedule(CASE_REFERENCE)));
        assertThat(schedule.getCanMarkNoticeOfEvictionServed() == YesOrNo.YES)
            .isEqualTo(starts(() -> evictionScheduleService.checkCanMarkNoticeServed(CASE_REFERENCE)));
    }

    @Test
    void shouldOfferOnlyApplyingForEnforcementAndRecordingAReturnWhenTheClaimHasNoEnforcement() {
        givenTheCurrentEnforcementIs(null);
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setEventAvailability(pcsCaseEntity, pcsCase, State.CASE_ISSUED);

        assertThat(pcsCase.getEnforcementOrder().getEventAvailability()).isEqualTo(
            EnforcementEventAvailability.builder()
                .canApplyForEnforcement(YesOrNo.YES)
                .canDecideEnforcement(YesOrNo.NO)
                .canAllocateBailiff(YesOrNo.NO)
                .canSetRiskLevel(YesOrNo.NO)
                .canRecordWarrantReturn(YesOrNo.YES)
                .canAddBailiffNote(YesOrNo.NO)
                .build());
    }

    @Test
    void shouldOfferTheDecisionOnAnApplicationAwaitingReview() {
        EnforcementEventAvailability flags =
            EnforcementEventAvailabilityView.availability(enforcement(SelectEnforcementType.WARRANT, AWAITING_REVIEW));

        assertThat(flags.getCanDecideEnforcement()).isEqualTo(YesOrNo.YES);
        assertThat(flags.getCanApplyForEnforcement()).isEqualTo(YesOrNo.NO);
        assertThat(flags.getCanAllocateBailiff()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldHideRecordWarrantReturnUntilABailiffIsAllocated() {
        EnforcementEventAvailability flags = EnforcementEventAvailabilityView.availability(
            enforcement(SelectEnforcementType.WARRANT, AWAITING_BAILIFF_ALLOCATION));

        assertThat(flags.getCanRecordWarrantReturn()).isEqualTo(YesOrNo.NO);
        assertThat(flags.getCanAllocateBailiff()).isEqualTo(YesOrNo.YES);
        assertThat(flags.getCanSetRiskLevel()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldLetTheClaimantApplyAgainOnceTheWarrantIsClosed() {
        EnforcementEventAvailability flags =
            EnforcementEventAvailabilityView.availability(enforcement(SelectEnforcementType.WARRANT, CLOSED));

        assertThat(flags.getCanApplyForEnforcement()).isEqualTo(YesOrNo.YES);
        assertThat(flags.getCanAddBailiffNote()).isEqualTo(YesOrNo.YES);
        assertThat(flags.getCanRecordWarrantReturn()).isEqualTo(YesOrNo.YES);
        assertThat(flags.getCanAllocateBailiff()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldKeepTheRestOfTheEnforcementOrder() {
        givenTheCurrentEnforcementIs(null);
        EnforcementOrder enforcementOrder = new EnforcementOrder();
        enforcementOrder.setHasUnsubmittedEnforcementData(YesOrNo.YES);
        PCSCase pcsCase = PCSCase.builder().enforcementOrder(enforcementOrder).build();

        underTest.setEventAvailability(pcsCaseEntity, pcsCase, State.CASE_ISSUED);

        assertThat(pcsCase.getEnforcementOrder()).isSameAs(enforcementOrder);
        assertThat(enforcementOrder.getHasUnsubmittedEnforcementData()).isEqualTo(YesOrNo.YES);
        assertThat(enforcementOrder.getEventAvailability()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = State.class,
        names = {"AWAITING_SUBMISSION_TO_HMCTS", "AWAITING_RESUBMISSION_TO_HMCTS", "PENDING_CASE_ISSUED"})
    void shouldSetNoFlagBeforeTheCaseIsIssued(State state) {
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setEventAvailability(pcsCaseEntity, pcsCase, state);

        assertThat(pcsCase.getEnforcementOrder()).isNull();
        verifyNoInteractions(currentEnforcementService);
    }

    private static Stream<Arguments> currentEnforcements() {
        List<Arguments> cases = new ArrayList<>();
        cases.add(Arguments.of("no enforcement", null));
        for (SelectEnforcementType type : SelectEnforcementType.values()) {
            for (EnforcementStatus status : EnforcementStatus.values()) {
                cases.add(Arguments.of(type + " " + status, enforcement(type, status)));
                EnforcementOrderEntity served = enforcement(type, status);
                served.setNoticeServedAt(LocalDateTime.parse("2026-06-20T09:00"));
                cases.add(Arguments.of(type + " " + status + ", notice served", served));
                EnforcementOrderEntity dispensed = enforcement(type, status);
                dispensed.setDispenseWithService(true);
                cases.add(Arguments.of(type + " " + status + ", service dispensed with", dispensed));
            }
        }
        return cases.stream();
    }

    private static EnforcementOrderEntity enforcement(SelectEnforcementType type, EnforcementStatus status) {
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setEnforcementOrder(buildEnforcementOrderWithSpecifiedType(type));
        if (type == SelectEnforcementType.WARRANT) {
            enforcement.setWarrantDetails(new WarrantEntity());
        } else if (type == SelectEnforcementType.WARRANT_OF_RESTITUTION) {
            enforcement.setWarrantOfRestitutionDetails(new WarrantOfRestitutionEntity());
        }
        enforcement.setStatus(status);
        enforcement.setBailiffDate(LocalDateTime.parse("2026-06-25T10:00"));
        enforcement.setDispenseWithService(false);
        return enforcement;
    }

    private void givenTheCurrentEnforcementIs(EnforcementOrderEntity current) {
        lenient().when(currentEnforcementService.findCurrentEnforcement(pcsCaseEntity))
            .thenReturn(Optional.ofNullable(current));
        lenient().when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE))
            .thenReturn(Optional.ofNullable(current));
    }

    /**
     * Whether a start check lets the event start. Every refusal is a runtime exception the callback exception
     * handler turns into an error.
     */
    private static boolean starts(Runnable startCheck) {
        try {
            startCheck.run();
            return true;
        } catch (RuntimeException refused) {
            return false;
        }
    }

    private static boolean refusedAsNotAvailable(Runnable startCheck) {
        try {
            startCheck.run();
            return false;
        } catch (EnforcementEventNotAvailableException refused) {
            return true;
        } catch (RuntimeException refusedWithAnotherError) {
            return false;
        }
    }
}
