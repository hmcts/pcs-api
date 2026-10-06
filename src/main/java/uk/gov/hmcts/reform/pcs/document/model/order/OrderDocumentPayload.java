package uk.gov.hmcts.reform.pcs.document.model.order;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

import java.util.List;

/**
 * Typed payload rendered into the order Docmosis template ({@code CV-PCS-ORD-ENG-Order.docx}).
 * {@code orderHtml} is merged by the template's {@code html:} field; the rest is plain text,
 * formatted by {@link uk.gov.hmcts.reform.pcs.ccd.service.order.OrderDocumentService}.
 */
@Data
@Builder
public class OrderDocumentPayload implements FormPayload {

    private String caseNumber;
    private String courtName;
    private String courtAddress;
    private String orderTitle;
    private List<OrderParty> parties;
    private String propertyAddress;
    private String judgeName;
    private String orderDate;
    private String orderHtml;
    private String sealName;
    // The seal the template shows; Docmosis can't negate a condition, so each has its own.
    private boolean countyCourtSeal;
    private boolean highCourtSeal;

    /** A party named on the order, with their role, such as "Claimant" or "Defendant 2". */
    public record OrderParty(String name, String role) {
    }
}
