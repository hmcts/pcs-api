package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Payload for the defence-form-generation db-scheduler task. Mirrors {@link ClaimFormTaskData},
 * keyed by the defendant response id. {@code defendantPartyId} is carried so a generation failure
 * can be logged against the defendant after the generation transaction has rolled back.
 */
@Data
@Builder
@AllArgsConstructor
public class DefenceFormTaskData {

    private final String caseReference;
    private final UUID defendantResponseId;
    private final UUID defendantPartyId;

}
