package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PROOF TEST: Demonstrates clearFields behavior with realistic scenario.
 *
 * <p>Scenario: User initially selected ALL 5 income options, then on second submission
 * unchecked 4 options and kept only "moneyFromElsewhere".
 */
class ClearFieldsProofTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void proveScenario_AllFiveOptionsToOnlyMoneyFromElsewhere() throws Exception {
        // ============================================================
        // FRONTEND SENDS: Only moneyFromElsewhere selected
        // ============================================================
        String patchJson = """
            {
              "possessionClaimResponse": {
                "defendantResponses": {
                  "householdCircumstances": {
                    "incomeFromJobs": "NO",
                    "pension": "NO",
                    "universalCredit": "NO",
                    "otherBenefits": "NO",
                    "moneyFromElsewhere": "YES",
                    "moneyFromElsewhereDetails": "Test123"
                  }
                },
                "clearFields": [
                  "possessionClaimResponse.defendantResponses.householdCircumstances.incomeFromJobsAmount",
                  "possessionClaimResponse.defendantResponses.householdCircumstances.incomeFromJobsFrequency",
                  "possessionClaimResponse.defendantResponses.householdCircumstances.pensionAmount",
                  "possessionClaimResponse.defendantResponses.householdCircumstances.pensionFrequency",
                  "possessionClaimResponse.defendantResponses.householdCircumstances.universalCreditAmount",
                  "possessionClaimResponse.defendantResponses.householdCircumstances.universalCreditFrequency",
                  "possessionClaimResponse.defendantResponses.householdCircumstances.otherBenefitsAmount",
                  "possessionClaimResponse.defendantResponses.householdCircumstances.otherBenefitsFrequency"
                ]
              }
            }
            """;

        // ============================================================
        // STEP 1: Extract clearFields
        // ============================================================
        JsonNode patchNode = objectMapper.readTree(patchJson);
        JsonNode clearFieldsNode = patchNode.at("/possessionClaimResponse/clearFields");
        List<String> clearFields = objectMapper.convertValue(clearFieldsNode,
            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

        System.out.println("\n========== STEP 1: Extracted clearFields ==========");
        clearFields.forEach(field -> System.out.println("  - " + field));

        // ============================================================
        // STEP 2: Remove clearFields property from patch JSON
        // ============================================================
        ObjectNode patchRoot = (ObjectNode) patchNode;
        JsonNode pcr = patchRoot.at("/possessionClaimResponse");
        if (pcr.isObject()) {
            ((ObjectNode) pcr).remove("clearFields");
        }
        String patchWithoutClearFields = objectMapper.writeValueAsString(patchRoot);

        System.out.println("\n========== STEP 2: Patch JSON (clearFields removed) ==========");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(objectMapper.readTree(patchWithoutClearFields)));

        // ============================================================
        // STEP 3: Merge patch onto existing data
        // ============================================================
        // BEFORE: Database has ALL 5 income options populated
        final String existingDraftData = """
            {
              "possessionClaimResponse": {
                "defendantResponses": {
                  "householdCircumstances": {
                    "incomeFromJobs": "YES",
                    "incomeFromJobsAmount": 250000,
                    "incomeFromJobsFrequency": "MONTHLY",
                    "pension": "YES",
                    "pensionAmount": 150000,
                    "pensionFrequency": "MONTHLY",
                    "universalCredit": "YES",
                    "universalCreditAmount": 120000,
                    "universalCreditFrequency": "MONTHLY",
                    "otherBenefits": "YES",
                    "otherBenefitsAmount": 80000,
                    "otherBenefitsFrequency": "WEEKLY",
                    "moneyFromElsewhere": "YES",
                    "moneyFromElsewhereDetails": "Rental income"
                  }
                }
              }
            }
            """;
        JsonNode base = objectMapper.readTree(existingDraftData);
        JsonNode merged = objectMapper.readerForUpdating(base)
            .readValue(patchWithoutClearFields);

