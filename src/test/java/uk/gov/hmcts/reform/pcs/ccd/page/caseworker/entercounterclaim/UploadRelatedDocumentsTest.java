package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterCounterClaimDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.FileTypeService;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UploadRelatedDocumentsTest extends BasePageTest {

    @Mock
    private FileTypeService fileTypeService;

    @InjectMocks
    private UploadRelatedDocuments uploadRelatedDocuments;

    @BeforeEach
    void setUp() {
        setPageUnderTest(uploadRelatedDocuments);
    }

    @Test
    void shouldValidateFiles() {
        // Given
        Document document = Document.builder().build();
        List<ListValue<Document>> documentList = List.of(ListValue.<Document>builder().value(document).build());
        EnterCounterClaimDetails enterCounterClaimDetails = EnterCounterClaimDetails.builder()
            .relatedDocuments(documentList)
            .build();
        PCSCase pcsCase = PCSCase.builder().enterCounterClaim(enterCounterClaimDetails).build();

        // When
        callMidEventHandler(pcsCase);

        // Then
        verify(fileTypeService).validateNonMultiMediaFiles(documentList, List.of());
    }
}
