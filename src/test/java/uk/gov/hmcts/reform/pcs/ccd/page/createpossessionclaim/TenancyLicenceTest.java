package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.time.Clock;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
class TenancyLicenceTest  extends BasePageTest {

    private static final LocalDate FIXED_CURRENT_DATE = LocalDate.of(2025, 8, 27);

    @Mock
    private Clock ukClock;

    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        when(ukClock.instant()).thenReturn(FIXED_CURRENT_DATE.atTime(10, 20).atZone(UK_ZONE_ID).toInstant());
        when(ukClock.getZone()).thenReturn(UK_ZONE_ID);
        event = buildPageInTestEvent(new TenancyLicenceDetails(ukClock));
    }

    @ParameterizedTest
    @MethodSource("tenancyDateScenarios")
    void shouldThrowErrorWhenDateIsTodayOrInTheFuture(String date, Boolean isValid) {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();

        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDate(date)
            .build();

        caseDetails.setData(caseData);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response =
            getMidEventForPage(event, "tenancyLicenceDetails").handle(caseDetails,null);

        // Then
        if (!isValid) {
            assertThat(response.getErrors())
                .containsExactly("Date the tenancy or licence began must be in the past");
        } else {
            assertThat(response.getErrors()).isNull();
            assertThat(response.getData().getTenancyLicenceDate())
                .isEqualTo(date);
        }

    }

    private static Stream<Arguments> tenancyDateScenarios() {
        return Stream.of(
           arguments(FIXED_CURRENT_DATE.plusDays(1).toString(), false),
           arguments(FIXED_CURRENT_DATE.toString(), false),
           arguments(FIXED_CURRENT_DATE.minusDays(1).toString(), true),
           arguments(FIXED_CURRENT_DATE.minusYears(5).toString(), true)
        );
    }

}
