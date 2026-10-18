package uk.gov.hmcts.reform.pcs.enforcement.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Payload for the enforcement-document-generation db-scheduler task: one document for one enforcement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnforcementDocumentTaskData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private UUID enforcementId;
    private EnforcementOutput output;
}
