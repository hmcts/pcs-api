package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details;

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
public class RequiredDocumentsTabDetails {

    @CCD(label = "Can the claimant upload a copy of the energy performance certificate?")
    private String hasEnergyPerformanceCertificate;

    @CCD(label = "Why can the claimant not upload a copy of the energy performance certificate?")
    private String noEnergyPerformanceCertificateReason;

    @CCD(label = "Energy performance certificate")
    private List<ListValue<Document>> energyPerformance;

    @CCD(label = "Can the claimant upload a copy of the current gas safety report?")
    private String hasGasSafetyReport;

    @CCD(label = "Why can the claimant not upload a copy of the current gas safety report?")
    private String noGasSafetyReportReason;

    @CCD(label = "Gas safety report")
    private List<ListValue<Document>> gasSafetyReport;

    @CCD(label = "Can the claimant upload a copy of the Electrical Installation Condition Report (EICR)?")
    private String hasElectricalInstallationConditionReport;

    @CCD(label = "Why can the claimant not upload a copy of the Electrical Installation Condition Report (EICR)?")
    private String noElectricalInstallationConditionReportReason;

    @CCD(label = "Electrical Installation Condition Report (EICR)")
    private List<ListValue<Document>> electricalInstallation;
}
