package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public class EnterPropertyAddress implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("enterPropertyAddress", this::midEvent)
            .pageLabel("What is the address of the property you're claiming possession of?")
            .label("enterPropertyAddress-lineSeparator", "---")
            .mandatory(PCSCase::getPropertyAddress);
    }

    /*
    TODO: This MidEvent callback should be removed/refactored once we have integrated with MY HMCTS (Manage Org) as
     its formatting the property address to use as a placeholder for the registered contact address.
    */

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        AddressUK propertyAddress = caseData.getPropertyAddress();
        caseData.setClaimantContactAddress(propertyAddress);

        String formattedAddress = String.format(
            "%s<br>%s<br>%s",
            propertyAddress.getAddressLine1(),
            propertyAddress.getPostTown(),
            propertyAddress.getPostCode()
        );
        caseData.setFormattedClaimantContactAddress(formattedAddress);
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
