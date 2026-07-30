package uk.gov.hmcts.reform.pcs.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
public class TemplateIT extends AbstractPostgresContainerIT {


    @Autowired
    private PebbleEngine pebbleEngine;

    @Test
    void shouldRenderTemplate() throws Exception {
        Map<String, Object> context = Map.of(
            "caseReference", 1234L,
            "partyLabel", "Defendant 2",
            "genAppRank", 7,
            "filenames", List.of("test1.pdf", "test2.pdf")
        );

        String templateName = "workallocation/gen-app-review-additional-docs";
        String rendered = renderTemplate(templateName, context);

        System.out.println(rendered);

        String json = new ObjectMapper().writeValueAsString(rendered);

        System.out.println("#RSS: " + json);
    }

    private String renderTemplate(String templateName, Map<String, Object> context) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate(templateName);
        Writer writer = new StringWriter();

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render template", e);
        }

        return writer.toString();
    }

}
