package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

@Component
@RequiredArgsConstructor
public class DocumentsView {

    private final SecurityContextService securityContextService;
    private final GenAppVisibilityService genAppVisibilityService;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        pcsCase.setAllDocuments(mapAndWrapDocuments(pcsCaseEntity));
    }

    private List<ListValue<Document>> mapAndWrapDocuments(PcsCaseEntity pcsCaseEntity) {

        List<DocumentEntity> documents = pcsCaseEntity.getDocuments();
        if (documents.isEmpty()) {
            return List.of();
        }

        Set<UUID> visibleGenAppIds = getVisibleGenAppIds(pcsCaseEntity, documents);

        return documents.stream()
            .filter(documentEntity -> this.isDocumentVisibleToUser(documentEntity, visibleGenAppIds))
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

    public boolean isDocumentVisibleToUser(DocumentEntity documentEntity, Set<UUID> visibleGenAppIds) {
        GenAppEntity genAppEntity = documentEntity.getGeneralApplication();

        if (genAppEntity != null) {
            return visibleGenAppIds.contains(genAppEntity.getId());
        } else {
            return true;
        }
    }

    private Set<UUID> getVisibleGenAppIds(PcsCaseEntity pcsCaseEntity, List<DocumentEntity> documents) {
        boolean hasGenAppDocuments = documents.stream()
            .anyMatch(document -> document.getGeneralApplication() != null);
        if (!hasGenAppDocuments) {
            return Set.of();
        }

        if (pcsCaseEntity.getGenApps().isEmpty()) {
            return Set.of();
        }

        UUID currentUserId = securityContextService.getCurrentUserId();

        return pcsCaseEntity.getGenApps().stream()
            .filter(genAppEntity -> genAppVisibilityService.isGenAppVisibleToUser(genAppEntity, currentUserId))
            .map(GenAppEntity::getId)
            .collect(toSet());
    }

}
