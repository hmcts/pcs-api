package uk.gov.hmcts.reform.pcs.ccd.renderer.tabs;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseReviewDateEntity;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class CaseReviewDatesRendererTest {

    @Mock
    private PebbleEngine pebbleEngine;
    @Captor
    private ArgumentCaptor<Map<String, Object>> contextMapCaptor;

    private CaseReviewDatesRenderer underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseReviewDatesRenderer(pebbleEngine);
    }

    @Test
    void shouldRender() throws IOException {
        // Given
        List<CaseReviewDateEntity> caseReviewDateEntities = List.of(mock(CaseReviewDateEntity.class));
        String expectedRenderedContent = "some rendered content";
        PebbleTemplate pebbleTemplate = stubPebbleTemplate("tabs/case-review-dates", expectedRenderedContent);

        // When
        String actualRenderedContent = underTest.render(caseReviewDateEntities);

        // Then
        assertThat(actualRenderedContent).isEqualTo(expectedRenderedContent);

        verify(pebbleTemplate).evaluate(isA(StringWriter.class), contextMapCaptor.capture());
        Map<String, Object> contextMap = contextMapCaptor.getValue();
        assertThat(contextMap)
            .containsEntry("reviewDates", caseReviewDateEntities);
    }

    @Test
    void shouldThrowExceptionWhenUnableToRender() throws IOException {
        // Given
        PebbleTemplate pebbleTemplate = stubPebbleTemplate(
            "tabs/case-review-dates",
            "some content"
        );

        IOException pebbleException = mock(IOException.class);
        doThrow(pebbleException).when(pebbleTemplate).evaluate(any(StringWriter.class), anyMap());

        // When
        Throwable throwable = catchThrowable(() -> underTest.render(List.of()));

        // Then
        assertThat(throwable)
            .isInstanceOf(TemplateRenderingException.class)
            .hasMessage("Failed to render template")
            .hasCause(pebbleException);
    }

    @SuppressWarnings("SameParameterValue")
    private PebbleTemplate stubPebbleTemplate(String templatePath, String renderedContent) throws IOException {
        PebbleTemplate pebbleTemplate = mock(PebbleTemplate.class, withSettings().strictness(Strictness.LENIENT));
        when(pebbleEngine.getTemplate(templatePath)).thenReturn(pebbleTemplate);
        doAnswer(invocationOnMock -> {
            StringWriter stringWriter = invocationOnMock.getArgument(0);
            stringWriter.write(renderedContent);
            return null;
        }).when(pebbleTemplate).evaluate(isA(StringWriter.class), anyMap());

        return pebbleTemplate;
    }

}
