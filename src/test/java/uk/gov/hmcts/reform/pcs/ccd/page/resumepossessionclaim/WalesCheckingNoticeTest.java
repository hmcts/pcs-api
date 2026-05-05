package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.wales.WalesRentSectionRoutingService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalesCheckingNoticeTest extends BasePageTest {

    @Mock
    private WalesRentSectionRoutingService walesRentSectionRoutingService;

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new WalesCheckingNotice(walesRentSectionRoutingService, textAreaValidationService));
    }

    @Test
    void shouldSetShowRentSectionPageToYesWhenRoutingServiceReturnsYes() {
        // Given
        PCSCase caseData = PCSCase.builder().build();
        when(walesRentSectionRoutingService.shouldShowRentSection(any(PCSCase.class)))
            .thenReturn(YesOrNo.YES);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isSameAs(caseData);
        assertThat(response.getData().getShowRentSectionPage()).isEqualTo(YesOrNo.YES);
        verify(walesRentSectionRoutingService).shouldShowRentSection(caseData);
    }

    @Test
    void shouldSetShowRentSectionPageToNoWhenRoutingServiceReturnsNo() {
        // Given
        PCSCase caseData = PCSCase.builder().build();
        when(walesRentSectionRoutingService.shouldShowRentSection(any(PCSCase.class)))
            .thenReturn(YesOrNo.NO);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isSameAs(caseData);
        assertThat(response.getData().getShowRentSectionPage()).isEqualTo(YesOrNo.NO);
        verify(walesRentSectionRoutingService).shouldShowRentSection(caseData);
    }

    @Test
    void shouldValidateNoticeStatementWhenNoticeServedIsNo() {
        // Given
        WalesNoticeDetails walesNoticeDetails = WalesNoticeDetails.builder()
            .noticeServed(YesOrNo.NO)
            .noticeStatement("Valid notice statement")
            .build();
        PCSCase caseData = PCSCase.builder()
            .walesNoticeDetails(walesNoticeDetails)
            .build();
        when(walesRentSectionRoutingService.shouldShowRentSection(any(PCSCase.class)))
            .thenReturn(YesOrNo.NO);
        when(textAreaValidationService.validateSingleTextArea("Valid notice statement",
            WalesNoticeDetails.NOTICE_STATEMENT_LABEL, TextAreaValidationService.MEDIUM_TEXT_LIMIT))
            .thenReturn(Collections.emptyList());

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isSameAs(caseData);
        verify(textAreaValidationService).validateSingleTextArea("Valid notice statement",
            WalesNoticeDetails.NOTICE_STATEMENT_LABEL, TextAreaValidationService.MEDIUM_TEXT_LIMIT);
    }

    @Test
    void shouldReturnValidationErrorsWhenNoticeStatementExceeds500CharsAndNoticeServedIsNo() {
        // Given
        WalesNoticeDetails walesNoticeDetails = WalesNoticeDetails.builder()
            .noticeServed(YesOrNo.NO)
            .noticeStatement("x".repeat(501))
            .build();
        PCSCase caseData = PCSCase.builder()
            .walesNoticeDetails(walesNoticeDetails)
            .build();
        List<String> errors = List.of("Notice statement must not exceed 500 characters");
        when(walesRentSectionRoutingService.shouldShowRentSection(any(PCSCase.class)))
            .thenReturn(YesOrNo.NO);
        when(textAreaValidationService.validateSingleTextArea(any(), anyString(), anyInt()))
            .thenReturn(errors);
        when(textAreaValidationService.createValidationResponse(any(), any()))
            .thenReturn(AboutToStartOrSubmitResponse.builder().errorMessageOverride("error").build());

        // When
        callMidEventHandler(caseData);

        // Then
        verify(textAreaValidationService).validateSingleTextArea(any(), anyString(), anyInt());
        verify(textAreaValidationService).createValidationResponse(any(), any());
    }

    @Test
    void shouldNotValidateNoticeStatementWhenNoticeServedIsYes() {
        // Given
        WalesNoticeDetails walesNoticeDetails = WalesNoticeDetails.builder()
            .noticeServed(YesOrNo.YES)
            .typeOfNoticeServed("Form RHW20")
            .build();
        PCSCase caseData = PCSCase.builder()
            .walesNoticeDetails(walesNoticeDetails)
            .build();
        when(walesRentSectionRoutingService.shouldShowRentSection(any(PCSCase.class)))
            .thenReturn(YesOrNo.YES);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isSameAs(caseData);
        verify(textAreaValidationService, never()).validateSingleTextArea(anyString(), anyString(), anyInt());
    }

    @Test
    void shouldNotValidateWhenWalesNoticeDetailsIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder().build();
        when(walesRentSectionRoutingService.shouldShowRentSection(any(PCSCase.class)))
            .thenReturn(YesOrNo.NO);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isSameAs(caseData);
        verify(textAreaValidationService, never()).validateSingleTextArea(anyString(), anyString(), anyInt());
    }
}
