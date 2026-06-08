package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CanUploadNoticeServedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.NoticeDetailsService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NoticeDetailsTest extends BasePageTest {

    @Mock
    private NoticeDetailsService noticeDetailsService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new NoticeDetails(noticeDetailsService));
    }

    @Test
    void shouldHaveNullUnableToUploadReasonIfAableToUploadDocument() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .noticeServed(YesOrNo.YES)
            .noticeServedDetails(NoticeServedDetails.builder()
                .serviceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .postedDate(LocalDate.of(2023, 1, 1))
                .ableToUploadDocument(CanUploadNoticeServedDocument.YES)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getNoticeServedDetails().getUnableToUploadReason()).isNull();
    }

    @Test
    void shouldHaveNonEmptyUnableToUploadReasonIfUnableToUploadDocument() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                        .serviceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                        .postedDate(LocalDate.of(2023, 1, 1))
                        .ableToUploadDocument(CanUploadNoticeServedDocument.NO)
                        .unableToUploadReason("Unable to upload document")
                        .build())
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getNoticeServedDetails().getUnableToUploadReason())
                .isEqualTo("Unable to upload document");
    }
}
