package uk.gov.hmcts.reform.pcs.testingsupport.bulkprint;

import java.util.List;
import java.util.UUID;

/**
 * What the test bulk print event asked to post: the case, and each uploaded document's CDAM id and file name.
 */
public record TestBulkPrintTaskData(long caseReference, List<Upload> documents) {

    public record Upload(UUID documentId, String fileName) {
    }
}
