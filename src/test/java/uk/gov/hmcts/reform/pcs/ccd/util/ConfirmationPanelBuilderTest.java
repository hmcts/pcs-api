package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.service.template.TemplateService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@ExtendWith(MockitoExtension.class)
class ConfirmationPanelBuilderTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private TemplateService templateService;
    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    private ConfirmationPanelBuilder underTest;

    @BeforeEach
    void setUp() {
        underTest = new ConfirmationPanelBuilder(addressFormatter, templateService);
    }

    @Test
    void shouldBuildStandardConfirmationPanel() {
        // Given
        String title = "some title";
        String renderedTemplate = "rendered template";

        AddressUK propertyAddress = mock(AddressUK.class);

        String caseName = "some case name";
        String formattedAddress = "formatted address";

        PCSCase caseData = PCSCase.builder()
            .caseNameHmctsInternal(caseName)
            .propertyAddress(propertyAddress)
            .build();

        when(addressFormatter.formatShortAddress(propertyAddress, COMMA_DELIMITER))
            .thenReturn(formattedAddress);

        when(templateService.renderTemplate(anyString(), anyMap())).thenReturn(renderedTemplate);

        // When
        String confirmationPanel = underTest.buildStandardConfirmationPanel(title, caseData, CASE_REFERENCE);

        // Then
        assertThat(confirmationPanel).isEqualTo(renderedTemplate);
        verify(templateService).renderTemplate(eq("confirmationPanel"), mapCaptor.capture());
        assertThat(mapCaptor.getValue())
            .contains(entry("title", title))
            .contains(entry("bodyLines",
                            List.of(
                                "Case number #%d".formatted(CASE_REFERENCE),
                                formattedAddress,
                                caseName
                            )));
    }

    @Test
    void shouldBuildCustomConfirmationPanel() {
        // Given
        String title = "some title";
        List<String> bodyLines = List.of("a", "b", "c");
        String renderedTemplate = "rendered template";

        when(templateService.renderTemplate(anyString(), anyMap())).thenReturn(renderedTemplate);

        // When
        String confirmationPanel = underTest.buildConfirmationPanel(title, bodyLines);

        // Then
        assertThat(confirmationPanel).isEqualTo(renderedTemplate);
        verify(templateService).renderTemplate(eq("confirmationPanel"), mapCaptor.capture());
        assertThat(mapCaptor.getValue())
            .contains(entry("title", title))
            .contains(entry("bodyLines", bodyLines));
    }

}
