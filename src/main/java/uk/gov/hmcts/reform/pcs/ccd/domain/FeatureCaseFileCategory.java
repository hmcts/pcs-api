package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Case File View folders whose documents are filed by a feature of their own, rather than by the
 * caseworker document events.
 *
 * <p>They are kept out of {@link CaseFileCategory} because that enum is also the "Which folder is the
 * document in?" list in Amend documents and Remove document, and Amend documents files a document by
 * its document type, so it would move a document out of one of these folders.
 *
 * <p>Display orders share one sequence with {@link CaseFileCategory}, since CCD requires sibling
 * categories to have distinct display orders.
 */
@AllArgsConstructor
@Getter
public enum FeatureCaseFileCategory {

    ENFORCEMENT("enforcement", "Enforcement", 9);

    private final String id;
    private final String label;
    private final int displayOrder;
}
