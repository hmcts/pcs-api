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

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DocumentsView {

    private final GenAppVisibilityService genAppVisibilityService;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity, String organisationIdForCurrentUser) {
        pcsCase.setAllDocuments(mapAndWrapDocuments(pcsCaseEntity, organisationIdForCurrentUser));
    }

    private List<ListValue<Document>> mapAndWrapDocuments(PcsCaseEntity pcsCaseEntity,
                                                          String organisationIdForCurrentUser) {

        if (pcsCaseEntity.getDocuments().isEmpty()) {
            return List.of();
        }

        return pcsCaseEntity.getDocuments().stream()
            .filter(documentEntity -> this.isDocumentVisibleToUser(documentEntity,
                                                                   organisationIdForCurrentUser))
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

    public boolean isDocumentVisibleToUser(DocumentEntity documentEntity, String orgId) {
        GenAppEntity genAppEntity = documentEntity.getGeneralApplication();

        if (genAppEntity != null) {
            return genAppVisibilityService.isGenAppVisibleToUser(genAppEntity, orgId);
        } else {
            return true;
        }
    }

}
