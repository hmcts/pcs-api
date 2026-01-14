package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)

class TenancyLicenceDetailsTest extends BasePageTest {


    private static final LocalDate FIXED_CURRENT_DATE = LocalDate.of(2025, 8, 27);

    @Mock
    private Clock ukClock;
    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        when(ukClock.instant()).thenReturn(FIXED_CURRENT_DATE.atTime(10, 20).atZone(UK_ZONE_ID).toInstant());
        when(ukClock.getZone()).thenReturn(UK_ZONE_ID);

        setPageUnderTest(new TenancyLicenceDetailsPage(ukClock, textAreaValidationService));
    }

    @ParameterizedTest
    @MethodSource("tenancyDateScenarios")
    void shouldThrowErrorWhenDateIsTodayOrInTheFuture(LocalDate date,
                                                      Boolean isValid,
                                                      TenancyLicenceType  tenancyLicence) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(
                TenancyLicenceDetails.builder()
                    .tenancyLicenceDate(date)
                    .typeOfTenancyLicence(tenancyLicence)
                    .build()
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        if (!isValid) {
            assertThat(response.getErrors())
                .containsExactly("Date the tenancy or licence began must be in the past");
        } else {
            assertThat(response.getErrors()).isNull();
            assertThat(response.getData().getTenancyLicenceDetails().getTenancyLicenceDate())
                .isEqualTo(date);
        }

    }

    private static Stream<Arguments> tenancyDateScenarios() {
        return Stream.of(
            arguments(FIXED_CURRENT_DATE.plusDays(1), false, TenancyLicenceType.ASSURED_TENANCY),
            arguments(FIXED_CURRENT_DATE, false, null, TenancyLicenceType.DEMOTED_TENANCY),
            arguments(FIXED_CURRENT_DATE.minusDays(1), true, TenancyLicenceType.INTRODUCTORY_TENANCY),
            arguments(FIXED_CURRENT_DATE.minusYears(5), true, TenancyLicenceType.OTHER)
        );
    }
}
