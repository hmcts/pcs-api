package uk.gov.hmcts.reform.pcs.ccd.renderer.tabs;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseReviewDateEntity;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CaseReviewDatesRenderer {

    private static final String TEMPLATE_PATH = "tabs/case-review-dates";

    private final PebbleEngine pebbleEngine;

    public String render(List<CaseReviewDateEntity> reviewDates) {
        Map<String, Object> context = Map.of(
            "reviewDates", reviewDates
        );

        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate(TEMPLATE_PATH);
        Writer writer = new StringWriter();

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render template", e);
        }

        return writer.toString();
    }


}
