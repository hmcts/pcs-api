package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;

import java.util.Map;
import java.util.UUID;

/**
 * What goes in the case bundle and how its outbox jobs are keyed. Until the business defines the
 * bundle's folders, it holds every document in case file view (from the system user's point of
 * view), one folder per case file category.
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

    /** Selector parameter naming the pcs_case row a job bundles. */
    public static final String CASE_ID_PARAMETER = "caseId";

    private static final String COALESCE_KEY_PREFIX = "pcs-case-bundle:";

    private CaseBundleScope() {
    }

    /** Whether the case bundle includes this document. */
    public static boolean belongsInBundle(DocumentEntity document) {
        return !document.isRemoved() && isBundleable(document);
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
            && document.getType() != DocumentType.CASE_BUNDLE
            && document.getType() != DocumentType.DEFENDANT_ACCESS_CODE
            && !BUNDLES_CATEGORY_ID.equals(document.getCategoryId());
    }

    /** One waiting job per case: changes made before it is claimed collapse onto it. */
    public static String coalesceKey(UUID caseId) {
        return COALESCE_KEY_PREFIX + caseId;
    }

    public static Map<String, String> selectorParameters(UUID caseId) {
        return Map.of(CASE_ID_PARAMETER, caseId.toString());
    }

    public static UUID caseId(Map<String, String> selectorParameters) {
        String caseId = selectorParameters.get(CASE_ID_PARAMETER);
        if (caseId == null) {
            throw new IllegalStateException("Case bundle job has no " + CASE_ID_PARAMETER + " parameter");
        }
        return UUID.fromString(caseId);
    }
}
