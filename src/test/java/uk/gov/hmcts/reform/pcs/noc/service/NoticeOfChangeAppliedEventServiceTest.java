package uk.gov.hmcts.reform.pcs.noc.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.ccd.sdk.SystemEventRecordingService;
import uk.gov.hmcts.ccd.sdk.SystemEventRecordingService.ActorAttribution;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.noticeOfChangeApplied;

@ExtendWith(MockitoExtension.class)
class NoticeOfChangeAppliedEventServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("6f4a7b7e-1111-2222-3333-444455556666");

    @Mock
    private SystemEventRecordingService systemEventRecordingService;
    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @Mock
    private IdamAuthenticator idamAuthenticator;
    @Mock
    private User systemUser;

    @InjectMocks
    private NoticeOfChangeAppliedEventService underTest;

    @Test
    void shouldRecordTheEventAttributedToTheActingSolicitor() {
        stubSystemUser();

        underTest.submit(CASE_REFERENCE, taskData().build());

        verify(systemEventRecordingService).recordSystemEvent(
            CASE_REFERENCE,
            noticeOfChangeApplied.name(),
            "Bearer system",
            "Notice of change by solicitor@example.com",
            new ActorAttribution("uid-1", "Jane", "Doe"),
            IDEMPOTENCY_KEY
        );
    }

    @Test
    void shouldRecordTheEventWithoutASummaryWhenTheEmailIsBlank() {
        stubSystemUser();

        underTest.submit(CASE_REFERENCE, taskData().email(" ").build());

        verify(systemEventRecordingService).recordSystemEvent(
            eq(CASE_REFERENCE),
            eq(noticeOfChangeApplied.name()),
            eq("Bearer system"),
            isNull(),
            eq(new ActorAttribution("uid-1", "Jane", "Doe")),
            eq(IDEMPOTENCY_KEY)
        );
    }

    @Test
    void shouldRestoreTheSecurityContextWhenRecordingFails() {
        stubSystemUser();
        doThrow(new IllegalStateException("boom")).when(systemEventRecordingService)
            .recordSystemEvent(anyLong(), anyString(), anyString(), any(), any(), any());

        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, taskData().build()))
            .isInstanceOf(IllegalStateException.class);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void stubSystemUser() {
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn("Bearer system");
        when(idamAuthenticator.validateAuthToken("Bearer system")).thenReturn(systemUser);
    }

    private NocAccessChangeTaskData.NocAccessChangeTaskDataBuilder taskData() {
        return NocAccessChangeTaskData.builder()
            .caseReference(String.valueOf(CASE_REFERENCE))
            .userId("uid-1")
            .email("solicitor@example.com")
            .firstName("Jane")
            .lastName("Doe")
            .eventIdempotencyKey(IDEMPOTENCY_KEY);
    }
}
