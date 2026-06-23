package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.RequiredDocumentsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

@Component
public class RequiredDocumentsTabDetailsBuilder {

    private static final String NO_ANSWER = " ";

    public RequiredDocumentsTabDetails buildRequiredDocumentsTabDetails(PCSCase pcsCase) {
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
            requiredDocumentsTabDetails.setEnergyPerformanceCertificates(walesDocuments.getEnergyPerformance());
        }

        if (hasGasSafetyReport == VerticalYesNo.NO) {
            requiredDocumentsTabDetails.setNoGasSafetyReportReason(
                walesDocuments.getNoGasReportReason()
            );
        } else {
            requiredDocumentsTabDetails.setGasSafetyReports(walesDocuments.getGasSafetyReport());
        }

        if (hasElectricalInstallationConditionReport == VerticalYesNo.NO) {
            requiredDocumentsTabDetails.setNoElectricalInstallationConditionReportReason(
                walesDocuments.getNoEicrReason()
            );
        } else {
            requiredDocumentsTabDetails.setElectricalInstallationReports(
                walesDocuments.getElectricalInstallation()
            );
        }

        return requiredDocumentsTabDetails;
    }
}
