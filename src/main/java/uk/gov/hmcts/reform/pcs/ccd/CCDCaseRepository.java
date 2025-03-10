package uk.gov.hmcts.reform.pcs.ccd;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

@Component
public class CCDCaseRepository extends DecentralisedCaseRepository<PCSCase> {
    @Autowired
    private PebbleEngine pebl;

    @Autowired
    private PCSCaseRepository repository;

    @Override
    public PCSCase getCase(long caseRef, String roleAssignments) {
        // Load the case from our database.
        var pcsCase = repository.getReferenceById(caseRef);
        // Translate it into the CCD model.
        return
            PCSCase.builder().
                exampleTabMarkdown(renderExampleTab(pcsCase.getCaseDescription()))
            .build();
    }
    @SneakyThrows
    private String renderExampleTab(String description) {
        PebbleTemplate compiledTemplate = pebl.getTemplate("example");
        Writer writer = new StringWriter();

        compiledTemplate.evaluate(writer, Map.of("description", description));
        return writer.toString();
    }
}
