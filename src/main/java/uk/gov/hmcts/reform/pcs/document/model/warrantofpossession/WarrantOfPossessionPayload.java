package uk.gov.hmcts.reform.pcs.document.model.warrantofpossession;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

import java.time.LocalDate;

/**
 * Docmosis merge-field payload for the warrant of possession application. Field names must match
 * {@code CV-PCS-APP-ENG-Warrant-Of-Possession.docx}.
 */
@Data
@Builder
public class WarrantOfPossessionPayload implements FormPayload {

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
    private String defendantNames;

    //Application details
    private String applicationType;
    private String possessionOrderType;
    private String possessionOrderDate;
    private String breathingSpaceAnswer;
    private String missedPaymentsAnswer;
    private String propertyAddress;

    //Statement of truth
    private String statementOfTruthCompletedBy;
    private String signatoryFullName;
    private String signatoryPosition;
    private String signatoryOrganisation;
    private LocalDate submissionDate;

}
