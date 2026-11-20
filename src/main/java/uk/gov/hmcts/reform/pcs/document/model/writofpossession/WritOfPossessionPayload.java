package uk.gov.hmcts.reform.pcs.document.model.writofpossession;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

/**
 * Docmosis merge-field payload for the writ of possession application. Field names must match
 * {@code CV-PCS-APP-ENG-Writ-Of-Possession.docx}.
 */
@Data
@Builder
public class WritOfPossessionPayload implements FormPayload {

    //Title block
    private String referenceNumber;
    private String caseName;

    //Claimant details
    private String claimantName;
    private String claimantAddress;
    private String claimantTelephoneNumber;
    private String claimantEmailAddress;

    //Defendant details
    private String defendantNames;
    private String propertyAddress;

    //Application details
    private String breathingSpaceAnswer;
    private String highCourtPermissionAnswer;

    //High Court Enforcement Officer
    private String hceoHiredAnswer;
    private String hceoName;
    private String hceoSelectionOutcome;

    //Statement of truth
    private String statementOfTruthCompletedBy;
    private String signatoryFullName;
    private String signatoryPosition;
    private String signatoryOrganisation;
    private String submissionDate;

    //In the template but not captured in the enf journeys yet
    private String claimantReference;
    private String possessionOrderType;
    private String possessionOrderDate;
    private String highCourtPermissionOrderDate;
}
