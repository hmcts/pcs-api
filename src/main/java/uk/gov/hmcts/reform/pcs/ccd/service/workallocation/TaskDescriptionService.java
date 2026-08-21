package uk.gov.hmcts.reform.pcs.ccd.service.workallocation;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.CLAIM_NOT_FOUND;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.TEMPLATE_RENDERING;

@Service
@RequiredArgsConstructor
public class TaskDescriptionService {

    private final PartyService partyService;
    private final PebbleEngine pebbleEngine;
    private final ClaimRepository claimRepository;

    public String createReviewGenAppDescription(long caseReference,
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

        String templateName = switch (genAppEntity.getType()) {
            case ADJOURN -> "review-adjourn-gen-app";
            case SET_ASIDE -> "review-set-aside-gen-app";
            case SOMETHING_ELSE -> "review-gen-app";
        };

        return renderTemplate(templateName, context);
    }

    public String createGenAppAdditionalDocumentsDescription(long caseReference,
                                                             ClaimEntity mainClaim,
                                                             PartyEntity partyEntity,
                                                             GenAppEntity genAppEntity,
                                                             List<DocumentEntity> documentEntities) {

        Map<String, Object> customFields = Map.of("genAppRank", genAppEntity.getRank());

        String templateName = "gen-app-review-additional-docs";
        return createDocumentDescription(caseReference, mainClaim, partyEntity,
                                         documentEntities, templateName, customFields);
    }

    public String createClaimAdditionalDocumentsDescription(long caseReference,
                                                            ClaimEntity mainClaim,
                                                            PartyEntity partyEntity,
                                                            List<DocumentEntity> documentEntities) {



        String templateName = "claim-review-additional-docs";
        return createDocumentDescription(caseReference, mainClaim, partyEntity, documentEntities, templateName, null);
    }

    public String createTranslateClaimantDocumentDescription(long caseReference,
                                                              List<DocumentEntity> documentEntities) {

        List<String> filenames = documentEntities.stream()
            .map(DocumentEntity::getFileName)
            .toList();

        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "filenames", filenames
        );

        String templateName = "translate-claimant-submitted-document";
        return renderTemplate(templateName, context);
    }

    public String createTranslateDefendantDocumentDescription(long caseReference,
                                                               ClaimEntity mainClaim,
                                                               PartyEntity partyEntity,
                                                               List<DocumentEntity> documentEntities) {

        String partyLabel = partyService.getPartyLabel(mainClaim, partyEntity.getId());

        List<String> filenames = documentEntities.stream()
            .map(DocumentEntity::getFileName)
            .toList();

        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "partyLabel", partyLabel,
            "filenames", filenames
        );

        String templateName = "translate-defendant-submitted-document";
        return renderTemplate(templateName, context);
    }

    private String createDocumentDescription(long caseReference,
                                             ClaimEntity mainClaim,
                                             PartyEntity partyEntity,
                                             List<DocumentEntity> documentEntities,
                                             String templateName,
                                             Map<String, Object> customFields) {

        String partyLabel = partyService.getPartyLabel(mainClaim, partyEntity.getId());

        List<String> filenames = extractFilenames(documentEntities);

        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "partyLabel", partyLabel,
            "filenames", filenames
        );

        if (!CollectionUtils.isEmpty(customFields)) {
            context = Stream.concat(context.entrySet().stream(), customFields.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        return renderTemplate(templateName, context);
    }

    public String createReviewCaseFlagDescription(long caseReference, List<String> flags) {
        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "flags", flags
        );

        String templateName = "review-case-flag";
        return renderTemplate(templateName, context);
    }

    public String createReviewResponseAndCounterClaimDescription(long caseReference,
                                                                 CounterClaimEntity counterClaimEntity,
                                                                 FeeDetails feeDetails) {

        String partyLabel = getPartyLabel(counterClaimEntity.getParty(), caseReference);

        String hwfReference = counterClaimEntity.getHwfReferenceNumber();
        BigDecimal feeAmount = feeDetails.getFeeAmount();

        Map<String, Object> context = Map.of(
            "partyLabel", partyLabel,
            "hwfReference", hwfReference,
            "feeAmount", feeAmount
        );

        String templateName = "review-response-and-counterclaim";
        return renderTemplate(templateName, context);
    }

    public String createReviewDueDateDescription(long caseReference) {
        Map<String, Object> context = Map.of(
            "caseReference", caseReference
        );

        String templateName = "review-due-date";
        return renderTemplate(templateName, context);
    }

    private String renderTemplate(String templateName, Map<String, Object> context) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("workallocation/" + templateName);
        Writer writer = new StringWriter();

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new TemplateRenderingException(TEMPLATE_RENDERING, e);
        }

        return writer.toString()
            .replace("(", "&#40;")
            .replace(")", "&#41;");
    }

    private String getPartyLabel(PartyEntity partyEntity, long caseReference) {
        ClaimEntity mainClaim = claimRepository.findClaimByCaseReference(caseReference)
                .orElseThrow(() -> new ClaimNotFoundException(CLAIM_NOT_FOUND,
                    RedactionContext.of("No claim found for case reference", caseReference)));

        return partyService.getPartyLabel(mainClaim, partyEntity.getId());
    }

    private static List<String> extractFilenames(List<DocumentEntity> documentEntities) {
        return documentEntities.stream()
            .map(DocumentEntity::getFileName)
            .toList();
    }

}
