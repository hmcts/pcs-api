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
        String filename = counterClaimFormFilename(
            featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4),
            counterClaimRank,
            defendantNumber
        );
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

    /**
     * Name of the counterclaim form PDF. Translation looks the form up by this name before the file exists,
     * so the generator and those lookups must use this method.
     */
    public static String counterClaimFormFilename(boolean release14Enabled,
                                                  Integer counterClaimRank,
                                                  int defendantNumber) {
        if (release14Enabled) {
            int rank = counterClaimRank != null ? counterClaimRank : 1;
            return String.format(OUTPUT_FILENAME_TEMPLATE, rank, defendantNumber);
        }
        return expectedCounterClaimFormFilename(defendantNumber);
    }

    public static String expectedCounterClaimFormFilename(int defendantNumber) {
        return OUTPUT_FILENAME_PREFIX + defendantNumber;
    }
}
