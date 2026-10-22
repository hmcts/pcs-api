package uk.gov.hmcts.reform.pcs.ccd.service.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentCategoryUpdateService {

    private final PcsCaseService pcsCaseService;
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void updateCategories(long caseReference, PCSCase caseData) {
        var documents = pcsCaseService.loadCase(caseReference).getDocuments();
        Map<String, String> storedCategories = new HashMap<>();
        documents.forEach(document -> storedCategories.put(document.getUrl(), document.getCategoryId()));
        Map<String, Optional<String>> changes = new HashMap<>();
        findChanges(objectMapper.valueToTree(caseData), storedCategories, changes);

        documents.forEach(entity -> {
            Optional<String> category = changes.get(entity.getUrl());
            if (category != null) {
                entity.setCategoryId(category.orElse(null));
                documentRepository.save(entity);
            }
        });
    }

    private void findChanges(JsonNode node, Map<String, String> storedCategories, Map<String, Optional<String>> changes) {
        if (node.isObject()) {
            if (node.hasNonNull("document_url")) {
                String url = node.get("document_url").asText();
                String category = node.path("category_id").asText(null);
                if (storedCategories.containsKey(url) && !Objects.equals(storedCategories.get(url), category)) {
                    Optional<String> target = Optional.ofNullable(category);
                    Optional<String> existing = changes.computeIfAbsent(url, ignored -> target);
                    if (!existing.equals(target)) {
                        throw new IllegalArgumentException("Conflicting document categories for " + url);
                    }
                }
            }
            node.elements().forEachRemaining(child -> findChanges(child, storedCategories, changes));
        } else if (node.isArray()) {
            node.forEach(child -> findChanges(child, storedCategories, changes));
        }
    }
}
