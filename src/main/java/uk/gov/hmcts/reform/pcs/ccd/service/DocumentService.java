package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.UploadedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.EvidenceDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.EvidenceOfDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public List<DocumentEntity> createAllDocuments(PCSCase pcsCase) {

        List<DocumentHolder> allDocuments = getPcsCaseDocuments(pcsCase);

        return documentRepository.saveAll(createDocumentEntities(allDocuments));
    }

    public List<DocumentEntity> createAllDocuments(EnforcementOrder enforcementOrder) {

        List<DocumentHolder> allDocuments = getWarrantOfRestitutionDocuments(enforcementOrder);

        return documentRepository.saveAll(createDocumentEntities(allDocuments));
    }

    private List<DocumentHolder> getPcsCaseDocuments(PCSCase pcsCase) {
        List<DocumentHolder> allDocuments = new ArrayList<>();

        allDocuments.addAll(mapAdditionalDocumentsWithType(pcsCase.getAdditionalDocuments()));

        allDocuments.addAll(mapDocumentsWithType(
            Optional.ofNullable(pcsCase.getRentArrears())
                    .map(RentArrearsSection::getStatementDocuments)
                    .orElse(null), DocumentType.RENT_STATEMENT));

        allDocuments.addAll(mapDocumentsWithType(
            Optional.ofNullable(pcsCase.getTenancyLicenceDetails())
                    .map(TenancyLicenceDetails::getTenancyLicenceDocuments)
                    .orElse(null), DocumentType.TENANCY_LICENCE));

        allDocuments.addAll(mapDocumentsWithType(
            Optional.ofNullable(pcsCase.getOccupationLicenceDetailsWales())
                    .map(OccupationLicenceDetailsWales::getLicenceDocuments)
                    .orElse(null), DocumentType.OCCUPATION_LICENCE));

        allDocuments.addAll(mapDocumentsWithType(
            Optional.ofNullable(pcsCase.getNoticeServedDetails())
                    .map(NoticeServedDetails::getNoticeDocuments)
                    .orElse(null), DocumentType.NOTICE_SERVED));

        return allDocuments;
    }

    private List<DocumentHolder> getWarrantOfRestitutionDocuments(EnforcementOrder enforcementOrder) {

        return new ArrayList<>(mapEvidenceOfDefendantsDocumentsWithType(
                enforcementOrder.getWarrantOfRestitutionDetails().getAdditionalDocuments()));
    }

    private List<DocumentHolder> mapDocumentsWithType(
            List<ListValue<Document>> docs, DocumentType type) {

        if (CollectionUtils.isEmpty(docs)) {
            return Collections.emptyList();
        }

        return docs.stream()
                .map(ListValue::getValue)
                .filter(Objects::nonNull)
                .map(doc -> DocumentHolder.builder()
                        .document(doc)
                        .type(type)
                        .description("")
                        .build())
                .toList();
    }

    private List<DocumentHolder> mapAdditionalDocumentsWithType(
            List<ListValue<AdditionalDocument>> documents) {

        if (CollectionUtils.isEmpty(documents)) {
            return Collections.emptyList();
        }

        return ListValueUtils.unwrapListItems(documents).stream()
            .map(doc -> DocumentHolder.builder()
                .document(doc.getDocument())
                .type(mapAdditionalDocumentTypeToDocumentType(doc.getDocumentType()))
                .description(doc.getDescription())
                .build())
            .toList();
    }

    private List<DocumentHolder> mapEvidenceOfDefendantsDocumentsWithType(
            List<ListValue<EvidenceOfDefendants>> documents) {

        if (CollectionUtils.isEmpty(documents)) {
            return Collections.emptyList();
        }

        return ListValueUtils.unwrapListItems(documents).stream()
            .map(doc -> DocumentHolder.builder()
                .document(doc.getDocument())
                .type(mapEvidenceDocumentTypeToDocumentType(doc.getDocumentType()))
                .description(doc.getDescription())
                .build())
            .toList();
    }

    private List<DocumentEntity> createDocumentEntities(
            List<DocumentHolder> documents) {

        if (CollectionUtils.isEmpty(documents)) {
            return List.of();
        }

        return documents.stream()
                .map(holder -> DocumentEntity.builder()
                        .url(holder.getDocument().getUrl())
                        .fileName(holder.getDocument().getFilename())
                        .displayFileName(holder.getDocument().getFilename())
                        .binaryUrl(holder.getDocument().getBinaryUrl())
                        .categoryId(holder.getDocument().getCategoryId())
                        .type(holder.getType())
                        .description(StringUtils.isEmpty(holder.getDescription()) ? null : holder.getDescription())
                        .build())
                .toList();
    }

    private DocumentType mapAdditionalDocumentTypeToDocumentType(AdditionalDocumentType additionalType) {
        return switch (additionalType) {
            case WITNESS_STATEMENT -> DocumentType.WITNESS_STATEMENT;
            case RENT_STATEMENT -> DocumentType.RENT_STATEMENT;
            case TENANCY_AGREEMENT -> DocumentType.TENANCY_AGREEMENT;
            case CERTIFICATE_OF_SERVICE -> DocumentType.CERTIFICATE_OF_SERVICE;
            case CORRESPONDENCE_FROM_DEFENDANT -> DocumentType.CORRESPONDENCE_FROM_DEFENDANT;
            case CORRESPONDENCE_FROM_CLAIMANT -> DocumentType.CORRESPONDENCE_FROM_CLAIMANT;
            case POSSESSION_NOTICE -> DocumentType.POSSESSION_NOTICE;
            case NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION -> DocumentType.NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION;
            case PHOTOGRAPHIC_EVIDENCE -> DocumentType.PHOTOGRAPHIC_EVIDENCE;
            case INSPECTION_OR_REPORT -> DocumentType.INSPECTION_OR_REPORT;
            case CERTIFICATE_OF_SUITABILITY_AS_LF -> DocumentType.CERTIFICATE_OF_SUITABILITY_AS_LF;
            case LEGAL_AID_CERTIFICATE -> DocumentType.LEGAL_AID_CERTIFICATE;
            case OTHER -> DocumentType.OTHER;
        };
    }

    public List<DocumentEntity> createDefendantEvidenceDocuments(
        List<ListValue<UploadedDocument>> defendantDocuments,
        DefendantResponseEntity defendantResponse,
        PcsCaseEntity pcsCase,
        PartyEntity party
    ) {
        if (CollectionUtils.isEmpty(defendantDocuments)) {
            log.info("No defendant evidence documents to save");
            return Collections.emptyList();
        }

        List<DocumentEntity> documentEntities = defendantDocuments.stream()
            .map(ListValue::getValue)
            .filter(Objects::nonNull)
            .map(defDoc -> DocumentEntity.builder()
                .pcsCase(pcsCase)
                .party(party)
                .defendantResponse(defendantResponse)
                .url(defDoc.getDocument().getUrl())
                .fileName(defDoc.getDocument().getFilename())
                .binaryUrl(defDoc.getDocument().getBinaryUrl())
                .categoryId(defDoc.getDocument().getCategoryId())
                .type(DocumentType.DEFENDANT_EVIDENCE)
                .contentType(defDoc.getContentType())
                .size(defDoc.getSize())
                .build())
            .toList();

        List<DocumentEntity> saved = documentRepository.saveAll(documentEntities);

        log.info("Saved {} defendant evidence documents for defendant response {}",
            saved.size(), defendantResponse.getId());

        return saved;
    }

    private DocumentType mapEvidenceDocumentTypeToDocumentType(EvidenceDocumentType evidenceDocumentType) {
        return switch (evidenceDocumentType) {
            case PHOTOGRAPHIC_EVIDENCE -> DocumentType.PHOTOGRAPHIC_EVIDENCE;
            case POLICE_REPORT -> DocumentType.POLICE_REPORT;
            case WITNESS_STATEMENT -> DocumentType.WITNESS_STATEMENT;
            case OTHER -> DocumentType.OTHER;
        };
    }

    @Builder
    @Data
    private static class DocumentHolder {
        private Document document;
        private DocumentType type;
        private String description;
    }
}
