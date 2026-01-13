package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class NameAndAddressForEvictionWritPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("nameAndAddressForEvictionWrit", this::midEvent)
            .pageLabel("The name and address for the eviction")
            .showCondition("selectEnforcementType=\"WRIT\"")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .readonly(WritDetails::getFormattedDefendantNames, NEVER_SHOW)
            .readonly(WritDetails::getFormattedPropertyAddress, NEVER_SHOW)
            .label(
                "nameAndAddressForEvictionWrit-defendants-check",
                """
                    <hr />
                    <table class="govuk-table">
                      <caption class="govuk-table__caption govuk-table__caption--s govuk-!-padding-bottom-2">
                      Check the name and address for the eviction</caption>
                      <tbody class="govuk-table__body">
                        <tr class="govuk-table__row">
                          <th scope="row" class="govuk-table__header">Defendants</th>
                          <td class="govuk-table__cell">${formattedDefendantNames}</td>
                        </tr>
                        <tr class="govuk-table__row">
                          <th scope="row" class="govuk-table__header">Address</th>
                          <td class="govuk-table__cell">${formattedPropertyAddress}</td>
                        </tr>
                      </tbody>
                    </table>
                """
            )
            .complex(WritDetails::getNameAndAddressForEviction)
            .mandatory(NameAndAddressForEviction::getCorrectNameAndAddress)
            .done()
            .done()
            .done()
            .label("nameAndAddressForEvictionWrit-save-and-return", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
            CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> before) {

        PCSCase caseData = details.getData();

        // Set navigation flags based on user selection
        NameAndAddressForEviction nameAndAddress =
                caseData.getEnforcementOrder().getWritDetails().getNameAndAddressForEviction();

        VerticalYesNo correctNameAndAddress = nameAndAddress.getCorrectNameAndAddress();

        WritDetails writDetails = caseData.getEnforcementOrder().getWritDetails();

        if (correctNameAndAddress == VerticalYesNo.NO) {
            // Navigate to ChangeNameAddressPage
            writDetails.setShowChangeNameAddressPage(VerticalYesNo.YES);
            writDetails.setShowPeopleWhoWillBeEvictedPage(VerticalYesNo.NO);
        } else if (correctNameAndAddress == VerticalYesNo.YES) {
            // Navigate to PeopleWhoWillBeEvictedPage
            writDetails.setShowChangeNameAddressPage(VerticalYesNo.NO);
            writDetails.setShowPeopleWhoWillBeEvictedPage(VerticalYesNo.YES);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }
}

