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

/**
 * Tests for the NoticeDetailsService.
 * Verifies validation logic for different notice service methods.
 */
class NoticeDetailsServiceTest {

    private NoticeDetailsService noticeDetailsService;

    @BeforeEach
    void setUp() {
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        noticeDetailsService = new NoticeDetailsService(textAreaValidationService);
    }

    @Nested
    class ConditionalValidationTests {

        @Test
        void shouldReturnNoErrorsWhenNoticeNotServed() {
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.NO)
                .build();
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            assertThat(errors).isEmpty();
        }

        @Test
        void shouldReturnNoErrorsWhenNoticeServedIsNull() {
            PCSCase caseData = PCSCase.builder()
                .noticeServed(null)
                    .noticeServedDetails(NoticeServedDetails.builder()
                        .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                        .noticePostedDate(LocalDate.now().minusDays(1))
                        .build())
                    .build();
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            assertThat(errors).isEmpty();
        }

        @Test
        void shouldValidateNoticeDetailsWhenNoticeServedIsYes() {
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .noticeServiceMethod(null)
                    .build())
                .build();
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            assertThat(errors)
                .isNotEmpty()
                .contains("You must select how you served the notice");
        }
    }

    @Nested
    class NoticeServiceMethodValidation {

        @Test
        void shouldRequireNoticeServiceMethod() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .build();

            caseData.setNoticeServedDetails(NoticeServedDetails.builder().build());

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors)
                .isNotEmpty()
                .contains("You must select how you served the notice");
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
                    .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                    .noticePostedDate(LocalDate.of(2023, 1, 1))
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
                    .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                    .noticePostedDate(null) // Null date is allowed for optional fields
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
                    .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                    .noticePostedDate(LocalDate.now().plusDays(1))
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
                    .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                    .noticePostedDate(LocalDate.now())
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
                    .noticeServiceMethod(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE)
                    .noticeDeliveredDate(LocalDate.of(2023, 1, 1))
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
                    .noticeServiceMethod(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE)
                    .noticeDeliveredDate(null)
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
                    .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                    .noticePostedDate(LocalDate.of(2025, 11, 10))
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
                    .noticeServiceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                    .noticeHandedOverDateTime(pastDateTime) // Valid past date-time
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
                    .noticeServiceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                    .noticeHandedOverDateTime(pastDateOnly) // Date with default time (midnight)
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
                    .noticeServiceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                    .noticeHandedOverDateTime(futureDateTime) // Future date-time
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
                    .noticeServiceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                    .noticeHandedOverDateTime(todayDateTime) // Today's date-time
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
                    .noticeServiceMethod(NoticeServiceMethod.OTHER_ELECTRONIC)
                    .noticeOtherElectronicDateTime(pastDateTime) // Valid past date-time
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
                    .noticeServiceMethod(NoticeServiceMethod.OTHER_ELECTRONIC)
                    .noticeOtherElectronicDateTime(futureDateTime) // Future date-time
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
        void shouldValidateEmailWithValidExplanation() {
            // Given
            LocalDateTime pastDateTime = LocalDateTime.now().minusDays(1);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .noticeServiceMethod(NoticeServiceMethod.EMAIL)
                    .noticeEmailSentDateTime(pastDateTime)
                    .noticeEmailExplanation("Sent to tenant@example.com") // Valid explanation
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldValidateEmailWithTooLongExplanation() {
            // Given
            LocalDateTime pastDateTime = LocalDateTime.now().minusDays(1);

            // Create a string that exceeds 250 characters
            String longText = "0123456789".repeat(26); // 10 chars x 26 = 260 chars

            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .noticeServiceMethod(NoticeServiceMethod.EMAIL)
                    .noticeEmailSentDateTime(pastDateTime)
                    .noticeEmailExplanation(longText) // Too long explanation
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            // Text area length validation is now handled by TextAreaValidationService in NoticeDetailsService
            assertThat(errors)
                .isNotEmpty()
                .anyMatch(error -> error.contains("more than the maximum number of characters"));
        }

        @Test
        void shouldValidateEmailWithFutureDateTime() {
            // Given
            LocalDateTime futureDateTime = LocalDateTime.now().plusDays(1);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .noticeServiceMethod(NoticeServiceMethod.EMAIL)
                    .noticeEmailSentDateTime(futureDateTime)
                    .noticeEmailExplanation("Valid explanation")
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
        void shouldValidateOtherWithValidExplanation() {
            // Given
            LocalDateTime pastDateTime = LocalDateTime.now().minusDays(1);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .noticeServiceMethod(NoticeServiceMethod.OTHER)
                    .noticeOtherDateTime(pastDateTime)
                    .noticeOtherExplanation("Hand delivered by courier service") // Valid explanation
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldValidateOtherWithTooLongExplanation() {
            // Given
            LocalDateTime pastDateTime = LocalDateTime.now().minusDays(1);

            // Create a string that exceeds 250 characters
            String longText = "0123456789".repeat(26); // 10 chars x 26 = 260 chars

            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .noticeServiceMethod(NoticeServiceMethod.OTHER)
                    .noticeOtherDateTime(pastDateTime)
                    .noticeOtherExplanation(longText) // Too long explanation
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            // Text area length validation is now handled by TextAreaValidationService in NoticeDetailsService
            assertThat(errors)
                .isNotEmpty()
                .anyMatch(error -> error.contains("more than the maximum number of characters"));
        }

        @Test
        void shouldValidateOtherWithFutureDateTime() {
            // Given
            LocalDateTime futureDateTime = LocalDateTime.now().plusDays(1);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .noticeServiceMethod(NoticeServiceMethod.OTHER)
                    .noticeOtherDateTime(futureDateTime)
                    .noticeOtherExplanation("Valid explanation")
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
    class NameOfPersonalDocumentLeftWithValidation {

        @Test
        void shouldValidateCorrectNameLength() {
            // Given
            LocalDateTime pastDateTime = LocalDateTime.now().minusDays(1);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                                         .noticeServiceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                                         .noticeHandedOverDateTime(pastDateTime)
                                         .noticePersonName("James Jackson") // Valid explanation
                                         .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldValidateNameWithTooLongName() {
            // Given
            LocalDateTime pastDateTime = LocalDateTime.now().minusDays(1);

            // Create a name that exceeds 60 characters
            String longName = "J".repeat(61);

            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                                         .noticeServiceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                                         .noticeHandedOverDateTime(pastDateTime)
                                         .noticePersonName(longName)
                                         .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors)
                .isNotEmpty()
                .anyMatch(error -> error.contains("more than the maximum number of characters"));
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void shouldHandleNullValuesGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(NoticeServedDetails.builder()
                    .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                    .noticePostedDate(null)
                    .build())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }
    }
}
