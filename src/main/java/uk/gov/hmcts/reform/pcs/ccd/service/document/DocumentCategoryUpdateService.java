package uk.gov.hmcts.reform.pcs.ccd.service.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DocumentMoveRoles.DOCUMENT_MOVE_ROLES;

@Service
@RequiredArgsConstructor
public class DocumentCategoryUpdateService {

    private final PcsCaseService pcsCaseService;
    private final DocumentRepository documentRepository;
    private final UserRoleService userRoleService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void updateCategories(long caseReference, PCSCase caseData) {
        var roles = userRoleService.getCurrentUserCaseRoles(caseReference).roles();
        if (Arrays.stream(DOCUMENT_MOVE_ROLES).noneMatch(role -> roles.contains(role.getRole()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only internal users can move documents");
        }

        var documents = pcsCaseService.loadCase(caseReference).getDocuments();
        Map<String, String> storedCategories = new HashMap<>();
        documents.forEach(document ->
                              storedCategories.put(document.getUrl(), normaliseCategory(document.getCategoryId())));
        Map<String, String> categoriesByUrl = new HashMap<>();
        findChanges(objectMapper.valueToTree(caseData), storedCategories, categoriesByUrl);
        if (categoriesByUrl.size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only one document move is allowed");
        }

        Map<DocumentEntity, String> changes = new HashMap<>();
        for (DocumentEntity entity : pcsCaseService.loadCase(caseReference).getDocuments()) {
            if (categoriesByUrl.containsKey(entity.getUrl())) {
                String category = categoriesByUrl.get(entity.getUrl());
                if (Arrays.stream(CaseFileCategory.values())
                    .noneMatch(folder -> Objects.equals(folder.getId(), category))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document category");
                }
                changes.put(entity, category);
            }
        }
        changes.forEach((entity, category) -> {
            entity.setCategoryId(category);
            documentRepository.save(entity);
        });
    }

    private void findChanges(JsonNode submitted, Map<String, String> storedCategories, Map<String, String> changes) {
        if (submitted.isNull()) {
            return;
        }
        if (submitted.isObject()) {
            if (submitted.hasNonNull("document_url")) {
                String url = submitted.get("document_url").asText();
                String target = normaliseCategory(submitted.path("category_id").asText(null));
                if (!Objects.equals(target, storedCategories.get(url))) {
                    String existing = changes.putIfAbsent(url, target);
                    if (existing != null && existing.equals(target)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conflicting document categories");
                    }
                }
            }
            submitted.fields().forEachRemaining(field -> findChanges(field.getValue(), storedCategories, changes));
        } else if (submitted.isArray()) {
            submitted.forEach(item -> findChanges(item, storedCategories, changes));
        }
    }

    private String normaliseCategory(String category) {
        return category == null ? CaseFileCategory.UNCATEGORISED_DOCUMENTS.getId() : category;
    }
}
