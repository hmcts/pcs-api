package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.hearingnotice.HearingNoticePayload;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingNoticeServiceTest {

    private static final Integer HEARING_ID = 7;
    private static final UUID CLAIMANT_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final HearingNoticePayload CLAIMANT_PAYLOAD = HearingNoticePayload.builder().build();
    private static final HearingNoticePayload DEFENDANT_PAYLOAD = HearingNoticePayload.builder().build();

    @Mock
    private HearingNoticePersistenceService persistenceService;
    @Mock
    private HearingNoticeDocumentGenerator documentGenerator;
    @Mock
    private DocumentImportService documentImportService;

    @InjectMocks
    private HearingNoticeService underTest;

    @Test
    void shouldGenerateEachNoticeThenAttachTheirUrls() {
        stubContexts();
        when(documentGenerator.generate(CLAIMANT_PAYLOAD, "claimant notice")).thenReturn("claimant url");
        when(documentGenerator.generate(DEFENDANT_PAYLOAD, "defendant notice")).thenReturn("defendant url");

        underTest.generateAndAttach(HEARING_ID);

        InOrder order = inOrder(persistenceService, documentGenerator);
        order.verify(persistenceService).buildNoticeContexts(HEARING_ID);
        order.verify(documentGenerator).generate(CLAIMANT_PAYLOAD, "claimant notice");
        order.verify(documentGenerator).generate(DEFENDANT_PAYLOAD, "defendant notice");
        order.verify(persistenceService)
            .attachNoticesAndQueueEmails(HEARING_ID,
                Map.of(CLAIMANT_ID, "claimant url", DEFENDANT_ID, "defendant url"));
    }

    @Test
    void shouldSkipGenerateAndAttachWhenThereAreNoNotices() {
        when(persistenceService.buildNoticeContexts(HEARING_ID)).thenReturn(List.of());

        underTest.generateAndAttach(HEARING_ID);

        verifyNoInteractions(documentGenerator, documentImportService);
        verify(persistenceService, never()).attachNoticesAndQueueEmails(any(), anyMap());
    }

    @Test
    void shouldDeleteGeneratedNoticesAndNotAttachWhenGenerationFails() {
        stubContexts();
        when(documentGenerator.generate(CLAIMANT_PAYLOAD, "claimant notice")).thenReturn("claimant url");
        when(documentGenerator.generate(DEFENDANT_PAYLOAD, "defendant notice"))
            .thenThrow(new RuntimeException("docassembly 500"));

        assertThatThrownBy(() -> underTest.generateAndAttach(HEARING_ID)).isInstanceOf(RuntimeException.class);

        verify(persistenceService, never()).attachNoticesAndQueueEmails(any(), anyMap());
        verify(documentImportService).deleteDocument("claimant url");
    }

    @Test
    void shouldDeleteGeneratedNoticesWhenAttachFails() {
        stubContexts();
        when(documentGenerator.generate(CLAIMANT_PAYLOAD, "claimant notice")).thenReturn("claimant url");
        when(documentGenerator.generate(DEFENDANT_PAYLOAD, "defendant notice")).thenReturn("defendant url");
        doThrow(new RuntimeException("attach failed"))
            .when(persistenceService).attachNoticesAndQueueEmails(any(), anyMap());

        assertThatThrownBy(() -> underTest.generateAndAttach(HEARING_ID)).isInstanceOf(RuntimeException.class);

        verify(documentImportService).deleteDocument("claimant url");
        verify(documentImportService).deleteDocument("defendant url");
    }

    private void stubContexts() {
        when(persistenceService.buildNoticeContexts(HEARING_ID)).thenReturn(List.of(
            new HearingNoticeRenderContext(CLAIMANT_ID, CLAIMANT_PAYLOAD, "claimant notice"),
            new HearingNoticeRenderContext(DEFENDANT_ID, DEFENDANT_PAYLOAD, "defendant notice")));
    }
}
