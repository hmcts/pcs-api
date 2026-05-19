package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Slf4j
@AllArgsConstructor
public class AppliedForHelpWithFees implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("appliedForHelpWithFees")
            .pageLabel("Confirm if they have they already applied for help with their application fee")
            .showCondition(ShowConditions.and(
                fieldEquals("xui_genapp_ShowHwfScreens", VerticalYesNo.YES),
                fieldEquals("xui_genapp_NeedHwf", VerticalYesNo.YES)
            ))
            .label("appliedForHelpWithFees-lineSeparator", "---")
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getAppliedForHwf)
            .mandatory(XuiGenAppRequest::getHwfReference, fieldEquals("xui_genapp_AppliedForHwf", VerticalYesNo.YES))
            .done();
    }

    /*

    MyHMCTS
Manage cases
Sign out
Case list
Create case
Find case
PROTOTYPE
This is a new service – your feedback will help us to improve it.
Make an application
Confirm if they have they already applied for help with their
application fee
Case number: 1234-5678-9101-1121
15 Garden Drive, Luton, Bedfordshire, LU1 1AB
Warrant number: 123456789
Lorem ipsum dolor sit amet, consectetur adipiscing elit. Ut et massa mi. Aliquam in hendrerit urna.
Have they already applied for help with their application fee?
Label
This will affect the fee you will invoice to the defendant. They will not need to pay a fee if their court hearing is (at least) 14 days away.
Yes
No
Previous
Continue
Cancel
All content is available under the Open Government Licence v3.0, except where otherwise stated
© Crown copyright

     */

}
