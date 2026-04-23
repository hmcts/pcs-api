package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Component
public class DraftCaseJsonMerger {

    private static final Set<String> REPLACE_FIELDS = Set.of("address");
    private static final Set<String> REGULAR_INCOME_KEYS = Set.of(
        "incomeFromJobs",
        "pension",
        "otherBenefits",
        "moneyFromElsewhere"
    );


    private final ObjectMapper objectMapper;

    public DraftCaseJsonMerger(@Qualifier("draftCaseDataObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Peforms a merge of case data from `patchJson` onto `baseJson`.
     * @param baseJson The JSON string to merge onto
     * @param patchJson The updated JSON data to apply
     * @return A string containing the merged JSON
     * @throws JsonProcessingException If the JSON strings could not be parsed or written back
     *     to a string value after being combined
     */
    public String mergeJson(String baseJson, String patchJson) throws JsonProcessingException {
        JsonNode base = objectMapper.readValue(baseJson, JsonNode.class);
        JsonNode patch = objectMapper.readValue(patchJson, JsonNode.class);

        applyReplaceRulesRecursively(base, patch);
        applyRegularIncomeUniversalCreditClearRuleRecursively(base, patch);

        JsonNode merged = objectMapper.readerForUpdating(base)
            .readValue(patchJson);

        return objectMapper.writeValueAsString(merged);
    }

    /**
     * Clears fields in the base JSON where the patch contains an address object.
     * Fully replaces the old fields rather than merging individual fields.
     */
    private void applyReplaceRulesRecursively(JsonNode base, JsonNode patch) {
        if (!patch.isObject() || !base.isObject()) {
            return;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = patch.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode patchChild = field.getValue();

            if (base.has(fieldName)) {
                JsonNode baseChild = base.get(fieldName);

                if (REPLACE_FIELDS.contains(fieldName.toLowerCase())
                    && patchChild.isObject()
                    && base instanceof ObjectNode) {

                    ((ObjectNode) base).set(fieldName, objectMapper.createObjectNode());

                } else {
                    applyReplaceRulesRecursively(baseChild, patchChild);
                }
            }
        }
    }

    private void applyRegularIncomeUniversalCreditClearRuleRecursively(JsonNode base, JsonNode patch) {
        if (!patch.isObject() || !base.isObject()) {
            return;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = patch.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode patchChild = field.getValue();

            if ("householdCircumstances".equals(fieldName)
                && patchChild.isObject()
                && base.has(fieldName)
                && base.get(fieldName).isObject()) {

                ObjectNode patchHouseholdCircumstances = (ObjectNode) patchChild;
                ObjectNode baseHouseholdCircumstances = (ObjectNode) base.get(fieldName);

                if (isRegularIncomeSubmission(patchHouseholdCircumstances)
                    && !isUniversalCreditSelected(patchHouseholdCircumstances)) {
                    baseHouseholdCircumstances.putNull("universalCreditAmount");
                    baseHouseholdCircumstances.putNull("universalCreditFrequency");
                }
            }

            if (base.has(fieldName)) {
                applyRegularIncomeUniversalCreditClearRuleRecursively(base.get(fieldName), patchChild);
            }
        }
    }

    private boolean isRegularIncomeSubmission(ObjectNode householdCircumstancesPatch) {
        return REGULAR_INCOME_KEYS.stream().anyMatch(householdCircumstancesPatch::has);
    }

    private boolean isUniversalCreditSelected(ObjectNode householdCircumstancesPatch) {
        JsonNode universalCredit = householdCircumstancesPatch.get("universalCredit");
        return universalCredit != null
            && universalCredit.isTextual()
            && "YES".equalsIgnoreCase(universalCredit.asText());
    }

}
