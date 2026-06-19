package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DocumentsView {

    private final SecurityContextService securityContextService;
    private final GenAppVisibilityService genAppVisibilityService;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        pcsCase.setAllDocuments(mapAndWrapDocuments(pcsCaseEntity));
    }

    private List<ListValue<Document>> mapAndWrapDocuments(PcsCaseEntity pcsCaseEntity) {

        if (pcsCaseEntity.getDocuments().isEmpty()) {
            return List.of();
        }

        UUID currentUserId = securityContextService.getCurrentUserId();

        return pcsCaseEntity.getDocuments().stream()
            .filter(documentEntity -> this.isDocumentVisibleToUser(documentEntity, currentUserId))
            .filter(this::isNotInCaseDetailsTab)
            .map(entity -> ListValue.<Document>builder()
                .id(entity.getId().toString())
                .value(Document.builder()
                           .filename(entity.getFileName())
                           .url(entity.getUrl())
                           .binaryUrl(entity.getBinaryUrl())
                           .categoryId(entity.getCategoryId())
                           .uploadTimestamp(entity.getSubmittedDate() == null
                                                ? null
                                                : entity.getSubmittedDate()
                               .atZone(java.time.ZoneOffset.UTC).toLocalDateTime())
                           .build())
                .build())
            .collect(Collectors.toList());
    }

    private boolean isDocumentVisibleToUser(DocumentEntity documentEntity, UUID currentUserId) {
        GenAppEntity genAppEntity = documentEntity.getGeneralApplication();

        if (genAppEntity != null) {
            return genAppVisibilityService.isGenAppVisibleToUser(genAppEntity, currentUserId);
        } else {
            return true;
        }
    }

    public static boolean isDescriptionEmpty(DocumentEntity documentEntity) {
        return ObjectUtils.isEmpty(documentEntity.getDescription())
                || documentEntity.getDescription().trim().isEmpty();
    }

    private boolean isNotInCaseDetailsTab(DocumentEntity documentEntity) {
        List<DocumentType> caseDetailsDocuments = List.of(
            DocumentType.TENANCY_AGREEMENT,
            DocumentType.POSSESSION_NOTICE,
            DocumentType.RENT_STATEMENT,
            DocumentType.ENERGY_PERFORMANCE_CERTIFICATE,
            DocumentType.EICR_REPORT,
            DocumentType.GAS_SAFETY_CERTIFICATE,
            DocumentType.OCCUPATION_LICENCE
        );

        if (!caseDetailsDocuments.contains(documentEntity.getType())) {
            return true;
        }

        // Is not an additional document
        return documentEntity.getDescription() != null;
    }

}
