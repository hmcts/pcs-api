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
import java.util.UUID;

/**
 * Builds the payload, renders the defence form PDF via Docmosis, stores it, and attaches it to the
 * case under "Statements of case".
 *
 * <p>Runs in a transaction so the payload builder can read lazy JPA relations on the defendant
 * response. Skips generation when the response already has a submission document, so a re-run never
 * creates a second one.</p>
 */
@Service
@Slf4j
public class DefenceFormService {

    private final DefendantResponseRepository defendantResponseRepository;
    private final DefenceFormPayloadBuilder payloadBuilder;
    private final DefenceFormDocumentGenerator documentGenerator;
    private final DocumentImportService documentImportService;
    private final ClaimActivityLogService claimActivityLogService;

    public DefenceFormService(DefendantResponseRepository defendantResponseRepository,
                              DefenceFormPayloadBuilder payloadBuilder,
                              DefenceFormDocumentGenerator documentGenerator,
                              DocumentImportService documentImportService,
                              ClaimActivityLogService claimActivityLogService) {
        this.defendantResponseRepository = defendantResponseRepository;
        this.payloadBuilder = payloadBuilder;
        this.documentGenerator = documentGenerator;
        this.documentImportService = documentImportService;
        this.claimActivityLogService = claimActivityLogService;
    }

    @Transactional
    public void generateAndAttach(UUID defendantResponseId) {
        DefendantResponseEntity response = defendantResponseRepository.findById(defendantResponseId)
            .orElseThrow(() -> new IllegalStateException(
                "No defendant response found for id: " + defendantResponseId));

        if (response.getSubmissionDocument() != null) {
            log.info("Defence form already attached for defendant response {}, skipping", defendantResponseId);
            return;
        }

        PcsCaseEntity pcsCase = response.getPcsCase();
        DefenceFormPayload payload = payloadBuilder.build(response);
        String dmStoreUrl = documentGenerator.generate(payload, defendantNumber(response));
        DocumentEntity document = documentImportService.addDocumentToCase(
            pcsCase, dmStoreUrl, CaseFileCategory.STATEMENTS_OF_CASE);
        document.setType(DocumentType.DEFENDANT_RESPONSE);
        response.setSubmissionDocument(document);
        // Success log shares this transaction, so the form and its activity-log row commit together.
        claimActivityLogService.logGenerationSuccess(pcsCase, response.getParty());
        log.info("Generated and attached defence form for defendant response {}: {}",
                 defendantResponseId, dmStoreUrl);
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
