package uk.gov.hmcts.reform.pcs.document.model.accesscode;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

import java.time.LocalDate;

/**
 * Docmosis merge-field payload for the defendant access-code letter. Field names must match the template.
 */
@Data
@Builder
public class AccessCodeFormPayload implements FormPayload {

    private String caseReference;
    private String claimantName;
    private String defendantName;
    private String defendantAddress;
    private String propertyAddress;
    private String respondByPostCourtAddress;
    // Excluded from toString so the plaintext access code can never leak into logs.
    @ToString.Exclude
    private String accessCode;
    private LocalDate issuedOn;
    private String url;

}
