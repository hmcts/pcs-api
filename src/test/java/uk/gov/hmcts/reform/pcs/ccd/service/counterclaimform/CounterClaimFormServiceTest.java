package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterClaimFormServiceTest {

    private static final UUID COUNTER_CLAIM_ID = UUID.randomUUID();
    private static final String DM_STORE_URL = "https://dm-store/xyz";

    @Mock
    private CounterClaimFormPersistenceService persistenceService;
    @Mock
    private CounterClaimFormDocumentGenerator documentGenerator;
    @Mock
    private DocumentImportService documentImportService;

    @InjectMocks
    private CounterClaimFormService underTest;

    @Test
    void buildsThenRendersThenAttaches() {
        CounterClaimFormPayload payload = CounterClaimFormPayload.builder().build();
        CounterClaimFormRenderContext context = new CounterClaimFormRenderContext(payload, 2);
        when(persistenceService.buildContextIfNotAttached(COUNTER_CLAIM_ID)).thenReturn(Optional.of(context));
        when(documentGenerator.generate(payload, 2)).thenReturn(DM_STORE_URL);

        underTest.generateAndAttach(COUNTER_CLAIM_ID);

        InOrder order = inOrder(persistenceService, documentGenerator);
        order.verify(persistenceService).buildContextIfNotAttached(COUNTER_CLAIM_ID);
        order.verify(documentGenerator).generate(payload, 2);
        order.verify(persistenceService).attach(COUNTER_CLAIM_ID, DM_STORE_URL);
        verifyNoInteractions(documentImportService);
    }

    @Test
    void skipsRenderAndAttachWhenAlreadyAttached() {
        when(persistenceService.buildContextIfNotAttached(COUNTER_CLAIM_ID)).thenReturn(Optional.empty());

        underTest.generateAndAttach(COUNTER_CLAIM_ID);

        verifyNoInteractions(documentGenerator, documentImportService);
        verify(persistenceService, never()).attach(any(), anyString());
    }

    @Test
    void deletesRenderedDocumentWhenAttachFails() {
        CounterClaimFormPayload payload = CounterClaimFormPayload.builder().build();
        CounterClaimFormRenderContext context = new CounterClaimFormRenderContext(payload, 1);
        when(persistenceService.buildContextIfNotAttached(COUNTER_CLAIM_ID)).thenReturn(Optional.of(context));
        when(documentGenerator.generate(any(), anyInt())).thenReturn(DM_STORE_URL);
        doThrow(new RuntimeException("attach failed"))
            .when(persistenceService).attach(COUNTER_CLAIM_ID, DM_STORE_URL);

        assertThatThrownBy(() -> underTest.generateAndAttach(COUNTER_CLAIM_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("attach failed");

        verify(documentImportService).deleteDocument(DM_STORE_URL);
    }

    @Test
    void orphanCleanupFailureDoesNotMaskOriginalException() {
        CounterClaimFormPayload payload = CounterClaimFormPayload.builder().build();
        CounterClaimFormRenderContext context = new CounterClaimFormRenderContext(payload, 1);
        when(persistenceService.buildContextIfNotAttached(COUNTER_CLAIM_ID)).thenReturn(Optional.of(context));
        when(documentGenerator.generate(any(), anyInt())).thenReturn(DM_STORE_URL);
        doThrow(new RuntimeException("attach failed"))
            .when(persistenceService).attach(COUNTER_CLAIM_ID, DM_STORE_URL);
        doThrow(new RuntimeException("dm-store unreachable"))
            .when(documentImportService).deleteDocument(DM_STORE_URL);

        assertThatThrownBy(() -> underTest.generateAndAttach(COUNTER_CLAIM_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("attach failed");
    }

    @Test
    void recordGenerationFailureDelegatesAndReturnsCaseReference() {
        when(persistenceService.recordGenerationFailure(eq(COUNTER_CLAIM_ID), any(), anyBoolean()))
            .thenReturn(1234567812345678L);

        long caseReference = underTest.recordGenerationFailure(COUNTER_CLAIM_ID, new RuntimeException("boom"), true);

        assertThat(caseReference).isEqualTo(1234567812345678L);
    }

    @Test
    void recordGenerationFailureSwallowsLoggingFailureAndReturnsZero() {
        when(persistenceService.recordGenerationFailure(eq(COUNTER_CLAIM_ID), any(), anyBoolean()))
            .thenThrow(new RuntimeException("log write failed"));

        long caseReference = underTest.recordGenerationFailure(COUNTER_CLAIM_ID, new RuntimeException("boom"), true);

        assertThat(caseReference).isZero();
    }
}
