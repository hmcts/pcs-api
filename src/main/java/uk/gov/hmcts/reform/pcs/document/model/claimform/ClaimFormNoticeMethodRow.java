package uk.gov.hmcts.reform.pcs.document.model.claimform;

import lombok.Builder;
import lombok.Data;

/**
 * One row in the "How the notice was served" repeat block: a method, the date it was served by that
 * method, and the name, email address or other details given for it.
 */
@Data
@Builder
public class ClaimFormNoticeMethodRow {

    private String methodLabel;
    private String servedOn;
    private boolean hasServedOn;
    private String details;
    private boolean hasDetails;

}
