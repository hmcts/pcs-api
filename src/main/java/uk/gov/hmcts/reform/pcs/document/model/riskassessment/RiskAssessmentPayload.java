package uk.gov.hmcts.reform.pcs.document.model.riskassessment;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

/**
 * Docmosis merge-field payload for the eviction risk assessment that accompanies a warrant of possession. Field names
 * must match {@code CV-PCS-APP-ENG-Risk-Assessment.docx}.
 */
@Data
@Builder
public class RiskAssessmentPayload implements FormPayload {

    //Title block
    private String referenceNumber;
    private String caseName;

    //Application and property details
    private String applicationType;
    private String claimantName;
    private String defendantNames;
    private String evictionAddress;

    //Defendants and occupants
    private String defendantDatesOfBirthKnown;
    private String defendantDatesOfBirth;
    private String propertyRiskResponse;
    private String selectedPropertyRisks;

    //Risk details
    private String violentAggressiveDetails;
    private String firearmHistoryDetails;
    private String criminalAntisocialDetails;
    private String threatDetails;
    private String evictionProtestGroupDetails;
    private String policeVisitDetails;
    private String animalDetails;

    //Vulnerable adults and children
    private String vulnerabilityResponse;
    private String vulnerablePeopleType;
    private String vulnerabilityDetails;

    //Access and other information
    private String accessDifficultyResponse;
    private String accessDifficultyDetails;
    private String otherEvictionInformationResponse;
    private String otherEvictionInformation;

    //In the template but not captured in the enf journeys yet
    private String claimantReference;
}
