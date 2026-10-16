package uk.gov.hmcts.reform.pcs.enforcement.view;

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
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;

import java.util.Optional;
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

    @Test
    void shouldKeepViewDerivedFieldsWhenDraftReplacesEnforcementOrder() {
        // Given
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder)).thenReturn(true);
        EnforcementOrder draftOrder = EnforcementOrder.builder()
            .deleteDraftApplication(YesOrNo.NO)
            .confirmEvictionSummaryMarkup("stale draft markup")
            .showConfirmEvictionJourney(YesOrNo.NO)
            .formattedDefendantNames("draft defendant names")
            .formattedPropertyAddress("draft property address")
            .build();
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder))
            .thenReturn(Optional.of(PCSCase.builder().enforcementOrder(draftOrder).build()));

        PCSCase pcsCase = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .confirmEvictionSummaryMarkup("case view markup")
                                  .showConfirmEvictionJourney(YesOrNo.YES)
                                  .build())
            .build();

        // When
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, State.CASE_ISSUED);

        // Then
        EnforcementOrder result = pcsCase.getEnforcementOrder();
        assertThat(result.getDeleteDraftApplication()).isEqualTo(YesOrNo.NO);
        assertThat(result.getConfirmEvictionSummaryMarkup()).isEqualTo("case view markup");
        assertThat(result.getShowConfirmEvictionJourney()).isEqualTo(YesOrNo.YES);
        assertThat(result.getFormattedDefendantNames()).isNull();
        assertThat(result.getFormattedPropertyAddress()).isNull();
        assertThat(result.getHasUnsubmittedEnforcementData()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldKeepCurrentEnforcementOrderWhenDraftHasNone() {
        // Given
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder)).thenReturn(true);
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder))
            .thenReturn(Optional.of(PCSCase.builder().build()));

        EnforcementOrder currentOrder = EnforcementOrder.builder()
            .confirmEvictionSummaryMarkup("case view markup")
            .showConfirmEvictionJourney(YesOrNo.YES)
            .formattedDefendantNames("defendant names")
            .formattedPropertyAddress("property address")
            .build();
        PCSCase pcsCase = PCSCase.builder().enforcementOrder(currentOrder).build();

        // When
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, State.CASE_ISSUED);

        // Then
        EnforcementOrder result = pcsCase.getEnforcementOrder();
        assertThat(result).isSameAs(currentOrder);
        assertThat(result.getConfirmEvictionSummaryMarkup()).isEqualTo("case view markup");
        assertThat(result.getShowConfirmEvictionJourney()).isEqualTo(YesOrNo.YES);
        assertThat(result.getFormattedDefendantNames()).isEqualTo("defendant names");
        assertThat(result.getFormattedPropertyAddress()).isEqualTo("property address");
        assertThat(result.getHasUnsubmittedEnforcementData()).isEqualTo(YesOrNo.YES);
    }

    private static Stream<Arguments> enforcementDraftScenarios() {
        return Stream.of(
            Arguments.of(State.CASE_ISSUED, true, YesOrNo.YES),
            Arguments.of(State.CASE_ISSUED, false, YesOrNo.NO),
            Arguments.of(State.PENDING_CASE_ISSUED, false, YesOrNo.NO)
        );
    }
}
