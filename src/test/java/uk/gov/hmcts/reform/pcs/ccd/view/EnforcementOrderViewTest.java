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

        // When
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, state);

        // Then
        if (state == State.CASE_ISSUED) {
            verify(draftCaseDataService).hasUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder);
            assertThat(pcsCase.getEnforcementOrder().getHasUnsubmittedEnforcementData()).isEqualTo(expectedFlag);
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
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, State.PENDING_CASE_ISSUED);

        // Then
        verify(draftCaseDataService, never()).hasUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder);
        assertThat(pcsCase.getEnforcementOrder()).isNull();
    }

    private static Stream<Arguments> enforcementDraftScenarios() {
        return Stream.of(
            Arguments.of(State.CASE_ISSUED, true, YesOrNo.YES),
            Arguments.of(State.CASE_ISSUED, false, YesOrNo.NO),
            Arguments.of(State.PENDING_CASE_ISSUED, false, YesOrNo.NO)
        );
    }
}
