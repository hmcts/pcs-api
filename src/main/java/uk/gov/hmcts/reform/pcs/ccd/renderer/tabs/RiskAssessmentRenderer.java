package uk.gov.hmcts.reform.pcs.ccd.renderer.tabs;

import io.pebbletemplates.pebble.PebbleEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.riskassessment.RiskAssessmentRow;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RiskAssessmentRenderer {

    private final PebbleEngine pebbleEngine;

    public String render(String submittedDate, List<RiskAssessmentRow> rows, boolean claimantView) {
        Map<String, Object> context = Map.of(
            "submittedDate", submittedDate,
            "rows", rows,
            "claimantView", claimantView
        );
        StringWriter writer = new StringWriter();
        try {
            pebbleEngine.getTemplate("tabs/risk-assessment").evaluate(writer, context);
        } catch (IOException exception) {
            throw new TemplateRenderingException("Failed to render risk assessment", exception);
        }
        return writer.toString();
    }
}
