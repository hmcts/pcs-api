package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.NoticeDetailsService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the Notice Details page configuration.
 */
class NoticeDetailsTest extends BasePageTest {

    private NoticeDetailsService noticeDetailsService;
    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        noticeDetailsService = mock(NoticeDetailsService.class);
        event = buildPageInTestEvent(new NoticeDetails(noticeDetailsService));
    }

    @Nested
    class PageConfigurationTests {
        
        @Test
        void shouldBuildPageWithCorrectStructure() {
            MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "noticeDetails");
            assertThat(midEvent).isNotNull();
        }
    }

    @Nested
    class ServiceIntegrationTests {
        
        @Test
        void shouldCallNoticeDetailsServiceForValidation() {
            CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .build();
            caseDetails.setData(caseData);

            List<String> validationErrors = new ArrayList<>();
            validationErrors.add("Enter a valid date in the format DD MM YYYY");
            when(noticeDetailsService.validateNoticeDetails(caseData)).thenReturn(validationErrors);

            MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "noticeDetails");
            AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains("Enter a valid date in the format DD MM YYYY");
        }

        @Test
        void shouldReturnNoErrorsWhenServiceValidationPasses() {
            CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .build();
            caseDetails.setData(caseData);

            when(noticeDetailsService.validateNoticeDetails(caseData)).thenReturn(new ArrayList<>());

            MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "noticeDetails");
            AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

            assertThat(response.getErrors()).isNull();
        }

        @Test
        void shouldHandleMultipleValidationErrorsFromService() {
            CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.EMAIL)
                .build();
            caseDetails.setData(caseData);

            List<String> validationErrors = new ArrayList<>();
            validationErrors.add("The date and time cannot be today or in the future");
            validationErrors.add("The explanation must be 250 characters or fewer");
            when(noticeDetailsService.validateNoticeDetails(caseData)).thenReturn(validationErrors);

            MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "noticeDetails");
            AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

            assertThat(response.getErrors()).hasSize(2);
            assertThat(response.getErrors()).contains("The date and time cannot be today or in the future");
            assertThat(response.getErrors()).contains("The explanation must be 250 characters or fewer");
        }
    }

    @Nested
    class PageBehaviorTests {
        
        @Test
        void shouldRequireNoticeServiceMethodSelection() {
            CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .build();
            caseDetails.setData(caseData);

            List<String> validationErrors = new ArrayList<>();
            validationErrors.add("You must select how you served the notice");
            when(noticeDetailsService.validateNoticeDetails(caseData)).thenReturn(validationErrors);

            MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "noticeDetails");
            AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains("You must select how you served the notice");
        }

        @Test
        void shouldAllowProceedingWithValidData() {
            CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate(LocalDate.of(2023, 1, 1))
                .build();
            caseDetails.setData(caseData);

            when(noticeDetailsService.validateNoticeDetails(caseData)).thenReturn(new ArrayList<>());

            MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "noticeDetails");
            AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

            assertThat(response.getErrors()).isNull();
            assertThat(response.getData()).isEqualTo(caseData);
        }
    }
}
