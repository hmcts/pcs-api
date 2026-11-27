package uk.gov.hmcts.reform.pcs.ccd.service.warrantofpossession;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.EnforcementApplicationNotFoundException;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class WarrantOfPossessionPersistenceService {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final WarrantOfPossessionPayloadBuilder payloadBuilder;
    private final DocumentImportService documentImportService;
    private final ClaimActivityLogService claimActivityLogService;
    private final PartyService partyService;
    private final NotificationService notificationService;

    /**
     * Builds the data needed to render the warrant, or nothing when the order isn't a warrant or its warrant has
     * already been generated (so a retried or re-scheduled task doesn't attach a second copy).
     */
    @Transactional(readOnly = true)
    public Optional<WarrantOfPossessionRenderContext> buildRenderContext(UUID enforcementOrderId) {
        EnforcementOrderEntity enforcementOrder = loadEnforcementOrder(enforcementOrderId);
        if (enforcementOrder.getWarrantDetails() == null || hasWarrantDocument(enforcementOrder)) {
            return Optional.empty();
        }
        return Optional.of(new WarrantOfPossessionRenderContext(
            payloadBuilder.build(enforcementOrder), Objects.requireNonNullElse(enforcementOrder.getRank(), 1)));
    }

    @Transactional
    public void attachDocumentAndQueueEmail(UUID enforcementOrderId, String documentUrl) {
        EnforcementOrderEntity enforcementOrder = loadEnforcementOrder(enforcementOrderId);
        PcsCaseEntity pcsCase = enforcementOrder.getPcsCase();
        PartyEntity claimant = partyService.getPrimaryClaimantPartyEntity(pcsCase);

        DocumentEntity document = documentImportService.addDocumentToCase(
            pcsCase, documentUrl, CaseFileCategory.ENFORCEMENT);
        document.setType(DocumentType.WARRANT_OF_POSSESSION);
        enforcementOrder.addDocuments(List.of(document));

        claimActivityLogService.logGenerationSuccess(pcsCase, claimant);
        notificationService.sendWarrantOfPossessionEmail(claimant);
    }

    private boolean hasWarrantDocument(EnforcementOrderEntity enforcementOrder) {
        return enforcementOrder.getDocuments().stream()
            .anyMatch(document -> document.getType() == DocumentType.WARRANT_OF_POSSESSION);
    }

    private EnforcementOrderEntity loadEnforcementOrder(UUID enforcementOrderId) {
        return enforcementOrderRepository.findById(enforcementOrderId)
            .orElseThrow(() -> new EnforcementApplicationNotFoundException(
                "Unable to find Enforcement app with ID " + enforcementOrderId));
    }
}
