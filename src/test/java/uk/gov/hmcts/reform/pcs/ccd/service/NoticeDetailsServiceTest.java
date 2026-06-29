package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NoticeDetailsServiceTest {

    private static final String FUTURE_DATE_TIME_ERROR_MESSAGE = "The date and time cannot be today or in the future";
    private static final String FUTURE_DATE_ERROR_MESSAGE = "The date cannot be today or in the future";
    @Mock
    private TextAreaValidationService textAreaValidationService;

    @InjectMocks
    private NoticeDetailsService noticeDetailsService;

    @ParameterizedTest(name = "[{index}] {0} with date={1} => expectNoErrors={2}")
    @MethodSource("dateBasedProvider")
    void validateDateBasedServiceMethods(NoticeServiceMethod method, LocalDate date, boolean expectNoErrors,
                                         String expectedMessage) {

        NoticeServedDetails.NoticeServedDetailsBuilder builder = NoticeServedDetails.builder()
                .serviceMethod(method);

        switch (method) {
            case FIRST_CLASS_POST -> builder.postedDate(date);
            case DELIVERED_PERMITTED_PLACE -> builder.deliveredDate(date);
            default -> throw new IllegalArgumentException("Unexpected date-based method: " + method);
        }

        PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(builder.build())
                .build();

        List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

        if (expectNoErrors) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            if (expectedMessage != null) {
                assertThat(errors).anyMatch(e -> e.contains(expectedMessage));
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0} with dateTime={1} => expectNoErrors={2}")
    @MethodSource("dateTimeBasedProvider")
    void validateDateTimeBasedServiceMethods(NoticeServiceMethod method, LocalDateTime dateTime, boolean expectNoErrors,
                                             String expectedMessage) {
        NoticeServedDetails.NoticeServedDetailsBuilder builder = NoticeServedDetails.builder()
                .serviceMethod(method);

        switch (method) {
            case PERSONALLY_HANDED -> builder.handedOverDateTime(dateTime);
            case EMAIL -> builder.emailSentDateTime(dateTime).emailAddress("test@example.com");
            case OTHER_ELECTRONIC -> builder.otherElectronicDateTime(dateTime);
            case OTHER -> builder.otherDateTime(dateTime);
            default -> throw new IllegalArgumentException("Unexpected date-time based method: " + method);
        }

        PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .noticeServedDetails(builder.build())
                .build();

        List<String> errors = noticeDetailsService.validateNoticeDetails(caseData);

        if (expectNoErrors) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            if (expectedMessage != null) {
                assertThat(errors).anyMatch(e -> e.contains(expectedMessage));
            }
        }
    }

    private static Stream<Arguments> dateBasedProvider() {
        LocalDate past = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();
        LocalDate future = LocalDate.now().plusDays(1);

        return Stream.of(
                Arguments.of(NoticeServiceMethod.FIRST_CLASS_POST, past, true, null),
                Arguments.of(NoticeServiceMethod.FIRST_CLASS_POST, null, true, null),
                Arguments.of(NoticeServiceMethod.FIRST_CLASS_POST, today, false, FUTURE_DATE_ERROR_MESSAGE),
                Arguments.of(NoticeServiceMethod.FIRST_CLASS_POST, future, false, FUTURE_DATE_ERROR_MESSAGE),

                Arguments.of(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE, past, true, null),
                Arguments.of(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE, null, true, null),
                Arguments.of(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE, today, false, FUTURE_DATE_ERROR_MESSAGE),
                Arguments.of(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE, future, false, FUTURE_DATE_ERROR_MESSAGE)
        );
    }

    private static Stream<Arguments> dateTimeBasedProvider() {
        LocalDateTime past = LocalDateTime.now().minusDays(1).withNano(0);
        LocalDateTime today = LocalDateTime.now().withNano(0);
        LocalDateTime future = LocalDateTime.now().plusDays(1).withNano(0);

        return Stream.of(
                Arguments.of(NoticeServiceMethod.PERSONALLY_HANDED, past, true, null),
                Arguments.of(NoticeServiceMethod.PERSONALLY_HANDED, null, true, null),
                Arguments.of(NoticeServiceMethod.PERSONALLY_HANDED, today, false, FUTURE_DATE_TIME_ERROR_MESSAGE),
                Arguments.of(NoticeServiceMethod.PERSONALLY_HANDED, future, false, FUTURE_DATE_TIME_ERROR_MESSAGE),

                Arguments.of(NoticeServiceMethod.EMAIL, past, true, null),
                Arguments.of(NoticeServiceMethod.EMAIL, null, true, null),
                Arguments.of(NoticeServiceMethod.EMAIL, today, false, FUTURE_DATE_TIME_ERROR_MESSAGE),
                Arguments.of(NoticeServiceMethod.EMAIL, future, false, FUTURE_DATE_TIME_ERROR_MESSAGE),

                Arguments.of(NoticeServiceMethod.OTHER_ELECTRONIC, past, true, null),
                Arguments.of(NoticeServiceMethod.OTHER_ELECTRONIC, null, true, null),
                Arguments.of(NoticeServiceMethod.OTHER_ELECTRONIC, today, false, FUTURE_DATE_TIME_ERROR_MESSAGE),
                Arguments.of(NoticeServiceMethod.OTHER_ELECTRONIC, future, false, FUTURE_DATE_TIME_ERROR_MESSAGE),

                Arguments.of(NoticeServiceMethod.OTHER, past, true, null),
                Arguments.of(NoticeServiceMethod.OTHER, null, true, null),
                Arguments.of(NoticeServiceMethod.OTHER, today, false, FUTURE_DATE_TIME_ERROR_MESSAGE),
                Arguments.of(NoticeServiceMethod.OTHER, future, false, FUTURE_DATE_TIME_ERROR_MESSAGE)
        );
    }
}
