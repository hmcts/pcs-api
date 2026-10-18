package uk.gov.hmcts.reform.pcs.enforcement.document;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;

import java.time.LocalDateTime;

/**
 * The data every enforcement document template is rendered with. It is the same for every
 * {@link EnforcementOutput}, so a template can use any of it, and it carries the claimant's answers as
 * submitted ({@link #enforcementOrder}) rather than a per-template selection of them.
 */
@Data
@Builder
public class EnforcementDocumentPayload implements FormPayload {

    private String documentTitle;
    private String caseReference;
    private String caseName;
    private String propertyAddress;
    private String enforcementType;
    /**
     * The warrant or writ number, once the application has been approved.
     */
    private String enforcementNumber;
    /**
     * "County Court" or "High Court", or null for an unsealed document.
     */
    private String seal;
    /**
     * When the document was generated, UK time, as "DD/MM/YYYY HH:MM AM/PM".
     */
    private String generatedAt;
    /**
     * The case management court, from Location Reference data, or null when the case has none.
     */
    private String courtName;
    /**
     * The court's address and postcode, one part per line.
     */
    private String courtAddress;
    /**
     * The bailiff the enforcement is allocated to, or null before it is allocated.
     */
    private String bailiffName;
    /**
     * The claimant's answer to "Which language did you use to complete this service?" ("English", "Welsh" or
     * "English and Welsh"), or null when the journey did not ask it.
     */
    private String language;
    private LocalDateTime evictionDateTime;
    private Boolean dispenseWithService;
    private EnforcementOrder enforcementOrder;
}
