package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventAction;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventContext;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventResult;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormPayload;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimFormServiceTest {

    private static final long CASE_REFERENCE = 1234567812345678L;
    private static final String DM_STORE_URL = "https://dm-store/xyz";

    @Mock
    private ClaimFormPersistenceService persistenceService;
    @Mock
    private ClaimFormDocumentGenerator documentGenerator;
    @Mock
    private DocumentImportService documentImportService;
    @Mock
    private SystemCaseEventService systemCaseEventService;

    @InjectMocks
    private ClaimFormService claimFormService;

    @org.junit.jupiter.api.BeforeEach
    void runSystemEventActions() {
        lenient().doAnswer(invocation -> {
            SystemCaseEventAction<Object, Object> action = invocation.getArgument(3);
            action.execute(new SystemCaseEventContext<>(CASE_REFERENCE, null, null));
            return new SystemCaseEventResult(CASE_REFERENCE, 1L, false);
        }).when(systemCaseEventService).submit(anyLong(), any(), any(), any());
    }

    @Test
    void buildsThenRendersOutsideTransactionThenAttaches() {
        ClaimFormPayload payload = ClaimFormPayload.builder().build();
        when(persistenceService.buildPayloadIfNotAttached(CASE_REFERENCE)).thenReturn(Optional.of(payload));
        when(documentGenerator.generate(payload)).thenReturn(DM_STORE_URL);

        claimFormService.generateAndAttach(CASE_REFERENCE);

        InOrder order = inOrder(persistenceService, documentGenerator);
        order.verify(persistenceService).buildPayloadIfNotAttached(CASE_REFERENCE);
        order.verify(documentGenerator).generate(payload);
        order.verify(persistenceService).attach(CASE_REFERENCE, DM_STORE_URL);
    }

    @Test
    void skipsRenderAndAttachWhenAlreadyAttached() {
        when(persistenceService.buildPayloadIfNotAttached(CASE_REFERENCE)).thenReturn(Optional.empty());

        claimFormService.generateAndAttach(CASE_REFERENCE);

        verifyNoInteractions(documentGenerator, documentImportService);
        verify(persistenceService, never()).attach(anyLong(), anyString());
    }

    @Test
    void deletesRenderedDocumentWhenAttachFails() {
        ClaimFormPayload payload = ClaimFormPayload.builder().build();
        when(persistenceService.buildPayloadIfNotAttached(CASE_REFERENCE)).thenReturn(Optional.of(payload));
        when(documentGenerator.generate(payload)).thenReturn(DM_STORE_URL);
        doThrow(new RuntimeException("attach failed")).when(persistenceService).attach(CASE_REFERENCE, DM_STORE_URL);

        assertThatThrownBy(() -> claimFormService.generateAndAttach(CASE_REFERENCE))
            .isInstanceOf(RuntimeException.class);

        verify(documentImportService).deleteDocument(DM_STORE_URL);
    }
}
