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

    public boolean isDocumentVisibleToUser(DocumentEntity documentEntity, UUID currentUserId) {
        GenAppEntity genAppEntity = documentEntity.getGeneralApplication();

        if (genAppEntity != null) {
            return genAppVisibilityService.isGenAppVisibleToUser(genAppEntity, currentUserId);
        } else {
            return true;
        }
    }

}
