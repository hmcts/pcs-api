package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class DocumentService {

    private DocumentRepository documentRepository;

    public List<DocumentEntity> createAllDocuments(PCSCase pcsCase) {

        Map<Document, DocumentType> allDocuments = new HashMap<>();

        allDocuments.putAll(mapAdditionalDocumentsWithType(pcsCase.getAdditionalDocuments()));

        allDocuments.putAll(
            mapDocumentsWithType(
                Optional.ofNullable(pcsCase.getRentArrears())
                    .map(RentArrearsSection::getStatementDocuments)
                    .orElse(null), DocumentType.RENT_STATEMENT));

        allDocuments.putAll(
            mapDocumentsWithType(
                Optional.ofNullable(pcsCase.getTenancyLicenceDetails())
                    .map(TenancyLicenceDetails::getTenancyLicenceDocuments)
                    .orElse(null), DocumentType.TENANCY_AGREEMENT));

        allDocuments.putAll(
            mapDocumentsWithType(
                Optional.ofNullable(pcsCase.getOccupationLicenceDetailsWales())
                    .map(OccupationLicenceDetailsWales::getLicenceDocuments)
                    .orElse(null), DocumentType.OCCUPATION_LICENSE));

        return documentRepository.saveAll(createDocumentEntities(allDocuments));
    }

    private Map<Document, DocumentType> mapDocumentsWithType(
        List<ListValue<Document>> docs, DocumentType type) {

        if (docs == null || docs.isEmpty()) {
            return Map.of();
        }

        return docs.stream()
            .map(ListValue::getValue)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                document -> document,
                document -> type
            ));
    }

    private Map<Document, DocumentType> mapAdditionalDocumentsWithType(
        List<ListValue<AdditionalDocument>> documents) {

        return ListValueUtils.unwrapListItems(documents).stream()
            .collect(Collectors.toMap(
                AdditionalDocument::getDocument,
                doc -> mapAdditionalDocumentTypeToDocumentType(doc.getDocumentType())
            ));
    }

    private List<DocumentEntity> createDocumentEntities(
        Map<Document, DocumentType> documents) {

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        return documents.entrySet().stream()
            .map(documentMap -> DocumentEntity.builder()
                .url(documentMap.getKey().getUrl())
                .fileName(documentMap.getKey().getFilename())
                .binaryUrl(documentMap.getKey().getBinaryUrl())
                .categoryId(documentMap.getKey().getCategoryId())
                .type(documentMap.getValue())
                .build())
            .toList();
    }

    private DocumentType mapAdditionalDocumentTypeToDocumentType(
        AdditionalDocumentType additionalType) {

        return switch (additionalType) {
            case WITNESS_STATEMENT -> DocumentType.WITNESS_STATEMENT;
            case RENT_STATEMENT -> DocumentType.RENT_STATEMENT;
            case TENANCY_AGREEMENT -> DocumentType.TENANCY_AGREEMENT;
            case LETTER_FROM_CLAIMANT -> DocumentType.LETTER_FROM_CLAIMANT;
            case STATEMENT_OF_SERVICE -> DocumentType.STATEMENT_OF_SERVICE;
            case VIDEO_EVIDENCE -> DocumentType.VIDEO_EVIDENCE;
            case PHOTOGRAPHIC_EVIDENCE -> DocumentType.PHOTOGRAPHIC_EVIDENCE;
        };
    }
}
