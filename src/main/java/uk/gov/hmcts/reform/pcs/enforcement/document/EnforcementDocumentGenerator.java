package uk.gov.hmcts.reform.pcs.enforcement.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import java.util.Optional;
import java.util.UUID;

/**
 * The Docmosis call must run outside any transaction, so this is only driven from the db-scheduler task.
 */
@Slf4j
@Service
public class EnforcementDocumentGenerator {

    private final EnforcementDocumentPersistenceService persistenceService;
    private final EnforcementDocumentTemplates templates;
    private final DocAssemblyService docAssemblyService;
    private final DocumentImportService documentImportService;

    public EnforcementDocumentGenerator(EnforcementDocumentPersistenceService persistenceService,
                                        EnforcementDocumentTemplates templates,
                                        DocAssemblyService docAssemblyService,
                                        DocumentImportService documentImportService) {
        this.persistenceService = persistenceService;
        this.templates = templates;
        this.docAssemblyService = docAssemblyService;
        this.documentImportService = documentImportService;
    }

    public Optional<DocumentEntity> generate(UUID enforcementId, EnforcementOutput output) {
        Optional<String> templateId = templates.templateId(output);
        if (templateId.isEmpty()) {
            log.warn("No Docmosis template is configured for the enforcement document \"{}\", so it was not "
                         + "generated for enforcement {}. Set enforcement.documents.templates.{}.",
                     output.getTitle(), enforcementId, output.getTemplateKey());
            return Optional.empty();
        }

        Optional<EnforcementDocumentPayload> payload =
            persistenceService.buildPayloadIfNotGenerated(enforcementId, output);
        if (payload.isEmpty()) {
            return Optional.empty();
        }

        String documentUrl = docAssemblyService.generateDocument(
            payload.get(), templateId.get(), OutputType.PDF,
            persistenceService.outputFilename(output, payload.get()));
        try {
            Optional<DocumentEntity> filed = persistenceService.attach(enforcementId, output, documentUrl);
            if (filed.isEmpty()) {
                // Another attempt filed the document first, so this render was never put on the case.
                deleteOrphanedDocument(enforcementId, documentUrl);
            }
            return filed;
        } catch (Exception e) {
            deleteOrphanedDocument(enforcementId, documentUrl);
            throw e;
        }
    }

    private void deleteOrphanedDocument(UUID enforcementId, String documentUrl) {
        try {
            documentImportService.deleteDocument(documentUrl);
        } catch (Exception e) {
            log.error("Failed to delete the orphaned enforcement document {} for enforcement {}", documentUrl,
                      enforcementId, e);
        }
    }
}
