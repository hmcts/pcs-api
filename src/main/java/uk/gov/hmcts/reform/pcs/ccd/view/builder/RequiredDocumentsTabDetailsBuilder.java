package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.RequiredDocumentsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;

@Component
public class RequiredDocumentsTabDetailsBuilder {

    private static final String NO_ANSWER = " ";

    public RequiredDocumentsTabDetails buildRequiredDocumentsTabDetails(PCSCase pcsCase, boolean isSubmitted) {
        WalesDocuments walesDocuments = pcsCase.getRequiredDocumentsWales();
        if (pcsCase.getLegislativeCountry() != LegislativeCountry.WALES || walesDocuments == null) {
            return null;
        }

        VerticalYesNo hasEnergyPerformanceCertificate = walesDocuments.getHasEnergyPerformanceCertificate();
        VerticalYesNo hasGasSafetyReport = walesDocuments.getHasGasSafetyReport();
        VerticalYesNo hasElectricalInstallationConditionReport
            = walesDocuments.getHasElectricalInstallationConditionReport();

        RequiredDocumentsTabDetails requiredDocumentsTabDetails = RequiredDocumentsTabDetails.builder()
            .hasEnergyPerformanceCertificate(
                hasEnergyPerformanceCertificate != null ? hasEnergyPerformanceCertificate.getLabel() : NO_ANSWER
            )
            .hasGasSafetyReport(hasGasSafetyReport != null ? hasGasSafetyReport.getLabel() : NO_ANSWER)
            .hasElectricalInstallationConditionReport(
                hasElectricalInstallationConditionReport != null
                    ? hasElectricalInstallationConditionReport.getLabel() : NO_ANSWER
            )
            .build();

        if (hasEnergyPerformanceCertificate == VerticalYesNo.NO) {
            requiredDocumentsTabDetails.setNoEnergyPerformanceCertificateReason(
                walesDocuments.getNoEpcReason()
            );
        } else {
            List<ListValue<Document>> energyPerformance = walesDocuments.getEnergyPerformance();
            if (isSubmitted) {
                walesDocuments.setEnergyPerformance(null);
            }
            requiredDocumentsTabDetails.setEnergyPerformanceCertificates(energyPerformance);
        }

        if (hasGasSafetyReport == VerticalYesNo.NO) {
            requiredDocumentsTabDetails.setNoGasSafetyReportReason(
                walesDocuments.getNoGasReportReason()
            );
        } else {
            List<ListValue<Document>> gasSafetyReport = walesDocuments.getGasSafetyReport();
            if (isSubmitted) {
                walesDocuments.setGasSafetyReport(null);
            }
            requiredDocumentsTabDetails.setGasSafetyReports(gasSafetyReport);
        }

        if (hasElectricalInstallationConditionReport == VerticalYesNo.NO) {
            requiredDocumentsTabDetails.setNoElectricalInstallationConditionReportReason(
                walesDocuments.getNoEicrReason()
            );
        } else {
            List<ListValue<Document>> electricalInstallation = walesDocuments.getElectricalInstallation();
            if (isSubmitted) {
                walesDocuments.setElectricalInstallation(null);
            }
            requiredDocumentsTabDetails.setElectricalInstallationReports(electricalInstallation);
        }

        return requiredDocumentsTabDetails;
    }
}
