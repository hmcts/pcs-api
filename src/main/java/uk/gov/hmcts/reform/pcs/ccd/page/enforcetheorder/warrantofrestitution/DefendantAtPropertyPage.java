package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

public class DefendantAtPropertyPage implements CcdPageConfiguration {

    public static final String DEFENDANT_AT_PROPERTY_INFORMATION = """
                    <div>
                        <p class="govuk-body govuk-!-font-weight-bold">Upload any evidence you have to show that the
                        defendants are currently at the property
                        </p>
                        <p class="govuk-body govuk-!-margin-bottom-1">For example, you can upload:</p>
                         <ul class="govuk-list govuk-list--bullet">
                           <li class="govuk-!-font-size-19">a police report about the defendants returning to the
                           property
                           </li>
                           <li class="govuk-!-font-size-19 govuk-!-padding-bottom-1">a witness statement, for example
                           from a neighbour who knows they are at the property
                           </li>
                           <li class="govuk-!-font-size-19 govuk-!-padding-bottom-1">photographs of damage to the
                           property caused by the defendants when they returned, for example a broken window or door
                           </li>
                         </ul>
                         <p class="govuk-body govuk-!-margin-bottom-1">You can also upload:</p>
                         <ul class="govuk-list govuk-list--bullet">
                           <li class="govuk-!-font-size-19">any contact you have had with the bailiffs, for example
                           emails about the defendants returning to the property
                           </li>
                           <li class="govuk-!-font-size-19 govuk-!-padding-bottom-1">evidence of lost income, for
                           example if you had a new tenant ready to move in after the eviction
                           </li>
                         </ul>
                         <p class="govuk-body">You must select the type of document you’re
                          uploading and give it a short description.
                         </p>
                         <p class="govuk-body govuk-!-font-weight-bold">Before you upload your documents</p>
                         <p class="govuk-body">Give your document a name that explains what it
                          is.</p>
                    </div>
                    """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantAtProperty")
            .pageLabel("Evidence that the defendants are at the property")
            .showCondition(ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW)
            .label("defendantAtProperty-line-separator", "---")
            .label("defendantAtProperty-notice", DEFENDANT_AT_PROPERTY_INFORMATION)
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantOfRestitutionDetails)
            .mandatory(WarrantOfRestitutionDetails::getAdditionalDocuments)
            .done()
            .done()
            .label("defendantAtProperty-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
