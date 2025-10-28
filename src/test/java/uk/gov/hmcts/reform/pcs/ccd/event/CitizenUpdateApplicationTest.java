package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitizenUpdateApplicationTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new CitizenUpdateApplication(pcsCaseService));
    }

    @Test
    void shouldUpdateCaseOnSubmit() {
        long caseReference = 1234L;
        PCSCase caseData = mock(PCSCase.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        callSubmitHandler(caseData);

        verify(pcsCaseService).loadCase(caseReference);
        verify(pcsCaseService).mergeCaseData(pcsCaseEntity, caseData);
        verify(pcsCaseService).save(pcsCaseEntity);
    }

    @Test
    void shouldUpdateCaseWhenCitizenDocumentsAreNull() {
        long caseReference = 1234L;
        PCSCase caseData = mock(PCSCase.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        when(caseData.getCitizenDocuments()).thenReturn(null);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        callSubmitHandler(caseData);

        verify(pcsCaseService).loadCase(caseReference);
        verify(pcsCaseService).mergeCaseData(pcsCaseEntity, caseData);
        verify(pcsCaseService).save(pcsCaseEntity);
    }

    @Test
    void shouldUpdateCaseWhenCitizenDocumentsAreEmpty() {
        long caseReference = 1234L;
        PCSCase caseData = mock(PCSCase.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        when(caseData.getCitizenDocuments()).thenReturn(Collections.emptyList());
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        callSubmitHandler(caseData);

        verify(pcsCaseService).loadCase(caseReference);
        verify(pcsCaseService).mergeCaseData(pcsCaseEntity, caseData);
        verify(pcsCaseService).save(pcsCaseEntity);
    }

    @Test
    void shouldUpdateCaseWithCitizenDocuments() {
        long caseReference = 1234L;
        PCSCase caseData = mock(PCSCase.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        Document document = Document.builder()
            .filename("evidence.pdf")
            .url("http://example.com/evidence.pdf")
            .binaryUrl("http://example.com/evidence.pdf/binary")
            .build();

        AdditionalDocument additionalDocument = AdditionalDocument.builder()
            .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
            .document(document)
            .description("Witness statement from tenant")
            .build();

        List<ListValue<AdditionalDocument>> citizenDocuments = List.of(
            ListValue.<AdditionalDocument>builder()
                .id("1")
                .value(additionalDocument)
                .build()
        );

        when(caseData.getCitizenDocuments()).thenReturn(citizenDocuments);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        callSubmitHandler(caseData);

        verify(pcsCaseService).loadCase(caseReference);
        verify(pcsCaseService).mergeCaseData(pcsCaseEntity, caseData);
        verify(pcsCaseService).save(pcsCaseEntity);
    }

    @Test
    void shouldUpdateCaseWithMultipleCitizenDocuments() {
        long caseReference = 1234L;
        PCSCase caseData = mock(PCSCase.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        Document document1 = Document.builder()
            .filename("witness_statement.pdf")
            .build();

        Document document2 = Document.builder()
            .filename("rent_statement.pdf")
            .build();

        AdditionalDocument additionalDocument1 = AdditionalDocument.builder()
            .documentType(AdditionalDocumentType.WITNESS_STATEMENT)
            .document(document1)
            .description("Witness statement")
            .build();

        AdditionalDocument additionalDocument2 = AdditionalDocument.builder()
            .documentType(AdditionalDocumentType.RENT_STATEMENT)
            .document(document2)
            .description("Rent statement")
            .build();

        List<ListValue<AdditionalDocument>> citizenDocuments = List.of(
            ListValue.<AdditionalDocument>builder()
                .id("1")
                .value(additionalDocument1)
                .build(),
            ListValue.<AdditionalDocument>builder()
                .id("2")
                .value(additionalDocument2)
                .build()
        );

        when(caseData.getCitizenDocuments()).thenReturn(citizenDocuments);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        callSubmitHandler(caseData);

        verify(pcsCaseService).loadCase(caseReference);
        verify(pcsCaseService).mergeCaseData(pcsCaseEntity, caseData);
        verify(pcsCaseService).save(pcsCaseEntity);
    }

}
