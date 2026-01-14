package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@AllArgsConstructor
@Service
public class DocumentService {

    private DocumentRepository documentRepository;

    public List<DocumentEntity> createAllDocuments(PCSCase pcsCase) {

        List<DocumentEntity> entities = new ArrayList<>();

        List<Document> additionalDocuments = ListValueUtils.unwrapListItems(pcsCase.getAdditionalDocuments()).stream()
            .map(additionalDocument -> additionalDocument.getDocument()).toList();

        entities.addAll(mapToDocumentEntity(ListValueUtils.wrapListItems(additionalDocuments),DocumentType.ADDITIONAL));
        entities.addAll(mapToDocumentEntity(pcsCase.getRentStatementDocuments(),DocumentType.RENT_STATEMENT));

        entities.addAll(mapToDocumentEntity(
            Optional.ofNullable(pcsCase.getTenancyLicenceDetails())
                .map(TenancyLicenceDetails::getTenancyLicenceDocuments)
                .orElse(null),DocumentType.TENANCY_LICENSE));

        entities.addAll(mapToDocumentEntity(
            Optional.ofNullable(pcsCase.getOccupationLicenceDetailsWales())
                .map(OccupationLicenceDetailsWales::getLicenceDocuments)
                .orElse(null),
            DocumentType.OCCUPATION_LICENSE
        ));

        return documentRepository.saveAll(entities);
    }

    private List<DocumentEntity> mapToDocumentEntity(List<ListValue<Document>> docs, DocumentType type) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }

        return docs.stream()
            .map(ListValue::getValue)
            .filter(Objects::nonNull)
            .map(doc -> DocumentEntity.builder()
                .url(doc.getUrl())
                .fileName(doc.getFilename())
                .binaryUrl(doc.getBinaryUrl())
                .categoryId(doc.getCategoryId())
                .type(type)
                .build())
            .toList();
    }

}
