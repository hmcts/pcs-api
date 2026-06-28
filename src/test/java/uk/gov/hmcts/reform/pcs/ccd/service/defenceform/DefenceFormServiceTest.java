package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.defenceform.DefenceFormPayload;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefenceFormServiceTest {

    private static final UUID RESPONSE_ID = UUID.randomUUID();
    private static final String DM_STORE_URL = "https://dm-store/xyz";

    @Mock
    private DefenceFormPersistenceService persistenceService;
    @Mock
    private DefenceFormDocumentGenerator documentGenerator;
    @Mock
    private DocumentImportService documentImportService;

    @InjectMocks
    private DefenceFormService defenceFormService;

    @Test
    void buildsThenRendersOutsideTransactionThenAttaches() {
        DefenceFormPayload payload = DefenceFormPayload.builder().build();
        DefenceFormRenderContext context = new DefenceFormRenderContext(payload, 2);
        when(persistenceService.buildContextIfNotAttached(RESPONSE_ID)).thenReturn(Optional.of(context));
        when(documentGenerator.generate(payload, 2)).thenReturn(DM_STORE_URL);

        defenceFormService.generateAndAttach(RESPONSE_ID);

        InOrder order = inOrder(persistenceService, documentGenerator);
        order.verify(persistenceService).buildContextIfNotAttached(RESPONSE_ID);
        order.verify(documentGenerator).generate(payload, 2);
        order.verify(persistenceService).attach(RESPONSE_ID, DM_STORE_URL);
    }

    @Test
    void skipsRenderAndAttachWhenAlreadyAttached() {
        when(persistenceService.buildContextIfNotAttached(RESPONSE_ID)).thenReturn(Optional.empty());

        defenceFormService.generateAndAttach(RESPONSE_ID);

        verifyNoInteractions(documentGenerator, documentImportService);
        verify(persistenceService, never()).attach(any(), anyString());
    }

    @Test
    void deletesRenderedDocumentWhenAttachFails() {
        DefenceFormPayload payload = DefenceFormPayload.builder().build();
        DefenceFormRenderContext context = new DefenceFormRenderContext(payload, 1);
        when(persistenceService.buildContextIfNotAttached(RESPONSE_ID)).thenReturn(Optional.of(context));
        when(documentGenerator.generate(any(), anyInt())).thenReturn(DM_STORE_URL);
        doThrow(new RuntimeException("attach failed")).when(persistenceService).attach(RESPONSE_ID, DM_STORE_URL);

        assertThatThrownBy(() -> defenceFormService.generateAndAttach(RESPONSE_ID))
            .isInstanceOf(RuntimeException.class);

        verify(documentImportService).deleteDocument(DM_STORE_URL);
    }
}
