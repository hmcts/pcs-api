package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentArrearsTabDetails {

    @CCD(label = "Rent amount")
    private String rentAmount;

    @CCD(label = "How rent is calculated")
    private String calculationFrequency;

    @CCD(label = "Frequency")
    private String frequency;

    @CCD(label = "Daily rate")
    private String dailyRate;

    @CCD(label = "Previous steps taken to recover rent arrears?")
    private String stepsToRecoverArrears;

    @CCD(label = "Details of previous steps taken")
    private String stepsToRecoverArrearsDetails;

    @CCD(label = "Rent statement")
    private List<ListValue<Document>> rentStatement;

    @CCD(label = "Rent statement")
    private String rentStatementPlaceholder;

    @CCD(label = "Rent arrears total at the time of claim issue")
    private String arrearsTotal;

    @CCD(label = "Judgment requested for the outstanding arrears?")
    private String judgmentRequested;
}
