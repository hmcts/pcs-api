package uk.gov.hmcts.reform.pcs.document.model;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

import java.time.LocalDate;

/**
 * Docmosis merge-field payload for the defendant pin pack letter. Field names must match the template.
 */
@Data
@Builder
public class PinPackFormPayload implements FormPayload {

    private String caseReference;
    private String claimantName;
    private String defendantName;
    private String defendantAddress;
    private String propertyAddress;
    private String respondByPostCourtName;
    private String respondByPostCourtAddress;
    private String accessCode;
    private LocalDate issuedOn;
    private String url;

}
