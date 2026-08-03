package uk.gov.hmcts.reform.pcs.ccd.event.service;

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
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.claimIssuePayment;

@ExtendWith(MockitoExtension.class)
class CcdPaymentStateUpdateServiceTest {

    private static final long CASE_ID = 1234L;
    private static final String IDAM_TOKEN = "Bearer idam";
    private static final String S2S_TOKEN = "Bearer s2s";

    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @Mock
    private AuthTokenGenerator s2sAuthTokenGenerator;
    @Mock
    private CoreCaseDataApi coreCaseDataApi;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ClaimEntity claim;

    @InjectMocks
    private CcdPaymentStateUpdateService underTest;

    @Test
    void shouldStartEventAndSubmitPaymentSuccessfully() {
        // Given
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(IDAM_TOKEN);
        when(s2sAuthTokenGenerator.generate()).thenReturn(S2S_TOKEN);

        StartEventResponse startEventResponse = StartEventResponse.builder().token(IDAM_TOKEN).build();
        when(coreCaseDataApi.startEvent(IDAM_TOKEN, S2S_TOKEN, String.valueOf(CASE_ID), claimIssuePayment.name()))
            .thenReturn(startEventResponse);

        CaseResource expectedCaseResource = new CaseResource();
        when(coreCaseDataApi.createEvent(eq(IDAM_TOKEN), eq(S2S_TOKEN), eq(String.valueOf(CASE_ID)),
                                         any(CaseDataContent.class)))
            .thenReturn(expectedCaseResource);
        when(objectMapper.valueToTree(any())).thenReturn(mock(JsonNode.class));

        // When
        CaseResource result = underTest.submitPaymentSuccess(CASE_ID);

        // Then
        assertThat(result).isSameAs(expectedCaseResource);
        verify(coreCaseDataApi).startEvent(IDAM_TOKEN, S2S_TOKEN, String.valueOf(CASE_ID), claimIssuePayment.name());
        ArgumentCaptor<CaseDataContent> contentCaptor = ArgumentCaptor.forClass(CaseDataContent.class);
        verify(coreCaseDataApi).createEvent(eq(IDAM_TOKEN), eq(S2S_TOKEN), eq(String.valueOf(CASE_ID)),
                                            contentCaptor.capture());
        CaseDataContent submitted = contentCaptor.getValue();
        assertThat(submitted.getEventToken()).isEqualTo(IDAM_TOKEN);
        assertThat(submitted.getEvent().getId()).isEqualTo(claimIssuePayment.name());
    }
}
