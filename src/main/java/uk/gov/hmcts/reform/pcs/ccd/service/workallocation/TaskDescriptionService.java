package uk.gov.hmcts.reform.pcs.ccd.service.workallocation;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormDocumentGenerator.OUTPUT_FILENAME_PREFIX;

@Service
@RequiredArgsConstructor
public class TaskDescriptionService {

    private final PartyService partyService;
    private final PebbleEngine pebbleEngine;
    private final ClaimRepository claimRepository;

    public String createReviewAdjournGenAppDescription(long caseReference,
                                                       GenAppEntity genAppEntity) {

        String partyLabel = getPartyLabel(genAppEntity.getParty(), caseReference);

        List<String> additionalDocumentFilenames = genAppEntity.getDocuments().stream()
            .map(DocumentEntity::getFileName)
            .toList();

        List<String> filenames = new ArrayList<>();
        filenames.add(genAppEntity.getSubmissionDocument().getFileName());
        filenames.addAll(additionalDocumentFilenames);

        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "partyLabel", partyLabel,
            "filenames", filenames
        );

        String templateName = "review-adjourn-gen-app";
        return renderTemplate(templateName, context);
    }

    public String createGenAppAdditionalDocumentsDescription(long caseReference,
                                                             ClaimEntity mainClaim,
                                                             PartyEntity partyEntity,
                                                             GenAppEntity genAppEntity,
                                                             List<DocumentEntity> documentEntities) {

        String partyLabel = partyService.getPartyLabel(mainClaim, partyEntity.getId());

        List<String> filenames = extractFilenames(documentEntities);

        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "partyLabel", partyLabel,
            "genAppRank", genAppEntity.getRank(),
            "filenames", filenames
        );

        String templateName = "gen-app-review-additional-docs";
        return renderTemplate(templateName, context);
    }

    public String createReviewResponseAndCounterClaimDescription(long caseReference,
                                                                 ClaimEntity mainClaim,
                                                                 DefendantResponseEntity defendantResponseEntity,
                                                                 List<DocumentEntity> counterClaimDocumentEntities) {

        String partyLabel = partyService.getPartyLabel(mainClaim, defendantResponseEntity.getParty().getId());

        String responseSubmissionFilename = OUTPUT_FILENAME_PREFIX + " - " + partyLabel + ".pdf";
        List<String> responseDocumentFilenames = extractFilenames(defendantResponseEntity.getUploadedDocuments());
        List<String> counterClaimDocumentFilenames = extractFilenames(counterClaimDocumentEntities);

        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "partyLabel", partyLabel,
            "responseSubmissionFilename", responseSubmissionFilename,
            "responseDocumentFilenames", responseDocumentFilenames,
            "counterClaimDocumentFilenames", counterClaimDocumentFilenames
        );

        String templateName = "review-response-and-counterclaim";
        return renderTemplate(templateName, context);
    }

    private String renderTemplate(String templateName, Map<String, Object> context) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("workallocation/" + templateName);
        Writer writer = new StringWriter();

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render template", e);
        }

        return writer.toString();
    }

    private String getPartyLabel(PartyEntity partyEntity, long caseReference) {
        ClaimEntity mainClaim = claimRepository.findClaimByCaseReference(caseReference)
                .orElseThrow(() -> new ClaimNotFoundException(caseReference));

        return partyService.getPartyLabel(mainClaim, partyEntity.getId());
    }

    private static List<String> extractFilenames(List<DocumentEntity> documentEntities) {
        return documentEntities.stream()
            .map(DocumentEntity::getFileName)
            .toList();
    }

}
