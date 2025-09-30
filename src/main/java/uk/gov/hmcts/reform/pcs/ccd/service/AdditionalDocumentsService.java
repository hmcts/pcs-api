package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@Service
public class AdditionalDocumentsService {

    // Pick whatever you want as the default
    private static final AdditionalDocumentType DEFAULT_TYPE =
        AdditionalDocumentType.WITNESS_STATEMENT;

    /** Prefill a type for any row that doesn't have one yet. */
    public void applyDefaultType(PCSCase data) {
        if (data == null || data.getAdditionalDocuments() == null) return;

        for (ListValue<AdditionalDocument> lv : data.getAdditionalDocuments()) {
            AdditionalDocument doc = lv != null ? lv.getValue() : null;
            if (doc != null && doc.getDocumentType() == null) {
                doc.setDocumentType(DEFAULT_TYPE);
            }
        }
    }
}
