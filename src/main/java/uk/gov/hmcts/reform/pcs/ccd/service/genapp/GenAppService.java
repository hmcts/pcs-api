package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.UploadedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterGenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.HelpWithFeesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentNameService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.exception.GenAppException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter.toYesOrNo;

@Service
public class GenAppService {

    private static final String GENERAL_APPLICATION_FILENAME = "General Application";

    private final GenAppRepository genAppRepository;
    private final DocumentService documentService;
    private final DocumentNameService documentNameService;
    private final DocumentRepository documentRepository;
    private final Clock utcClock;

    public GenAppService(GenAppRepository genAppRepository,
                         DocumentService documentService,
                         DocumentNameService documentNameService,
                         DocumentRepository documentRepository,
                         @Qualifier("utcClock") Clock utcClock) {

        this.genAppRepository = genAppRepository;
        this.documentService = documentService;
        this.documentNameService = documentNameService;
        this.documentRepository = documentRepository;
        this.utcClock = utcClock;
    }

    public GenAppEntity createGenAppEntity(GenAppRequest genAppRequest,
                                           PcsCaseEntity pcsCaseEntity,
                                           PartyEntity applicantParty,
                                           GenAppState initialState) {

        GenAppEntity genAppEntity = GenAppEntity.builder()
            .type(genAppRequest.getApplicationType())
            .party(applicantParty)
            .state(initialState)
            .clientReference(genAppRequest.getClientReference())
            .within14Days(genAppRequest.getWithin14Days())
            .needHwf(genAppRequest.getNeedHwf())
            .build();

        // Adding the Gen App to the PcsCaseEntity allocates it a rank,
        // which we rely on later on in this method to rename the supporting documents
        pcsCaseEntity.addGenApp(genAppEntity);

        if (genAppRequest.getNeedHwf() == VerticalYesNo.YES) {
            genAppEntity.setAppliedForHwf(genAppRequest.getAppliedForHwf());

            if (genAppRequest.getAppliedForHwf() == VerticalYesNo.YES
                && genAppRequest.getHwfReference() != null) {

                HelpWithFeesEntity helpWithFeesEntity = new HelpWithFeesEntity();
                helpWithFeesEntity.setHwfReference(genAppRequest.getHwfReference());
                genAppEntity.setHelpWithFeesEntity(helpWithFeesEntity);
            }
        }

        genAppEntity.setOtherPartiesAgreed(genAppRequest.getOtherPartiesAgreed());
        if (genAppRequest.getOtherPartiesAgreed() == VerticalYesNo.NO) {
            genAppEntity.setWithoutNotice(genAppRequest.getWithoutNotice());
            if (genAppRequest.getWithoutNotice() == VerticalYesNo.YES) {
                genAppEntity.setWithoutNoticeReason(genAppRequest.getWithoutNoticeReason());
            }
        }

        genAppEntity.setWhatOrderWanted(genAppRequest.getWhatOrderWanted());

        genAppEntity.setDocumentsUploaded(genAppRequest.getHasSupportingDocuments());
        if (genAppRequest.getHasSupportingDocuments() == VerticalYesNo.YES) {
            List<DocumentEntity> documentEntities
                = createDocumentEntities(genAppRequest.getUploadedDocuments(),
                                         pcsCaseEntity,
                                         genAppEntity,
                                         applicantParty.getId());

            genAppEntity.setDocuments(documentEntities);
        }

        genAppEntity.setLanguageUsed(genAppRequest.getLanguageUsed());
        genAppEntity.setApplicationSubmittedDate(LocalDateTime.now(utcClock));

        if (genAppRequest.getSotAccepted() != VerticalYesNo.YES) {
            throw new GenAppException("Statement of truth must be accepted to create a gen app");
        }

        StatementOfTruthEntity statementOfTruthEntity = StatementOfTruthEntity.builder()
            .accepted(toYesOrNo(genAppRequest.getSotAccepted()))
            .fullName(genAppRequest.getSotFullName())
            .firmName(genAppRequest.getSotFirmName())
            .positionHeld(genAppRequest.getSotPositionHeld())
            .completedDate(LocalDateTime.now(utcClock))
            .build();
        genAppEntity.setStatementOfTruth(statementOfTruthEntity);

        return genAppRepository.save(genAppEntity);
    }

