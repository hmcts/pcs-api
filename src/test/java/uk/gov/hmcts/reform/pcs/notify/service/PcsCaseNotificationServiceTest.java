package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PcsCaseNotificationService Tests")
class PcsCaseNotificationServiceTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PcsCaseNotificationService underTest;

    private static final long CASE_REFERENCE = 1234567890L;

    @Test
    @DisplayName("Should successfully send claim issued notification")
    void shouldSuccessfullySendClaimIssuedNotification() {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        ClaimEntity claim = new ClaimEntity();
        pcsCaseEntity.setClaims(List.of(claim));

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        underTest.sendClaimIssuedNotificationOnPayment(CASE_REFERENCE);

        verify(pcsCaseService).loadCase(CASE_REFERENCE);
        verify(notificationService).sendClaimantClaimIssuedEmailNotification(claim);
    }

    @Test
    @DisplayName("Should throw ClaimNotFoundException when case has no claims")
    void shouldThrowClaimNotFoundExceptionWhenCaseHasNoClaims() {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setClaims(Collections.emptyList());

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        assertThatThrownBy(() -> underTest.sendClaimIssuedNotificationOnPayment(CASE_REFERENCE))
            .isInstanceOf(ClaimNotFoundException.class)
            .hasMessageContaining(String.valueOf(CASE_REFERENCE));

        verify(pcsCaseService).loadCase(CASE_REFERENCE);
    }
}
