package uk.gov.hmcts.reform.pcs.ccd.view.globalsearch;

import java.util.List;

/**
 * Supplies references, other than the case reference, that global search should find a case by. They are
 * indexed as the case's {@code SearchCriteria.OtherCaseReferences}, which XUI searches as "Other reference".
 */
public interface OtherCaseReferenceProvider {

    /**
     * The other references for the case, in a stable order. Empty if it has none.
     * @param caseReference the CCD case reference
     */
    List<String> otherCaseReferences(long caseReference);

}
