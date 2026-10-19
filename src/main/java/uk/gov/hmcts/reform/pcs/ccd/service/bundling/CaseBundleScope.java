package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;

/**
 * Documents included in the judges' bundle, grouped by case file category.
 */
public final class CaseBundleScope {

    /** The document-bundling provider name for documents held in CDAM. */
    public static final String CDAM_PROVIDER = "cdam";

    /** Case file view folder for generated bundles; not a CaseFileCategory, as those drive the upload journeys. */
    public static final String BUNDLES_CATEGORY_ID = "bundles";
    public static final String BUNDLES_CATEGORY_LABEL = "Bundles";

    private CaseBundleScope() {
    }

    /** Whether the bundle includes this document: what an internal user sees in case file view (DocumentsView). */
    public static boolean isVisibleToInternalReaders(DocumentEntity document) {
        if (document.getDocumentId() == null || document.getType() == DocumentType.DEFENDANT_ACCESS_CODE) {
            return false;
        }
        GenAppEntity genApp = document.getGeneralApplication();
        if (genApp != null && genApp.getWithoutNotice() != VerticalYesNo.YES
            && genApp.getState() != GenAppState.GEN_APP_ISSUED) {
            return false;
        }
        CounterClaimEntity counterClaim = document.getCounterClaim();
        return counterClaim == null || counterClaim.getStatus() == CounterClaimState.COUNTER_CLAIM_ISSUED;
    }
}
