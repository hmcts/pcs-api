package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureCaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentHolder;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.EvidenceDocumentType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.EvidenceOfDefendants;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class EnforcementDocumentService {

    /**
     * Every enforcement document, generated or uploaded, is filed in the Enforcement folder of Case File View.
     */
    public static final String ENFORCEMENT_CATEGORY_ID = FeatureCaseFileCategory.ENFORCEMENT.getId();

    private final DocumentService documentService;

    /**
     * Saves the documents uploaded with the enforcement application in the Enforcement folder, attached to
     * the case, so that they are in Case File View, and to the enforcement order.
     */
    public List<DocumentEntity> createAllDocuments(EnforcementOrderEntity enforcementOrderEntity,
                                                   EnforcementOrder enforcementOrder) {
        List<DocumentEntity> documents =
            documentService.createDocuments(getWarrantOfRestitutionDocuments(enforcementOrder));
        if (!documents.isEmpty()) {
            enforcementOrderEntity.getClaim().getPcsCase().addDocuments(documents);
            enforcementOrderEntity.addDocuments(documents);
        }
        return documents;
    }

    private List<DocumentHolder> getWarrantOfRestitutionDocuments(EnforcementOrder enforcementOrder) {

        return new ArrayList<>(mapEvidenceOfDefendantsDocumentsWithType(
                enforcementOrder.getWarrantOfRestitutionDetails().getAdditionalDocuments()));
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
                .categoryId(ENFORCEMENT_CATEGORY_ID)
                .build())
            .toList();
    }

    private DocumentType mapEvidenceDocumentTypeToDocumentType(EvidenceDocumentType evidenceDocumentType) {
        return switch (evidenceDocumentType) {
            case PHOTOGRAPHIC_EVIDENCE -> DocumentType.PHOTOGRAPHIC_EVIDENCE;
            case POLICE_REPORT -> DocumentType.POLICE_REPORT;
            case WITNESS_STATEMENT -> DocumentType.WITNESS_STATEMENT;
            case OTHER -> DocumentType.OTHER;
        };
    }
}
