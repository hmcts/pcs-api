package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeDetailsServiceTest {

    private NoticeDetailsService noticeDetailsService;

    @BeforeEach
    void setUp() {
        noticeDetailsService = new NoticeDetailsService();
    }

    @Nested
    class ConditionalValidationTests {

        @Test
        void shouldValidateNoticeDetailsWhenNoticeServedIsYes() {
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.EMAIL)
                    .build())
                .build();
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            assertThat(errors)
                .isEmpty();
        }
    }

    @Nested
    class DateFieldValidation {

        @Test
        void shouldValidateFirstClassPostWithValidDate() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                    .postedDate(LocalDate.of(2023, 1, 1))
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldValidateFirstClassPostWithNullDate() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                    .postedDate(null) // Null date is allowed for optional fields
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldValidateFirstClassPostWithFutureDate() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                    .postedDate(LocalDate.now().plusDays(1))
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors)
                .isNotEmpty()
                .contains("The date cannot be today or in the future");
        }

        @Test
        void shouldValidateFirstClassPostWithTodayDate() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                    .postedDate(LocalDate.now())
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors)
                .isNotEmpty()
                .contains("The date cannot be today or in the future");
        }

        @Test
        void shouldValidateDeliveredPermittedPlaceWithValidDate() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE)
                    .deliveredDate(LocalDate.of(2023, 1, 1))
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldValidateDeliveredPermittedPlaceWithNullDate() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE)
                    .deliveredDate(null)
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldAcceptValidDateFormat() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                    .postedDate(LocalDate.of(2025, 11, 10))
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }
    }

    @Nested
    class DateTimeFieldValidation {

        @Test
        void shouldValidatePersonallyHandedWithValidDateTime() {
            // Given
            LocalDateTime pastDateTime = LocalDateTime.now().minusDays(1);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                    .handedOverDateTime(pastDateTime) // Valid past date-time
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldAcceptPartialTimeEntries() {
            // Given
            // Create a LocalDateTime with only the date portion (time is midnight by default)
            LocalDateTime pastDateOnly = LocalDateTime.now().minusDays(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                    .handedOverDateTime(pastDateOnly) // Date with default time (midnight)
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldValidatePersonallyHandedWithFutureDateTime() {
            // Given
            LocalDateTime futureDateTime = LocalDateTime.now().plusDays(1);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                    .handedOverDateTime(futureDateTime) // Future date-time
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors)
                .isNotEmpty()
                .contains("The date and time cannot be today or in the future");
        }

        @Test
        void shouldValidatePersonallyHandedWithTodayDateTime() {
            // Given
            LocalDateTime todayDateTime = LocalDateTime.now()
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                    .handedOverDateTime(todayDateTime) // Today's date-time
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors)
                .isNotEmpty()
                .contains("The date and time cannot be today or in the future");
        }

        @Test
        void shouldValidateOtherElectronicWithValidDateTime() {
            // Given
            LocalDateTime pastDateTime = LocalDateTime.now().minusDays(1);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.OTHER_ELECTRONIC)
                    .otherElectronicDateTime(pastDateTime) // Valid past date-time
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldValidateOtherElectronicWithFutureDateTime() {
            // Given
            LocalDateTime futureDateTime = LocalDateTime.now().plusDays(1);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.OTHER_ELECTRONIC)
                    .otherElectronicDateTime(futureDateTime) // Future date-time
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors)
                .isNotEmpty()
                .contains("The date and time cannot be today or in the future");
        }
    }

    @Nested
    class EmailValidation {

        @Test
        void shouldValidateEmailWithFutureDateTime() {
            // Given
            LocalDateTime futureDateTime = LocalDateTime.now().plusDays(1);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.EMAIL)
                    .emailSentDateTime(futureDateTime)
                    .emailAddress("test@example.com")
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors)
                .isNotEmpty()
                .contains("The date and time cannot be today or in the future");
        }
    }

    @Nested
    class OtherValidation {

        @Test
        void shouldValidateOtherWithFutureDateTime() {
            // Given
            LocalDateTime futureDateTime = LocalDateTime.now().plusDays(1);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .serviceMethod(NoticeServiceMethod.OTHER)
                    .otherDateTime(futureDateTime)
                    .otherExplanation("Valid explanation")
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors)
                .isNotEmpty()
                .contains("The date and time cannot be today or in the future");
        }

        @Test
        void shouldNotValidateOtherWithNullDateTime() {
            // Given
            LocalDateTime dateTime = null;
            PCSCase caseData = PCSCase.builder()
                    .noticeServed(YesOrNo.YES)
                    .noticeServedDetails(NoticeServedDetails.builder()
                            .serviceMethod(NoticeServiceMethod.OTHER)
                            .otherDateTime(dateTime)
                            .otherExplanation("Valid explanation")
                            .build())
                    .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors)
                    .isEmpty();
        }
    }
}
