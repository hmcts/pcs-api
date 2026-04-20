package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Context for clearing fields from draft data.
 * Contains the root node name where clearFields was found and the list of field paths to clear.
 */
@Data
@AllArgsConstructor
public class ClearFieldsContext {
    /**
     * The JSON path to the root node where clearFields was found.
     * Examples: "possessionClaimResponse", "enforcementOrder", "" (for root level).
     */
    private final String rootNodeName;

    /**
     * The list of field paths to clear, relative to the root node.
     */
    private final List<String> clearFields;
}
