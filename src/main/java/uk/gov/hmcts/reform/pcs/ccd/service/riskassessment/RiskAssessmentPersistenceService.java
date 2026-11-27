package uk.gov.hmcts.reform.pcs.ccd.service.riskassessment;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.EnforcementApplicationNotFoundException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RiskAssessmentPersistenceService {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final RiskAssessmentPayloadBuilder payloadBuilder;
    private final DocumentImportService documentImportService;
    private final ClaimActivityLogService claimActivityLogService;
    private final PartyService partyService;

    /**
     * Builds the data needed to render the risk assessment, or nothing when the order isn't a warrant, has no risk
     * profile, or its risk assessment has already been generated (so a retried task doesn't attach a second copy).
     */
    @Transactional(readOnly = true)
    public Optional<RiskAssessmentRenderContext> buildRenderContext(UUID enforcementOrderId) {
        EnforcementOrderEntity enforcementOrder = loadEnforcementOrder(enforcementOrderId);
        if (enforcementOrder.getWarrantDetails() == null
            || enforcementOrder.getRiskProfileDetails() == null
            || hasRiskAssessmentDocument(enforcementOrder)) {
            return Optional.empty();
        }
        return Optional.of(new RiskAssessmentRenderContext(
            payloadBuilder.build(enforcementOrder), Objects.requireNonNullElse(enforcementOrder.getRank(), 1)));
    }

    @Transactional
    public void attachDocument(UUID enforcementOrderId, String documentUrl) {
        EnforcementOrderEntity enforcementOrder = loadEnforcementOrder(enforcementOrderId);
        PcsCaseEntity pcsCase = enforcementOrder.getPcsCase();

        DocumentEntity document = documentImportService.addDocumentToCase(
            pcsCase, documentUrl, CaseFileCategory.ENFORCEMENT);
        document.setType(DocumentType.RISK_ASSESSMENT);
        enforcementOrder.addDocuments(List.of(document));

        claimActivityLogService.logGenerationSuccess(pcsCase, partyService.getPrimaryClaimantPartyEntity(pcsCase));
    }

    private boolean hasRiskAssessmentDocument(EnforcementOrderEntity enforcementOrder) {
        return enforcementOrder.getDocuments().stream()
            .anyMatch(document -> document.getType() == DocumentType.RISK_ASSESSMENT);
    }

    private EnforcementOrderEntity loadEnforcementOrder(UUID enforcementOrderId) {
        return enforcementOrderRepository.findById(enforcementOrderId)
            .orElseThrow(() -> new EnforcementApplicationNotFoundException(
                "Unable to find Enforcement app with ID " + enforcementOrderId));
    }
}
