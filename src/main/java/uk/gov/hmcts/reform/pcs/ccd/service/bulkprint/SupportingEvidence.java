package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;

import java.time.Instant;

/**
 * Tells uploads made in a submission journey (a general application or a counterclaim) apart from uploads
 * linked to the same submission later, such as through Upload additional documents, which any party can use.
 * Only the journey uploads go in a pack.
 */
final class SupportingEvidence {

    private SupportingEvidence() {
    }

    /**
     * The generated form is saved after the journey has saved its uploads, and a later upload can only be
     * linked once the submission exists. So an upload saved after the form came later and is left out.
     */
    static boolean savedWithSubmission(DocumentEntity upload, DocumentEntity form) {
        Instant formSaved = form.getSubmittedDate();
        Instant uploadSaved = upload.getSubmittedDate();
        return formSaved == null || uploadSaved == null || !uploadSaved.isAfter(formSaved);
    }
}
