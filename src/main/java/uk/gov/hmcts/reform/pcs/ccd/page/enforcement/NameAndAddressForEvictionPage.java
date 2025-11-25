package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.NameAndAddressForEviction;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class NameAndAddressForEvictionPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("nameAndAddressForEviction", this::midEvent)
            .pageLabel("The name and address for the eviction")
            .readonly(PCSCase::getFormattedDefendantNames, NEVER_SHOW)
            .readonly(PCSCase::getFormattedPropertyAddress, NEVER_SHOW)
            .label(
                "nameAndAddressForEviction-defendants-check",
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
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getNameAndAddressForEviction)
            .mandatory(NameAndAddressForEviction::getCorrectNameAndAddress)
            .label("nameAndAddressForEviction-save-and-return", SAVE_AND_RETURN);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details,
        CaseDetails<PCSCase, State> before) {

        PCSCase caseData = details.getData();

        // Set navigation flags based on user selection
        NameAndAddressForEviction nameAndAddress =
            caseData.getEnforcementOrder().getNameAndAddressForEviction();

        VerticalYesNo correctNameAndAddress = nameAndAddress.getCorrectNameAndAddress();

        EnforcementOrder enforcementOrder = caseData.getEnforcementOrder();

        if (correctNameAndAddress == VerticalYesNo.NO) {
            // Navigate to ChangeNameAddressPage
            enforcementOrder.setShowChangeNameAddressPage(VerticalYesNo.YES);
            enforcementOrder.setShowPeopleWhoWillBeEvictedPage(VerticalYesNo.NO);
        } else if (correctNameAndAddress == VerticalYesNo.YES) {
            // Navigate to PeopleWhoWillBeEvictedPage
            enforcementOrder.setShowChangeNameAddressPage(VerticalYesNo.NO);
            enforcementOrder.setShowPeopleWhoWillBeEvictedPage(VerticalYesNo.YES);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
