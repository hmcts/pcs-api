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
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.enforcementorder.EnforcementOrderMediator;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementEventShowConditionsViewTest {

    @Mock
    private EnforcementOrderMediator enforcementOrderMediator;

    @InjectMocks
    private EnforcementEventShowConditionsView enforcementEventShowConditionsView;

    @ParameterizedTest
    @MethodSource("recordWarrantReturnScenarios")
    void shouldSetCanRecordWarrantReturnFromActiveEnforcementState(EnforcementState enforcementState,
                                                                   YesOrNo expected) {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        when(enforcementOrderMediator.getEnforcementOrder(pcsCaseEntity))
            .thenReturn(Optional.of(EnforcementOrderEntity.builder().state(enforcementState).build()));

        // When
        enforcementEventShowConditionsView.setCaseFields(pcsCaseEntity, pcsCase);

        // Then
        assertThat(pcsCase.getEnforcementEventShowConditions().getCanRecordWarrantReturn()).isEqualTo(expected);
    }

    @Test
    void shouldNotAllowRecordWarrantReturnWhenNoEnforcementState() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        when(enforcementOrderMediator.getEnforcementOrder(pcsCaseEntity)).thenReturn(Optional.empty());

        // When
        enforcementEventShowConditionsView.setCaseFields(pcsCaseEntity, pcsCase);

        // Then
        assertThat(pcsCase.getEnforcementEventShowConditions().getCanRecordWarrantReturn()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldNotAllowRecordWarrantReturnWhenEnforcementOrderHasNoState() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        when(enforcementOrderMediator.getEnforcementOrder(pcsCaseEntity))
            .thenReturn(Optional.of(EnforcementOrderEntity.builder().build()));

        // When
        enforcementEventShowConditionsView.setCaseFields(pcsCaseEntity, pcsCase);

        // Then
        assertThat(pcsCase.getEnforcementEventShowConditions().getCanRecordWarrantReturn()).isEqualTo(YesOrNo.NO);
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
