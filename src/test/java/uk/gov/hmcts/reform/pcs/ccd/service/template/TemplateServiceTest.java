package uk.gov.hmcts.reform.pcs.ccd.service.template;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private PebbleEngine pebbleEngine;

    private TemplateService underTest;

    @BeforeEach
    void setUp() {
        underTest = new TemplateService(pebbleEngine);
    }

    @Test
    void shouldRenderTemplate() throws IOException {
        // Given
        String templateName = "test-template";
        String expectedContent = "some rendered content";

        Map<String, Object> context = Map.of("a", 1);

        stubPebbleTemplate(templateName, expectedContent);

        // When
        String rendered = underTest.renderTemplate(templateName, context);

        // Then
        assertThat(rendered).isEqualTo(expectedContent);
    }

    @Test
    void shouldWrapRenderingException() throws IOException {
        // Given
        String templateName = "test-template";
        String expectedContent = "some rendered content";

        IOException ioException = mock(IOException.class);

        Map<String, Object> context = Map.of("a", 1);

        PebbleTemplate pebbleTemplate = stubPebbleTemplate(templateName, expectedContent);
        doThrow(ioException).when(pebbleTemplate).evaluate(any(Writer.class), anyMap());

        // When
        Throwable throwable = catchThrowable(() -> underTest.renderTemplate(templateName, context));

        // Then
        assertThat(throwable)
            .isInstanceOf(TemplateRenderingException.class)
            .hasCause(ioException);
    }

    private PebbleTemplate stubPebbleTemplate(String templateName, String renderedContent) throws IOException {
        PebbleTemplate pebbleTemplate = mock(PebbleTemplate.class, withSettings().strictness(Strictness.LENIENT));
        when(pebbleEngine.getTemplate(templateName)).thenReturn(pebbleTemplate);
        doAnswer(invocationOnMock -> {
            StringWriter stringWriter = invocationOnMock.getArgument(0);
            stringWriter.write(renderedContent);
            return null;
        }).when(pebbleTemplate).evaluate(isA(StringWriter.class), anyMap());

        return pebbleTemplate;
    }
}
