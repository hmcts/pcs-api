package uk.gov.hmcts.reform.pcs.document.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a case Document used in a template for document generation.
 */
@Data
@Builder
public class Document {

    private String filename;

}
