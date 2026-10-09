package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Payload for the gen-app-document-generation db-scheduler task. Mirrors {@link CounterClaimFormTaskData}.
 */
@Data
@Builder
@AllArgsConstructor
public class GenAppDocumentTaskData {

    private final UUID genAppId;

}
