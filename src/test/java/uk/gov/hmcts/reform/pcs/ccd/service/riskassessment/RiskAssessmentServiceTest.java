package uk.gov.hmcts.reform.pcs.ccd.service.riskassessment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.riskassessment.RiskAssessmentPayload;

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
class RiskAssessmentServiceTest {

    private static final UUID ENFORCEMENT_ORDER_ID = UUID.randomUUID();
    private static final RiskAssessmentPayload PAYLOAD = RiskAssessmentPayload.builder().build();
    private static final String DOCUMENT_URL = "risk assessment url";
    private static final Integer RANK = 1;
    private static final RiskAssessmentRenderContext CONTEXT = new RiskAssessmentRenderContext(PAYLOAD, RANK);

    @Mock
    private RiskAssessmentPersistenceService persistenceService;
    @Mock
    private RiskAssessmentDocumentGenerator documentGenerator;
    @Mock
    private DocumentImportService documentImportService;

    @InjectMocks
    private RiskAssessmentService underTest;

    @Test
    void shouldGenerateTheRiskAssessmentThenAttachIt() {
        when(persistenceService.buildRenderContext(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.of(CONTEXT));
        when(documentGenerator.generate(PAYLOAD, RANK)).thenReturn(DOCUMENT_URL);

        underTest.generateAndAttach(ENFORCEMENT_ORDER_ID);

        InOrder order = inOrder(persistenceService, documentGenerator);
        order.verify(persistenceService).buildRenderContext(ENFORCEMENT_ORDER_ID);
        order.verify(documentGenerator).generate(PAYLOAD, RANK);
        order.verify(persistenceService).attachDocument(ENFORCEMENT_ORDER_ID, DOCUMENT_URL);
        verifyNoInteractions(documentImportService);
    }

    @Test
    void shouldSkipGenerateAndAttachWhenThereIsNoRiskAssessmentToGenerate() {
        when(persistenceService.buildRenderContext(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.empty());

        underTest.generateAndAttach(ENFORCEMENT_ORDER_ID);

        verifyNoInteractions(documentGenerator, documentImportService);
        verify(persistenceService, never()).attachDocument(any(), any());
    }

    @Test
    void shouldNotAttachWhenGenerationFails() {
        when(persistenceService.buildRenderContext(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.of(CONTEXT));
        when(documentGenerator.generate(PAYLOAD, RANK))
            .thenThrow(new RuntimeException("docassembly 500"));

        assertThatThrownBy(() -> underTest.generateAndAttach(ENFORCEMENT_ORDER_ID))
            .isInstanceOf(RuntimeException.class);

        verify(persistenceService, never()).attachDocument(any(), any());
        verifyNoInteractions(documentImportService);
    }

    @Test
    void shouldDeleteTheGeneratedRiskAssessmentWhenAttachFails() {
        when(persistenceService.buildRenderContext(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.of(CONTEXT));
        when(documentGenerator.generate(PAYLOAD, RANK)).thenReturn(DOCUMENT_URL);
        doThrow(new RuntimeException("attach failed"))
            .when(persistenceService).attachDocument(ENFORCEMENT_ORDER_ID, DOCUMENT_URL);

        assertThatThrownBy(() -> underTest.generateAndAttach(ENFORCEMENT_ORDER_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("attach failed");

        verify(documentImportService).deleteDocument(DOCUMENT_URL);
    }

    @Test
    void shouldStillThrowTheAttachErrorWhenDeletingTheGeneratedRiskAssessmentFails() {
        when(persistenceService.buildRenderContext(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.of(CONTEXT));
        when(documentGenerator.generate(PAYLOAD, RANK)).thenReturn(DOCUMENT_URL);
        doThrow(new RuntimeException("attach failed"))
            .when(persistenceService).attachDocument(ENFORCEMENT_ORDER_ID, DOCUMENT_URL);
        doThrow(new RuntimeException("delete failed")).when(documentImportService).deleteDocument(DOCUMENT_URL);

        assertThatThrownBy(() -> underTest.generateAndAttach(ENFORCEMENT_ORDER_ID))
            .hasMessage("attach failed");
    }
}
