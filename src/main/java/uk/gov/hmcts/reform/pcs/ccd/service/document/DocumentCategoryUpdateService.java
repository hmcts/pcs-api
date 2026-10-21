package uk.gov.hmcts.reform.pcs.ccd.service.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentCategoryUpdateService {

    private final PcsCaseService pcsCaseService;
    private final DocumentRepository documentRepository;

    @Transactional
    public void updateCategories(long caseReference, List<ListValue<Document>> documents) {
        var documentsById = documents.stream()
            .collect(Collectors.toMap(ListValue::getId, ListValue::getValue));

        pcsCaseService.loadCase(caseReference).getDocuments().forEach(entity -> {
            Document document = documentsById.get(entity.getId().toString());
            if (document != null && !Objects.equals(entity.getCategoryId(), document.getCategoryId())) {
                entity.setCategoryId(document.getCategoryId());
                documentRepository.save(entity);
            }
        });
    }
}
