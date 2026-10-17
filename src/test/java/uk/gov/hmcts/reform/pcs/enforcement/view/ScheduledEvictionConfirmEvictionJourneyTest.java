package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffNoteService;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementStatusTransitions;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionScheduleService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * The claimant's Confirm eviction journey lights up from the bailiff date. Scheduling the eviction sets it.
 */
@ExtendWith(MockitoExtension.class)
class ScheduledEvictionConfirmEvictionJourneyTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private BailiffNoteService bailiffNoteService;

    @Test
    void shouldShowTheClaimantTheConfirmEvictionJourneyOnceTheEvictionIsScheduled() {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        ClaimEntity claim = new ClaimEntity();
        claim.setId(UUID.randomUUID());
        pcsCaseEntity.setClaims(List.of(claim));
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setStatus(EnforcementStatus.AWAITING_SCHEDULING);
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.of(enforcement));
        when(enforcementOrderRepository.findByClaimId(claim.getId())).thenReturn(List.of(enforcement));
        EnforcementOrderMediator mediator = new EnforcementOrderMediator(enforcementOrderRepository);

        PCSCase before = PCSCase.builder().build();
        mediator.handleEnforcementRequirements(pcsCaseEntity, before);
        assertThat(before.getEnforcementOrder().getShowConfirmEvictionJourney()).isEqualTo(YesOrNo.NO);

        new EvictionScheduleService(currentEnforcementService, enforcementOrderRepository,
                                    new EnforcementStatusTransitions(
                                        Clock.fixed(Instant.parse("2026-06-20T09:00:00Z"), ZoneOffset.UTC)),
                                    bailiffNoteService)
            .schedule(CASE_REFERENCE, LocalDateTime.parse("2026-06-25T10:00"), false);

        PCSCase after = PCSCase.builder().build();
        mediator.handleEnforcementRequirements(pcsCaseEntity, after);
        assertThat(after.getEnforcementOrder().getShowConfirmEvictionJourney()).isEqualTo(YesOrNo.YES);
        assertThat(after.getEnforcementOrder().getConfirmEvictionSummaryMarkup())
            .contains("The bailiff has given you an eviction date of Thursday, 25 June 2026 at 10:00am")
            .contains("You must confirm the eviction details before 22 June 2026");
    }
}
