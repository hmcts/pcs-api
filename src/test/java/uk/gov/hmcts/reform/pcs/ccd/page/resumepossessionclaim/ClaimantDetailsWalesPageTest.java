package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotApplicable;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimantDetailsWalesPageTest {

    @Test
    void shouldReturnErrorWhenAgentAppointmentDateIsInTheFuture() {
        // Given
        LocalDate today = LocalDate.of(2023, 6, 15);
        Clock fixedClock = Clock.fixed(today.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        ClaimantDetailsWalesPage page = new ClaimantDetailsWalesPage(fixedClock);
        
        LocalDate futureDate = today.plusDays(1);
        
        ClaimantDetailsWales walesDetails = ClaimantDetailsWales.builder()
            .walesRegistrationLicensed(YesNoNotApplicable.YES)
            .walesRegistrationNumber("REG123")
            .walesLicenseLicensed(YesNoNotApplicable.YES)
            .walesLicenseNumber("LIC456")
            .walesLicensedAgentAppointed(YesNoNotApplicable.YES)
            .walesAgentFirstName("John")
            .walesAgentLastName("Smith")
            .walesAgentLicenseNumber("AGENT789")
            .walesAgentAppointmentDate(futureDate)
            .build();

        PCSCase caseData = PCSCase.builder()
            .walesClaimantDetails(walesDetails)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .id(1234L)
            .data(caseData)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = page.midEvent(caseDetails, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0)).isEqualTo("The agent's date of appointment must be in the past");
    }
}