    public void createGenAppEntity(PCSCase caseData,
                                   PcsCaseEntity pcsCaseEntity,
                                   PartyEntity applicantParty,
                                   GenAppState initialState) {

        EnterGenAppRequest enterGenAppRequest = caseData.getEnterGenAppRequest();

        GenAppEntity genAppEntity = GenAppEntity.builder()
            .type(enterGenAppRequest.getApplicationTypeOption().getStandardGenAppType())
            .party(applicantParty)
            .applicationReceivedDate(enterGenAppRequest.getDateReceived())
            .applicationSubmittedDate(LocalDateTime.now(utcClock))
            .within14Days(enterGenAppRequest.getWithin14Days())
            .state(initialState)
            .feeAmountReceived(enterGenAppRequest.getFeeAmountReceived())
            .appliedForHwf(enterGenAppRequest.getAppliedForHwf())
            .build();

        if (enterGenAppRequest.getApplicationTypeOption() == EnterGenAppType.SOMETHING_ELSE) {
            genAppEntity.setSomethingElseDetails(enterGenAppRequest.getSomethingElseDetails());
        }

        // Adding the Gen App to the PcsCaseEntity allocates it a rank,
        // which we rely on later on in this method to rename the supporting documents
        pcsCaseEntity.addGenApp(genAppEntity);

        if (enterGenAppRequest.getAppliedForHwf() == VerticalYesNo.YES) {
            HelpWithFeesEntity helpWithFeesEntity = new HelpWithFeesEntity();
            helpWithFeesEntity.setHwfReference(enterGenAppRequest.getHwfReference());
            genAppEntity.setHelpWithFeesEntity(helpWithFeesEntity);
        }

        Document uploadedGenApp = caseData.getUploadSingleDocument();
        DocumentEntity submissionDocument = createDocumentEntity(uploadedGenApp, pcsCaseEntity,
                                                                 genAppEntity, applicantParty.getId());
        genAppEntity.setSubmissionDocument(submissionDocument);

        List<DocumentEntity> additionalEvidenceDocuments = createRelatedEvidenceDocumentEntities(
            enterGenAppRequest.getRelatedEvidence(), pcsCaseEntity, genAppEntity, applicantParty.getId());
        genAppEntity.setDocuments(additionalEvidenceDocuments);

        genAppRepository.save(genAppEntity);
    }

    private DocumentEntity createDocumentEntity(Document document,
                                                PcsCaseEntity pcsCaseEntity,
                                                GenAppEntity genAppEntity,
                                                UUID applicantPartyId) {

        if (document == null) {
            return null;
        }

        ClaimEntity mainClaimEntity = pcsCaseEntity.getClaims().getFirst();
        String extension = FilenameUtils.getExtension(document.getFilename());
        String newFileName = GENERAL_APPLICATION_FILENAME + "." + extension;
        String renamedFilename = documentNameService
            .appendGenAppPostfix(newFileName, genAppEntity, mainClaimEntity, applicantPartyId);

        DocumentEntity documentEntity = DocumentEntity.builder()
            .pcsCase(pcsCaseEntity)
            .generalApplication(genAppEntity)
            .url(document.getUrl())
            .fileName(renamedFilename)
            .binaryUrl(document.getBinaryUrl())
            .categoryId(CaseFileCategory.APPLICATIONS.getId())
            .type(DocumentType.GENERAL_APPLICATION)
            .build();

        return documentRepository.save(documentEntity);
    }

    private List<DocumentEntity> createRelatedEvidenceDocumentEntities(
        List<ListValue<Document>> relatedEvidenceDocuments, PcsCaseEntity pcsCaseEntity, GenAppEntity genAppEntity,
        UUID applicantPartyId) {

        if (relatedEvidenceDocuments == null) {
            return List.of();
        }

        ClaimEntity mainClaimEntity = pcsCaseEntity.getClaims().getFirst();

        List<DocumentEntity> documentEntities = relatedEvidenceDocuments.stream()
            .map(ListValue::getValue)
            .map(document -> {
                String renamedFilename = documentNameService
                    .appendGenAppPostfix(document.getFilename(), genAppEntity, mainClaimEntity, applicantPartyId);

                return DocumentEntity.builder()
                    .pcsCase(pcsCaseEntity)
                    .generalApplication(genAppEntity)
                    .url(document.getUrl())
                    .fileName(renamedFilename)
                    .binaryUrl(document.getBinaryUrl())
                    .type(DocumentType.OTHER)
                    .build();
            })
            .toList();

        return documentRepository.saveAll(documentEntities);
    }

    private List<DocumentEntity> createDocumentEntities(List<ListValue<UploadedDocument>> uploadedDocuments,
                                                        PcsCaseEntity pcsCaseEntity,
                                                        GenAppEntity genAppEntity,
                                                        UUID applicantPartyId) {

        if (uploadedDocuments == null) {
            return List.of();
        }

        ClaimEntity mainClaimEntity = pcsCaseEntity.getClaims().getFirst();

        List<DocumentEntity> documentEntities = uploadedDocuments.stream()
            .map(ListValue::getValue)
            .map(uploadedDocument -> {
                String originalFilename = uploadedDocument.getDocument().getFilename();
                String updatedFilename = documentNameService
                    .appendGenAppPostfix(originalFilename, genAppEntity, mainClaimEntity, applicantPartyId);

                return DocumentEntity.builder()
                    .pcsCase(pcsCaseEntity)
                    .generalApplication(genAppEntity)
                    .url(uploadedDocument.getDocument().getUrl())
                    .fileName(updatedFilename)
                    .binaryUrl(uploadedDocument.getDocument().getBinaryUrl())
                    .categoryId(CaseFileCategory.APPLICATIONS.getId())
                    .type(uploadedDocument.getDocumentType() != null
                        ? documentService.mapAdditionalDocumentTypeToDocumentType(uploadedDocument.getDocumentType())
                        : null)
                    .contentType(uploadedDocument.getContentType())
                    .size(uploadedDocument.getSizeInBytes())
                    .build();
            })
            .toList();

        return documentRepository.saveAll(documentEntities);
    }

}