        System.out.println("\n========== STEP 3: After Merge (BEFORE clearFields) ==========");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(merged));

        // ASSERT: Merged data has old amounts/frequencies still present
        JsonNode hcAfterMerge = merged.at("/possessionClaimResponse/defendantResponses/householdCircumstances");
        assertThat(hcAfterMerge.get("incomeFromJobs").asText()).isEqualTo("NO");
        assertThat(hcAfterMerge.get("incomeFromJobsAmount")).isNotNull(); // OLD VALUE STILL HERE!
        assertThat(hcAfterMerge.get("incomeFromJobsAmount").asInt()).isEqualTo(250000);
        assertThat(hcAfterMerge.get("incomeFromJobsFrequency")).isNotNull(); // OLD VALUE STILL HERE!
        assertThat(hcAfterMerge.get("pensionAmount").asInt()).isEqualTo(150000);
        assertThat(hcAfterMerge.get("universalCreditAmount").asInt()).isEqualTo(120000);
        assertThat(hcAfterMerge.get("otherBenefitsAmount").asInt()).isEqualTo(80000);

        // ============================================================
        // STEP 4: Apply clearFields (SET to NULL, not remove!)
        // ============================================================
        ObjectNode mergedRoot = (ObjectNode) merged;

        // Set clearFields to null
        for (String fieldPath : clearFields) {
            setFieldToNull(mergedRoot, fieldPath);
        }

        // CRITICAL: Remove all OTHER null fields before serialization
        // Only clearFields should have nulls in the output
        java.util.Set<String> clearFieldsSet = new java.util.HashSet<>(clearFields);
        removeNullFieldsExcept(mergedRoot, "", clearFieldsSet);

        // NOW safe to serialize with null-including mapper
        // Only clearFields have nulls, all other nulls were removed
        ObjectMapper nullIncludingMapper = new ObjectMapper();
        nullIncludingMapper.setSerializationInclusion(
            com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS);

        String finalJson = nullIncludingMapper.writeValueAsString(mergedRoot);

        System.out.println("\n========== STEP 4: After clearFields Applied (SET TO NULL) ==========");
        System.out.println(nullIncludingMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(objectMapper.readTree(finalJson)));

        // ============================================================
        // ASSERTIONS: Verify final state
        // ============================================================
        JsonNode hcFinal = mergedRoot.at("/possessionClaimResponse/defendantResponses/householdCircumstances");

        // 1. Flags updated correctly
        assertThat(hcFinal.get("incomeFromJobs").asText()).isEqualTo("NO");
        assertThat(hcFinal.get("pension").asText()).isEqualTo("NO");
        assertThat(hcFinal.get("universalCredit").asText()).isEqualTo("NO");
        assertThat(hcFinal.get("otherBenefits").asText()).isEqualTo("NO");
        assertThat(hcFinal.get("moneyFromElsewhere").asText()).isEqualTo("YES");

        // 2. Cleared fields SET TO NULL (field exists but value is null!)
        assertThat(hcFinal.has("incomeFromJobsAmount")).isTrue();
        assertThat(hcFinal.get("incomeFromJobsAmount").isNull()).isTrue();
        assertThat(hcFinal.has("incomeFromJobsFrequency")).isTrue();
        assertThat(hcFinal.get("incomeFromJobsFrequency").isNull()).isTrue();

        assertThat(hcFinal.has("pensionAmount")).isTrue();
        assertThat(hcFinal.get("pensionAmount").isNull()).isTrue();
        assertThat(hcFinal.has("pensionFrequency")).isTrue();
        assertThat(hcFinal.get("pensionFrequency").isNull()).isTrue();

        assertThat(hcFinal.has("universalCreditAmount")).isTrue();
        assertThat(hcFinal.get("universalCreditAmount").isNull()).isTrue();
        assertThat(hcFinal.has("universalCreditFrequency")).isTrue();
        assertThat(hcFinal.get("universalCreditFrequency").isNull()).isTrue();

        assertThat(hcFinal.has("otherBenefitsAmount")).isTrue();
        assertThat(hcFinal.get("otherBenefitsAmount").isNull()).isTrue();
        assertThat(hcFinal.has("otherBenefitsFrequency")).isTrue();
        assertThat(hcFinal.get("otherBenefitsFrequency").isNull()).isTrue();

        // 3. moneyFromElsewhereDetails updated correctly
        assertThat(hcFinal.get("moneyFromElsewhereDetails").asText()).isEqualTo("Test123");

        System.out.println("\n========== PROOF COMPLETE ==========");
        System.out.println("✅ All 4 unchecked options have their amounts/frequencies SET TO NULL");
        System.out.println("✅ moneyFromElsewhere kept with updated details");
        System.out.println("✅ Fields exist in JSON with null values (not removed)");
    }

    @Test
    void proveOnlyClearFieldsNullsAreIncluded_NotOtherNulls() throws Exception {
        // Scenario: Merged JSON has some unrelated null fields
        // Only clearFields nulls should be in the output, not the other nulls

        String mergedJsonWithUnrelatedNulls = """
            {
              "possessionClaimResponse": {
                "defendantResponses": {
                  "householdCircumstances": {
                    "incomeFromJobs": "NO",
                    "incomeFromJobsAmount": 250000,
                    "incomeFromJobsFrequency": "MONTHLY",
                    "someUnrelatedField": null,
                    "anotherUnrelatedField": null
                  }
                }
              }
            }
            """;

        List<String> clearFields = List.of(
            "possessionClaimResponse.defendantResponses.householdCircumstances.incomeFromJobsAmount",
            "possessionClaimResponse.defendantResponses.householdCircumstances.incomeFromJobsFrequency"
        );

        ObjectNode root = (ObjectNode) objectMapper.readTree(mergedJsonWithUnrelatedNulls);

        System.out.println("\n========== BEFORE: Merged JSON with unrelated nulls ==========");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));

        // Set clearFields to null
        for (String fieldPath : clearFields) {
            setFieldToNull(root, fieldPath);
        }

        System.out.println("\n========== AFTER setting clearFields to null ==========");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));

        // Remove all OTHER nulls (not in clearFields)
        java.util.Set<String> clearFieldsSet = new java.util.HashSet<>(clearFields);
        removeNullFieldsExcept(root, "", clearFieldsSet);

        System.out.println("\n========== AFTER removing non-clearFields nulls ==========");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));

        // Serialize with null-including mapper
        ObjectMapper nullIncludingMapper = new ObjectMapper();
        nullIncludingMapper.setSerializationInclusion(
            com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS);
        String finalJson = nullIncludingMapper.writeValueAsString(root);

        System.out.println("\n========== FINAL: Serialized with ALWAYS ==========");
        System.out.println(nullIncludingMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
            objectMapper.readTree(finalJson)));

        // ASSERT: Only clearFields nulls are present
        JsonNode hc = root.at("/possessionClaimResponse/defendantResponses/householdCircumstances");

        // clearFields nulls are present
        assertThat(hc.has("incomeFromJobsAmount")).isTrue();
        assertThat(hc.get("incomeFromJobsAmount").isNull()).isTrue();
        assertThat(hc.has("incomeFromJobsFrequency")).isTrue();
        assertThat(hc.get("incomeFromJobsFrequency").isNull()).isTrue();

        // Unrelated nulls are REMOVED
        assertThat(hc.has("someUnrelatedField")).isFalse();
        assertThat(hc.has("anotherUnrelatedField")).isFalse();

        // Other field preserved
        assertThat(hc.get("incomeFromJobs").asText()).isEqualTo("NO");

        System.out.println("\n========== PROOF COMPLETE ==========");
        System.out.println("✅ Only clearFields have nulls in final JSON");
        System.out.println("✅ Unrelated null fields were removed");
        System.out.println("✅ No bloat from unrelated nulls");
    }

    private void setFieldToNull(ObjectNode root, String fieldPath) {
        String[] pathSegments = fieldPath.split("\\.");
        ObjectNode current = root;

        for (int i = 0; i < pathSegments.length - 1; i++) {
            JsonNode next = current.get(pathSegments[i]);
            if (next == null || !next.isObject()) {
                return;
            }
            current = (ObjectNode) next;
        }

        String fieldName = pathSegments[pathSegments.length - 1];
        current.set(fieldName, com.fasterxml.jackson.databind.node.NullNode.getInstance());
    }

    /**
     * Recursively remove all null fields from the tree EXCEPT the ones in keepNulls set.
     * This ensures that only explicitly cleared fields have nulls in the final JSON.
     */
    private void removeNullFieldsExcept(ObjectNode node, String currentPath, java.util.Set<String> keepNulls) {
        java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = node.fields();
        java.util.List<String> toRemove = new java.util.ArrayList<>();

        while (fields.hasNext()) {
            java.util.Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode value = field.getValue();
            String fullPath = currentPath.isEmpty() ? fieldName : currentPath + "." + fieldName;

            if (value.isNull()) {
                // Only keep this null if it's in clearFields
                if (!keepNulls.contains(fullPath)) {
                    toRemove.add(fieldName);
                }
            } else if (value.isObject()) {
                // Recursively process nested objects
                removeNullFieldsExcept((ObjectNode) value, fullPath, keepNulls);
            }
        }

        // Remove null fields that aren't in clearFields
        for (String fieldName : toRemove) {
            node.remove(fieldName);
        }
    }
}
