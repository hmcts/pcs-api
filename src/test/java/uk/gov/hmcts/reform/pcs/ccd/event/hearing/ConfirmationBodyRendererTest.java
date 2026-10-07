package uk.gov.hmcts.reform.pcs.ccd.event.hearing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmationBodyRendererTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private AddressUK propertyAddress;

    private ConfirmationBodyRenderer underTest;

    @BeforeEach
    void setUp() {
        underTest = new ConfirmationBodyRenderer(addressFormatter);
    }

    @Test
    void shouldRenderAddHearingConfirmationBody() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .caseNameHmctsInternal("internal case name")
            .build();

        when(addressFormatter.formatMediumAddress(propertyAddress, AddressFormatter.COMMA_DELIMITER))
            .thenReturn("formatted property address");

        // When
        String confirmationBody = underTest.renderHearingAddedConfirmationBody(caseData, CASE_REFERENCE);

        // Then
        assertThat(confirmationBody)
            .contains("<span class=\"govuk-panel__title govuk-!-font-size-36\">Hearing added</span>")
            .contains("<span class=\"govuk-panel__body\">Case number #1234</span>")
            .contains("<span class=\"govuk-panel__body\">formatted property address</span>")
            .contains("<span class=\"govuk-panel__body\">internal case name</span>");
    }

    @Test
    void shouldRenderCancelHearingConfirmationBody() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .caseNameHmctsInternal("internal case name")
            .build();

        when(addressFormatter.formatMediumAddress(propertyAddress, AddressFormatter.COMMA_DELIMITER))
            .thenReturn("formatted property address");

        // When
        String confirmationBody = underTest.renderHearingCancelledConfirmationBody(caseData, CASE_REFERENCE);

        // Then
        assertThat(confirmationBody)
            .contains("<span class=\"govuk-panel__title govuk-!-font-size-36\">Hearing cancelled</span>")
            .contains("<span class=\"govuk-panel__body\">Case number #1234</span>")
            .contains("<span class=\"govuk-panel__body\">formatted property address</span>")
            .contains("<span class=\"govuk-panel__body\">internal case name</span>");
    }
}
