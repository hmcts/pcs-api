package uk.gov.hmcts.reform.pcs.ccd;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.entity.PcsCase;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;

import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Invoked by CCD to load PCS cases under the decentralised model.
 */
@Component
public class CCDCaseRepository extends DecentralisedCaseRepository<PCSCase> {
    @Autowired
    private PebbleEngine pebl;

    @Autowired
    private PCSCaseRepository repository;

    /**
     * Invoked by CCD to load PCS cases by reference.
     * @param caseRef The case to load
     */
    @Override
    public PCSCase getCase(long caseRef) {
        // Load the case from our database.
        var pcsCase = repository.getReferenceById(caseRef);
        // Translate it into the CCD model.
        return
            PCSCase.builder()
                .caseDescription(pcsCase.getCaseDescription())
                .exampleTabMarkdown(renderExampleTab(pcsCase))
                .organisationName("Luton Local Housing")
                .state("Pre-Submission")
                .country(pcsCase.getCountry())
                .build();
    }

    @SneakyThrows
    private String renderExampleTab(PcsCase c) {
        PebbleTemplate compiledTemplate = pebl.getTemplate("example");
        Writer writer = new StringWriter();

        compiledTemplate.evaluate(writer, Map.of(
            "description", "A possession claim",
            "parties", c.getParties(),
            "caseReference", c.getReference(),
            "time", LocalDateTime.now()
        ));
        return writer.toString();
    }
}
