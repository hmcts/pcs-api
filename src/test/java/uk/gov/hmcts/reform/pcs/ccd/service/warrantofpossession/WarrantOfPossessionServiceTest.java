package uk.gov.hmcts.reform.pcs.ccd.service.warrantofpossession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.warrantofpossession.WarrantOfPossessionPayload;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantOfPossessionServiceTest {

    private static final UUID ENFORCEMENT_ORDER_ID = UUID.randomUUID();
    private static final WarrantOfPossessionPayload PAYLOAD = WarrantOfPossessionPayload.builder().build();
    private static final String DOCUMENT_URL = "warrant url";
    private static final Integer RANK = 1;
    private static final WarrantOfPossessionRenderContext CONTEXT = new WarrantOfPossessionRenderContext(PAYLOAD, RANK);

    @Mock
    private WarrantOfPossessionPersistenceService persistenceService;
    @Mock
    private WarrantOfPossessionDocumentGenerator documentGenerator;
    @Mock
    private DocumentImportService documentImportService;

    @InjectMocks
    private WarrantOfPossessionService underTest;

    @Test
    void shouldGenerateTheWarrantThenAttachIt() {
        when(persistenceService.buildRenderContext(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.of(CONTEXT));
        when(documentGenerator.generate(PAYLOAD, RANK)).thenReturn(DOCUMENT_URL);

        underTest.generateAndAttach(ENFORCEMENT_ORDER_ID);

        InOrder order = inOrder(persistenceService, documentGenerator);
        order.verify(persistenceService).buildRenderContext(ENFORCEMENT_ORDER_ID);
        order.verify(documentGenerator).generate(PAYLOAD, RANK);
        order.verify(persistenceService).attachDocumentAndQueueEmail(ENFORCEMENT_ORDER_ID, DOCUMENT_URL);
        verifyNoInteractions(documentImportService);
    }

    @Test
    void shouldSkipGenerateAndAttachWhenThereIsNoWarrantToGenerate() {
        when(persistenceService.buildRenderContext(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.empty());

        underTest.generateAndAttach(ENFORCEMENT_ORDER_ID);

        verifyNoInteractions(documentGenerator, documentImportService);
        verify(persistenceService, never()).attachDocumentAndQueueEmail(any(), any());
    }

    @Test
    void shouldNotAttachWhenGenerationFails() {
        when(persistenceService.buildRenderContext(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.of(CONTEXT));
        when(documentGenerator.generate(PAYLOAD, RANK))
            .thenThrow(new RuntimeException("docassembly 500"));

        assertThatThrownBy(() -> underTest.generateAndAttach(ENFORCEMENT_ORDER_ID))
            .isInstanceOf(RuntimeException.class);

        verify(persistenceService, never()).attachDocumentAndQueueEmail(any(), any());
        verifyNoInteractions(documentImportService);
    }

    @Test
    void shouldDeleteTheGeneratedWarrantWhenAttachFails() {
        when(persistenceService.buildRenderContext(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.of(CONTEXT));
        when(documentGenerator.generate(PAYLOAD, RANK)).thenReturn(DOCUMENT_URL);
        doThrow(new RuntimeException("attach failed"))
            .when(persistenceService).attachDocumentAndQueueEmail(ENFORCEMENT_ORDER_ID, DOCUMENT_URL);

        assertThatThrownBy(() -> underTest.generateAndAttach(ENFORCEMENT_ORDER_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("attach failed");

        verify(documentImportService).deleteDocument(DOCUMENT_URL);
    }

    @Test
    void shouldStillThrowTheAttachErrorWhenDeletingTheGeneratedWarrantFails() {
        when(persistenceService.buildRenderContext(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.of(CONTEXT));
        when(documentGenerator.generate(PAYLOAD, RANK)).thenReturn(DOCUMENT_URL);
        doThrow(new RuntimeException("attach failed"))
            .when(persistenceService).attachDocumentAndQueueEmail(ENFORCEMENT_ORDER_ID, DOCUMENT_URL);
        doThrow(new RuntimeException("delete failed")).when(documentImportService).deleteDocument(DOCUMENT_URL);

        assertThatThrownBy(() -> underTest.generateAndAttach(ENFORCEMENT_ORDER_ID))
            .hasMessage("attach failed");
    }
}
