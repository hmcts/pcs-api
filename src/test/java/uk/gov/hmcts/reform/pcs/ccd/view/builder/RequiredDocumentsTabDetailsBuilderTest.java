package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.RequiredDocumentsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequiredDocumentsTabDetailsBuilderTest {

    private RequiredDocumentsTabDetailsBuilder requiredDocumentsTabDetailsBuilder;

    @BeforeEach
    void setUp() {
        requiredDocumentsTabDetailsBuilder = new RequiredDocumentsTabDetailsBuilder();
    }

    @Test
    void shouldNotBuildRequiredDocumentsDetailsForEngland() {
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .requiredDocumentsWales(WalesDocuments.builder().build())
            .build();

        RequiredDocumentsTabDetails requiredDocumentsTabDetails =
            requiredDocumentsTabDetailsBuilder.buildRequiredDocumentsTabDetails(pcsCase, false);

        assertThat(requiredDocumentsTabDetails).isNull();
    }

    @Test
    void shouldNotBuildRequiredDocumentsDetailsWithoutWalesDocuments() {
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .build();

        RequiredDocumentsTabDetails requiredDocumentsTabDetails =
            requiredDocumentsTabDetailsBuilder.buildRequiredDocumentsTabDetails(pcsCase, false);

        assertThat(requiredDocumentsTabDetails).isNull();
    }

    @Test
    void shouldBuildRequiredDocumentsDetailsWithReasons() {
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .requiredDocumentsWales(
                WalesDocuments.builder()
                    .hasEnergyPerformanceCertificate(VerticalYesNo.NO)
                    .hasGasSafetyReport(VerticalYesNo.NO)
                    .hasElectricalInstallationConditionReport(VerticalYesNo.NO)
                    .noEpcReason("No EPC")
                    .noGasReportReason("No gas report")
                    .noEicrReason("No EICR")
                    .build()
            )
            .build();

        RequiredDocumentsTabDetails requiredDocumentsTabDetails =
            requiredDocumentsTabDetailsBuilder.buildRequiredDocumentsTabDetails(pcsCase, false);

        assertThat(requiredDocumentsTabDetails.getHasEnergyPerformanceCertificate()).isEqualTo("No");
        assertThat(requiredDocumentsTabDetails.getHasGasSafetyReport()).isEqualTo("No");
        assertThat(requiredDocumentsTabDetails.getHasElectricalInstallationConditionReport()).isEqualTo("No");
        assertThat(requiredDocumentsTabDetails.getNoEnergyPerformanceCertificateReason()).isEqualTo("No EPC");
        assertThat(requiredDocumentsTabDetails.getNoGasSafetyReportReason()).isEqualTo("No gas report");
        assertThat(requiredDocumentsTabDetails.getNoElectricalInstallationConditionReportReason()).isEqualTo(
            "No EICR"
        );
        assertThat(requiredDocumentsTabDetails.getEnergyPerformanceCertificates()).isNull();
        assertThat(requiredDocumentsTabDetails.getGasSafetyReports()).isNull();
        assertThat(requiredDocumentsTabDetails.getElectricalInstallationReports()).isNull();
    }

    @Test
    void shouldBuildRequiredDocumentsDetailsWithDocuments() {
        List<ListValue<Document>> documents = List.of(ListValue.<Document>builder().value(Document.builder().build())
                                                           .build());
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .requiredDocumentsWales(
                WalesDocuments.builder()
                    .hasEnergyPerformanceCertificate(VerticalYesNo.YES)
                    .hasGasSafetyReport(VerticalYesNo.YES)
                    .hasElectricalInstallationConditionReport(VerticalYesNo.YES)
                    .energyPerformance(documents)
                    .gasSafetyReport(documents)
                    .electricalInstallation(documents)
                    .build()
            )
            .build();

        RequiredDocumentsTabDetails requiredDocumentsTabDetails =
            requiredDocumentsTabDetailsBuilder.buildRequiredDocumentsTabDetails(pcsCase, false);

        assertThat(requiredDocumentsTabDetails.getHasEnergyPerformanceCertificate()).isEqualTo("Yes");
        assertThat(requiredDocumentsTabDetails.getHasGasSafetyReport()).isEqualTo("Yes");
        assertThat(requiredDocumentsTabDetails.getHasElectricalInstallationConditionReport()).isEqualTo("Yes");
        assertThat(requiredDocumentsTabDetails.getNoEnergyPerformanceCertificateReason()).isNull();
        assertThat(requiredDocumentsTabDetails.getNoGasSafetyReportReason()).isNull();
        assertThat(requiredDocumentsTabDetails.getNoElectricalInstallationConditionReportReason()).isNull();
        assertThat(requiredDocumentsTabDetails.getEnergyPerformanceCertificates()).isEqualTo(documents);
        assertThat(requiredDocumentsTabDetails.getGasSafetyReports()).isEqualTo(documents);
        assertThat(requiredDocumentsTabDetails.getElectricalInstallationReports()).isEqualTo(documents);
    }

    @Test
    void shouldUseNoAnswerWhenRequiredDocumentAnswersAreNotProvided() {
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .requiredDocumentsWales(WalesDocuments.builder().build())
            .build();

        RequiredDocumentsTabDetails requiredDocumentsTabDetails =
            requiredDocumentsTabDetailsBuilder.buildRequiredDocumentsTabDetails(pcsCase, false);

        assertThat(requiredDocumentsTabDetails.getHasEnergyPerformanceCertificate()).isEqualTo(" ");
        assertThat(requiredDocumentsTabDetails.getHasGasSafetyReport()).isEqualTo(" ");
        assertThat(requiredDocumentsTabDetails.getHasElectricalInstallationConditionReport()).isEqualTo(" ");
        assertThat(requiredDocumentsTabDetails.getNoEnergyPerformanceCertificateReason()).isNull();
        assertThat(requiredDocumentsTabDetails.getNoGasSafetyReportReason()).isNull();
        assertThat(requiredDocumentsTabDetails.getNoElectricalInstallationConditionReportReason()).isNull();
        assertThat(requiredDocumentsTabDetails.getEnergyPerformanceCertificates()).isNull();
        assertThat(requiredDocumentsTabDetails.getGasSafetyReports()).isNull();
        assertThat(requiredDocumentsTabDetails.getElectricalInstallationReports()).isNull();
    }

    @Test
    void shouldBuildRequiredDocumentsDetailsWithMixedAnswers() {
        List<ListValue<Document>> documents = List.of(ListValue.<Document>builder().value(Document.builder().build())
                                                           .build());
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .requiredDocumentsWales(
                WalesDocuments.builder()
                    .hasEnergyPerformanceCertificate(VerticalYesNo.NO)
                    .hasGasSafetyReport(VerticalYesNo.YES)
                    .hasElectricalInstallationConditionReport(VerticalYesNo.NO)
                    .noEpcReason("No EPC")
                    .gasSafetyReport(documents)
                    .noEicrReason("No EICR")
                    .build()
            )
            .build();

        RequiredDocumentsTabDetails requiredDocumentsTabDetails =
            requiredDocumentsTabDetailsBuilder.buildRequiredDocumentsTabDetails(pcsCase, false);

        assertThat(requiredDocumentsTabDetails.getNoEnergyPerformanceCertificateReason()).isEqualTo("No EPC");
        assertThat(requiredDocumentsTabDetails.getEnergyPerformanceCertificates()).isNull();
        assertThat(requiredDocumentsTabDetails.getNoGasSafetyReportReason()).isNull();
        assertThat(requiredDocumentsTabDetails.getGasSafetyReports()).isEqualTo(documents);
        assertThat(requiredDocumentsTabDetails.getNoElectricalInstallationConditionReportReason()).isEqualTo(
            "No EICR"
        );
        assertThat(requiredDocumentsTabDetails.getElectricalInstallationReports()).isNull();
    }

    @Test
    void shouldUnsetRequiredDocumentsIfCaseIsSubmitted() {
        List<ListValue<Document>> energyPerformance = List.of(ListValue.<Document>builder().value(Document.builder().build())
                                                          .build());
        List<ListValue<Document>> gasSafetyReport = List.of(ListValue.<Document>builder().value(Document.builder().build())
                                                                             .build());
        List<ListValue<Document>> electricalInstallation = List.of(ListValue.<Document>builder().value(Document.builder().build())
                                                                .build());

        WalesDocuments walesDocuments = WalesDocuments.builder()
            .hasEnergyPerformanceCertificate(VerticalYesNo.YES)
            .hasGasSafetyReport(VerticalYesNo.YES)
            .hasElectricalInstallationConditionReport(VerticalYesNo.YES)
            .energyPerformance(energyPerformance)
            .gasSafetyReport(gasSafetyReport)
            .electricalInstallation(electricalInstallation)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .requiredDocumentsWales(walesDocuments)
            .build();

        RequiredDocumentsTabDetails requiredDocumentsTabDetails =
            requiredDocumentsTabDetailsBuilder.buildRequiredDocumentsTabDetails(pcsCase, true);

        assertThat(requiredDocumentsTabDetails.getEnergyPerformanceCertificates()).isEqualTo(energyPerformance);
        assertThat(walesDocuments.getEnergyPerformance()).isNull();
        assertThat(requiredDocumentsTabDetails.getGasSafetyReports()).isEqualTo(gasSafetyReport);
        assertThat(walesDocuments.getGasSafetyReport()).isNull();
        assertThat(requiredDocumentsTabDetails.getElectricalInstallationReports()).isEqualTo(electricalInstallation);
        assertThat(walesDocuments.getElectricalInstallation()).isNull();
    }

    @Test
    void shouldNotUnsetRequiredDocumentsIfCaseIsInDraft() {
        List<ListValue<Document>> energyPerformance = List.of(ListValue.<Document>builder().value(Document.builder().build())
                                                                  .build());
        List<ListValue<Document>> gasSafetyReport = List.of(ListValue.<Document>builder().value(Document.builder().build())
                                                                .build());
        List<ListValue<Document>> electricalInstallation =
            List.of(ListValue.<Document>builder().value(Document.builder().build()).build());

        WalesDocuments walesDocuments = WalesDocuments.builder()
            .hasEnergyPerformanceCertificate(VerticalYesNo.YES)
            .hasGasSafetyReport(VerticalYesNo.YES)
            .hasElectricalInstallationConditionReport(VerticalYesNo.YES)
            .energyPerformance(energyPerformance)
            .gasSafetyReport(gasSafetyReport)
            .electricalInstallation(electricalInstallation)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .requiredDocumentsWales(walesDocuments)
            .build();

        RequiredDocumentsTabDetails requiredDocumentsTabDetails =
            requiredDocumentsTabDetailsBuilder.buildRequiredDocumentsTabDetails(pcsCase, false);

        assertThat(requiredDocumentsTabDetails.getEnergyPerformanceCertificates()).isEqualTo(energyPerformance);
        assertThat(walesDocuments.getEnergyPerformance()).isEqualTo(energyPerformance);
        assertThat(requiredDocumentsTabDetails.getGasSafetyReports()).isEqualTo(gasSafetyReport);
        assertThat(walesDocuments.getGasSafetyReport()).isEqualTo(gasSafetyReport);
        assertThat(requiredDocumentsTabDetails.getElectricalInstallationReports()).isEqualTo(electricalInstallation);
        assertThat(walesDocuments.getElectricalInstallation()).isEqualTo(electricalInstallation);
    }
}
