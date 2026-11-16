package uk.gov.hmcts.reform.pcs.document.model.writofpossession;



import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.pcs.document.model.enforcement.EnforcementDefendant;

import java.time.LocalDate;
import java.util.List;

/**
 * Docmosis merge-field payload for the Writ of possession, one per recipient. Field names must match
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
    private String claimantReference;
    private String claimantEmailAddress;

    //Defendant details
    private List<EnforcementDefendant> defendants;
    private String propertyAddress;

    //Application details
    private String possessionOrderType;
    private String possessionOrderDate;
    private String breathingSpaceAnswer;
    private String highCourtPermissionAnswer;
    private LocalDate highCourtPermissionOrderDate;

    //High Court Enforcement Officer
    private String hceoHiredAnswer;
    private String hceoName;
    private String hceoSelectionOutcome;

    //Statement of truth
    private String statementOfTruthCompletedBy;
    private String signatoryFullName;
    private String signatoryPosition;
    private String signatoryOrganisation;
    private LocalDate submissionDate;

}

