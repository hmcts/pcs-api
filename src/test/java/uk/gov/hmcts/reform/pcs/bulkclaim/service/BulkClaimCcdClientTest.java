package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkValidationError;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkClaimCcdClientTest {

    private static final String IDAM_TOKEN = "Bearer system";
    private static final String S2S_TOKEN = "s2s";
    private static final String EVENT_TOKEN = "event-token";
    private static final UUID SUBMISSION_ID = UUID.randomUUID();

    @Mock
    private IdamTokenProvider tokenProvider;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private CoreCaseDataApi coreCaseDataApi;

    private BulkClaimCcdClient underTest;

    @BeforeEach
    void setUp() {
        underTest = new BulkClaimCcdClient(tokenProvider, authTokenGenerator, coreCaseDataApi, new ObjectMapper());
        when(tokenProvider.getAuthToken()).thenReturn(IDAM_TOKEN);
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
    }

    @Test
    void shouldStartAndSubmitCreateEventCarryingOnlyTheSubmissionId() {
        when(coreCaseDataApi.startCase(eq(IDAM_TOKEN), eq(S2S_TOKEN), any(), eq("createBulkPossessionClaim")))
            .thenReturn(StartEventResponse.builder().token(EVENT_TOKEN).build());
        when(coreCaseDataApi.submitCaseCreation(eq(IDAM_TOKEN), eq(S2S_TOKEN), any(), any(CaseDataContent.class)))
            .thenReturn(CaseDetails.builder().id(1781000000000001L).build());

        BulkClaimCcdResult result = underTest.createCase(SUBMISSION_ID);

        assertThat(result.isCreated()).isTrue();
        assertThat(result.caseReference()).isEqualTo(1781000000000001L);
        ArgumentCaptor<CaseDataContent> content = ArgumentCaptor.forClass(CaseDataContent.class);
        verify(coreCaseDataApi).submitCaseCreation(eq(IDAM_TOKEN), eq(S2S_TOKEN), any(), content.capture());
        assertThat(content.getValue().getEventToken()).isEqualTo(EVENT_TOKEN);
        assertThat(content.getValue().getEvent().getId()).isEqualTo("createBulkPossessionClaim");
        assertThat(content.getValue().getData())
            .isEqualTo(Map.of("bulkClaimSubmissionId", SUBMISSION_ID.toString()));
    }

    @Test
    void shouldMapCallbackErrorsOfA422ToFailReasonAndValidationErrors() {
        givenStartedEvent();
        String body = """
            {"message":"Unable to proceed because there are one or more callback validation errors",
             "callbackErrors":["BULK|9|Defendant/1|The defendant is not valid","BULK|9|Property|Bad address"],
             "callbackWarnings":[]}""";
        when(coreCaseDataApi.submitCaseCreation(any(), any(), any(), any(CaseDataContent.class)))
            .thenThrow(unprocessable(body));

        BulkClaimCcdResult result = underTest.createCase(SUBMISSION_ID);

        assertThat(result.isCreated()).isFalse();
        assertThat(result.retryable()).isFalse();
        assertThat(result.failReason()).isEqualTo(BulkFailReason.VALIDATION_FAILED);
        assertThat(result.errors()).containsExactly(
            new BulkValidationError("Defendant/1", "CCD_REJECTED", "The defendant is not valid"),
            new BulkValidationError("Property", "CCD_REJECTED", "Bad address"));
    }

    @Test
    void shouldReportSaveFailedWhenA422BodyHasNoBulkErrors() {
        givenStartedEvent();
        when(coreCaseDataApi.submitCaseCreation(any(), any(), any(), any(CaseDataContent.class)))
            .thenThrow(unprocessable("{\"message\":\"field not valid\"}"));

        BulkClaimCcdResult result = underTest.createCase(SUBMISSION_ID);

        assertThat(result.failReason()).isEqualTo(BulkFailReason.SAVE_FAILED);
        assertThat(result.retryable()).isFalse();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void shouldReportSaveFailedWhenA422BodyIsNotJson() {
        givenStartedEvent();
        when(coreCaseDataApi.submitCaseCreation(any(), any(), any(), any(CaseDataContent.class)))
            .thenThrow(unprocessable("<html>nope</html>"));

        BulkClaimCcdResult result = underTest.createCase(SUBMISSION_ID);

        assertThat(result.failReason()).isEqualTo(BulkFailReason.SAVE_FAILED);
        assertThat(result.retryable()).isFalse();
    }

    @Test
    void shouldTreatServerErrorsAsRetryableSaveFailed() {
        givenStartedEvent();
        when(coreCaseDataApi.submitCaseCreation(any(), any(), any(), any(CaseDataContent.class)))
            .thenThrow(FeignException.errorStatus("submitCaseCreation", response(503, "unavailable")));

        BulkClaimCcdResult result = underTest.createCase(SUBMISSION_ID);

        assertThat(result.failReason()).isEqualTo(BulkFailReason.SAVE_FAILED);
        assertThat(result.retryable()).isTrue();
    }

    @Test
    void shouldTreatTimeoutsAsRetryableSaveFailed() {
        when(coreCaseDataApi.startCase(any(), any(), any(), any()))
            .thenThrow(new RetryableException(-1, "Read timed out", Request.HttpMethod.POST, new Date(), request()));

        BulkClaimCcdResult result = underTest.createCase(SUBMISSION_ID);

        assertThat(result.failReason()).isEqualTo(BulkFailReason.SAVE_FAILED);
        assertThat(result.retryable()).isTrue();
    }

    @Test
    void shouldTreatOtherClientErrorsAsFinalSaveFailed() {
        when(coreCaseDataApi.startCase(any(), any(), any(), any()))
            .thenThrow(FeignException.errorStatus("startCase", response(403, "forbidden")));

        BulkClaimCcdResult result = underTest.createCase(SUBMISSION_ID);

        assertThat(result.failReason()).isEqualTo(BulkFailReason.SAVE_FAILED);
        assertThat(result.retryable()).isFalse();
    }

    private void givenStartedEvent() {
        when(coreCaseDataApi.startCase(any(), any(), any(), any()))
            .thenReturn(StartEventResponse.builder().token(EVENT_TOKEN).build());
    }

    private static FeignException unprocessable(String body) {
        return FeignException.errorStatus("submitCaseCreation", response(422, body));
    }

    private static feign.Response response(int status, String body) {
        return feign.Response.builder()
            .status(status)
            .reason("reason")
            .request(request())
            .headers(Map.<String, Collection<String>>of())
            .body(body, StandardCharsets.UTF_8)
            .build();
    }

    private static Request request() {
        return Request.create(Request.HttpMethod.POST, "http://ccd/case-types/PCS/cases", Map.of(),
                              null, StandardCharsets.UTF_8, null);
    }
}
