package uk.gov.hmcts.reform.pcs.document.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a Party used in a template for document generation.
 */
@Data
@Builder
public class Party {

    private String name;
    private String correspondenceAddress;
    private String telephoneNumber;
    private String emailAddress;

}
