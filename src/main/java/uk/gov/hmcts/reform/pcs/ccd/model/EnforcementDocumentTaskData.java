package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Task data for generating an enforcement application's documents (writ, warrant, risk assessment).
 */
@Data
@Builder
@AllArgsConstructor
public class EnforcementDocumentTaskData {

    private final long caseReference;
    private final UUID enforcementOrderId;

}
