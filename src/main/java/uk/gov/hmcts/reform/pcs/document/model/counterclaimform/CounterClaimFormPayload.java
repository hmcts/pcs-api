package uk.gov.hmcts.reform.pcs.document.model.counterclaimform;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

import java.time.LocalDate;

@Data
@Builder
public class CounterClaimFormPayload implements FormPayload {
    private String referenceNumber;
    private String caseName;
    private LocalDate issueDateSealed;
    private LocalDate submittedOn;
    private String claimingFor;
    private String claimingSpecificSum;
    private String claimAmount;
    private String maximumClaimValue;
    private String needsHelpWithFees;
    private String hwfReferenceNumber;
    private String respondentNames;
    private String counterClaimFor;
    private String counterClaimReasons;
    private String otherOrderRequestDetails;
    private String otherOrderRequestFacts;
    private String statementOfTruthName;

    private Boolean showCounterClaimDetailsSection;
    private Boolean showClaimingFor;
    private Boolean showClaimingSpecificSum;
    private Boolean showClaimAmount;
    private Boolean showMaximumClaimValue;
    private Boolean showNeedsHelpWithFees;
    private Boolean showHwfRef;
    private Boolean showRespondentNames;
    private Boolean showAboutCounterClaimSection;
    private Boolean showCounterClaimFor;
    private Boolean showCounterClaimReasons;
    private Boolean showOtherOrderSection;
    private Boolean showStatementOfTruthName;
}
