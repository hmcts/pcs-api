package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CanUploadNoticeServedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.NoticeTabDetails;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NoticeDetailsBuilderTest {

    private final String noAnswer = " ";

    @InjectMocks
    private NoticeDetailsBuilder noticeDetailsBuilder = new NoticeDetailsBuilder();

    @Test
    void shouldHandleNullNoticeServed() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
                .noticeServed(null)
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeServed()).isEqualTo(" ");
        assertThat(noticeTabDetails.getNoticeMethod()).isEqualTo(" ");
        assertThat(noticeTabDetails.getNoticeDate()).isEqualTo(" ");
    }

    @Test
    void shouldHandleNullNoticeServedDetails() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(null)
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeMethod()).isEqualTo(" ");
        assertThat(noticeTabDetails.getNoticeDate()).isEqualTo(" ");
    }

    @Test
    void shouldNotPopulateNoticeTabDetailsIfNotServedNotice() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
                .noticeServed(YesOrNo.NO)
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeMethod()).isEqualTo(" ");
        assertThat(noticeTabDetails.getNoticeDate()).isEqualTo(" ");
    }

    @Test
    void shouldSetNoticeDetailsForFirstClassPost() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                        .serviceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                        .postedDate(LocalDate.of(2026, 5, 11))
                        .build())
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeMethod())
                .isEqualTo(NoticeServiceMethod.FIRST_CLASS_POST.getLabel());
        assertThat(noticeTabDetails.getNoticeServed()).isEqualTo("Yes");
        assertThat(noticeTabDetails.getNoticeDate()).isEqualTo("11 May 2026");
    }

    @Test
    void shouldSetNoticeDetailsForPermittedPlace() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                        .serviceMethod(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE)
                        .deliveredDate(LocalDate.of(2026, 5, 11))
                        .build())
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeMethod())
                .isEqualTo(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE.getLabel());
        assertThat(noticeTabDetails.getNoticeServed()).isEqualTo("Yes");
        assertThat(noticeTabDetails.getNoticeDate()).isEqualTo("11 May 2026");
    }

    @Test
    void shouldSetNoticeDetailsForPersonallyHanded() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                        .serviceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                        .handedOverDateTime(LocalDateTime.of(2026, 5, 11, 9, 0, 0))
                        .personName("Notice name")
                        .build())
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeMethod())
                .isEqualTo(NoticeServiceMethod.PERSONALLY_HANDED.getLabel());
        assertThat(noticeTabDetails.getNoticeServed()).isEqualTo("Yes");
        assertThat(noticeTabDetails.getNoticeDate()).isEqualTo("11 May 2026, 9:00:00AM");
        assertThat(noticeTabDetails.getNoticePersonName()).isEqualTo("Notice name");
    }

    @Test
    void shouldSetNoticeDetailsForEmail() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                        .serviceMethod(NoticeServiceMethod.EMAIL)
                        .emailSentDateTime(LocalDateTime.of(2026, 5, 11, 9, 0, 0))
                        .emailAddress("joebloggs@domain.com")
                        .build())
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeMethod())
                .isEqualTo(NoticeServiceMethod.EMAIL.getLabel());
        assertThat(noticeTabDetails.getNoticeEmailAddress()).isEqualTo("joebloggs@domain.com");
        assertThat(noticeTabDetails.getNoticeDate()).isEqualTo("11 May 2026, 9:00:00AM");
    }

    @Test
    void shouldSetNoticeDetailsForOtherElectronic() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                        .serviceMethod(NoticeServiceMethod.OTHER_ELECTRONIC)
                        .otherElectronicDateTime(LocalDateTime.of(2026, 5, 11, 9, 0, 0))
                        .otherElectronicExplanation("explanation")
                        .build())
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeMethod())
                .isEqualTo(NoticeServiceMethod.OTHER_ELECTRONIC.getLabel());
        assertThat(noticeTabDetails.getNoticeServed()).isEqualTo("Yes");
        assertThat(noticeTabDetails.getNoticeDate()).isEqualTo("11 May 2026, 9:00:00AM");
        assertThat(noticeTabDetails.getNoticeOtherElectronicDetails())
                .isEqualTo("explanation");
    }

    @Test
    void shouldSetNoticeDetailsForOther() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                        .serviceMethod(NoticeServiceMethod.OTHER)
                        .otherDateTime(LocalDateTime.of(2026, 5, 11, 9, 0, 0))
                        .build())
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeMethod())
                .isEqualTo(NoticeServiceMethod.OTHER.getLabel());
        assertThat(noticeTabDetails.getNoticeServed()).isEqualTo("Yes");
        assertThat(noticeTabDetails.getNoticeDate()).isEqualTo("11 May 2026, 9:00:00AM");
    }

    @Test
    void shouldSetReasonForUnableToUploadIfUnableToUpload() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                        .serviceMethod(NoticeServiceMethod.OTHER)
                        .otherDateTime(LocalDateTime.of(2026, 5, 11, 9, 0, 0))
                        .ableToUploadDocument(CanUploadNoticeServedDocument.No)
                        .unableToUploadReason("reason for unable to upload")
                        .build())
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeUploaded()).isEqualTo("No");
        assertThat(noticeTabDetails.getReasonsForNoNoticeDocument())
                .isEqualTo("reason for unable to upload");
    }

    @Test
    void shouldSetReasonForUnableToNoAnswerUploadIfAbleToUpload() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                        .serviceMethod(NoticeServiceMethod.OTHER)
                        .otherDateTime(LocalDateTime.of(2026, 5, 11, 9, 0, 0))
                        .ableToUploadDocument(CanUploadNoticeServedDocument.Yes)
                        .unableToUploadReason(null)
                        .build())
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeUploaded()).isEqualTo("Yes");
        assertThat(noticeTabDetails.getReasonsForNoNoticeDocument()).isNull();
    }

    @ParameterizedTest
    @MethodSource("noticeServiceMethodWithNullDatesProvider")
    void shouldHandleNullDatesForVariousServiceMethods(NoticeServiceMethod serviceMethod,
                                                       String expectedMethod) {
        // Given
        NoticeServedDetails noticeServedDetails = NoticeServedDetails.builder()
                .serviceMethod(serviceMethod)
                .build();

        PCSCase pcsCase = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(noticeServedDetails)
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeMethod()).isEqualTo(expectedMethod);
        assertThat(noticeTabDetails.getNoticeDate()).isEqualTo(" ");
    }

    @Test
    void shouldSetNoanswerIfWalesNoticeDetailsIsNull() {
        PCSCase pcsCase = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.WALES)
                .walesNoticeDetails(null)
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeServed()).isEqualTo(" ");
        assertThat(noticeTabDetails.getStatement()).isNull();
        assertThat(noticeTabDetails.getTypeOfNoticeServed()).isNull();
    }

    @Test
    void shouldSetNoticeStatementIfNoticeServedIsNoWales() {
        PCSCase pcsCase = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.WALES)
                .walesNoticeDetails(
                        WalesNoticeDetails.builder()
                                .noticeServed(YesOrNo.NO)
                                .typeOfNoticeServed("notice type")
                                .noticeStatement("notice statement")
                                .build()
                )
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeServed()).isEqualTo("No");
        assertThat(noticeTabDetails.getStatement()).isEqualTo("notice statement");
        assertThat(noticeTabDetails.getTypeOfNoticeServed()).isNull();
    }

    @Test
    void shouldNotSetDetailedNoticeDetailsWhenNoticeServedIsNoEngland() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
                .noticeServed(YesOrNo.NO)
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        assertThat(noticeTabDetails.getNoticeServed()).isEqualTo("No");
        assertThat(noticeTabDetails.getNoticeDate()).isEqualTo(noAnswer);
        assertThat(noticeTabDetails.getNoticeMethod()).isEqualTo(noAnswer);
    }

    @Test
    void shouldNotSetDetailedNoticeDetailsWhenNoticeServedIsNoWales() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.WALES)
                .walesNoticeDetails(
                        WalesNoticeDetails.builder()
                                .noticeServed(YesOrNo.NO)
                                .build()
                )
                .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeServed()).isEqualTo("No");
        assertThat(noticeTabDetails.getNoticeDate()).isEqualTo(noAnswer);
        assertThat(noticeTabDetails.getNoticeMethod()).isEqualTo(noAnswer);
    }

    @Test
    void shouldUnsetNoticeDocumentsIfCaseIsSubmitted() {
        // Given
        List<ListValue<Document>> noticeDocuments = List.of(
            ListValue.<Document>builder().value(Document.builder().build()).build()
        );
        NoticeServedDetails noticeServedDetails = NoticeServedDetails.builder()
            .serviceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
            .documents(noticeDocuments)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .noticeServed(YesOrNo.YES)
            .noticeServedDetails(noticeServedDetails)
            .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, true);

        // Then
        assertThat(noticeTabDetails.getNoticeDocuments()).isEqualTo(noticeDocuments);
        assertThat(noticeServedDetails.getDocuments()).isNull();
    }

    @Test
    void shouldNotUnsetNoticeDocumentsIfCaseIsInDraft() {
        // Given
        List<ListValue<Document>> noticeDocuments = List.of(
            ListValue.<Document>builder().value(Document.builder().build()).build()
        );
        NoticeServedDetails noticeServedDetails = NoticeServedDetails.builder()
            .serviceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
            .documents(noticeDocuments)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .noticeServed(YesOrNo.YES)
            .noticeServedDetails(noticeServedDetails)
            .build();

        // When
        NoticeTabDetails noticeTabDetails = noticeDetailsBuilder.buildNoticeTabDetails(pcsCase, false);

        // Then
        assertThat(noticeTabDetails.getNoticeDocuments()).isEqualTo(noticeDocuments);
        assertThat(noticeServedDetails.getDocuments()).isEqualTo(noticeDocuments);
    }

    private static Stream<Arguments> noticeServiceMethodWithNullDatesProvider() {
        return Stream.of(
                Arguments.of(NoticeServiceMethod.FIRST_CLASS_POST, NoticeServiceMethod.FIRST_CLASS_POST.getLabel()),
                Arguments.of(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE,
                        NoticeServiceMethod.DELIVERED_PERMITTED_PLACE.getLabel()),
                Arguments.of(NoticeServiceMethod.PERSONALLY_HANDED, NoticeServiceMethod.PERSONALLY_HANDED.getLabel()),
                Arguments.of(NoticeServiceMethod.EMAIL, NoticeServiceMethod.EMAIL.getLabel()),
                Arguments.of(NoticeServiceMethod.OTHER_ELECTRONIC, NoticeServiceMethod.OTHER_ELECTRONIC.getLabel()),
                Arguments.of(NoticeServiceMethod.OTHER, NoticeServiceMethod.OTHER.getLabel())
        );
    }
}
