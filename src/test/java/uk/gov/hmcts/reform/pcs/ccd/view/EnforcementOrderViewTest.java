package uk.gov.hmcts.reform.pcs.ccd.view;

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
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderStepRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderViewTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID CASE_ID = UUID.randomUUID();

    @Mock
    private DraftCaseDataService draftCaseDataService;

    @Mock
    private OrderStepRepository orderStepRepository;

    @InjectMocks
    private EnforcementOrderView underTest;

    @ParameterizedTest
    @MethodSource("enforcementDraftScenarios")
    void shouldSetUnsubmittedEnforcementDataFlag(State state, boolean hasDraftData, YesOrNo expectedFlag) {
        // Given
        if (state == State.CASE_ISSUED) {
            when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder)).thenReturn(hasDraftData);
        }

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, CASE_ID, state);

        // Then
        if (state == State.CASE_ISSUED) {
            verify(draftCaseDataService).hasUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder);
            assertThat(pcsCase.getHasUnsubmittedEnforcementData()).isEqualTo(expectedFlag);
            if (hasDraftData) {
                verify(draftCaseDataService).getUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder);
            }
        } else {
            verify(draftCaseDataService, never()).hasUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder);
        }
    }

    @Test
    void shouldNotPopulateEnforcementOrderWhenCaseNotIssued() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, CASE_ID, State.PENDING_CASE_ISSUED);

        // Then
        verify(draftCaseDataService, never()).hasUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder);
        verify(orderStepRepository, never()).findLatestIssuedByCaseId(any());
        assertThat(pcsCase.getEnforcementOrder()).isNull();
        assertThat(pcsCase.getEnforcementIsSuspendedOrder()).isNull();
    }

    @Test
    void shouldTakeTheSuspendedJourneyWhenTheLatestIssuedOrderIsSuspended() {
        // Given
        stubLatestIssuedOrder(OrderType.SUSPENDED_POSSESSION);
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, CASE_ID, State.CASE_ISSUED);

        // Then
        assertThat(pcsCase.getEnforcementIsSuspendedOrder()).isEqualTo(YesOrNo.YES);
    }

    @ParameterizedTest
    @EnumSource(value = OrderType.class, names = "SUSPENDED_POSSESSION", mode = EnumSource.Mode.EXCLUDE)
    void shouldTakeTheNotSuspendedJourneyWhenTheLatestIssuedOrderIsAnyOtherType(OrderType orderType) {
        // Given
        stubLatestIssuedOrder(orderType);
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, CASE_ID, State.CASE_ISSUED);

        // Then
        assertThat(pcsCase.getEnforcementIsSuspendedOrder()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldTakeTheNotSuspendedJourneyWhenNoOrderHasBeenIssued() {
        // Given
        when(orderStepRepository.findLatestIssuedByCaseId(CASE_ID)).thenReturn(Optional.empty());
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, CASE_ID, State.CASE_ISSUED);

        // Then
        assertThat(pcsCase.getEnforcementIsSuspendedOrder()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldFollowTheLatestIssuedOrderWhenResumingADraft() {
        // Given a draft saved before the case's suspended order was issued
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder)).thenReturn(true);
        PCSCase draft = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build())
            .enforcementIsSuspendedOrder(YesOrNo.NO)
            .build();
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder))
            .thenReturn(Optional.of(draft));
        stubLatestIssuedOrder(OrderType.SUSPENDED_POSSESSION);
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, CASE_ID, State.CASE_ISSUED);

        // Then
        assertThat(pcsCase.getEnforcementOrder()).isSameAs(draft.getEnforcementOrder());
        assertThat(pcsCase.getEnforcementIsSuspendedOrder()).isEqualTo(YesOrNo.YES);
    }

    private void stubLatestIssuedOrder(OrderType orderType) {
        OrderStepEntity step = OrderStepEntity.builder().orderType(orderType).build();
        when(orderStepRepository.findLatestIssuedByCaseId(CASE_ID)).thenReturn(Optional.of(step));
    }

    private static Stream<Arguments> enforcementDraftScenarios() {
        return Stream.of(
            Arguments.of(State.CASE_ISSUED, true, YesOrNo.YES),
            Arguments.of(State.CASE_ISSUED, false, YesOrNo.NO),
            Arguments.of(State.PENDING_CASE_ISSUED, false, YesOrNo.NO)
        );
    }
}
