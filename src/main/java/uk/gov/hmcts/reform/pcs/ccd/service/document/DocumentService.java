package uk.gov.hmcts.reform.pcs.ccd.service.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.EvidenceDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.EvidenceOfDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@AllArgsConstructor
@Service
public class DocumentService {

    private DocumentRepository documentRepository;

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
                    .orElse(null), DocumentType.NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION));

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
                        .binaryUrl(holder.getDocument().getBinaryUrl())
                        .categoryId(mapDocumentTypeToCategory(holder.getType()).getId())
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

    private CaseFileCategory mapDocumentTypeToCategory(DocumentType documentType) {
        return switch (documentType) {
            case NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION -> CaseFileCategory.STATEMENTS_OF_CASE;
            case RENT_STATEMENT,
                 TENANCY_AGREEMENT,
                 TENANCY_LICENCE,
                 OCCUPATION_LICENCE,
                 POSSESSION_NOTICE -> CaseFileCategory.PROPERTY_DOCUMENTS;
            case WITNESS_STATEMENT,
                 CERTIFICATE_OF_SERVICE,
                 CORRESPONDENCE_FROM_DEFENDANT,
                 CORRESPONDENCE_FROM_CLAIMANT,
                 PHOTOGRAPHIC_EVIDENCE,
                 INSPECTION_OR_REPORT -> CaseFileCategory.EVIDENCE;
            case CERTIFICATE_OF_SUITABILITY_AS_LF,
                 LEGAL_AID_CERTIFICATE -> CaseFileCategory.CORRESPONDENCE;
            case NOTICE_SERVED,
                 POLICE_REPORT,
                 OTHER -> CaseFileCategory.UNCATEGORISED;
        };
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
