package uk.gov.hmcts.reform.pcs.ccd.service.workallocation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentNameService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormDocumentGenerator.expectedClaimFormFilename;
import static uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform.CounterClaimFormDocumentGenerator.expectedCounterClaimFormFilename;
import static uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform.CounterClaimFormPersistenceService.defendantNumber;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormDocumentGenerator.expectedDefenceFormFilename;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormPersistenceService.defendantNumber;

@Service
@RequiredArgsConstructor
public class TranslationWAService {

    private final CamundaService camundaService;
    private final TaskDescriptionService taskDescriptionService;
    private final PartyService partyService;
    private final DocumentNameService documentNameService;

    public void createTranslateDefendantSubmittedDocumentTask(PcsCaseEntity pcsCaseEntity, PartyEntity party,
                                                      List<DocumentEntity> documents) {
        if (documents.isEmpty()) {
            return;
        }

        long caseReference = pcsCaseEntity.getCaseReference();
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();

        String description = taskDescriptionService.createTranslateDefendantDocumentDescription(
            caseReference, mainClaim, party, documents);

        camundaService.createTask(caseReference, TaskType.TRANSLATE_DEFENDANT_SUBMITTED_DOCUMENT, description);
    }

    public void createTranslateClaimantSubmittedDocumentTask(long caseReference, List<DocumentEntity> documents) {
        if (documents.isEmpty()) {
            return;
        }

        String description = taskDescriptionService.createTranslateClaimantDocumentDescription(
            caseReference, documents);

        camundaService.createTask(caseReference, TaskType.TRANSLATE_CLAIMANT_SUBMITTED_DOCUMENT, description);
    }

    public boolean isTranslationRequired(LanguageUsed languageUsed) {
        return languageUsed == LanguageUsed.WELSH || languageUsed == LanguageUsed.ENGLISH_AND_WELSH;
    }

    public void triggerTranslationTasksForFlaggingParty(PartyEntity flaggingParty) {
        triggerDefendantDocumentTranslationTask(flaggingParty);
        triggerClaimantDocumentTranslationTask(flaggingParty);
    }

    private void triggerClaimantDocumentTranslationTask(PartyEntity flaggingParty) {
        PcsCaseEntity pcsCaseEntity = flaggingParty.getPcsCase();
        long caseReference = pcsCaseEntity.getCaseReference();
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();

        for (PartyEntity party : pcsCaseEntity.getParties()) {
            boolean isOtherClaimant = !party.getId().equals(flaggingParty.getId())
                && partyService.getPartyRole(party) == PartyRole.CLAIMANT;

            if (isOtherClaimant) {
                // The claim form belongs to whichever claimant created the claim
                List<DocumentEntity> documents = new ArrayList<>();
                if (party.isClaimCreator()) {
                    documents.add(resolveGeneratedClaimForm());
                }
                documents.addAll(resolveGeneratedGenAppDocuments(pcsCaseEntity, party, mainClaim));

                documents.addAll(pcsCaseEntity.getDocuments().stream()
                    .filter(document -> !document.isRemoved()
                        && document.getClaim() != null
                        && document.getClaim().getId().equals(mainClaim.getId()))
                    .toList());

                createTranslateClaimantSubmittedDocumentTask(caseReference, documents);
            }
        }
    }

    private void triggerDefendantDocumentTranslationTask(PartyEntity flaggingParty) {
        PcsCaseEntity pcsCaseEntity = flaggingParty.getPcsCase();
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();

        for (PartyEntity party : pcsCaseEntity.getParties()) {
            boolean isOtherDefendant = !party.getId().equals(flaggingParty.getId())
                && partyService.getPartyRole(party) == PartyRole.DEFENDANT;

            if (isOtherDefendant) {
                List<DocumentEntity> documents = new ArrayList<>();
                resolveGeneratedDefenceForm(pcsCaseEntity, party).ifPresent(documents::add);
                resolveGeneratedCounterClaimForm(pcsCaseEntity, party).ifPresent(documents::add);
                documents.addAll(resolveGeneratedGenAppDocuments(pcsCaseEntity, party, mainClaim));

                documents.addAll(pcsCaseEntity.getDocuments().stream()
                    .filter(document -> !document.isRemoved())
                    .filter(document -> isDefendantDocument(document, party))
                    .toList());

                createTranslateDefendantSubmittedDocumentTask(pcsCaseEntity, party, documents);
            }
        }
    }

    private DocumentEntity resolveGeneratedClaimForm() {
        return DocumentEntity.builder().fileName(expectedClaimFormFilename()).build();
    }

    private Optional<DocumentEntity> resolveGeneratedDefenceForm(PcsCaseEntity pcsCaseEntity, PartyEntity party) {
        return pcsCaseEntity.getDefendantResponses().stream()
            .filter(response -> response.getParty() != null
                && response.getParty().getId().equals(party.getId()))
            .findFirst()
            .map(response -> DocumentEntity.builder()
                .fileName(expectedDefenceFormFilename(defendantNumber(response)))
                .build());
    }

    private Optional<DocumentEntity> resolveGeneratedCounterClaimForm(PcsCaseEntity pcsCaseEntity,
                                                                       PartyEntity party) {
        return pcsCaseEntity.getCounterClaims().stream()
            .filter(counterClaim -> counterClaim.getParty() != null
                && counterClaim.getParty().getId().equals(party.getId()))
            .findFirst()
            .map(counterClaim -> DocumentEntity.builder()
                .fileName(expectedCounterClaimFormFilename(defendantNumber(counterClaim)))
                .build());
    }

    private List<DocumentEntity> resolveGeneratedGenAppDocuments(PcsCaseEntity pcsCaseEntity, PartyEntity party,
                                                                   ClaimEntity mainClaim) {
        return pcsCaseEntity.getGenApps().stream()
            .filter(genApp -> genApp.getParty() != null && genApp.getParty().getId().equals(party.getId()))
            .map(genApp -> DocumentEntity.builder()
                .fileName(documentNameService.expectedGenAppFilename(genApp, mainClaim))
                .build())
            .toList();
    }

    private boolean isDefendantDocument(DocumentEntity document, PartyEntity partyEntity) {
        if (document.getType() == DocumentType.DEFENDANT_ACCESS_CODE
            || document.getType() == DocumentType.COUNTERCLAIM) {
            return false;
        }

        GenAppEntity generalApplication = document.getGeneralApplication();
        if (generalApplication != null && document.equals(generalApplication.getSubmissionDocument())) {
            return false;
        }

        PartyEntity documentParty = resolveOwningParty(document);
        return documentParty != null && documentParty.getId().equals(partyEntity.getId());
    }

    private PartyEntity resolveOwningParty(DocumentEntity document) {
        if (document.getCounterClaim() != null) {
            return document.getCounterClaim().getParty();
        }
        if (document.getGeneralApplication() != null) {
            return document.getGeneralApplication().getParty();
        }
        return document.getParty();
    }

}
