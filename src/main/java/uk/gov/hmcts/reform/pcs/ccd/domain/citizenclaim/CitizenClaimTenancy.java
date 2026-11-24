package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenClaimTenancy {

    private CitizenClaimTenancyType tenancyType;

    private String otherTenancyTypeDetails;

    // Asked instead of the tenancy type for a property in Wales; the start date and agreement fields are shared
    private OccupationLicenceTypeWales occupationContractType;

    private String otherOccupationContractDetails;

    private LocalDate tenancyStartDate;

    private VerticalYesNo tenancyAgreementAvailable;

    @CCD(typeOverride = TextArea)
    private String noTenancyAgreementReason;

    private List<ListValue<Document>> tenancyAgreementDocuments;

    // Wales only: the documents a landlord must have given the contract holder
    private VerticalYesNo hasEnergyPerformanceCertificate;

    private List<ListValue<Document>> energyPerformanceDocuments;

    @CCD(typeOverride = TextArea)
    private String noEnergyPerformanceCertificateReason;

    private VerticalYesNo hasGasSafetyReport;

    private List<ListValue<Document>> gasSafetyReportDocuments;

    @CCD(typeOverride = TextArea)
    private String noGasSafetyReportReason;

    private VerticalYesNo hasElectricalInstallationConditionReport;

    private List<ListValue<Document>> electricalInstallationDocuments;

    @CCD(typeOverride = TextArea)
    private String noElectricalInstallationConditionReason;

}
