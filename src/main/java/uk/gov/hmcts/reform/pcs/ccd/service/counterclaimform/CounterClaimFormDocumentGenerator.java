package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

@Service
@AllArgsConstructor
public class CounterClaimFormDocumentGenerator {
    static final String LIP_TEMPLATE_ID = "CV-PCS-CLM-ENG-Counterclaim-Form.docx";
    static final String LR_TEMPLATE_ID = "CV-PCS-CLM-ENG-Counterclaim-Form-LR.docx";
    static final String OUTPUT_FILENAME_PREFIX = "Counterclaim - Defendant ";
    static final String OUTPUT_FILENAME_TEMPLATE = "Counterclaim CC%d - Defendant %d";

    private final DocAssemblyService docAssemblyService;
    private final FeatureToggleService featureToggleService;

    public String generate(CounterClaimFormPayload payload, int counterClaimRank, int defendantNumber) {
        String filename = featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)
            ? String.format(OUTPUT_FILENAME_TEMPLATE, counterClaimRank, defendantNumber)
            : OUTPUT_FILENAME_PREFIX + defendantNumber;
        return docAssemblyService.generateDocument(
            payload,
            getTemplateId(payload),
            OutputType.PDF,
            filename
        );
    }

    private static String getTemplateId(CounterClaimFormPayload payload) {
        return payload.isCompletedByLegalRepresentative() ? LR_TEMPLATE_ID : LIP_TEMPLATE_ID;
    }

    public static String expectedCounterClaimFormFilename(int defendantNumber) {
        return OUTPUT_FILENAME_PREFIX + defendantNumber;
    }
}
