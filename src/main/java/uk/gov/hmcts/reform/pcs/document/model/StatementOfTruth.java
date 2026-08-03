package uk.gov.hmcts.reform.pcs.document.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Represents a Statement of Truth used in a template for document generation.
 */
@Data
@Builder
public class StatementOfTruth {

    private LocalDate submittedOn;
    private String fullName;

}
