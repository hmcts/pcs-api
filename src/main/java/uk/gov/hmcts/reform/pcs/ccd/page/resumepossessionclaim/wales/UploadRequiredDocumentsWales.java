package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments.NO_ELECTRICAL_INSTALLATION_CONDITION_REPORT_REASON_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments.NO_ENERGY_PERFORMANCE_CERTIFICATE_REASON_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments.NO_GAS_SAFETY_REPORT_REASON_LABEL;

@AllArgsConstructor
@Component
public class UploadRequiredDocumentsWales implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadRequiredDocumentsWales", this::midEvent)
            .showCondition("legislativeCountry=\"Wales\"")
            .pageLabel("Upload Required documents")
            .label("uploadRequiredDocuments-infomation", """
                <p class="govuk-body">You must upload copies of the following documents:</p>
                <ul class="govuk-list govuk-list--bullet">
                    <li class="govuk-!-font-size-19">energy performance certificate</li>
                    <li class="govuk-!-font-size-19">current gas safety report</li>
                    <li class="govuk-!-font-size-19">current Electrical Installation Condition Report (EICR)</li>
                </ul>
                """)
            .complex(PCSCase::getRequiredDocumentsWales)
            .mandatory(WalesDocuments::getHasEnergyPerformanceCertificate)
            .mandatory(
                WalesDocuments::getEnergyPerformance,
                "walesDocs_HasEnergyPerformanceCertificate=\"YES\""
            )
            .mandatory(
                WalesDocuments::getNoEnergyPerformanceCertificateReason,
                "walesDocs_HasEnergyPerformanceCertificate=\"NO\""
            )
            .mandatory(WalesDocuments::getHasGasSafetyReport)
            .mandatory(
                WalesDocuments::getGasSafetyReport,
                "walesDocs_HasGasSafetyReport=\"YES\""
            )
            .mandatory(
                WalesDocuments::getNoGasSafetyReportReason,
                "walesDocs_HasGasSafetyReport=\"NO\""
            )
            .mandatory(WalesDocuments::getHasElectricalInstallationConditionReport)
            .mandatory(
                WalesDocuments::getElectricalInstallation,
                "walesDocs_HasElectricalInstallationConditionReport=\"YES\""
            )
            .mandatory(
                WalesDocuments::getNoElectricalInstallationConditionReportReason,
                "walesDocs_HasElectricalInstallationConditionReport=\"NO\""
            );
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        List<String> validationErrors = textAreaValidationService.validateMultipleTextAreas(
            TextAreaValidationService.FieldValidation.of(
                caseData.getRequiredDocumentsWales().getNoEnergyPerformanceCertificateReason(),
                NO_ENERGY_PERFORMANCE_CERTIFICATE_REASON_LABEL,
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                caseData.getRequiredDocumentsWales().getNoGasSafetyReportReason(),
                NO_GAS_SAFETY_REPORT_REASON_LABEL,
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                caseData.getRequiredDocumentsWales().getNoElectricalInstallationConditionReportReason(),
                NO_ELECTRICAL_INSTALLATION_CONDITION_REPORT_REASON_LABEL,
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        );

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
