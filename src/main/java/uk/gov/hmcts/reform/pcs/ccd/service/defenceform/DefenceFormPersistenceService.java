package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.defenceform.DefenceFormPayload;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Transactional reads and writes for defence form generation, separated from the orchestration in
 * {@link DefenceFormService} so the Docmosis render runs outside any transaction. The render context
 * is built in a read-only transaction (so the builder can read lazy JPA relations on the defendant
 * response), and the rendered document is attached in a short write transaction. Both phases re-check
 * idempotency so a re-run never attaches a second form.
 */
@Service
@Slf4j
public class DefenceFormPersistenceService {

    private final DefendantResponseRepository defendantResponseRepository;
    private final DefenceFormPayloadBuilder payloadBuilder;
    private final DocumentImportService documentImportService;
    private final ClaimActivityLogService claimActivityLogService;

    public DefenceFormPersistenceService(DefendantResponseRepository defendantResponseRepository,
                                         DefenceFormPayloadBuilder payloadBuilder,
                                         DocumentImportService documentImportService,
                                         ClaimActivityLogService claimActivityLogService) {
        this.defendantResponseRepository = defendantResponseRepository;
        this.payloadBuilder = payloadBuilder;
        this.documentImportService = documentImportService;
        this.claimActivityLogService = claimActivityLogService;
    }

    @Transactional(readOnly = true)
    public Optional<DefenceFormRenderContext> buildContextIfNotAttached(UUID defendantResponseId) {
        DefendantResponseEntity response = loadResponse(defendantResponseId);
        if (response.getSubmissionDocument() != null) {
            log.info("Defence form already attached for defendant response {}, skipping", defendantResponseId);
            return Optional.empty();
        }
        DefenceFormPayload payload = payloadBuilder.build(response);
        return Optional.of(new DefenceFormRenderContext(payload, defendantNumber(response)));
    }

    @Transactional
    public void attach(UUID defendantResponseId, String dmStoreUrl) {
        DefendantResponseEntity response = loadResponse(defendantResponseId);
        if (response.getSubmissionDocument() != null) {
            log.info("Defence form already attached for defendant response {}, skipping attach", defendantResponseId);
            return;
        }
        PcsCaseEntity pcsCase = response.getPcsCase();
        DocumentEntity document = documentImportService.addDocumentToCase(
            pcsCase, dmStoreUrl, CaseFileCategory.STATEMENTS_OF_CASE);
        document.setType(DocumentType.DEFENDANT_RESPONSE);
        response.setSubmissionDocument(document);
        claimActivityLogService.logGenerationSuccess(pcsCase, response.getParty());
    }

    private DefendantResponseEntity loadResponse(UUID defendantResponseId) {
        return defendantResponseRepository.findById(defendantResponseId)
            .orElseThrow(() -> new IllegalStateException(
                "No defendant response found for id: " + defendantResponseId));
    }

    private static int defendantNumber(DefendantResponseEntity response) {
        ClaimEntity claim = response.getClaim();
        UUID defendantPartyId = response.getParty().getId();
        return claim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .filter(claimParty -> claimParty.getParty().getId().equals(defendantPartyId))
            .map(ClaimPartyEntity::getRank)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(1);
    }
}
