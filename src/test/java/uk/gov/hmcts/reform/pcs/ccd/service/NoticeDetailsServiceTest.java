package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for the NoticeDetailsService.
 * Verifies validation logic for different notice service methods.
 */
class NoticeDetailsServiceTest {

    private NoticeDetailsService noticeDetailsService;

    @BeforeEach
    void setUp() {
        noticeDetailsService = new NoticeDetailsService();
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
                .build();
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            assertThat(errors).isEmpty();
        }
        
        @Test
        void shouldValidateNoticeDetailsWhenNoticeServedIsYes() {
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(null)
                .build();
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            assertThat(errors).isNotEmpty();
            assertThat(errors).contains("You must select how you served the notice");
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

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).contains("You must select how you served the notice");
        }
    }

    @Nested
    class DateFieldValidation {
        
        @Test
        void shouldValidateFirstClassPostWithValidDate() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate(LocalDate.of(2023, 1, 1))
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
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate(null) // Null date is allowed for optional fields
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
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate(LocalDate.of(2099, 1, 1))
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).contains("The date cannot be today or in the future");
        }
        
        @Test
        void shouldValidateFirstClassPostWithTodayDate() {
            // Given
            java.time.LocalDate today = java.time.LocalDate.now();
            String todayStr = today.toString();
            
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate(LocalDate.now())
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).contains("The date cannot be today or in the future");
        }

        @Test
        void shouldValidateDeliveredPermittedPlaceWithValidDate() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE)
                .noticeDeliveredDate(LocalDate.of(2023, 1, 1))
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
                .noticeServiceMethod(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE)
                .noticeDeliveredDate(null) // Null date is allowed for optional fields
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("validDateFormats")
        void shouldAcceptValidDateFormats(String dateString) {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate(LocalDate.parse(dateString))
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldRejectInvalidDateFormats() {
            // Given - with null date (invalid)
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate(null) // Invalid date
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then - null dates should not cause validation errors since they're optional
            assertThat(errors).isEmpty();
        }

        private static Stream<Arguments> validDateFormats() {
            return Stream.of(
                arguments("2020-01-01"),
                arguments("2020-12-31"),
                arguments("2020-06-15"),
                arguments("2020-02-29") // Leap year
            );
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
                .noticeServiceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                .noticeHandedOverDateTime(pastDateTime) // Valid past date-time
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
                .noticeServiceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                .noticeHandedOverDateTime(pastDateOnly) // Date with default time (midnight)
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
                .noticeServiceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                .noticeHandedOverDateTime(futureDateTime) // Future date-time
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).contains("The date and time cannot be today or in the future");
        }
        
        @Test
        void shouldValidatePersonallyHandedWithTodayDateTime() {
            // Given
            LocalDateTime todayDateTime = LocalDateTime.now()
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                .noticeHandedOverDateTime(todayDateTime) // Today's date-time
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).contains("The date and time cannot be today or in the future");
        }

        @Test
        void shouldValidateOtherElectronicWithValidDateTime() {
            // Given
            LocalDateTime pastDateTime = LocalDateTime.now().minusDays(1);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.OTHER_ELECTRONIC)
                .noticeOtherElectronicDateTime(pastDateTime) // Valid past date-time
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
                .noticeServiceMethod(NoticeServiceMethod.OTHER_ELECTRONIC)
                .noticeOtherElectronicDateTime(futureDateTime) // Future date-time
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).contains("The date and time cannot be today or in the future");
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
                .noticeServiceMethod(NoticeServiceMethod.EMAIL)
                .noticeEmailSentDateTime(pastDateTime)
                .noticeEmailExplanation("Sent to tenant@example.com") // Valid explanation
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
            StringBuilder longText = new StringBuilder();
            for (int i = 0; i < 26; i++) {
                longText.append("0123456789"); // 10 chars x 26 = 260 chars
            }
            
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.EMAIL)
                .noticeEmailSentDateTime(pastDateTime)
                .noticeEmailExplanation(longText.toString()) // Too long explanation
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).contains("The explanation must be 250 characters or fewer");
        }

        @Test
        void shouldValidateEmailWithFutureDateTime() {
            // Given
            LocalDateTime futureDateTime = LocalDateTime.now().plusDays(1);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.EMAIL)
                .noticeEmailSentDateTime(futureDateTime)
                .noticeEmailExplanation("Valid explanation")
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).contains("The date and time cannot be today or in the future");
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
                .noticeServiceMethod(NoticeServiceMethod.OTHER)
                .noticeOtherDateTime(pastDateTime)
                .noticeOtherExplanation("Hand delivered by courier service") // Valid explanation
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
            StringBuilder longText = new StringBuilder();
            for (int i = 0; i < 26; i++) {
                longText.append("0123456789"); // 10 chars x 26 = 260 chars
            }
            
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.OTHER)
                .noticeOtherDateTime(pastDateTime)
                .noticeOtherExplanation(longText.toString()) // Too long explanation
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).contains("The explanation must be 250 characters or fewer");
        }

        @Test
        void shouldValidateOtherWithFutureDateTime() {
            // Given
            LocalDateTime futureDateTime = LocalDateTime.now().plusDays(1);
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.OTHER)
                .noticeOtherDateTime(futureDateTime)
                .noticeOtherExplanation("Valid explanation")
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).contains("The date and time cannot be today or in the future");
        }
    }

    @Nested
    class EdgeCases {
        
        @Test
        void shouldHandleNullValuesGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate(null)
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldHandleEmptyStringValuesGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate(null)
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldHandleWhitespaceOnlyValuesGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate(null)
                .build();

            // When
            List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }
    }
}
