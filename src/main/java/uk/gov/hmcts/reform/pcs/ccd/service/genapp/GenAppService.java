package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.UploadedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
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

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter.toYesOrNo;

@Service
public class GenAppService {

    private final GenAppRepository genAppRepository;
    private final DocumentNameService documentNameService;
    private final DocumentRepository documentRepository;
    private final Clock utcClock;

    public GenAppService(GenAppRepository genAppRepository,
                         DocumentNameService documentNameService,
                         DocumentRepository documentRepository,
                         @Qualifier("utcClock") Clock utcClock) {

        this.genAppRepository = genAppRepository;
        this.documentNameService = documentNameService;
        this.documentRepository = documentRepository;
        this.utcClock = utcClock;
    }

    public GenAppEntity createGenAppEntity(GenAppRequest genAppRequest,
                                           PcsCaseEntity pcsCaseEntity,
                                           PartyEntity applicantParty) {

        GenAppEntity genAppEntity = GenAppEntity.builder()
            .type(genAppRequest.getApplicationType())
            .party(applicantParty)
            .state(GenAppState.SUBMITTED)
            .clientReference(genAppRequest.getClientReference())
            .within14Days(genAppRequest.getWithin14Days())
            .needHwf(genAppRequest.getNeedHwf())
            .appliedForHwf(genAppRequest.getAppliedForHwf())
            .build();

        // Adding the Gen App to the PcsCaseEntity allocates it a rank,
        // which we rely on later on in this method to rename the supporting documents
        pcsCaseEntity.addGenApp(genAppEntity);

        if (genAppRequest.getAppliedForHwf() == VerticalYesNo.YES
                && genAppRequest.getHwfReference() != null) {
            HelpWithFeesEntity helpWithFeesEntity = new HelpWithFeesEntity();
            helpWithFeesEntity.setHwfReference(genAppRequest.getHwfReference());
            genAppEntity.setHelpWithFeesEntity(helpWithFeesEntity);
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

        if (genAppRequest.getSotAccepted() != null) {
            StatementOfTruthEntity statementOfTruthEntity = StatementOfTruthEntity.builder()
                .accepted(toYesOrNo(genAppRequest.getSotAccepted()))
                .fullName(genAppRequest.getSotFullName())
                .completedDate(LocalDateTime.now(utcClock))
                .build();
            genAppEntity.setStatementOfTruth(statementOfTruthEntity);
        }

        return genAppRepository.save(genAppEntity);
    }

    public List<DocumentEntity> createDocumentEntities(List<ListValue<UploadedDocument>> uploadedDocuments,
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
                    .contentType(uploadedDocument.getContentType())
                    .size(uploadedDocument.getSizeInBytes())
                    .build();
            })
            .toList();

        return documentRepository.saveAll(documentEntities);
    }

}
