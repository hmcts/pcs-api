package uk.gov.hmcts.reform.pcs.ccd.service.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentCategoryUpdateService {

    private final PcsCaseService pcsCaseService;
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void updateCategories(long caseReference, PCSCase caseData) {
        List<DocumentEntity> documents = pcsCaseService.loadCase(caseReference).getDocuments();
        Map<String, String> storedCategories = new HashMap<>();
        documents.forEach(document -> storedCategories.put(document.getUrl(), document.getCategoryId()));

        // A document can appear in more than one field, so only the copy that was moved has a new category
        Map<String, String> movedCategories = new HashMap<>();
        findMoved(objectMapper.valueToTree(caseData), storedCategories, movedCategories);

        documents.stream()
            .filter(document -> movedCategories.containsKey(document.getUrl()))
            .forEach(document -> {
                document.setCategoryId(movedCategories.get(document.getUrl()));
                documentRepository.save(document);
            });
    }

    private void findMoved(JsonNode node, Map<String, String> storedCategories, Map<String, String> moved) {
        if (node.isObject() && node.hasNonNull("document_url")) {
            String url = node.get("document_url").asText();
            String category = node.path("category_id").asText(null);
            if (category != null && storedCategories.containsKey(url) && !category.equals(storedCategories.get(url))) {
                moved.put(url, category);
            }
        }
        node.forEach(child -> findMoved(child, storedCategories, moved));
    }
}
