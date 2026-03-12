package uk.gov.hmcts.reform.pcs.ccd.page;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TextValidatingPageTest {

    @Mock
    private TextAreaValidationService service;

    private ConcretePageUnderTest pageUnderTest;

    @BeforeEach
    void setup() {
        pageUnderTest = new ConcretePageUnderTest(service);
    }

    @Test
    void shouldSetErrorMessageOverrideWhenValidationReturnsErrors() {
        // Given
        CaseDetails<PCSCase, State> details = CaseDetails.<PCSCase, State>builder()
            .id(123L)
            .data(null)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = pageUnderTest.midEvent(details, null);

        // Then
        assertThat(response.getErrorMessageOverride()).isEqualTo("error 1\nerror 2");
    }

    @Test
    void shouldReturnErrorsWhenValidationServiceReturnsErrors() {
        // Given
        when(service.validateSingleTextArea("txt", "msg", 10)).thenReturn(List.of("x"));

        // When
        List<String> result = pageUnderTest.getValidationErrors("txt", "msg", 10);

        // Then
        assertThat(result).containsExactly("x");
        verify(service).validateSingleTextArea("txt", "msg", 10);
    }

    static class ConcretePageUnderTest extends TextValidatingPage {

        static final List<String> errorList = List.of("error 1", "error 2");

        public ConcretePageUnderTest(TextAreaValidationService textAreaValidationService) {
            super(textAreaValidationService);
        }

        @Override
        public List<String> performValidation(PCSCase data) {
            return errorList;
        }

        @Override
        public String getPageKey() {
            return "someCcdPageId";
        }
    }
}

