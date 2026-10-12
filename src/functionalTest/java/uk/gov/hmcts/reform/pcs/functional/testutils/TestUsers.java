package uk.gov.hmcts.reform.pcs.functional.testutils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class TestUsers {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestUsers() {
    }

    public static String getUsername(String userPath) {
        return getUserProperty(userPath, "username");
    }

    public static String getIdamId(String userPath) {
        return getUserProperty(userPath, "idamId");
    }

    public static String getOptionalUserProperty(String userPath, String property) {
        try {
            JsonNode root = MAPPER.readTree(TestUsers.class.getResourceAsStream("/users.json"));
            JsonNode node = root;
            for (String part : userPath.split("\\.")) {
                node = node.get(part);
                if (node == null) {
                    throw new IllegalArgumentException("Path not found in users.json: " + userPath);
                }
            }

            JsonNode value = node.get(property);
            return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve user property for path: " + userPath, e);
        }
    }

    public static boolean isCitizenUser(String userPath) {
        return java.util.Arrays.stream(userPath.split("\\."))
            .anyMatch(part -> part.toUpperCase().startsWith("CITIZEN"));
    }

    private static String getUserProperty(String userPath, String property) {
        try {
            JsonNode root = MAPPER.readTree(TestUsers.class.getResourceAsStream("/users.json"));
            JsonNode node = root;
            for (String part : userPath.split("\\.")) {
                node = node.get(part);
                if (node == null) {
                    throw new IllegalArgumentException("Path not found in users.json: " + userPath);
                }
            }

            JsonNode value = node.get(property);
            if (value == null) {
                throw new IllegalArgumentException(
                    "Property not found in users.json: " + userPath + "." + property);
            }

            return value.asText();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve username for path: " + userPath, e);
        }
    }
}