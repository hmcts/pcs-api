package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementStateEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderViewTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private DraftCaseDataService draftCaseDataService;

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
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        // When
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, state, pcsCaseEntity);

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
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        // When
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, State.PENDING_CASE_ISSUED, pcsCaseEntity);

        // Then
        verify(draftCaseDataService, never()).hasUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder);
        assertThat(pcsCase.getEnforcementOrder()).isNull();
    }

    @Test
    void shouldPopulateActiveEnforcementState() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .enforcementState(
                EnforcementStateEntity.builder()
                    .state(EnforcementState.AWAITING_BAILIFF_ALLOCATION)
                    .build()
            ).build();

        // When
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, State.PENDING_CASE_ISSUED, pcsCaseEntity);

        // Then
        assertThat(pcsCase.getActiveEnforcementState()).isEqualTo(EnforcementState.AWAITING_BAILIFF_ALLOCATION);
    }

    @ParameterizedTest
    @MethodSource("recordWarrantReturnScenarios")
    void shouldSetCanRecordWarrantReturnFromActiveEnforcementState(EnforcementState enforcementState,
                                                                   YesOrNo expected) {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .enforcementState(EnforcementStateEntity.builder().state(enforcementState).build())
            .build();

        // When
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, State.CASE_ISSUED, pcsCaseEntity);

        // Then
        assertThat(pcsCase.getCanRecordWarrantReturn()).isEqualTo(expected);
    }

    @Test
    void shouldNotAllowRecordWarrantReturnWhenNoEnforcementState() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        // When
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, State.CASE_ISSUED, pcsCaseEntity);

        // Then
        assertThat(pcsCase.getCanRecordWarrantReturn()).isEqualTo(YesOrNo.NO);
    }

    private static Stream<Arguments> enforcementDraftScenarios() {
        return Stream.of(
            Arguments.of(State.CASE_ISSUED, true, YesOrNo.YES),
            Arguments.of(State.CASE_ISSUED, false, YesOrNo.NO),
            Arguments.of(State.PENDING_CASE_ISSUED, false, YesOrNo.NO)
        );
    }

    private static Stream<Arguments> recordWarrantReturnScenarios() {
        return Stream.of(
            Arguments.of(EnforcementState.AWAITING_SUBMISSION_TO_HMCTS, YesOrNo.NO),
            Arguments.of(EnforcementState.PENDING_CASE_ISSUED, YesOrNo.NO),
            Arguments.of(EnforcementState.CASE_PROGRESSION, YesOrNo.YES),
            Arguments.of(EnforcementState.BREATHING_SPACE, YesOrNo.YES),
            Arguments.of(EnforcementState.AWAITING_ENFORCEMENT_OUTCOME, YesOrNo.YES),
            Arguments.of(EnforcementState.AWAITING_PROPERTY_VISIT, YesOrNo.YES),
            Arguments.of(EnforcementState.AWAITING_SCHEDULING, YesOrNo.YES),
            Arguments.of(EnforcementState.AWAITING_BAILIFF_ALLOCATION, YesOrNo.YES),
            Arguments.of(EnforcementState.JUDICIAL_REFERRAL, YesOrNo.YES),
            Arguments.of(EnforcementState.HEARING_READINESS, YesOrNo.YES),
            Arguments.of(EnforcementState.PREPARE_FOR_HEARING_CONDUCT_HEARING, YesOrNo.YES),
            Arguments.of(EnforcementState.CLOSED, YesOrNo.YES),
            Arguments.of(EnforcementState.CASE_STAYED, YesOrNo.YES),
            Arguments.of(EnforcementState.REQUESTED_FOR_DELETION, YesOrNo.NO),
            Arguments.of(EnforcementState.DRAFT_DISCARDED, YesOrNo.NO)
        );
    }
}
