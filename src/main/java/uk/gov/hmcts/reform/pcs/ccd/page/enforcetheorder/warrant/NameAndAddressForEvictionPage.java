package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementApplicationPage.*;

public class NameAndAddressForEvictionPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        var nameAddressPageBuilder = pageBuilder
            .page("nameAndAddressForEviction", this::midEvent)
            .pageLabel("The name and address for the eviction")
            .showCondition(ShowConditionsEnforcementType.WARRANT_FLOW)
            .complex(PCSCase::getEnforcementOrder)
            .label("nameAndAddressForEviction-caption-hr", "<hr>")
            .label("nameAndAddressForEviction-caption",
                   "<h3 class=\"govuk-heading-s\">Check the name and address for the eviction</h3>")
            .label("nameAndAddressForEviction-defendants-heading",
                   "<p class=\"govuk-body govuk-!-font-weight-bold\">Defendants</p>")
            .label("nameAndAddressForEviction-defendants-value", "${formattedDefendantNames}");

        for (int i = 0; i < MAX_UNKNOWN_DEFENDANTS; i++) {
            nameAddressPageBuilder.label("nameAndAddressForEviction-defendant-unknown-" + i,
                                   PERSON_UNKNOWN_LABEL, unknownDefendantShowCondition(i));
        }

        nameAddressPageBuilder
            .label("nameAndAddressForEviction-defendants-hr", "<hr>")
            .label("nameAndAddressForEviction-address-heading",
                   "<p class=\"govuk-body govuk-!-font-weight-bold\">Address</p>")
            .label("nameAndAddressForEviction-address-value", "${formattedPropertyAddress}")
            .label("nameAndAddressForEviction-address-hr", "<hr>")
            .complex(EnforcementOrder::getWarrantDetails)
            .complex(WarrantDetails::getNameAndAddressForEviction)
            .mandatory(NameAndAddressForEviction::getCorrectNameAndAddress)
            .done()
            .done()
            .done()
            .label("nameAndAddressForEviction-save-and-return", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details,
        CaseDetails<PCSCase, State> before) {

        PCSCase caseData = details.getData();

        // Set navigation flags based on user selection
        NameAndAddressForEviction nameAndAddress =
            caseData.getEnforcementOrder().getWarrantDetails().getNameAndAddressForEviction();

        VerticalYesNo correctNameAndAddress = nameAndAddress.getCorrectNameAndAddress();

        WarrantDetails warrantDetails = caseData.getEnforcementOrder().getWarrantDetails();

        if (correctNameAndAddress == VerticalYesNo.NO) {
            // Navigate to ChangeNameAddressPage
            warrantDetails.setShowChangeNameAddressPage(YesOrNo.YES);
            warrantDetails.setShowPeopleWhoWillBeEvictedPage(YesOrNo.NO);
        } else if (correctNameAndAddress == VerticalYesNo.YES) {
            // Navigate to PeopleWhoWillBeEvictedPage
            warrantDetails.setShowChangeNameAddressPage(YesOrNo.NO);
            warrantDetails.setShowPeopleWhoWillBeEvictedPage(YesOrNo.YES);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
