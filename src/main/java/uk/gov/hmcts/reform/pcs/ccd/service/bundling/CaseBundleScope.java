package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;


/**
 * Documents included in the judges' bundle, grouped by case file category.
 */
public final class CaseBundleScope {

    /** The document-bundling provider name for documents held in CDAM. */
    public static final String CDAM_PROVIDER = "cdam";

    /**
     * The case file view folder generated bundles are filed under. It is deliberately not a
     * {@code CaseFileCategory}: those drive the document upload, amend and removal journeys, and a
     * generated bundle is replaced by regeneration rather than managed through them.
     */
    public static final String BUNDLES_CATEGORY_ID = "bundles";
    public static final String BUNDLES_CATEGORY_LABEL = "Bundles";


    private CaseBundleScope() {
    }

    /** Whether the case bundle includes this document. */
    public static boolean belongsInBundle(DocumentEntity document) {
        // Hibernate filters soft-deleted rows; also exclude removals still in the current collection.
        return document.getRemovedAt() == null && isBundleable(document);
    }

    /**
     * Whether the bundle includes this document: what an internal user sees in case file view
     * (DocumentsView). Unissued general applications and counterclaims are hidden there, so they
     * stay out of the bundle until issued. Reads the document's associations, so call it where
     * they can be loaded, not from a flush.
     */
    public static boolean isVisibleToInternalReaders(DocumentEntity document) {
        if (!belongsInBundle(document)) {
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

    /** Whether this kind of document goes in the bundle when it is not removed. */
    public static boolean isBundleable(DocumentEntity document) {
        return document.getDocumentId() != null
            && document.getType() != DocumentType.DEFENDANT_ACCESS_CODE;
    }

}
