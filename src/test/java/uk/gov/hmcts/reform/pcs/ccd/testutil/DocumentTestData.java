package uk.gov.hmcts.reform.pcs.ccd.testutil;

import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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

    public static FileUploadValidationService restrictionEnabledFileUploadValidationService() {
        FeatureToggleService featureToggleService = mock(FeatureToggleService.class);
        lenient().when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);
        return new FileUploadValidationService(featureToggleService);
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
