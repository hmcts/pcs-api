package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
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
    private final ObjectMapper objectMapper;

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

        return documents.stream()
            .map(pair -> DocumentEntity.builder()
                .url(pair.getKey().getUrl())
                .fileName(pair.getKey().getFilename())
                .binaryUrl(pair.getKey().getBinaryUrl())
                .categoryId(pair.getKey().getCategoryId())
                .type(pair.getValue())
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

    /**
     * Creates defendant evidence documents from uploaded documents in draft.
     * Extracts mimeType and size from categoryId JSON (AC07 requirement).
     * Called during final submit to persist document metadata to database.
     *
     * @param uploadedDocuments List of documents from DefendantResponses.uploadedDocuments
     * @param pcsCase The PCS case to link documents to
     * @return List of saved DocumentEntity objects
     */
    public List<DocumentEntity> createDefendantEvidenceDocuments(
        List<ListValue<Document>> uploadedDocuments,
        PcsCaseEntity pcsCase
    ) {
        if (uploadedDocuments == null || uploadedDocuments.isEmpty()) {
            log.info("No defendant evidence documents to save");
            return Collections.emptyList();
        }

        List<DocumentEntity> documentEntities = uploadedDocuments.stream()
            .map(ListValue::getValue)
            .filter(Objects::nonNull)
            .map(doc -> DocumentEntity.builder()
                .pcsCase(pcsCase)
                .url(doc.getUrl())
                .fileName(doc.getFilename())
                .binaryUrl(doc.getBinaryUrl())
                .categoryId(doc.getCategoryId())
                .type(DocumentType.DEFENDANT_EVIDENCE)
                .contentType(extractContentType(doc))
                .size(extractSize(doc))
                .build())
            .toList();

        List<DocumentEntity> saved = documentRepository.saveAll(documentEntities);

        log.info("Saved {} defendant evidence documents for case {}",
            saved.size(), pcsCase.getCaseReference());

        return saved;
    }

    /**
     * Extract content type (MIME type) from Document categoryId.
     * Frontend stores CDAM metadata as JSON: {"mimeType":"application/pdf","size":2048576}
     *
     * @param doc Document from CCD
     * @return MIME type string, or null if not available
     */
    private String extractContentType(Document doc) {
        if (doc.getCategoryId() == null || doc.getCategoryId().isBlank()) {
            return null;
        }

        try {
            JsonNode metadata = objectMapper.readTree(doc.getCategoryId());
            if (metadata.has("mimeType")) {
                return metadata.get("mimeType").asText();
            }
        } catch (Exception e) {
            log.warn("Failed to extract mimeType from categoryId for document: {}",
                doc.getFilename(), e);
        }

        return null;
    }

    /**
     * Extract file size from Document categoryId.
     * Frontend stores CDAM metadata as JSON: {"mimeType":"application/pdf","size":2048576}
     *
     * @param doc Document from CCD
     * @return File size in bytes, or null if not available
     */
    private Long extractSize(Document doc) {
        if (doc.getCategoryId() == null || doc.getCategoryId().isBlank()) {
            return null;
        }

        try {
            JsonNode metadata = objectMapper.readTree(doc.getCategoryId());
            if (metadata.has("size")) {
                return metadata.get("size").asLong();
            }
        } catch (Exception e) {
            log.warn("Failed to extract size from categoryId for document: {}",
                doc.getFilename(), e);
        }

        return null;
    }
}
