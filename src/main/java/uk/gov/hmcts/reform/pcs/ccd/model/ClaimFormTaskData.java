package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Payload for the claim-form-generation db-scheduler task. Mirrors {@link AccessCodeTaskData}.
 */
@Data
@Builder
@AllArgsConstructor
public class ClaimFormTaskData {

    private final String caseReference;

}
