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

import java.time.LocalDateTime;
import java.util.Map;
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
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            assertThat(errors).isEmpty();
        }
        
        @Test
        void shouldReturnNoErrorsWhenNoticeServedIsNull() {
            PCSCase caseData = PCSCase.builder()
                .noticeServed(null)
                .build();
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            assertThat(errors).isEmpty();
        }
        
        @Test
        void shouldValidateNoticeDetailsWhenNoticeServedIsYes() {
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(null)
                .build();
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            assertThat(errors).isNotEmpty();
            assertThat(errors).containsKey("noticeServiceMethod");
            assertThat(errors.get("noticeServiceMethod")).isEqualTo("You must select how you served the notice");
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
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).containsKey("noticeServiceMethod");
            assertThat(errors.get("noticeServiceMethod")).isEqualTo("You must select how you served the notice");
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
                .noticePostedDate("2023-01-01")
                .build();

            // When
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldValidateFirstClassPostWithInvalidDate() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate("invalid date") // Invalid date format
                .build();

            // When
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).containsKey("noticePostedDate");
            assertThat(errors.get("noticePostedDate")).isEqualTo("Enter a valid date in the format YYYY-MM-DD");
        }

        @Test
        void shouldValidateFirstClassPostWithFutureDate() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate("2099-01-01")
                .build();

            // When
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).containsKey("noticePostedDate");
            assertThat(errors.get("noticePostedDate")).isEqualTo("The date cannot be today or in the future");
        }
        
        @Test
        void shouldValidateFirstClassPostWithTodayDate() {
            // Given
            java.time.LocalDate today = java.time.LocalDate.now();
            String todayStr = today.toString();
            
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate(todayStr) // Today's date
                .build();

            // When
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).containsKey("noticePostedDate");
            assertThat(errors.get("noticePostedDate")).isEqualTo("The date cannot be today or in the future");
        }

        @Test
        void shouldValidateDeliveredPermittedPlaceWithValidDate() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE)
                .noticeDeliveredDate("2023-01-01")
                .build();

            // When
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldValidateDeliveredPermittedPlaceWithInvalidDate() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE)
                .noticeDeliveredDate("invalid date") // Invalid date format
                .build();

            // When
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).containsKey("noticeDeliveredDate");
            assertThat(errors.get("noticeDeliveredDate")).isEqualTo("Enter a valid date in the format YYYY-MM-DD");
        }

        @ParameterizedTest
        @MethodSource("validDateFormats")
        void shouldAcceptValidDateFormats(String dateString) {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate(dateString)
                .build();

            // When
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("invalidDateFormats")
        void shouldRejectInvalidDateFormats(String dateString) {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate(dateString)
                .build();

            // When
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then - invalid formats should return validation errors
            if (!dateString.isEmpty()) {
                assertThat(errors).isNotEmpty();
                assertThat(errors).containsKey("noticePostedDate");
                assertThat(errors.get("noticePostedDate"))
                    .isEqualTo("Enter a valid date in the format YYYY-MM-DD");
            } else {
                assertThat(errors).isEmpty();
            }
        }

        private static Stream<Arguments> validDateFormats() {
            return Stream.of(
                arguments("2020-01-01"),
                arguments("2020-12-31"),
                arguments("2020-06-15"),
                arguments("2020-02-29") // Leap year
            );
        }

        private static Stream<Arguments> invalidDateFormats() {
            return Stream.of(
                arguments("01/01/2023"), // Wrong format
                arguments("01 01 2023"), // Wrong format
                arguments("01 01"), // Missing year
                arguments("01"), // Missing month and year
                arguments("abc"), // Non-numeric
                arguments("") // Empty string
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
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

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
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

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
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).containsKey("noticeHandedOverDateTime");
            assertThat(errors.get("noticeHandedOverDateTime"))
                .isEqualTo("The date and time cannot be today or in the future");
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
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).containsKey("noticeHandedOverDateTime");
            assertThat(errors.get("noticeHandedOverDateTime"))
                .isEqualTo("The date and time cannot be today or in the future");
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
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

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
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).containsKey("noticeOtherElectronicDateTime");
            assertThat(errors.get("noticeOtherElectronicDateTime"))
                .isEqualTo("The date and time cannot be today or in the future");
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
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

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
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).containsKey("noticeEmailExplanation");
            assertThat(errors.get("noticeEmailExplanation"))
                .isEqualTo("The explanation must be 250 characters or fewer");
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
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).containsKey("noticeEmailSentDateTime");
            assertThat(errors.get("noticeEmailSentDateTime"))
                .isEqualTo("The date and time cannot be today or in the future");
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
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

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
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).containsKey("noticeOtherExplanation");
            assertThat(errors.get("noticeOtherExplanation"))
                .isEqualTo("The explanation must be 250 characters or fewer");
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
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isNotEmpty();
            assertThat(errors).containsKey("noticeOtherDateTime");
            assertThat(errors.get("noticeOtherDateTime"))
                .isEqualTo("The date and time cannot be today or in the future");
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
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldHandleEmptyStringValuesGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate("")
                .build();

            // When
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        void shouldHandleWhitespaceOnlyValuesGracefully() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServiceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticePostedDate("   ")
                .build();

            // When
            Map<String, String> errors = noticeDetailsService.validateNoticeDetails(caseData);

            // Then
            assertThat(errors).isEmpty();
        }
    }
}
