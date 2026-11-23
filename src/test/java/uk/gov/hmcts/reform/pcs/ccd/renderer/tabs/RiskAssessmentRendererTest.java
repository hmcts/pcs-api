package uk.gov.hmcts.reform.pcs.ccd.renderer.tabs;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.riskassessment.RiskAssessmentRow;
import uk.gov.hmcts.reform.pcs.config.PebbleConfiguration;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskAssessmentRendererTest {

    @Test
    void shouldEscapeClaimantTextAndPreserveLineBreaksInSemanticMarkup() {
        RiskAssessmentRenderer renderer = new RiskAssessmentRenderer(new PebbleConfiguration().pebbleEngine());
        List<RiskAssessmentRow> rows = List.of(new RiskAssessmentRow(
            "Is it difficult to access the property?", "Yes",
            List.of("First line\n<script>alert(1)</script>\nA & B < C ${caseTitleMarkdown}")));

        String html = renderer.render("10 October 2026", rows, true);

        assertThat(html)
            .contains("<h2", "<dl", "<dt", "<dd", "First line<br>", "&#60;script&#62;", "&#38;", "&#60;",
                      "&#36;&#123;caseTitleMarkdown&#125;")
            .doesNotContain("<script>", "Bailiff note", "Risk assessment level", "onclick=", "Print",
                            "${caseTitleMarkdown}");
    }

    @Test
    void shouldWrapTemplateEvaluationFailures() throws IOException {
        PebbleEngine engine = mock(PebbleEngine.class);
        PebbleTemplate template = mock(PebbleTemplate.class);
        IOException cause = new IOException("Unable to write template");
        when(engine.getTemplate("tabs/risk-assessment")).thenReturn(template);
        doThrow(cause).when(template).evaluate(any(Writer.class), anyMap());
        RiskAssessmentRenderer renderer = new RiskAssessmentRenderer(engine);

        assertThatThrownBy(() -> renderer.render("10 October 2026", List.of(), false))
            .isInstanceOf(TemplateRenderingException.class)
            .hasCause(cause);
    }
}
