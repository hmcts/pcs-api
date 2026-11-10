package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DraftUploadsTest {

    @Test
    void shouldReadTheClaimUploadFieldsOnly() {
        Document additional = document("additional");
        Document statement = document("statement");
        Document tenancy = document("tenancy");
        PCSCase draft = PCSCase.builder()
            .additionalDocuments(List.of(ListValue.<AdditionalDocument>builder()
                                             .value(AdditionalDocument.builder().document(additional).build())
                                             .build()))
            .rentArrears(RentArrearsSection.builder().statementDocuments(wrap(statement)).build())
            .tenancyLicenceDetails(TenancyLicenceDetails.builder().tenancyLicenceDocuments(wrap(tenancy)).build())
            // A submitted case document copied into the draft by a mid-event save is not an upload.
            .allDocuments(wrap(document("submitted")))
            .build();

        assertThat(DraftUploads.forEvent(EventId.resumePossessionClaim, draft))
            .containsExactlyInAnyOrder(additional, statement, tenancy);
    }

    @Test
    void shouldSkipEmptyRowsAndDocumentsWithoutAUrl() {
        PCSCase draft = PCSCase.builder()
            .additionalDocuments(List.of(ListValue.<AdditionalDocument>builder().build(),
                                         ListValue.<AdditionalDocument>builder()
                                             .value(AdditionalDocument.builder().build())
                                             .build()))
            .rentArrears(RentArrearsSection.builder().statementDocuments(wrap(Document.builder().build())).build())
            .build();

        assertThat(DraftUploads.claimUploads(draft)).isEmpty();
    }

    @Test
    void shouldReturnNothingForAnEventWithoutUploadFieldsOrANullDraft() {
        PCSCase draft = PCSCase.builder()
            .rentArrears(RentArrearsSection.builder().statementDocuments(wrap(document("statement"))).build())
            .build();

        assertThat(DraftUploads.forEvent(EventId.respondPossessionClaim, draft)).isEmpty();
        assertThat(DraftUploads.forEvent(EventId.resumePossessionClaim, null)).isEmpty();
    }

    static Document document(String name) {
        return Document.builder()
            .url("http://dm-store/documents/" + name)
            .binaryUrl("http://dm-store/documents/" + name + "/binary")
            .filename(name + ".pdf")
            .build();
    }

    private static List<ListValue<Document>> wrap(Document document) {
        return List.of(ListValue.<Document>builder().value(document).build());
    }
}
