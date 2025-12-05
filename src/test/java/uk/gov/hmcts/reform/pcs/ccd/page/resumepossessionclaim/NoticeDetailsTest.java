package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.NoticeDetailsService;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class NoticeDetailsTest extends BasePageTest {

    @Mock
    private NoticeDetailsService noticeDetailsService;

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        // Configure TextAreaValidationService mocks
        lenient().doReturn(new ArrayList<>()).when(textAreaValidationService)
            .validateMultipleTextAreas(any(), any());
        doAnswer(invocation -> {
            Object caseData = invocation.getArgument(0);
            List<String> errors = invocation.getArgument(1);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data((PCSCase) caseData)
                .errors(errors.isEmpty() ? null : errors)
                .build();
        }).when(textAreaValidationService).createValidationResponse(any(), anyList());
        
        setPageUnderTest(new NoticeDetails(noticeDetailsService, textAreaValidationService));
    }

    @Nested
    class ServiceIntegrationTests {

        @Test
        void shouldCallNoticeDetailsServiceForValidation() {
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                    .build())
                .build();

            List<String> validationErrors = new ArrayList<>();
            validationErrors.add("Enter a valid date in the format DD MM YYYY");
            when(noticeDetailsService.validateNoticeDetails(caseData)).thenReturn(validationErrors);

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains("Enter a valid date in the format DD MM YYYY");
        }

        @Test
        void shouldReturnNoErrorsWhenServiceValidationPasses() {
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                    .build())
                .build();

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNull();
        }

        @Test
        void shouldHandleMultipleValidationErrorsFromService() {
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .noticeServiceMethod(NoticeServiceMethod.EMAIL)
                    .build())
                .build();

            List<String> validationErrors = new ArrayList<>();
            validationErrors.add("The date and time cannot be today or in the future");
            validationErrors.add("The explanation must be 250 characters or fewer");
            when(noticeDetailsService.validateNoticeDetails(caseData)).thenReturn(validationErrors);

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isEqualTo(validationErrors);
            assertThat(response.getErrors()).hasSize(2);
            assertThat(response.getErrors()).contains("The date and time cannot be today or in the future");
            assertThat(response.getErrors()).contains("The explanation must be 250 characters or fewer");
        }
    }

    @Nested
    class PageBehaviorTests {

        @Test
        void shouldRequireNoticeServiceMethodSelection() {
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .build();

            List<String> validationErrors = new ArrayList<>();
            validationErrors.add("You must select how you served the notice");
            when(noticeDetailsService.validateNoticeDetails(caseData)).thenReturn(validationErrors);

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains("You must select how you served the notice");
        }

        @Test
        void shouldAllowProceedingWithValidData() {
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                    .noticePostedDate(LocalDate.of(2023, 1, 1))
                    .build())
                .build();

            when(noticeDetailsService.validateNoticeDetails(caseData)).thenReturn(new ArrayList<>());

            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            assertThat(response.getErrors()).isNull();
            assertThat(response.getData()).isEqualTo(caseData);
        }
    }
}
