package uk.gov.hmcts.reform.pcs.enforcement.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementDocumentGeneratorTest {

    private static final UUID ENFORCEMENT_ID = UUID.randomUUID();
    private static final String DOCUMENT_URL = "http://dm-store/documents/" + UUID.randomUUID();

    @Mock
    private EnforcementDocumentPersistenceService persistenceService;
    @Mock
    private DocAssemblyService docAssemblyService;
    @Mock
    private DocumentImportService documentImportService;

    private final EnforcementDocumentTemplates templates = new EnforcementDocumentTemplates();
    private EnforcementDocumentGenerator underTest;
    private EnforcementDocumentPayload payload;

    @BeforeEach
    void setUp() {
        templates.setTemplates(Map.of("warrant-of-possession", "CV-PCS-ENF-Warrant.docx"));
        underTest = new EnforcementDocumentGenerator(persistenceService, templates, docAssemblyService,
                                                     documentImportService);
        payload = EnforcementDocumentPayload.builder().documentTitle("Warrant of possession").build();
    }

    @Test
    void shouldRenderTheDocumentWithTheConfiguredTemplateAndFileIt() {
        DocumentEntity filed = DocumentEntity.builder().url(DOCUMENT_URL).build();
        when(persistenceService.buildPayloadIfNotGenerated(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION))
            .thenReturn(Optional.of(payload));
        when(persistenceService.outputFilename(EnforcementOutput.WARRANT_OF_POSSESSION, payload))
            .thenReturn("Warrant of possession - WP-00000042");
        when(docAssemblyService.generateDocument(payload, "CV-PCS-ENF-Warrant.docx", OutputType.PDF,
                                                 "Warrant of possession - WP-00000042")).thenReturn(DOCUMENT_URL);
        when(persistenceService.attach(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION, DOCUMENT_URL))
            .thenReturn(Optional.of(filed));

        assertThat(underTest.generate(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION)).containsSame(filed);
    }

    @Test
    void shouldSkipADocumentWithNoTemplateConfigured() {
        assertThat(underTest.generate(ENFORCEMENT_ID, EnforcementOutput.RISK_ASSESSMENT)).isEmpty();

        verifyNoInteractions(persistenceService, docAssemblyService, documentImportService);
    }

    @Test
    void shouldSkipADocumentTheEnforcementAlreadyHas() {
        when(persistenceService.buildPayloadIfNotGenerated(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION))
            .thenReturn(Optional.empty());

        assertThat(underTest.generate(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION)).isEmpty();

        verifyNoInteractions(docAssemblyService, documentImportService);
    }

    @Test
    void shouldLetADocmosisFailureThroughSoTheTaskRetries() {
        when(persistenceService.buildPayloadIfNotGenerated(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION))
            .thenReturn(Optional.of(payload));
        when(docAssemblyService.generateDocument(any(), anyString(), any(), any()))
            .thenThrow(new DocAssemblyException("Document generation failed"));

        assertThatThrownBy(() -> underTest.generate(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION))
            .isInstanceOf(DocAssemblyException.class);

        verify(persistenceService, never()).attach(any(), any(), anyString());
        verifyNoInteractions(documentImportService);
    }

    @Test
    void shouldDeleteTheRenderedDocumentWhenAnotherAttemptFiledItFirst() {
        when(persistenceService.buildPayloadIfNotGenerated(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION))
            .thenReturn(Optional.of(payload));
        when(docAssemblyService.generateDocument(any(), anyString(), any(), any())).thenReturn(DOCUMENT_URL);
        when(persistenceService.attach(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION, DOCUMENT_URL))
            .thenReturn(Optional.empty());

        assertThat(underTest.generate(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION)).isEmpty();

        verify(documentImportService).deleteDocument(DOCUMENT_URL);
    }

    @Test
    void shouldDeleteTheRenderedDocumentWhenFilingItFails() {
        when(persistenceService.buildPayloadIfNotGenerated(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION))
            .thenReturn(Optional.of(payload));
        when(docAssemblyService.generateDocument(any(), anyString(), any(), any())).thenReturn(DOCUMENT_URL);
        when(persistenceService.attach(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION, DOCUMENT_URL))
            .thenThrow(new IllegalStateException("No enforcement with id " + ENFORCEMENT_ID));

        assertThatThrownBy(() -> underTest.generate(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION))
            .isInstanceOf(IllegalStateException.class);

        verify(documentImportService).deleteDocument(DOCUMENT_URL);
    }
}
