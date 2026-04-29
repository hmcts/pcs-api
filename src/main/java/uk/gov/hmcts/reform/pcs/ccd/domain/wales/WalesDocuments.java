package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class WalesDocuments {

    public static final String NO_ENERGY_PERFORMANCE_CERTIFICATE_REASON_LABEL =
        "Why can you not upload a copy of the energy performance certificate?";
    public static final String NO_GAS_SAFETY_REPORT_REASON_LABEL =
        "Why can you not upload a copy of the gas safety report?";
    public static final String NO_ELECTRICAL_INSTALLATION_CONDITION_REPORT_REASON_LABEL =
        "Why can you not upload a copy of the current Electrical Installation Condition Report (EICR)";

    @CCD(
        label = "Can you upload a copy of the energy performance certificate?"
    )
    private VerticalYesNo hasEnergyPerformanceCertificate;

    @CCD(
        label = "Add document"
    )
    private List<ListValue<Document>> energyPerformance;

    @CCD(
        label = NO_ENERGY_PERFORMANCE_CERTIFICATE_REASON_LABEL,
        hint = "You can enter up to 500 characters",
        typeOverride = FieldType.TextArea
    )
    private String noEnergyPerformanceCertificateReason;

    @CCD(
        label = "Can you upload a copy of the current gas safety report"
    )
    private VerticalYesNo hasGasSafetyReport;

    @CCD(
        label = "Add document"
    )
    private List<ListValue<Document>> gasSafetyReport;

    @CCD(
        label = NO_GAS_SAFETY_REPORT_REASON_LABEL,
        hint = "You can enter up to 500 characters",
        typeOverride = FieldType.TextArea
    )
    private String noGasSafetyReportReason;

    @CCD(
        label = "Can you upload a copy of the current Electrical Installation Condition Report (EICR)"
    )
    private VerticalYesNo hasElectricalInstallationConditionReport;

    @CCD(
        label = "Add document"
    )
    private List<ListValue<Document>> electricalInstallation;

    @CCD(
        label = NO_ELECTRICAL_INSTALLATION_CONDITION_REPORT_REASON_LABEL,
        hint = "You can enter up to 500 characters",
        typeOverride = FieldType.TextArea
    )
    private String noElectricalInstallationConditionReportReason;

}
