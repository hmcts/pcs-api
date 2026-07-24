package uk.gov.hmcts.reform.pcs.ccd.testutil;

import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;

/**
 * Shared helpers for building document {@link ListValue} lists in tests that exercise file uploads.
 */
public final class DocumentTestData {

    private DocumentTestData() {
    }

    public static List<ListValue<Document>> documentsWithFilenames(String... filenames) {
        List<Document> documents = new ArrayList<>();
        for (String filename : filenames) {
            documents.add(Document.builder().filename(filename).build());
        }
        return wrapListItems(documents);
    }

    public static List<ListValue<AdditionalDocument>> additionalDocumentsWithFilenames(String... filenames) {
        List<AdditionalDocument> additionalDocuments = new ArrayList<>();
        for (String filename : filenames) {
            additionalDocuments.add(AdditionalDocument.builder()
                .document(Document.builder().filename(filename).build())
                .build());
        }
        return wrapListItems(additionalDocuments);
    }
}
