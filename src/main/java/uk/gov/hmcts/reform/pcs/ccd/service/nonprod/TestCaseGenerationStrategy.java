package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

import org.springframework.core.io.Resource;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public interface TestCaseGenerationStrategy {

    // The label must be derivable from the file name removing the "-" and the ".json" extension
    boolean supports(String label);

    String getLabel();

    CaseSupportGenerationResponse generate(long caseReference, PCSCase caseData, Resource nonProdResource);

}
