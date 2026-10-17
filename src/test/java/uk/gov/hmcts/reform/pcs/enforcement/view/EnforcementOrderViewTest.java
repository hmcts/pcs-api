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
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderViewTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID CLAIM_ID = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");

    @Mock
    private DraftCaseDataService draftCaseDataService;

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;

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

    @Test
    void shouldShowTheLiveEnforcementStatusOverMoreRecentlyUpdatedFinishedOnes() {
        // Given
        PcsCaseEntity pcsCaseEntity = pcsCaseEntityWithClaim();
        when(enforcementOrderRepository.findByClaimId(CLAIM_ID)).thenReturn(List.of(
            enforcement(EnforcementStatus.CLOSED, Instant.parse("2026-10-01T00:00:00Z")),
            enforcement(EnforcementStatus.AWAITING_SCHEDULING, Instant.parse("2026-09-01T00:00:00Z")),
            enforcement(EnforcementStatus.REJECTED, Instant.parse("2026-10-02T00:00:00Z"))
        ));
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setEnforcementStatus(pcsCaseEntity, pcsCase);

        // Then
        assertThat(pcsCase.getEnforcementOrder().getEnforcementStatus())
            .isEqualTo(EnforcementStatus.AWAITING_SCHEDULING);
    }

    @Test
    void shouldShowTheMostRecentlyUpdatedStatusWhenNoEnforcementIsLive() {
        // Given
        PcsCaseEntity pcsCaseEntity = pcsCaseEntityWithClaim();
        when(enforcementOrderRepository.findByClaimId(CLAIM_ID)).thenReturn(List.of(
            enforcement(EnforcementStatus.CLOSED, null),
            enforcement(EnforcementStatus.REJECTED, Instant.parse("2026-10-02T00:00:00Z")),
            enforcement(EnforcementStatus.CLOSED, Instant.parse("2026-10-01T00:00:00Z")),
            enforcement(null, Instant.parse("2026-10-03T00:00:00Z"))
        ));
        PCSCase pcsCase = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().formattedDefendantNames("names").build())
            .build();

        // When
        underTest.setEnforcementStatus(pcsCaseEntity, pcsCase);

        // Then
        assertThat(pcsCase.getEnforcementOrder().getEnforcementStatus()).isEqualTo(EnforcementStatus.REJECTED);
        assertThat(pcsCase.getEnforcementOrder().getFormattedDefendantNames()).isEqualTo("names");
    }

    @Test
    void shouldLeaveTheStatusUnsetWhenTheClaimHasNoEnforcement() {
        // Given
        PcsCaseEntity pcsCaseEntity = pcsCaseEntityWithClaim();
        when(enforcementOrderRepository.findByClaimId(CLAIM_ID)).thenReturn(List.of());
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setEnforcementStatus(pcsCaseEntity, pcsCase);

        // Then
        assertThat(pcsCase.getEnforcementOrder()).isNull();
    }

    @Test
    void shouldNotLookUpEnforcementsWhenTheCaseHasNoClaim() {
        // Given
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setClaims(List.of());
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setEnforcementStatus(pcsCaseEntity, pcsCase);
        underTest.setEnforcementStatus(null, pcsCase);

        // Then
        verifyNoInteractions(enforcementOrderRepository);
        assertThat(pcsCase.getEnforcementOrder()).isNull();
    }

    @Test
    void shouldKeepTheCaseViewStatusWhenDraftReplacesEnforcementOrder() {
        // Given
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder)).thenReturn(true);
        EnforcementOrder draftOrder = EnforcementOrder.builder()
            .enforcementStatus(EnforcementStatus.AWAITING_PAYMENT)
            .build();
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, enforceTheOrder))
            .thenReturn(Optional.of(PCSCase.builder().enforcementOrder(draftOrder).build()));
        PCSCase pcsCase = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().enforcementStatus(EnforcementStatus.CLOSED).build())
            .build();

        // When
        underTest.setCaseFields(pcsCase, CASE_REFERENCE, State.CASE_ISSUED);

        // Then
        assertThat(pcsCase.getEnforcementOrder().getEnforcementStatus()).isEqualTo(EnforcementStatus.CLOSED);
    }

    private static PcsCaseEntity pcsCaseEntityWithClaim() {
        ClaimEntity claim = new ClaimEntity();
        claim.setId(CLAIM_ID);
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setClaims(List.of(claim));
        return pcsCaseEntity;
    }

    private static EnforcementOrderEntity enforcement(EnforcementStatus status, Instant statusUpdatedAt) {
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setStatus(status);
        enforcement.setStatusUpdatedAt(statusUpdatedAt);
        return enforcement;
    }

    private static Stream<Arguments> enforcementDraftScenarios() {
        return Stream.of(
            Arguments.of(State.CASE_ISSUED, true, YesOrNo.YES),
            Arguments.of(State.CASE_ISSUED, false, YesOrNo.NO),
            Arguments.of(State.PENDING_CASE_ISSUED, false, YesOrNo.NO)
        );
    }
}
