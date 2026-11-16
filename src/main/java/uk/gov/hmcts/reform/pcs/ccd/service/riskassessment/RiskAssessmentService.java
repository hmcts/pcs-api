package uk.gov.hmcts.reform.pcs.ccd.service.riskassessment;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.riskassessment.RiskAssessmentPayload;

import java.util.Optional;
import java.util.UUID;

/**
 * Generates the eviction risk assessment ({@code CV-PCS-APP-ENG-Risk-Assessment.docx}) for a submitted
 * warrant enforcement order that has a risk profile, and attaches it to the case. One per enforcement order.
 */
@Slf4j
@Service
@AllArgsConstructor
public class RiskAssessmentService {

    static final String OUTPUT_FILENAME = "Risk assessment";

    private final RiskAssessmentPersistenceService persistenceService;
    private final RiskAssessmentDocumentGenerator documentGenerator;
    private final DocumentImportService documentImportService;

    public void generateAndAttach(UUID enforcementOrderId) {
        Optional<RiskAssessmentPayload> payload = persistenceService.buildPayload(enforcementOrderId);
        if (payload.isEmpty()) {
            return;
        }

        String documentUrl = documentGenerator.generate(payload.get(), OUTPUT_FILENAME);
        try {
            persistenceService.attachDocument(enforcementOrderId, documentUrl);
        } catch (Exception e) {
            deleteOrphanedDocument(enforcementOrderId, documentUrl);
            throw e;
        }
    }

    private void deleteOrphanedDocument(UUID enforcementOrderId, String documentUrl) {
        try {
            documentImportService.deleteDocument(documentUrl);
        } catch (Exception e) {
            log.error("Failed to delete orphaned risk assessment for enforcement order {}: {}",
                enforcementOrderId, documentUrl, e);
        }
    }
}
