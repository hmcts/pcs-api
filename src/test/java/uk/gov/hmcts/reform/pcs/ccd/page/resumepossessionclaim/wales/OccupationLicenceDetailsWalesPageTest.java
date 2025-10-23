package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.time.Clock;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
class OccupationLicenceDetailsWalesPageTest extends BasePageTest {

    private static final LocalDate FIXED_CURRENT_DATE = LocalDate.of(2025, 1, 15);

    @Mock
    private Clock ukClock;

    @BeforeEach
    void setUp() {
        lenient().when(ukClock.instant()).thenReturn(FIXED_CURRENT_DATE.atTime(10, 20).atZone(UK_ZONE_ID).toInstant());
        lenient().when(ukClock.getZone()).thenReturn(UK_ZONE_ID);

        setPageUnderTest(new OccupationLicenceDetailsWalesPage(ukClock));
    }

    @Test
    void shouldReturnSuccessWhenOccupationLicenceDetailsIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .occupationLicenceDetailsWales(null)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    void shouldReturnSuccessWhenNoStartDateProvided() {
        // Given
        OccupationLicenceDetailsWales occupationDetails = OccupationLicenceDetailsWales.builder()
            .licenseType(OccupationLicenceTypeWales.SECURE_CONTRACT)
            .licenseStartDate(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .occupationLicenceDetailsWales(occupationDetails)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @ParameterizedTest
    @MethodSource("dateValidationScenarios")
    void shouldValidateOccupationLicenceStartDate(LocalDate startDate,
                                                 boolean expectError,
                                                 String expectedErrorMessage) {
        // Given
        OccupationLicenceDetailsWales occupationDetails = OccupationLicenceDetailsWales.builder()
            .licenseType(OccupationLicenceTypeWales.SECURE_CONTRACT)
            .licenseStartDate(startDate)
            .build();

        PCSCase caseData = PCSCase.builder()
            .occupationLicenceDetailsWales(occupationDetails)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        if (expectError) {
            assertThat(response.getErrors()).containsExactly(expectedErrorMessage);
        } else {
            assertThat(response.getErrors()).isNull();
            assertThat(response.getData()).isEqualTo(caseData);
        }
    }

    @Test
    void shouldReturnSuccessWhenDateIsInThePast() {
        // Given
        LocalDate pastDate = FIXED_CURRENT_DATE.minusDays(1);
        OccupationLicenceDetailsWales occupationDetails = OccupationLicenceDetailsWales.builder()
            .licenseType(OccupationLicenceTypeWales.OTHER)
            .otherLicenseTypeDetails("Some other type")
            .licenseStartDate(pastDate)
            .build();

        PCSCase caseData = PCSCase.builder()
            .occupationLicenceDetailsWales(occupationDetails)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    void shouldReturnSuccessWhenDateIsFarInThePast() {
        // Given
        LocalDate farPastDate = FIXED_CURRENT_DATE.minusYears(5);
        OccupationLicenceDetailsWales occupationDetails = OccupationLicenceDetailsWales.builder()
            .licenseType(OccupationLicenceTypeWales.STANDARD_CONTRACT)
            .licenseStartDate(farPastDate)
            .build();

        PCSCase caseData = PCSCase.builder()
            .occupationLicenceDetailsWales(occupationDetails)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    private static Stream<Arguments> dateValidationScenarios() {
        return Stream.of(
            // Date is today - should error
            arguments(
                FIXED_CURRENT_DATE,
                true,
                "Occupation contract or licence start date cannot be today"
            ),
            // Date is in the future - should error
            arguments(
                FIXED_CURRENT_DATE.plusDays(1),
                true,
                "Occupation contract or licence start date cannot be in the future"
            ),
            // Date is far in the future - should error
            arguments(
                FIXED_CURRENT_DATE.plusYears(1),
                true,
                "Occupation contract or licence start date cannot be in the future"
            ),
            // Date is yesterday - should be valid
            arguments(
                FIXED_CURRENT_DATE.minusDays(1),
                false,
                null
            ),
            // Date is far in the past - should be valid
            arguments(
                FIXED_CURRENT_DATE.minusYears(10),
                false,
                null
            )
        );
    }
}
