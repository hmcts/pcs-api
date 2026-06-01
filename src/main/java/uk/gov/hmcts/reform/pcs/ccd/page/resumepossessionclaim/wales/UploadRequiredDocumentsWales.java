package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.WALES;

@Component
public class UploadRequiredDocumentsWales implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadRequiredDocumentsWales")
            .showCondition(WALES)
            .pageLabel("Upload required documents")
            .label("uploadRequiredDocuments-information", """
                ---
                <p class="govuk-body">You must upload copies of the following documents:</p>
                <ul class="govuk-list govuk-list--bullet">
                    <li class="govuk-!-font-size-19">energy performance certificate</li>
                    <li class="govuk-!-font-size-19">current gas safety report</li>
                    <li class="govuk-!-font-size-19">current Electrical Installation Condition Report (EICR)</li>
                </ul>
                <p class="govuk-body">If you cannot upload a copy of a document, you must explain why.</p>
                """)
            .complex(PCSCase::getRequiredDocumentsWales)
            .mandatory(WalesDocuments::getHasEnergyPerformanceCertificate)
            .mandatory(
                WalesDocuments::getEnergyPerformance,
                "walesDocs_HasEnergyPerformanceCertificate=\"YES\""
            )
            .mandatory(
                WalesDocuments::getNoEpcReason,
                "walesDocs_HasEnergyPerformanceCertificate=\"NO\""
            )
            .mandatory(WalesDocuments::getHasGasSafetyReport)
            .mandatory(
                WalesDocuments::getGasSafetyReport,
                "walesDocs_HasGasSafetyReport=\"YES\""
            )
            .mandatory(
                WalesDocuments::getNoGasReportReason,
                "walesDocs_HasGasSafetyReport=\"NO\""
            )
            .mandatory(WalesDocuments::getHasElectricalInstallationConditionReport)
            .mandatory(
                WalesDocuments::getElectricalInstallation,
                "walesDocs_HasElectricalInstallationConditionReport=\"YES\""
            )
            .mandatory(
                WalesDocuments::getNoEicrReason,
                "walesDocs_HasElectricalInstallationConditionReport=\"NO\""
            )
            .done()
            .label("uploadRequiredDocumentsWales-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
