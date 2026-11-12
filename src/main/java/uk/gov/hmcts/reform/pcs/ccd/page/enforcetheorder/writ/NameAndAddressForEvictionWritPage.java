package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

@Component
@AllArgsConstructor
public class NameAndAddressForEvictionWritPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("nameAndAddressForEvictionWrit", this::midEvent)
            .pageLabel("The name and address for the eviction")
            .showCondition(ShowConditionsEnforcementType.WRIT_FLOW)
            .complex(PCSCase::getEnforcementOrder)
            .label("nameAndAddressForEvictionWrit-caption-hr", "<hr>")
            .label("nameAndAddressForEvictionWrit-caption",
                   "<h3 class=\"govuk-heading-s\">Check the name and address for the eviction</h3>")
            .label("nameAndAddressForEvictionWrit-defendants-heading",
                   "<p class=\"govuk-body govuk-!-font-weight-bold\">Defendants</p>")
            .label("nameAndAddressForEvictionWrit-defendants-value", "${formattedDefendantNames}")
            .label("nameAndAddressForEvictionWrit-defendants-unknown",
                   "<div class=\"govuk-!-margin-bottom-2\">Person(s) unknown</div>",
                   "hasUnknownDefendants=\"Yes\"")
            .label("nameAndAddressForEvictionWrit-defendants-hr", "<hr>")
            .label("nameAndAddressForEvictionWrit-address-heading",
                   "<p class=\"govuk-body govuk-!-font-weight-bold\">Address</p>")
            .label("nameAndAddressForEvictionWrit-address-value", "${formattedPropertyAddress}")
            .label("nameAndAddressForEvictionWrit-address-hr", "<hr>")
            .complex(EnforcementOrder::getWritDetails)
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
            writDetails.setShowChangeNameAddressPage(YesOrNo.YES);
        } else if (correctNameAndAddress == VerticalYesNo.YES) {
            // Navigate to PeopleWhoWillBeEvictedPage
            writDetails.setShowChangeNameAddressPage(YesOrNo.NO);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }
}

