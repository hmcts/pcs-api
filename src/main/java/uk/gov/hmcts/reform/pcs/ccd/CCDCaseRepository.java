package uk.gov.hmcts.reform.pcs.ccd;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

@Component
public class CCDCaseRepository extends DecentralisedCaseRepository<PCSCase> {
    @Autowired
    private PebbleEngine pebl;

    @Override
    public PCSCase getCase(long caseRef, String roleAssignments) {
        return PCSCase.builder().
            exampleTabMarkdown(renderExampleTab())
            .build();
    }
    @SneakyThrows
    private String renderExampleTab() {
        PebbleTemplate compiledTemplate = pebl.getTemplate("example");
        Writer writer = new StringWriter();

        compiledTemplate.evaluate(writer, Map.of());
        return writer.toString();
    }
}
