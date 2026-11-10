package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.DraftUpload;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimDraftUploadsViewTest {

    private final ClaimDraftUploadsView underTest =
        new ClaimDraftUploadsView(new JacksonConfiguration().draftCaseDataObjectMapper());

    @Test
    void shouldReturnEachDocumentInTheDraftOnceSoTheDataStoreCanFindItsDocumentUrl() {
        Document statement = Document.builder().url("http://dm-store/documents/abc").build();
        Document other = Document.builder().url("http://dm-store/documents/def").build();
        PCSCase draft = PCSCase.builder()
            .rentArrears(RentArrearsSection.builder()
                             .statementDocuments(List.of(ListValue.<Document>builder().value(statement).build()))
                             .build())
            .additionalDocuments(List.of(
                ListValue.<AdditionalDocument>builder()
                    .value(AdditionalDocument.builder().document(other).build()).build(),
                ListValue.<AdditionalDocument>builder()
                    .value(AdditionalDocument.builder().document(statement).build()).build()))
            .build();
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setCaseFields(pcsCase, draft);

        assertThat(pcsCase.getClaimDraftUploads())
            .extracting(ListValue::getValue)
            .containsExactlyInAnyOrder(new DraftUpload("http://dm-store/documents/abc"),
                                       new DraftUpload("http://dm-store/documents/def"));
    }

    @Test
    void shouldLeaveTheFieldNullWhenTheDraftHasNoUploads() {
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setCaseFields(pcsCase, PCSCase.builder().build());

        assertThat(pcsCase.getClaimDraftUploads()).isNull();
    }
}
