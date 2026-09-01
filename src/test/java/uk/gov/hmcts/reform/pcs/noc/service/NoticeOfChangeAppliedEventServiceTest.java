package uk.gov.hmcts.reform.pcs.noc.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.SystemEventRecordingService;
import uk.gov.hmcts.ccd.sdk.SystemEventRecordingService.ActorAttribution;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.noticeOfChangeApplied;

@ExtendWith(MockitoExtension.class)
class NoticeOfChangeAppliedEventServiceTest {

    private static final long CASE_REFERENCE = 1234L;

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
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn("Bearer system");
        when(idamAuthenticator.validateAuthToken("Bearer system")).thenReturn(systemUser);
        NocAccessChangeTaskData taskData = NocAccessChangeTaskData.builder()
            .caseReference(String.valueOf(CASE_REFERENCE))
            .userId("uid-1")
            .email("solicitor@example.com")
            .firstName("Jane")
            .lastName("Doe")
            .build();

        underTest.submit(CASE_REFERENCE, taskData);

        verify(systemEventRecordingService).recordSystemEvent(
            CASE_REFERENCE,
            noticeOfChangeApplied.name(),
            "Bearer system",
            "Notice of change by solicitor@example.com",
            new ActorAttribution("uid-1", "Jane", "Doe")
        );
    }
}
