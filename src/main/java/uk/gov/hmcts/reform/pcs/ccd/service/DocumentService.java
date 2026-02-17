package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
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

        List<Pair<Document, DocumentType>> allDocuments = new ArrayList<>();

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


        return documentRepository.saveAll(createDocumentEntities(allDocuments));
    }

    public List<DocumentEntity> saveDocument(Document document,
                                             DocumentType documentType,
                                             VerticalYesNo isAmendedDocument) {

        List<Pair<Document, DocumentType>> documentList = List.of(Pair.of(document, documentType));

        List<DocumentEntity> documentEntities = createDocumentEntities(documentList);
        documentEntities.forEach(documentEntity -> documentEntity.setIsAmendment(isAmendedDocument));

        return documentRepository.saveAll(documentEntities);
    }

    private List<Pair<Document, DocumentType>> mapDocumentsWithType(
        List<ListValue<Document>> docs, DocumentType type) {

        if (docs == null || docs.isEmpty()) {
            return Collections.emptyList();
        }

        return docs.stream()
            .map(ListValue::getValue)
            .filter(Objects::nonNull)
            .map(doc -> Pair.of(doc, type))
            .toList();
    }

    private List<Pair<Document, DocumentType>> mapAdditionalDocumentsWithType(
        List<ListValue<AdditionalDocument>> documents) {

        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        return ListValueUtils.unwrapListItems(documents).stream()
            .map(doc -> Pair.of(
                doc.getDocument(),
                mapAdditionalDocumentTypeToDocumentType(doc.getDocumentType())
            ))
            .toList();
    }

    private List<DocumentEntity> createDocumentEntities(
        List<Pair<Document, DocumentType>> documents) {

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        LocalDateTime currentDateTime = LocalDateTime.now(ZoneId.of("Europe/London"));

        return documents.stream()
            .map(pair -> DocumentEntity.builder()
                .url(pair.getKey().getUrl())
                .fileName(pair.getKey().getFilename())
                .binaryUrl(pair.getKey().getBinaryUrl())
                .categoryId(pair.getKey().getCategoryId())
                .type(pair.getValue())
                .lastModified(currentDateTime)
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
