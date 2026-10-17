package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;
import static uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDataUtil.buildPcsCaseEntity;

@ExtendWith(MockitoExtension.class)
class ClaimantEvictionConfirmationServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID CLAIM_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-17T09:00:00Z");
    private static final Instant EARLIER = Instant.parse("2026-10-01T09:00:00Z");

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;

    private ClaimantEvictionConfirmationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new ClaimantEvictionConfirmationService(
            pcsCaseService, enforcementOrderRepository,
            new EnforcementStatusTransitions(Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    @Test
    void shouldMoveToClaimantConfirmedWhenTheClaimantCanAttend() {
        EnforcementOrderEntity enforcement = givenEnforcements(enforcementWithStatus(AWAITING_CLAIMANT_CONFIRMATION))
            .getFirst();

        underTest.recordAttendance(CASE_REFERENCE, YesOrNo.YES);

        assertThat(enforcement.getStatus()).isEqualTo(CLAIMANT_CONFIRMED);
        assertThat(enforcement.getStatusUpdatedAt()).isEqualTo(NOW);
        verify(enforcementOrderRepository).save(enforcement);
    }

    @Test
    void shouldMoveToClaimantUnableToAttendWhenTheClaimantCannotAttend() {
        EnforcementOrderEntity enforcement = givenEnforcements(enforcementWithStatus(AWAITING_CLAIMANT_CONFIRMATION))
            .getFirst();

        underTest.recordAttendance(CASE_REFERENCE, YesOrNo.NO);

        assertThat(enforcement.getStatus()).isEqualTo(CLAIMANT_UNABLE_TO_ATTEND);
        assertThat(enforcement.getStatusUpdatedAt()).isEqualTo(NOW);
        verify(enforcementOrderRepository).save(enforcement);
    }

    @Test
    void shouldMoveTheLiveEnforcementAndNotAClosedOne() {
        EnforcementOrderEntity closed = enforcementWithStatus(CLOSED);
        EnforcementOrderEntity live = enforcementWithStatus(AWAITING_CLAIMANT_CONFIRMATION);
        givenEnforcements(closed, live);

        underTest.recordAttendance(CASE_REFERENCE, YesOrNo.YES);

        assertThat(live.getStatus()).isEqualTo(CLAIMANT_CONFIRMED);
        assertThat(closed.getStatus()).isEqualTo(CLOSED);
        assertThat(closed.getStatusUpdatedAt()).isEqualTo(EARLIER);
        verify(enforcementOrderRepository).save(live);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = "AWAITING_CLAIMANT_CONFIRMATION",
        mode = EnumSource.Mode.EXCLUDE)
    void shouldNotChangeTheStatusWhenTheEnforcementIsNotAwaitingConfirmation(EnforcementStatus status) {
        EnforcementOrderEntity enforcement = givenEnforcements(enforcementWithStatus(status)).getFirst();

        underTest.recordAttendance(CASE_REFERENCE, YesOrNo.YES);
        underTest.recordAttendance(CASE_REFERENCE, YesOrNo.NO);

        assertThat(enforcement.getStatus()).isEqualTo(status);
        assertThat(enforcement.getStatusUpdatedAt()).isEqualTo(EARLIER);
        verify(enforcementOrderRepository, never()).save(any());
    }

    @Test
    void shouldNotChangeAnythingWhenTheEnforcementHasNoStatus() {
        EnforcementOrderEntity enforcement = givenEnforcements(enforcementWithStatus(null)).getFirst();

        underTest.recordAttendance(CASE_REFERENCE, YesOrNo.YES);

        assertThat(enforcement.getStatus()).isNull();
        verify(enforcementOrderRepository, never()).save(any());
    }

    @Test
    void shouldNotChangeAnythingWhenTheClaimHasNoEnforcement() {
        givenEnforcements();

        underTest.recordAttendance(CASE_REFERENCE, YesOrNo.YES);

        verify(enforcementOrderRepository, never()).save(any());
    }

    @Test
    void shouldNotChangeAnythingWhenTheCaseHasNoClaim() {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.setClaims(List.of());
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);

        underTest.recordAttendance(CASE_REFERENCE, YesOrNo.YES);

        verifyNoInteractions(enforcementOrderRepository);
    }

    @Test
    void shouldNotChangeAnythingWhenThereIsNoAnswer() {
        underTest.recordAttendance(CASE_REFERENCE, null);

        verifyNoInteractions(pcsCaseService, enforcementOrderRepository);
    }

    private List<EnforcementOrderEntity> givenEnforcements(EnforcementOrderEntity... enforcements) {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(buildPcsCaseEntity(UUID.randomUUID(), CLAIM_ID));
        List<EnforcementOrderEntity> list = List.of(enforcements);
        when(enforcementOrderRepository.findByClaimId(CLAIM_ID)).thenReturn(list);
        return list;
    }

    private static EnforcementOrderEntity enforcementWithStatus(EnforcementStatus status) {
        return EnforcementOrderEntity.builder()
            .id(UUID.randomUUID())
            .status(status)
            .statusUpdatedAt(status == null ? null : EARLIER)
            .build();
    }
}
