package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.UploadedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.HelpWithFeesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter.toYesOrNo;

@Service
public class GenAppService {

    private final GenAppRepository genAppRepository;
    private final DocumentRepository documentRepository;
    private final Clock utcClock;

    public GenAppService(GenAppRepository genAppRepository,
                         DocumentRepository documentRepository,
                         @Qualifier("utcClock") Clock utcClock) {
        this.genAppRepository = genAppRepository;
        this.documentRepository = documentRepository;
        this.utcClock = utcClock;
    }

    public GenAppEntity createGenAppEntity(GenAppRequest citizenCreateGenApp,
                                           PcsCaseEntity pcsCaseEntity,
                                           PartyEntity applicantParty) {

        GenAppEntity genAppEntity = GenAppEntity.builder()
            .type(citizenCreateGenApp.getApplicationType())
            .party(applicantParty)
            .state(GenAppState.SUBMITTED)
            .clientReference(citizenCreateGenApp.getClientReference())
            .within14Days(citizenCreateGenApp.getWithin14Days())
            .needHwf(citizenCreateGenApp.getNeedHwf())
            .appliedForHwf(citizenCreateGenApp.getAppliedForHwf())
            .build();

        if (citizenCreateGenApp.getAppliedForHwf() == VerticalYesNo.YES
                && citizenCreateGenApp.getHwfReference() != null) {
            HelpWithFeesEntity helpWithFeesEntity = new HelpWithFeesEntity();
            helpWithFeesEntity.setHwfReference(citizenCreateGenApp.getHwfReference());
            genAppEntity.setHelpWithFeesEntity(helpWithFeesEntity);
        }

        genAppEntity.setOtherPartiesAgreed(citizenCreateGenApp.getOtherPartiesAgreed());
        if (citizenCreateGenApp.getOtherPartiesAgreed() == VerticalYesNo.NO) {
            genAppEntity.setWithoutNotice(citizenCreateGenApp.getWithoutNotice());
            if (citizenCreateGenApp.getWithoutNotice() == VerticalYesNo.YES) {
                genAppEntity.setWithoutNoticeReason(citizenCreateGenApp.getWithoutNoticeReason());
            }
        }

        genAppEntity.setWhatOrderWanted(citizenCreateGenApp.getWhatOrderWanted());

        genAppEntity.setDocumentsUploaded(citizenCreateGenApp.getHasSupportingDocuments());
        if (citizenCreateGenApp.getHasSupportingDocuments() == VerticalYesNo.YES) {
            List<DocumentEntity> documentEntities
                = createDocumentEntities(citizenCreateGenApp.getUploadedDocuments(), pcsCaseEntity, genAppEntity);

            genAppEntity.setDocuments(documentEntities);
        }

        genAppEntity.setLanguageUsed(citizenCreateGenApp.getLanguageUsed());
        genAppEntity.setApplicationSubmittedDate(LocalDateTime.now(utcClock));

        if (citizenCreateGenApp.getSotAccepted() != null) {
            StatementOfTruthEntity statementOfTruthEntity = StatementOfTruthEntity.builder()
                .accepted(toYesOrNo(citizenCreateGenApp.getSotAccepted()))
                .fullName(citizenCreateGenApp.getSotFullName())
                .completedDate(LocalDateTime.now(utcClock))
                .build();
            genAppEntity.setStatementOfTruth(statementOfTruthEntity);
        }

        pcsCaseEntity.addGenApp(genAppEntity);

        return genAppRepository.save(genAppEntity);
    }

    public List<DocumentEntity> createDocumentEntities(List<ListValue<UploadedDocument>> uploadedDocuments,
                                                       PcsCaseEntity pcsCaseEntity,
                                                       GenAppEntity genAppEntity) {

        if (uploadedDocuments == null) {
            return List.of();
        }

        List<DocumentEntity> documentEntities = uploadedDocuments.stream()
            .map(ListValue::getValue)
            .map(defDoc -> DocumentEntity.builder()
                .pcsCase(pcsCaseEntity)
                .generalApplication(genAppEntity)
                .url(defDoc.getDocument().getUrl())
                .fileName(defDoc.getDocument().getFilename())
                .binaryUrl(defDoc.getDocument().getBinaryUrl())
                .categoryId(null)
                .contentType(defDoc.getContentType())
                .size(defDoc.getSizeInBytes())
                .build())
            .toList();

        return documentRepository.saveAll(documentEntities);
    }

}
