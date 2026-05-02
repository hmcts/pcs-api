package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DocumentsView implements ViewComponent {

    @Override
    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        pcsCase.setAllDocuments(mapAndWrapDocuments(pcsCaseEntity));
    }

    private List<ListValue<Document>> mapAndWrapDocuments(PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity.getDocuments().isEmpty()) {
            return List.of();
        }

        return pcsCaseEntity.getDocuments().stream()
            .map(entity -> ListValue.<Document>builder()
                .id(entity.getId().toString())
                .value(Document.builder()
                           .filename(entity.getFileName())
                           .url(entity.getUrl())
                           .binaryUrl(entity.getBinaryUrl())
                           .categoryId(entity.getCategoryId())
                           .build())
                .build())
            .collect(Collectors.toList());
    }

}
