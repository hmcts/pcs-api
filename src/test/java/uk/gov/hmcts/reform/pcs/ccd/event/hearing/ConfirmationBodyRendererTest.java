package uk.gov.hmcts.reform.pcs.ccd.event.hearing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.util.ConfirmationPanelBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmationBodyRendererTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private ConfirmationPanelBuilder confirmationPanelBuilder;
    @Mock
    private AddressUK propertyAddress;

    private ConfirmationBodyRenderer underTest;

    @BeforeEach
    void setUp() {
        underTest = new ConfirmationBodyRenderer(confirmationPanelBuilder);
    }

    @Test
    void shouldRenderAddHearingConfirmationBody() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .caseNameHmctsInternal("internal case name")
            .build();

        String confirmationPanelMarkup = stubConfirmationPanelBuilder();

        // When
        String confirmationBody = underTest.renderHearingAddedConfirmationBody(caseData, CASE_REFERENCE);

        // Then
        verify(confirmationPanelBuilder)
            .buildStandardConfirmationPanel("Hearing added", caseData, CASE_REFERENCE);

        assertThat(confirmationBody)
            .contains(confirmationPanelMarkup);
    }

    @Test
    void shouldRenderCancelHearingConfirmationBody() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .build();

        String confirmationPanelMarkup = stubConfirmationPanelBuilder();

        // When
        String confirmationBody = underTest.renderHearingCancelledConfirmationBody(caseData, CASE_REFERENCE);

        // Then
        verify(confirmationPanelBuilder)
            .buildStandardConfirmationPanel("Hearing cancelled", caseData, CASE_REFERENCE);

        assertThat(confirmationBody)
            .contains(confirmationPanelMarkup);
    }

    private String stubConfirmationPanelBuilder() {
        String confirmationPanelMarkup = "confirmation panel markup";
        when(confirmationPanelBuilder
                 .buildStandardConfirmationPanel(anyString(), any(PCSCase.class), anyLong()))
            .thenReturn(confirmationPanelMarkup);

        return confirmationPanelMarkup;
    }
}
