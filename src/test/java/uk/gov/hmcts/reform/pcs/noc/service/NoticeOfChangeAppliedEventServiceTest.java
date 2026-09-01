package uk.gov.hmcts.reform.pcs.noc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.noticeOfChangeApplied;

@ExtendWith(MockitoExtension.class)
class NoticeOfChangeAppliedEventServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String USER_BEARER = "Bearer idam";
    private static final String SERVICE_BEARER = "Bearer s2s";
    private static final String START_EVENT_REF = "event-token";

    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private CoreCaseDataApi coreCaseDataApi;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NoticeOfChangeAppliedEventService underTest;

    @Test
    void shouldStartAndSubmitTheNoticeOfChangeAppliedEventAsSystemUser() {
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(USER_BEARER);
        when(authTokenGenerator.generate()).thenReturn(SERVICE_BEARER);
        when(coreCaseDataApi.startEvent(USER_BEARER, SERVICE_BEARER, "1234", noticeOfChangeApplied.name()))
            .thenReturn(StartEventResponse.builder().token(START_EVENT_REF).build());
        when(objectMapper.valueToTree(any())).thenReturn(mock(JsonNode.class));
        CaseResource expected = new CaseResource();
        when(coreCaseDataApi.createEvent(eq(USER_BEARER), eq(SERVICE_BEARER), eq("1234"), any(CaseDataContent.class)))
            .thenReturn(expected);

        CaseResource result = underTest.submit(CASE_REFERENCE, "solicitor@example.com");

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<CaseDataContent> captor = ArgumentCaptor.forClass(CaseDataContent.class);
        verify(coreCaseDataApi).createEvent(eq(USER_BEARER), eq(SERVICE_BEARER), eq("1234"), captor.capture());
        assertThat(captor.getValue().getEvent().getId()).isEqualTo(noticeOfChangeApplied.name());
        assertThat(captor.getValue().getEventToken()).isEqualTo(START_EVENT_REF);
        assertThat(captor.getValue().getEvent().getSummary()).isEqualTo("Notice of change by solicitor@example.com");
    }
}
