package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DraftUpload;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimDraftUploadsViewTest {

    private final ClaimDraftUploadsView underTest = new ClaimDraftUploadsView();

    @Test
    void shouldReturnTheDraftUploadsSoTheDataStoreCanFindTheirDocumentUrl() {
        Document statement = Document.builder()
            .url("http://dm-store/documents/abc")
            .binaryUrl("http://dm-store/documents/abc/binary")
            .filename("statement.pdf")
            .build();
        PCSCase draft = PCSCase.builder()
            .rentArrears(RentArrearsSection.builder()
                             .statementDocuments(List.of(ListValue.<Document>builder().value(statement).build()))
                             .build())
            .build();
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setCaseFields(pcsCase, draft);

        assertThat(pcsCase.getClaimDraftUploads())
            .extracting(ListValue::getValue)
            .containsExactly(DraftUpload.builder()
                                 .documentUrl("http://dm-store/documents/abc")
                                 .documentBinaryUrl("http://dm-store/documents/abc/binary")
                                 .documentFilename("statement.pdf")
                                 .build());
    }

    @Test
    void shouldLeaveTheFieldNullWhenTheDraftHasNoUploads() {
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setCaseFields(pcsCase, PCSCase.builder().build());

        assertThat(pcsCase.getClaimDraftUploads()).isNull();
    }
}
